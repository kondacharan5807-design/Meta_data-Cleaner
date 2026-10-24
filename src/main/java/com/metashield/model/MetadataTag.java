package com.metashield.model;

public class MetadataTag {
    private Long id;
    private Long fileId;
    private String category;
    private String tagName;
    private String rawValue;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW, SAFE
    private Integer riskWeight;
    private Boolean isPii;
    private Boolean isSanitized;

    public MetadataTag() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Integer getRiskWeight() { return riskWeight; }
    public void setRiskWeight(Integer riskWeight) { this.riskWeight = riskWeight; }

    public Boolean getIsPii() { return isPii; }
    public void setIsPii(Boolean pii) { isPii = pii; }

    public Boolean getIsSanitized() { return isSanitized; }
    public void setIsSanitized(Boolean sanitized) { isSanitized = sanitized; }
}
