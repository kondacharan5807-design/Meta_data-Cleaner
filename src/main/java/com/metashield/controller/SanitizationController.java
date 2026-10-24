package com.metashield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metashield.config.StorageProperties;
import com.metashield.model.FileRecord;
import com.metashield.model.SanitizationPolicy;
import com.metashield.repository.FileRecordRepository;
import com.metashield.repository.MetadataTagRepository;
import com.metashield.service.AuditLedgerService;
import com.metashield.service.EngineOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class SanitizationController {

    private final FileRecordRepository fileRecordRepository;
    private final MetadataTagRepository metadataTagRepository;
    private final EngineOrchestrationService orchestrationService;
    private final AuditLedgerService auditLedgerService;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SanitizationController(FileRecordRepository fileRecordRepository,
                                  MetadataTagRepository metadataTagRepository,
                                  EngineOrchestrationService orchestrationService,
                                  AuditLedgerService auditLedgerService,
                                  StorageProperties storageProperties) {
        this.fileRecordRepository = fileRecordRepository;
        this.metadataTagRepository = metadataTagRepository;
        this.orchestrationService = orchestrationService;
        this.auditLedgerService = auditLedgerService;
        this.storageProperties = storageProperties;
    }

    @PostMapping("/{uuid}/sanitize")
    public ResponseEntity<Map<String, Object>> sanitizeFile(@PathVariable String uuid,
                                                            @RequestBody(required = false) SanitizationPolicy policy) {
        Optional<FileRecord> fileOpt = fileRecordRepository.findByUuid(uuid);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = fileOpt.get();
        if (policy == null) {
            // Default to Balanced Legal Mode
            policy = new SanitizationPolicy();
            policy.setPreserveCopyright(true);
            policy.setStripGps(true);
            policy.setStripAuthor(true);
            policy.setStripDeviceInfo(true);
            policy.setStripTimestamps(true);
            policy.setStripRevisionHistory(true);
        }

        Path inputPath = Paths.get(file.getStoredPath());
        String cleanedFileName = "clean_" + inputPath.getFileName().toString();
        Path outputPath = storageProperties.getCleanedDir().resolve(cleanedFileName);

        try {
            // 1. Invoke Python sanitization core
            JsonNode engineResponse = orchestrationService.sanitizeFile(inputPath, outputPath, policy);
            if (!"ok".equals(engineResponse.path("status").asText())) {
                return ResponseEntity.internalServerError().body(Map.of("error", engineResponse.path("message").asText()));
            }

            // 2. Invoke C++ Anti-Corruption Binary Stream Guard
            JsonNode streamGuardResult = orchestrationService.validateStreamWithGuard(outputPath);

            // 3. Compute post-cleaning SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(outputPath)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            String postSha256 = sb.toString();

            // 4. Update file record
            file.setCleanedPath(outputPath.toAbsolutePath().normalize().toString());
            file.setCleanedSha256(postSha256);
            file.setStatus("SANITIZED");
            fileRecordRepository.save(file);

            // 5. Mark tags sanitized in SQLite
            metadataTagRepository.markAllSanitized(file.getId());

            // 6. Record in immutable audit ledger
            String eventType = policy.getPreserveCopyright() ? "SELECTIVE_WIPE" : "ZERO_TRACE_WIPE";
            String details = objectMapper.writeValueAsString(Map.of(
                    "policy_mode", policy.getPreserveCopyright() ? "BALANCED_LEGAL" : "ZERO_TRACE",
                    "stream_guard", streamGuardResult.path("integrity_status").asText("VALID_STREAM"),
                    "tags_removed", engineResponse.path("sanitization").path("tags_removed_count").asInt(0)
            ));

            auditLedgerService.recordEvent(
                    eventType,
                    file.getFileUuid(),
                    file.getOriginalFilename(),
                    "SANITIZATION_ENGINE",
                    file.getInitialSha256(),
                    postSha256,
                    details
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("file_uuid", file.getFileUuid());
            resp.put("original_filename", file.getOriginalFilename());
            resp.put("pre_sha256", file.getInitialSha256());
            resp.put("post_sha256", postSha256);
            resp.put("stream_guard", streamGuardResult);
            resp.put("sanitization_details", engineResponse.path("sanitization"));
            resp.put("mode", policy.getPreserveCopyright() ? "BALANCED_LEGAL" : "ZERO_TRACE");

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
