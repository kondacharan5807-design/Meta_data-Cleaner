package com.metashield.model;

public class SanitizationPolicy {
    private Long id;
    private String policyName;
    private Boolean preserveCopyright = true; // Default Balanced Legal Mode
    private Boolean stripGps = true;
    private Boolean stripAuthor = true;
    private Boolean stripDeviceInfo = true;
    private Boolean stripTimestamps = true;
    private Boolean stripRevisionHistory = true;
    private Boolean isDefault = false;

    public SanitizationPolicy() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public Boolean getPreserveCopyright() { return preserveCopyright; }
    public void setPreserveCopyright(Boolean preserveCopyright) { this.preserveCopyright = preserveCopyright; }

    public Boolean getStripGps() { return stripGps; }
    public void setStripGps(Boolean stripGps) { this.stripGps = stripGps; }

    public Boolean getStripAuthor() { return stripAuthor; }
    public void setStripAuthor(Boolean stripAuthor) { this.stripAuthor = stripAuthor; }

    public Boolean getStripDeviceInfo() { return stripDeviceInfo; }
    public void setStripDeviceInfo(Boolean stripDeviceInfo) { this.stripDeviceInfo = stripDeviceInfo; }

    public Boolean getStripTimestamps() { return stripTimestamps; }
    public void setStripTimestamps(Boolean stripTimestamps) { this.stripTimestamps = stripTimestamps; }

    public Boolean getStripRevisionHistory() { return stripRevisionHistory; }
    public void setStripRevisionHistory(Boolean stripRevisionHistory) { this.stripRevisionHistory = stripRevisionHistory; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
