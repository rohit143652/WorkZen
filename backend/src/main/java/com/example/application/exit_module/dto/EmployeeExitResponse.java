package com.example.application.exit_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeExitResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;
    private long noticePeriodDays;
    private String reason;
    private String status;
    private BigDecimal proratedSalary;
    private BigDecimal outstandingAdvanceDeduction;
    private BigDecimal netSettlementAmount;
    private LocalDateTime settledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public LocalDate getResignationDate() { return resignationDate; }
    public void setResignationDate(LocalDate resignationDate) { this.resignationDate = resignationDate; }
    public LocalDate getLastWorkingDay() { return lastWorkingDay; }
    public void setLastWorkingDay(LocalDate lastWorkingDay) { this.lastWorkingDay = lastWorkingDay; }
    public long getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(long noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }
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
}
