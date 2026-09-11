package com.example.application.attendance_module.dto;

import java.math.BigDecimal;

public class CheckOutRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
    /** Base64 data-URI selfie - required when the company's AttendanceRuleConfig.checkOutSelfieRequired is true. */
    private String selfieData;

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getSelfieData() { return selfieData; }
    public void setSelfieData(String selfieData) { this.selfieData = selfieData; }
}
