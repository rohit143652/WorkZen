package com.example.application.holiday_module.dto;

import java.time.LocalDate;

public class HolidayResponse {
    private final Long id;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String name;
    private final String description;
    private final int employeesMarkedPresent;

    public HolidayResponse(Long id, LocalDate startDate, LocalDate endDate, String name, String description, int employeesMarkedPresent) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.description = description;
        this.employeesMarkedPresent = employeesMarkedPresent;
    }

    public Long getId() { return id; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getEmployeesMarkedPresent() { return employeesMarkedPresent; }
}
