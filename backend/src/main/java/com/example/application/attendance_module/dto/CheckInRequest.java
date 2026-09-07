package com.example.application.attendance_module.dto;

import java.math.BigDecimal;

public class CheckInRequest {
    /** OFFICE, WORK_FROM_HOME, FIELD_WORK. */
    private String workMode;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}
