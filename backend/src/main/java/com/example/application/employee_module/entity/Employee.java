package com.example.application.employee_module.entity;

import com.example.application.login_module.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant ownership. NULL only for pre-existing "house" employee rows
     * created before multi-tenancy was introduced. Every employee created
     * by a CLIENT_ADMIN gets this set automatically from
     * TenantContextService - it is never accepted from EmployeeRequest.
     */
    @Column(name = "client_company_id")
    private Long clientCompanyId;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "alternate_mobile_number", length = 30)
    private String alternateMobileNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

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

    /** 12-digit Government of India Aadhar number - unique per tenant, mandatory for new employees (see EmployeeRequest). Nullable here only so pre-existing rows from before this field don't break. */
    private String aadharNumber;

    /** 10-character Income Tax PAN (format ABCDE1234F, always stored upper-case) - unique per tenant, mandatory for new employees (see EmployeeRequest). Nullable here only so pre-existing rows from before this field don't break. */
    private String panNumber;

    /** Universal Account Number (EPFO) - genuinely optional: often not issued until PF
        registration completes, sometimes weeks after joining. No format enforcement beyond
        length - UAN format has varied over the years and isn't worth hard-coding a pattern for. */
    @Column(name = "uan_number", length = 20)
    private String uanNumber;

    /** PF Member ID / PF Account Number - optional, same reasoning as UAN. */
    @Column(name = "pf_member_id", length = 30)
    private String pfMemberId;

    /** ESIC (Employee State Insurance) number - optional, same reasoning as UAN. */
    @Column(name = "esic_number", length = 20)
    private String esicNumber;

    /** Bank details - all optional, filled in whenever convenient rather than blocking employee creation. */
    @Column(name = "bank_account_holder_name", length = 150)
    private String bankAccountHolderName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 11)
    private String bankIfscCode;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_branch", length = 150)
    private String bankBranch;

    /** Base64 data-URI string (e.g. "data:image/jpeg;base64,...") - see V95 migration javadoc for why this isn't a file-on-disk reference instead. */
    @Column(columnDefinition = "LONGTEXT")
    private String photoData;

    /** ACTIVE or INACTIVE. Deliberately independent of the linked User's login status. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /** NOT_STARTED / INVITED / PASSWORD_SETUP_PENDING / PROFILE_IN_PROGRESS / MANDATORY_PROFILE_COMPLETE / PROFILE_COMPLETE.
        Deliberately separate from both `status` above (whether the person still works here) and the linked User's
        active/locked flags (whether login is enabled) - this tracks progress through the invitation + profile-completion
        journey specifically. See EmployeeOnboardingService for every transition. */
    @Column(name = "onboarding_status", nullable = false, length = 30)
    private String onboardingStatus = "NOT_STARTED";

    /** Emergency Contact - all optional, all employee-editable during onboarding (see EmployeeProfileCompletionService). */
    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "emergency_contact_mobile", length = 20)
    private String emergencyContactMobile;

    /**
     * Per-employee statutory-deduction toggles (spec sections 7-10): "never
     * assume every employee has the same payroll rules". Checked by
     * MonthlyAttendanceReportService before applying the tenant-wide
     * PayrollSettings EPF/ESI/PT rates - an employee with any of these set
     * to false simply never has that deduction computed, regardless of
     * what the tenant's default settings are.
     */
    @Column(name = "pf_applicable", nullable = false)
    private boolean pfApplicable = true;

    @Column(name = "esi_applicable", nullable = false)
    private boolean esiApplicable = true;

    @Column(name = "pt_applicable", nullable = false)
    private boolean ptApplicable = true;

    /**
     * Nullable: an employee can exist with no login account at all.
     * One Employee -> Zero or One User (owning side, unique FK).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getAlternateMobileNumber() { return alternateMobileNumber; }
    public void setAlternateMobileNumber(String alternateMobileNumber) { this.alternateMobileNumber = alternateMobileNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
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
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getUanNumber() { return uanNumber; }
    public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }
    public String getPfMemberId() { return pfMemberId; }
    public void setPfMemberId(String pfMemberId) { this.pfMemberId = pfMemberId; }
    public String getEsicNumber() { return esicNumber; }
    public void setEsicNumber(String esicNumber) { this.esicNumber = esicNumber; }
    public String getBankAccountHolderName() { return bankAccountHolderName; }
    public void setBankAccountHolderName(String bankAccountHolderName) { this.bankAccountHolderName = bankAccountHolderName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String photoData) { this.photoData = photoData; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOnboardingStatus() { return onboardingStatus; }
    public void setOnboardingStatus(String onboardingStatus) { this.onboardingStatus = onboardingStatus; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }
    public String getEmergencyContactMobile() { return emergencyContactMobile; }
    public void setEmergencyContactMobile(String emergencyContactMobile) { this.emergencyContactMobile = emergencyContactMobile; }
    public boolean isPfApplicable() { return pfApplicable; }
    public void setPfApplicable(boolean pfApplicable) { this.pfApplicable = pfApplicable; }
    public boolean isEsiApplicable() { return esiApplicable; }
    public void setEsiApplicable(boolean esiApplicable) { this.esiApplicable = esiApplicable; }
    public boolean isPtApplicable() { return ptApplicable; }
    public void setPtApplicable(boolean ptApplicable) { this.ptApplicable = ptApplicable; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean hasLogin() { return user != null; }
}
