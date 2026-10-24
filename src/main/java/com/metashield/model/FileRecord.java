package com.metashield.model;

import java.time.Instant;

public class FileRecord {
    private Long id;
    private String fileUuid;
    private String originalFilename;
    private String storedPath;
    private String cleanedPath;
    private String mimeType;
    private Long fileSize;
    private String initialSha256;
    private String cleanedSha256;
    private String status; // STAGED, SCANNED, SANITIZED, VERIFIED, EXPIRED_PURGED
    private Instant uploadTimestamp;
    private Instant expiresAt;

    public FileRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileUuid() { return fileUuid; }
    public void setFileUuid(String fileUuid) { this.fileUuid = fileUuid; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }

    public String getCleanedPath() { return cleanedPath; }
    public void setCleanedPath(String cleanedPath) { this.cleanedPath = cleanedPath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getInitialSha256() { return initialSha256; }
    public void setInitialSha256(String initialSha256) { this.initialSha256 = initialSha256; }

    public String getCleanedSha256() { return cleanedSha256; }
    public void setCleanedSha256(String cleanedSha256) { this.cleanedSha256 = cleanedSha256; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(Instant uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
