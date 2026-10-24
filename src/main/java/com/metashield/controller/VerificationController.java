package com.metashield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metashield.model.FileRecord;
import com.metashield.repository.FileRecordRepository;
import com.metashield.service.AuditLedgerService;
import com.metashield.service.EngineOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class VerificationController {

    private final FileRecordRepository fileRecordRepository;
    private final EngineOrchestrationService orchestrationService;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerificationController(FileRecordRepository fileRecordRepository,
                                  EngineOrchestrationService orchestrationService,
                                  AuditLedgerService auditLedgerService) {
        this.fileRecordRepository = fileRecordRepository;
        this.orchestrationService = orchestrationService;
        this.auditLedgerService = auditLedgerService;
    }

    @PostMapping("/{uuid}/rescan-verify")
    public ResponseEntity<Map<String, Object>> rescanAndVerify(@PathVariable String uuid) {
        Optional<FileRecord> fileOpt = fileRecordRepository.findByUuid(uuid);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = fileOpt.get();
        if (file.getCleanedPath() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "File has not been sanitized yet."));
        }

        Path cleanedPath = Paths.get(file.getCleanedPath());

        try {
            // Re-scan via persistent daemon
            JsonNode verifyResp = orchestrationService.verifyFile(cleanedPath);
            boolean verifiedClean = verifyResp.path("verified_clean").asBoolean(false);

            // Re-validate binary stream integrity with C++ guard
            JsonNode streamGuardResult = orchestrationService.validateStreamWithGuard(cleanedPath);

            file.setStatus("VERIFIED");
            fileRecordRepository.save(file);

            // Audit Log
            auditLedgerService.recordEvent(
                    "RE_SCAN_VERIFIED",
                    file.getFileUuid(),
                    file.getOriginalFilename(),
                    "VERIFICATION_GUARD",
                    file.getInitialSha256(),
                    file.getCleanedSha256(),
                    "{\"verified_clean\":" + verifiedClean + ",\"residual_count\":" + verifyResp.path("residual_count").asInt(0) + "}"
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("verified_clean", verifiedClean);
            resp.put("residual_count", verifyResp.path("residual_count").asInt(0));
            resp.put("post_risk_score", verifyResp.path("post_risk_score").asInt(0));
            resp.put("post_risk_level", verifyResp.path("post_risk_level").asText("LOW"));
            resp.put("stream_guard", streamGuardResult);
            resp.put("compliance", objectMapper.treeToValue(verifyResp.path("compliance"), Map.class));
            resp.put("initial_sha256", file.getInitialSha256());
            resp.put("cleaned_sha256", file.getCleanedSha256());
            resp.put("certificate_url", "/api/reports/" + file.getFileUuid() + "/print");

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
