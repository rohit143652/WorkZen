package com.example.application.employee_module.dto;

/** Public (unauthenticated) response for the onboarding landing page - deliberately minimal, see EmployeeOnboardingService.validateToken() javadoc for exactly what is and isn't exposed here. */
public class OnboardingValidateResponse {
    private String employeeName;
    private String employeeCode;
    private String companyName;
    private boolean expired;
    private boolean alreadyUsed;

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }
    public boolean isAlreadyUsed() { return alreadyUsed; }
    public void setAlreadyUsed(boolean alreadyUsed) { this.alreadyUsed = alreadyUsed; }
}
