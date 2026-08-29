package com.example.application.exit_module.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_exits")
public class EmployeeExit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientCompanyId;
    private Long employeeId;
    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;
    private String reason;
    private String status = "INITIATED";

    private BigDecimal proratedSalary;
    private BigDecimal outstandingAdvanceDeduction;
    private BigDecimal netSettlementAmount;
    private LocalDateTime settledAt;
    private Long settledBy;

    private Long createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getResignationDate() { return resignationDate; }
    public void setResignationDate(LocalDate resignationDate) { this.resignationDate = resignationDate; }
    public LocalDate getLastWorkingDay() { return lastWorkingDay; }
    public void setLastWorkingDay(LocalDate lastWorkingDay) { this.lastWorkingDay = lastWorkingDay; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getProratedSalary() { return proratedSalary; }
    public void setProratedSalary(BigDecimal proratedSalary) { this.proratedSalary = proratedSalary; }
    public BigDecimal getOutstandingAdvanceDeduction() { return outstandingAdvanceDeduction; }
    public void setOutstandingAdvanceDeduction(BigDecimal outstandingAdvanceDeduction) { this.outstandingAdvanceDeduction = outstandingAdvanceDeduction; }
    public BigDecimal getNetSettlementAmount() { return netSettlementAmount; }
    public void setNetSettlementAmount(BigDecimal netSettlementAmount) { this.netSettlementAmount = netSettlementAmount; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public Long getSettledBy() { return settledBy; }
    public void setSettledBy(Long settledBy) { this.settledBy = settledBy; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
