package com.example.application.employee_module.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String alternateMobileNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate joiningDate;
    private String department;
    private String designation;

    /**
     * Populated only when the caller has EMPLOYEE_SALARY_READ; left null
     * (with salaryVisible=false) otherwise, so the frontend can distinguish
     * "no permission to see this" from "no salary structure assigned yet".
     * Sourced from the employee's current (status=ACTIVE, effectiveTo=NULL)
     * EmployeeSalaryStructure assignment - see salary_structure_module.
     */
    private boolean salaryVisible;
    private Long currentSalaryStructureId;
    private String currentSalaryStructureCode;
    private String currentSalaryStructureName;
    private String currentSalaryType;
    private LocalDate currentSalaryEffectiveFrom;
    private java.math.BigDecimal currentGrossEarnings;
    private String employmentType;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String aadharNumber;
    private String panNumber;
    private boolean pfApplicable;
    private boolean esiApplicable;
    private boolean ptApplicable;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Login information (null-safe: absent when the employee has no login account)
    private boolean loginEnabled;
    private Long userId;
    private String username;
    private boolean userActive;
    private boolean userLocked;
    private List<String> roles;
    private LocalDateTime lastLoginAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public boolean isSalaryVisible() { return salaryVisible; }
    public void setSalaryVisible(boolean salaryVisible) { this.salaryVisible = salaryVisible; }
    public Long getCurrentSalaryStructureId() { return currentSalaryStructureId; }
    public void setCurrentSalaryStructureId(Long currentSalaryStructureId) { this.currentSalaryStructureId = currentSalaryStructureId; }
    public String getCurrentSalaryStructureCode() { return currentSalaryStructureCode; }
    public void setCurrentSalaryStructureCode(String currentSalaryStructureCode) { this.currentSalaryStructureCode = currentSalaryStructureCode; }
    public String getCurrentSalaryStructureName() { return currentSalaryStructureName; }
    public void setCurrentSalaryStructureName(String currentSalaryStructureName) { this.currentSalaryStructureName = currentSalaryStructureName; }
    public String getCurrentSalaryType() { return currentSalaryType; }
    public void setCurrentSalaryType(String currentSalaryType) { this.currentSalaryType = currentSalaryType; }
    public LocalDate getCurrentSalaryEffectiveFrom() { return currentSalaryEffectiveFrom; }
    public void setCurrentSalaryEffectiveFrom(LocalDate currentSalaryEffectiveFrom) { this.currentSalaryEffectiveFrom = currentSalaryEffectiveFrom; }
    public java.math.BigDecimal getCurrentGrossEarnings() { return currentGrossEarnings; }
    public void setCurrentGrossEarnings(java.math.BigDecimal currentGrossEarnings) { this.currentGrossEarnings = currentGrossEarnings; }
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
    public boolean isPfApplicable() { return pfApplicable; }
    public void setPfApplicable(boolean pfApplicable) { this.pfApplicable = pfApplicable; }
    public boolean isEsiApplicable() { return esiApplicable; }
    public void setEsiApplicable(boolean esiApplicable) { this.esiApplicable = esiApplicable; }
    public boolean isPtApplicable() { return ptApplicable; }
    public void setPtApplicable(boolean ptApplicable) { this.ptApplicable = ptApplicable; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isLoginEnabled() { return loginEnabled; }
    public void setLoginEnabled(boolean loginEnabled) { this.loginEnabled = loginEnabled; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isUserActive() { return userActive; }
    public void setUserActive(boolean userActive) { this.userActive = userActive; }
    public boolean isUserLocked() { return userLocked; }
    public void setUserLocked(boolean userLocked) { this.userLocked = userLocked; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
