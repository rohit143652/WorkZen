package com.example.application.employee_module.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Editing employee information never touches username/password/role -
 * those go through the dedicated enable-login / disable-login / reset-password
 * / assign-role endpoints instead (see spec section 35).
 */
public class EmployeeUpdateRequest {

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
     * Optional: reassign this employee's Salary Structure. Only takes effect
     * if the caller has EMPLOYEE_SALARY_UPDATE, and only actually changes
     * anything if salaryStructureId differs from the employee's current
     * structure (see EmployeeService.reconcileSalaryAssignment) - salaryEffectiveFrom
     * is required in that case since it becomes the new assignment's start date.
     */
    private Long salaryStructureId;
    private java.time.LocalDate salaryEffectiveFrom;

    private String employmentType;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;

    /** See EmployeeRequest's own doc comment - same semantics for updates. */
    private Boolean pfApplicable;
    private Boolean esiApplicable;
    private Boolean ptApplicable;

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
    public Boolean getPfApplicable() { return pfApplicable; }
    public void setPfApplicable(Boolean pfApplicable) { this.pfApplicable = pfApplicable; }
    public Boolean getEsiApplicable() { return esiApplicable; }
    public void setEsiApplicable(Boolean esiApplicable) { this.esiApplicable = esiApplicable; }
    public Boolean getPtApplicable() { return ptApplicable; }
    public void setPtApplicable(Boolean ptApplicable) { this.ptApplicable = ptApplicable; }
}
