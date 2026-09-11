package com.example.application.subscription_module.dto;

import java.time.LocalDate;
import java.util.List;

/** Super Admin's own dashboard - deliberately narrow: total companies and which ones are running out of subscription time. Nothing about any tenant's day-to-day operations (attendance, payroll, etc.) belongs here - that's Client Admin's dashboard, not the platform owner's. */
public class SuperAdminDashboardResponse {
    private long totalCompanies;
    private long activeCompanies;
    private List<ExpiringSubscription> expiringSoon;

    public static class ExpiringSubscription {
        private Long clientCompanyId;
        private String companyName;
        private String planName;
        private LocalDate endDate;
        private long daysRemaining;
        private String status;

        public Long getClientCompanyId() { return clientCompanyId; }
        public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public long getTotalCompanies() { return totalCompanies; }
    public void setTotalCompanies(long totalCompanies) { this.totalCompanies = totalCompanies; }
    public long getActiveCompanies() { return activeCompanies; }
    public void setActiveCompanies(long activeCompanies) { this.activeCompanies = activeCompanies; }
    public List<ExpiringSubscription> getExpiringSoon() { return expiringSoon; }
    public void setExpiringSoon(List<ExpiringSubscription> expiringSoon) { this.expiringSoon = expiringSoon; }
}
