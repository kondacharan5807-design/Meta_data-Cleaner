package com.metashield.model;

import java.time.Instant;

public class RiskAssessment {
    private Long id;
    private Long fileId;
    private Integer overallRiskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, SEVERE
    private String detectedHazardsJson;
    private String complianceJson;
    private String summary;
    private Instant assessedAt;

    public RiskAssessment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Integer getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(Integer overallRiskScore) { this.overallRiskScore = overallRiskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDetectedHazardsJson() { return detectedHazardsJson; }
    public void setDetectedHazardsJson(String detectedHazardsJson) { this.detectedHazardsJson = detectedHazardsJson; }

    public String getComplianceJson() { return complianceJson; }
    public void setComplianceJson(String complianceJson) { this.complianceJson = complianceJson; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
