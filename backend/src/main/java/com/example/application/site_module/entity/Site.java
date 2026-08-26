package com.example.application.site_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning tenant. Always set by the backend from TenantContextService, never from the request. */
    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "site_code", nullable = false, length = 50)
    private String siteCode;

    @Column(name = "site_name", nullable = false, length = 150)
    private String siteName;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String pincode;

    @Column(name = "site_contact_person", length = 150)
    private String siteContactPerson;

    @Column(name = "site_contact_number", length = 30)
    private String siteContactNumber;

    @Column(name = "required_employee_count", nullable = false)
    private int requiredEmployeeCount = 0;

    @Column(name = "allow_over_allocation", nullable = false)
    private boolean allowOverAllocation = false;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
