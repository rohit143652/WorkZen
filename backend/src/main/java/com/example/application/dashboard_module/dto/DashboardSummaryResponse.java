package com.example.application.dashboard_module.dto;

public class DashboardSummaryResponse {
    /** True for the SUPER_ADMIN global view, false for a CLIENT_ADMIN's tenant-scoped view. */
    private boolean global;

    private long totalEmployees;
    private long activeEmployees;
    private long employeesWithLogin;
    private long employeesWithoutLogin;
    private long activeUsers;
    private long lockedUsers;
    private long departments;
    private long assignedEmployees;
    private long unassignedEmployees;
    private long totalSites;

    // SUPER_ADMIN-only (global) fields - zero/unused in the tenant-scoped view
    private long totalClientCompanies;
    private long activeClientCompanies;

    public boolean isGlobal() { return global; }
    public void setGlobal(boolean global) { this.global = global; }
    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }
    public long getActiveEmployees() { return activeEmployees; }
    public void setActiveEmployees(long activeEmployees) { this.activeEmployees = activeEmployees; }
    public long getEmployeesWithLogin() { return employeesWithLogin; }
    public void setEmployeesWithLogin(long employeesWithLogin) { this.employeesWithLogin = employeesWithLogin; }
    public long getEmployeesWithoutLogin() { return employeesWithoutLogin; }
    public void setEmployeesWithoutLogin(long employeesWithoutLogin) { this.employeesWithoutLogin = employeesWithoutLogin; }
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    public long getLockedUsers() { return lockedUsers; }
    public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }
    public long getDepartments() { return departments; }
    public void setDepartments(long departments) { this.departments = departments; }
    public long getAssignedEmployees() { return assignedEmployees; }
    public void setAssignedEmployees(long assignedEmployees) { this.assignedEmployees = assignedEmployees; }
    public long getUnassignedEmployees() { return unassignedEmployees; }
    public void setUnassignedEmployees(long unassignedEmployees) { this.unassignedEmployees = unassignedEmployees; }
    public long getTotalSites() { return totalSites; }
    public void setTotalSites(long totalSites) { this.totalSites = totalSites; }
    public long getTotalClientCompanies() { return totalClientCompanies; }
    public void setTotalClientCompanies(long totalClientCompanies) { this.totalClientCompanies = totalClientCompanies; }
    public long getActiveClientCompanies() { return activeClientCompanies; }
    public void setActiveClientCompanies(long activeClientCompanies) { this.activeClientCompanies = activeClientCompanies; }
}
