package com.example.application.site_module.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SiteRequest {

    /** Optional. If left blank, the backend auto-generates the next sequential code for this tenant (e.g. SITE0001, SITE0002...). */
    private String siteCode;

    @NotBlank(message = "Site name is required")
    private String siteName;

    private String description;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String siteContactPerson;
    private String siteContactNumber;

    @Min(value = 0, message = "Required employee count cannot be negative")
    private int requiredEmployeeCount;

    private boolean allowOverAllocation = false;

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
    public boolean isAllowOverAllocation() { return allowOverAllocation; }
    public void setAllowOverAllocation(boolean allowOverAllocation) { this.allowOverAllocation = allowOverAllocation; }
}
