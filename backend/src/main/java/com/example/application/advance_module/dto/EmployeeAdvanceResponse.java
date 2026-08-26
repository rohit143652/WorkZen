package com.example.application.advance_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeAdvanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate advanceDate;
    private BigDecimal amount;
    private String reason;
    private String paymentMode;
    private BigDecimal monthlyRecoveryAmount;
    private int recoveryStartYear;
    private int recoveryStartMonth;
    private String remarks;
    private boolean recoverViaPayroll;
    private String status;
    private int installmentsPaid;
    private BigDecimal recoveredAmount;
    private BigDecimal outstandingAmount;
    private LocalDateTime createdAt;
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public LocalDate getAdvanceDate() { return advanceDate; }
    public void setAdvanceDate(LocalDate advanceDate) { this.advanceDate = advanceDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public BigDecimal getMonthlyRecoveryAmount() { return monthlyRecoveryAmount; }
    public void setMonthlyRecoveryAmount(BigDecimal monthlyRecoveryAmount) { this.monthlyRecoveryAmount = monthlyRecoveryAmount; }
    public int getRecoveryStartYear() { return recoveryStartYear; }
    public void setRecoveryStartYear(int recoveryStartYear) { this.recoveryStartYear = recoveryStartYear; }
    public int getRecoveryStartMonth() { return recoveryStartMonth; }
    public void setRecoveryStartMonth(int recoveryStartMonth) { this.recoveryStartMonth = recoveryStartMonth; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isRecoverViaPayroll() { return recoverViaPayroll; }
    public void setRecoverViaPayroll(boolean recoverViaPayroll) { this.recoverViaPayroll = recoverViaPayroll; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getInstallmentsPaid() { return installmentsPaid; }
    public void setInstallmentsPaid(int installmentsPaid) { this.installmentsPaid = installmentsPaid; }
    public BigDecimal getRecoveredAmount() { return recoveredAmount; }
    public void setRecoveredAmount(BigDecimal recoveredAmount) { this.recoveredAmount = recoveredAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
