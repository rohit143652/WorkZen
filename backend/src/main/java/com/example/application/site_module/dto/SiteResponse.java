package com.example.application.site_module.dto;

import java.time.LocalDateTime;

public class SiteResponse {
    private Long id;
    private String siteCode;
    private String siteName;
    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String siteContactPerson;
    private String siteContactNumber;
    private int requiredEmployeeCount;
    private long assignedEmployeeCount;
    private boolean allowOverAllocation;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getSiteContactPerson() { return siteContactPerson; }
    public void setSiteContactPerson(String siteContactPerson) { this.siteContactPerson = siteContactPerson; }
    public String getSiteContactNumber() { return siteContactNumber; }
    public void setSiteContactNumber(String siteContactNumber) { this.siteContactNumber = siteContactNumber; }
    public int getRequiredEmployeeCount() { return requiredEmployeeCount; }
    public void setRequiredEmployeeCount(int requiredEmployeeCount) { this.requiredEmployeeCount = requiredEmployeeCount; }
    public long getAssignedEmployeeCount() { return assignedEmployeeCount; }
    public void setAssignedEmployeeCount(long assignedEmployeeCount) { this.assignedEmployeeCount = assignedEmployeeCount; }
    public boolean isAllowOverAllocation() { return allowOverAllocation; }
    public void setAllowOverAllocation(boolean allowOverAllocation) { this.allowOverAllocation = allowOverAllocation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
