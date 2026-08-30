package com.example.application.employee_module.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class EmployeeRequest {

    /** Optional. If left blank, the backend auto-generates the next sequential code for this tenant (e.g. EMP0001, EMP0002...). */
    private String employeeCode;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String mobileNumber;
    private String alternateMobileNumber;
    private LocalDate dateOfBirth;
    private String gender;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Designation is required")
    private String designation;

    /**
     * Optional: assign a Salary Structure to this employee at creation time
     * (see salary_structure_module). Only takes effect if the caller has
     * EMPLOYEE_SALARY_UPDATE. If omitted, the employee is created with no
     * salary structure and one can be assigned later from their profile.
     */
    private Long salaryStructureId;

    /** Required if salaryStructureId is set. Defaults to joiningDate when left blank. */
    private java.time.LocalDate salaryEffectiveFrom;

    private String employmentType;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;

    @NotBlank(message = "Aadhar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhar number must be exactly 12 digits")
    private String aadharNumber;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$", message = "PAN number must be in the format ABCDE1234F")
    private String panNumber;

    /**
     * PF/ESI/PT deduction applicability for THIS employee - independent of the Salary Structure
     * type (fixed vs component-based). Both this flag AND the tenant's Payroll Settings must be
     * enabled for a deduction to actually apply - see PayrollCalculationService. Defaults to
     * true on the backend entity if omitted here, matching standard full-time employment.
     */
    private Boolean pfApplicable;
    private Boolean esiApplicable;
    private Boolean ptApplicable;

    /** Drives whether loginAccess is required and whether a User account is created atomically. */
    private boolean enableLogin = false;

    @Valid
    private EmployeeLoginAccessRequest loginAccess;

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
    public Long getSalaryStructureId() { return salaryStructureId; }
    public void setSalaryStructureId(Long salaryStructureId) { this.salaryStructureId = salaryStructureId; }
    public java.time.LocalDate getSalaryEffectiveFrom() { return salaryEffectiveFrom; }
    public void setSalaryEffectiveFrom(java.time.LocalDate salaryEffectiveFrom) { this.salaryEffectiveFrom = salaryEffectiveFrom; }
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
    public Boolean getPfApplicable() { return pfApplicable; }
    public void setPfApplicable(Boolean pfApplicable) { this.pfApplicable = pfApplicable; }
    public Boolean getEsiApplicable() { return esiApplicable; }
    public void setEsiApplicable(Boolean esiApplicable) { this.esiApplicable = esiApplicable; }
    public Boolean getPtApplicable() { return ptApplicable; }
    public void setPtApplicable(Boolean ptApplicable) { this.ptApplicable = ptApplicable; }
    public boolean isEnableLogin() { return enableLogin; }
    public void setEnableLogin(boolean enableLogin) { this.enableLogin = enableLogin; }
    public EmployeeLoginAccessRequest getLoginAccess() { return loginAccess; }
    public void setLoginAccess(EmployeeLoginAccessRequest loginAccess) { this.loginAccess = loginAccess; }
}
