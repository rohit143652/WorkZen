package com.example.application.designation_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tenant-scoped master data for the Employee form's Designation dropdown.
 *
 * NOTE: this entity used to also carry a default payroll structure (basic
 * salary / PF% / other deductions), duplicating what the dedicated Salary
 * Management module (salary_structure_module) now owns properly - with
 * configurable components, calculation types, and full history. Those
 * columns were removed (see migration V44) to remove that duplication;
 * an employee's salary is now assigned exclusively via a Salary Structure
 * (see EmployeeSalaryStructure), never derived from their Designation.
 *
 * Deactivating a designation never deletes the row, so employees already
 * using it keep a valid reference - it just stops appearing as a selectable
 * option for new/edited employees going forward.
 */
@Entity
@Table(name = "designations")
public class Designation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
