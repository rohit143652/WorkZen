package com.example.application.client_company_module.dto;

import java.time.LocalDateTime;

public class ClientCompanyResponse {
    private Long id;
    private String companyCode;
    private String companyName;
    private String legalName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Summary counts - populated by the service, not stored on the entity
    private long totalEmployees;
    private long totalSites;
    private boolean hasClientAdminLogin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
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
    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; }
    public String getContactPersonEmail() { return contactPersonEmail; }
    public void setContactPersonEmail(String contactPersonEmail) { this.contactPersonEmail = contactPersonEmail; }
    public String getContactPersonPhone() { return contactPersonPhone; }
    public void setContactPersonPhone(String contactPersonPhone) { this.contactPersonPhone = contactPersonPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }
    public long getTotalSites() { return totalSites; }
    public void setTotalSites(long totalSites) { this.totalSites = totalSites; }
    public boolean isHasClientAdminLogin() { return hasClientAdminLogin; }
    public void setHasClientAdminLogin(boolean hasClientAdminLogin) { this.hasClientAdminLogin = hasClientAdminLogin; }
}
