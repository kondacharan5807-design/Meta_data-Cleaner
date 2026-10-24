package com.metashield.controller;

import com.metashield.config.StorageProperties;
import com.metashield.model.FileRecord;
import com.metashield.repository.FileRecordRepository;
import com.metashield.service.AuditLedgerService;
import com.metashield.service.FilePurgeService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileRecordRepository fileRecordRepository;
    private final AuditLedgerService auditLedgerService;
    private final FilePurgeService filePurgeService;
    private final StorageProperties storageProperties;

    public FileUploadController(FileRecordRepository fileRecordRepository,
                                AuditLedgerService auditLedgerService,
                                FilePurgeService filePurgeService,
                                StorageProperties storageProperties) {
        this.fileRecordRepository = fileRecordRepository;
        this.auditLedgerService = auditLedgerService;
        this.filePurgeService = filePurgeService;
        this.storageProperties = storageProperties;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<FileRecord>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<FileRecord> records = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String uuid = UUID.randomUUID().toString();
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            // Sanitize filename
            String cleanName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedFileName = uuid + "_" + cleanName;
            Path targetPath = storageProperties.getUploadsDir().resolve(storedFileName);

            try {
                // Calculate SHA-256 and store file atomically
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (InputStream is = file.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        md.update(buffer, 0, read);
                    }
                }

                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                StringBuilder sb = new StringBuilder();
                for (byte b : md.digest()) {
                    sb.append(String.format("%02x", b));
                }
                String sha256 = sb.toString();

                Instant now = Instant.now();
                Instant expiresAt = now.plusSeconds(storageProperties.getTtlSeconds());

                FileRecord record = new FileRecord();
                record.setFileUuid(uuid);
                record.setOriginalFilename(originalName);
                record.setStoredPath(targetPath.toAbsolutePath().normalize().toString());
                record.setMimeType(file.getContentType());
                record.setFileSize(file.getSize());
                record.setInitialSha256(sha256);
                record.setStatus("STAGED");
                record.setUploadTimestamp(now);
                record.setExpiresAt(expiresAt);

                FileRecord saved = fileRecordRepository.save(record);
                records.add(saved);

                // Audit Log Record
                auditLedgerService.recordEvent(
                        "FILE_INGEST",
                        uuid,
                        originalName,
                        "INGEST_GATEWAY",
                        sha256,
                        null,
                        "{\"size\":" + file.getSize() + ",\"mime\":\"" + file.getContentType() + "\"}"
                );

            } catch (Exception e) {
                System.err.println("Error saving file " + originalName + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(records);
    }

    @GetMapping
    public ResponseEntity<List<FileRecord>> listFiles() {
        return ResponseEntity.ok(fileRecordRepository.findAll());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FileRecord> getFile(@PathVariable String uuid) {
        return fileRecordRepository.findByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{uuid}/download")
    public ResponseEntity<Resource> downloadCleanedFile(@PathVariable String uuid,
                                                        @RequestParam(value = "autoPurge", defaultValue = "false") boolean autoPurge) {
        Optional<FileRecord> opt = fileRecordRepository.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        FileRecord file = opt.get();
        String downloadPathStr = file.getCleanedPath() != null ? file.getCleanedPath() : file.getStoredPath();
        if (downloadPathStr == null) return ResponseEntity.notFound().build();

        try {
            Path path = Paths.get(downloadPathStr);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();

            Resource resource = new UrlResource(path.toUri());
            String downloadFilename = (file.getCleanedPath() != null ? "sanitized_" : "") + file.getOriginalFilename();

            ResponseEntity<Resource> response = ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .body(resource);

            // Audit download
            auditLedgerService.recordEvent(
                    "FILE_DOWNLOAD",
                    file.getFileUuid(),
                    file.getOriginalFilename(),
                    "CLIENT_DOWNLOAD",
                    file.getInitialSha256(),
                    file.getCleanedSha256(),
                    "{\"autoPurge\":" + autoPurge + "}"
            );

            // Trigger immediate GDPR purge if requested
            if (autoPurge) {
                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // 2 second grace period for stream completion
                        filePurgeService.purgeImmediate(uuid);
                    } catch (Exception ignored) {}
                }).start();
            }

            return response;

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{uuid}/purge")
    public ResponseEntity<Map<String, Object>> purgeFile(@PathVariable String uuid) {
        boolean purged = filePurgeService.purgeImmediate(uuid);
        return ResponseEntity.ok(Map.of("purged", purged, "file_uuid", uuid));
    }
}
