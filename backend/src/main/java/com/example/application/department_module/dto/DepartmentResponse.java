package com.example.application.department_module.dto;

public class DepartmentResponse {
    private Long id;
    private String name;
    private String status;
    private long employeeCount;

    public DepartmentResponse(Long id, String name, String status, long employeeCount) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.employeeCount = employeeCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public long getEmployeeCount() { return employeeCount; }
}
