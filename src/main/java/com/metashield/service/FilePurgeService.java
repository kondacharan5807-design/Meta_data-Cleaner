package com.metashield.service;

import com.metashield.config.StorageProperties;
import com.metashield.model.FileRecord;
import com.metashield.repository.FileRecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

/**
 * File Purge Service
 * Enforces GDPR Article 5(1)(e) Storage Limitation principle via strict 1-hour session TTL
 * and on-demand immediate file purging.
 */
@Service
public class FilePurgeService {

    private final FileRecordRepository fileRecordRepository;
    private final AuditLedgerService auditLedgerService;
    private final StorageProperties storageProperties;

    public FilePurgeService(FileRecordRepository fileRecordRepository,
                            AuditLedgerService auditLedgerService,
                            StorageProperties storageProperties) {
        this.fileRecordRepository = fileRecordRepository;
        this.auditLedgerService = auditLedgerService;
        this.storageProperties = storageProperties;
    }

    /**
     * Runs every 10 minutes to auto-purge expired session files
     */
    @Scheduled(fixedRate = 600000)
    public void purgeExpiredFiles() {
        Instant now = Instant.now();
        List<FileRecord> expired = fileRecordRepository.findExpiredFiles(now);
        for (FileRecord f : expired) {
            purgeFilePhysical(f, "SCHEDULED_TTL_EXPIRED (1-Hour Session Limit)");
        }
    }

    /**
     * Immediately purges physical files on demand (e.g. after download)
     */
    public boolean purgeImmediate(String fileUuid) {
        return fileRecordRepository.findByUuid(fileUuid).map(f -> {
            purgeFilePhysical(f, "USER_DEMAND_IMMEDIATE_PURGE");
            return true;
        }).orElse(false);
    }

    private void purgeFilePhysical(FileRecord f, String reason) {
        try {
            if (f.getStoredPath() != null) {
                Path p = Paths.get(f.getStoredPath());
                Files.deleteIfExists(p);
            }
            if (f.getCleanedPath() != null) {
                Path p = Paths.get(f.getCleanedPath());
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            System.err.println("Notice deleting file physical paths: " + e.getMessage());
        }

        fileRecordRepository.updateStatus(f.getId(), "EXPIRED_PURGED");
        auditLedgerService.recordEvent(
                "FILE_PURGED",
                f.getFileUuid(),
                f.getOriginalFilename(),
                "GDPR_STORAGE_LIMIT_DAEMON",
                f.getInitialSha256(),
                f.getCleanedSha256(),
                "{\"reason\":\"" + reason + "\",\"gdpr_article\":\"Art. 5(1)(e) Storage Limitation\"}"
        );
    }
}
