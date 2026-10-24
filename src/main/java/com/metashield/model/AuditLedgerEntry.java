package com.metashield.model;

import java.time.Instant;

public class AuditLedgerEntry {
    private Long id;
    private Long blockIndex;
    private Instant eventTimestamp;
    private String eventType; // FILE_INGEST, SCAN_COMPLETE, SELECTIVE_WIPE, ZERO_TRACE_WIPE, RE_SCAN_VERIFIED, PURGED
    private String fileUuid;
    private String filename;
    private String operatorId;
    private String preHash;
    private String postHash;
    private String detailsJson;
    private String prevBlockHash;
    private String currentBlockHash;

    public AuditLedgerEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBlockIndex() { return blockIndex; }
    public void setBlockIndex(Long blockIndex) { this.blockIndex = blockIndex; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getFileUuid() { return fileUuid; }
    public void setFileUuid(String fileUuid) { this.fileUuid = fileUuid; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getPreHash() { return preHash; }
    public void setPreHash(String preHash) { this.preHash = preHash; }

    public String getPostHash() { return postHash; }
    public void setPostHash(String postHash) { this.postHash = postHash; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }

    public String getPrevBlockHash() { return prevBlockHash; }
    public void setPrevBlockHash(String prevBlockHash) { this.prevBlockHash = prevBlockHash; }

    public String getCurrentBlockHash() { return currentBlockHash; }
    public void setCurrentBlockHash(String currentBlockHash) { this.currentBlockHash = currentBlockHash; }
}
