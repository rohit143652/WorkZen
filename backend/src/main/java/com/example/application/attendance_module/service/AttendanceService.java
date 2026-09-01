package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.*;
import com.example.application.attendance_module.entity.Attendance;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.site_module.entity.Site;
import com.example.application.site_module.service.SiteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance is intentionally append-mostly: SITE_ADMIN/SITE_SUPERVISOR can
 * mark a day exactly once (mark() rejects any date already recorded for
 * that employee, full stop - there is no "overwrite" path in this method
 * at all). Only a holder of ATTENDANCE_UPDATE (CLIENT_ADMIN by default
 * grant) can reach edit(), which is a completely separate, permission-gated
 * method/endpoint. This mirrors the business rule exactly: once filled,
 * nobody but Client Admin can change it.
 */
@Service
public class AttendanceService {

    private static final List<String> VALID_STATUSES = List.of("PRESENT", "ABSENT", "HALF_DAY", "ON_LEAVE");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSiteAssignmentRepository assignmentRepository;
    private final SiteService siteService;
    private final UserRepository userRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
                              EmployeeSiteAssignmentRepository assignmentRepository, SiteService siteService,
                              UserRepository userRepository, TenantContextService tenantContext,
                              AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.siteService = siteService;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional
    public AttendanceResponse mark(MarkAttendanceRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Attendance saved = markOne(tenantId, request.getEmployeeId(), request.getAttendanceDate(),
                request.getStatus(), request.getRemarks(), actorId, request.getLatitude(), request.getLongitude());
        auditService.log(actorId, "ATTENDANCE_MARKED",
                "Marked " + request.getStatus() + " for employee id " + request.getEmployeeId()
                        + " on " + request.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    /** For "Mark My Attendance" (self-service) - is today already marked for the logged-in employee? Lets the UI show the existing status instead of a mark button once they've already checked in. */
    @Transactional(readOnly = true)
    public AttendanceResponse findMyTodayStatus(Long userId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));
        return attendanceRepository.findByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, employee.getId(), LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * One-click self-service attendance marking - always PRESENT, always today, and the
     * employee is ALWAYS resolved from the caller's own login (never accepts an employeeId),
     * same structural guarantee already used for LeaveRequestService.selfCreate() and
     * PayslipService.generateMyPayslip() - there is no way to call this and mark anyone's
     * attendance but your own. Goes through the exact same markOne() (and therefore the same
     * geofence check) as the supervisor-driven mark()/bulkMark() above.
     */
    @Transactional
    public AttendanceResponse markMine(Long userId, java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                                        Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));

        Attendance saved = markOne(tenantId, employee.getId(), LocalDate.now(), "PRESENT",
                "Self-marked", actorId, latitude, longitude);

        auditService.log(actorId, "ATTENDANCE_SELF_MARKED",
                employee.getEmployeeCode() + " marked their own attendance for " + saved.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Marks attendance for many employees on the same date in one call - the
     * practical answer to "100 employees would mean 100 individual saves".
     * Each entry is validated and inserted independently; one employee's
     * failure (already marked, no active assignment, bad status) is
     * collected into `rejected` rather than aborting the whole batch, so
     * marking 99 people successfully isn't held hostage by one bad row.
     */
    @Transactional
    public BulkMarkAttendanceResult bulkMark(BulkMarkAttendanceRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<String> rejected = new ArrayList<>();
        int markedCount = 0;

        for (BulkAttendanceEntry entry : request.getEntries()) {
            try {
                markOne(tenantId, entry.getEmployeeId(), request.getAttendanceDate(),
                        entry.getStatus(), entry.getRemarks(), actorId, request.getLatitude(), request.getLongitude());
                markedCount++;
            } catch (RuntimeException ex) {
                String label = employeeRepository.findById(entry.getEmployeeId())
                        .map(Employee::getEmployeeCode)
                        .orElse("Employee " + entry.getEmployeeId());
                rejected.add(label + ": " + ex.getMessage());
            }
        }

        auditService.log(actorId, "ATTENDANCE_MARKED",
                markedCount + " of " + request.getEntries().size() + " attendance records marked in bulk for "
                        + request.getAttendanceDate(), httpRequest);
        return new BulkMarkAttendanceResult(request.getEntries().size(), markedCount, rejected);
    }

    /**
     * Auto-marks every currently active employee PRESENT for a given date - used when a Client
     * Admin adds a company Holiday (see HolidayService.create()), so that day flows into Payable
     * Days the exact same way any other PRESENT attendance record already does, with no separate
     * "holiday" concept needed in the payroll calculation itself.
     *
     * Employees who already have an attendance record for that date (marked manually beforehand,
     * or already covered by an earlier holiday) are silently skipped, not overwritten or treated
     * as failures - same for anyone with no active site assignment right now. Returns how many
     * were actually newly marked.
     */
    @Transactional
    public int markPresentForHoliday(Long tenantId, LocalDate date, String remarks, Long actorId) {
        List<Employee> activeEmployees = employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE");
        int marked = 0;
        for (Employee employee : activeEmployees) {
            try {
                markOne(tenantId, employee.getId(), date, "PRESENT", remarks, actorId);
                marked++;
            } catch (RuntimeException alreadyMarkedOrNoAssignment) {
                // Expected/benign for a bulk holiday sweep - just skip this employee.
            }
        }
        return marked;
    }

    /**
     * Marks ON_LEAVE for one employee across a date range - used when a leave request is
     * approved (LeaveRequestService.approve()/adminCreate()). Unlike markPresentForHoliday()
     * above, this does NOT silently skip conflicting dates: if ANY date in the range already
     * has attendance marked (present, absent, whatever), the whole thing is rejected upfront
     * with a clear list of exactly which dates conflict, before anything is written - approving
     * a leave request should never partially apply and leave a confusing half-marked state, and
     * the reviewer needs to know a conflict exists so they can resolve it (e.g. by adjusting the
     * requested dates) rather than have some days silently not count as leave.
     */
    @Transactional
    public void markOnLeaveForRange(Long tenantId, Long employeeId, LocalDate start, LocalDate end, String remarks, Long actorId) {
        List<LocalDate> conflicts = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (attendanceRepository.existsByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, employeeId, d)) {
                conflicts.add(d);
            }
        }
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Attendance is already marked for " + conflicts.size()
                    + " day(s) in this range (" + conflicts + ") - cannot approve leave over existing attendance records.");
        }
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            markOne(tenantId, employeeId, d, "ON_LEAVE", remarks, actorId);
        }
    }

    /** Shared core of mark() and bulkMark() - see class javadoc for the immutability guarantee this preserves either way. This overload is used by every OTHER caller (holiday auto-marking, leave approval, etc.) that has no real device location to report - those never had a geofence to check against anyway. */
    private Attendance markOne(Long tenantId, Long employeeId, LocalDate date, String status, String remarks, Long actorId) {
        return markOne(tenantId, employeeId, date, status, remarks, actorId, null, null);
    }

    /** Same as above, but for the two callers that DO have a real device GPS position to check - mark() and bulkMark(). See checkGeofence() for what actually happens with latitude/longitude. */
    private Attendance markOne(Long tenantId, Long employeeId, LocalDate date, String status, String remarks, Long actorId,
                                java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        validateStatus(status);

        Employee employee = employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee does not belong to the current tenant"));

        EmployeeSiteAssignment currentAssignment = assignmentRepository
                .findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(employee.getId(), tenantId, "ACTIVE")
                .orElseThrow(() -> new BadRequestException(
                        "no active site assignment - attendance can only be marked for currently assigned employees"));

        if (attendanceRepository.existsByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, employee.getId(), date)) {
            throw new DuplicateResourceException(
                    "attendance for " + date + " is already marked and cannot be changed");
        }

        checkGeofence(currentAssignment.getSiteId(), latitude, longitude);

        Attendance attendance = new Attendance();
        attendance.setClientCompanyId(tenantId);
        attendance.setEmployeeId(employee.getId());
        attendance.setSiteId(currentAssignment.getSiteId());
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);
        attendance.setMarkedBy(actorId);
        attendance.setMarkedLatitude(latitude);
        attendance.setMarkedLongitude(longitude);

        return attendanceRepository.save(attendance);
    }

    private static final int DEFAULT_GEOFENCE_RADIUS_METERS = 200;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /**
     * A site with no latitude/longitude configured has NO geofence at all - this is a silent
     * no-op for every site that hasn't opted into GPS-based attendance, so nothing about
     * existing behavior changes for them. Only once a site HAS both coordinates set does this
     * start requiring (and checking) the marking device's own position, using the standard
     * Haversine great-circle-distance formula - accurate enough for a same-city geofence check
     * without needing any external mapping service.
     */
    private void checkGeofence(Long siteId, java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        var site = siteService.findById(siteId);
        if (site.getLatitude() == null || site.getLongitude() == null) return;

        if (latitude == null || longitude == null) {
            throw new BadRequestException("Location access is required to mark attendance for \""
                    + site.getSiteName() + "\" - please allow location access on your device and try again.");
        }

        double lat1 = Math.toRadians(site.getLatitude().doubleValue());
        double lat2 = Math.toRadians(latitude.doubleValue());
        double dLat = Math.toRadians(latitude.doubleValue() - site.getLatitude().doubleValue());
        double dLon = Math.toRadians(longitude.doubleValue() - site.getLongitude().doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double distanceMeters = EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        int allowedRadius = site.getGeofenceRadiusMeters() != null ? site.getGeofenceRadiusMeters() : DEFAULT_GEOFENCE_RADIUS_METERS;
        if (distanceMeters > allowedRadius) {
            throw new BadRequestException(String.format(
                    "You are %.0fm away from \"%s\" - you must be within %dm of the site to mark attendance here.",
                    distanceMeters, site.getSiteName(), allowedRadius));
        }
    }

    /** Gated by ATTENDANCE_UPDATE at the controller - this method itself does not re-check the permission ceiling. */
    @Transactional
    public AttendanceResponse edit(Long id, UpdateAttendanceRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateStatus(request.getStatus());

        Attendance attendance = attendanceRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Attendance record does not belong to the current tenant"));

        String previousStatus = attendance.getStatus();
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setUpdatedBy(actorId);
        Attendance saved = attendanceRepository.save(attendance);

        auditService.log(actorId, "ATTENDANCE_UPDATED",
                "Changed attendance on " + attendance.getAttendanceDate() + " from " + previousStatus
                        + " to " + request.getStatus() + " (employee id " + attendance.getEmployeeId() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findForEmployeeInRange(Long employeeId, LocalDate from, LocalDate to) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        if (from.isAfter(to)) {
            throw new BadRequestException("Start date must be on or before end date");
        }
        // Tenant check on the employee happens implicitly via the query below being scoped
        // to clientCompanyId - if the employee belongs to another tenant, this simply returns nothing.
        employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee does not belong to the current tenant"));

        return attendanceRepository
                .findAllByClientCompanyIdAndEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(tenantId, employeeId, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findAll(LocalDate from, LocalDate to, Long siteId, Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        if (from.isAfter(to)) {
            throw new BadRequestException("Start date must be on or before end date");
        }
        Page<Attendance> page = siteId != null
                ? attendanceRepository.findAllByClientCompanyIdAndSiteIdAndAttendanceDateBetween(tenantId, siteId, from, to, pageable)
                : attendanceRepository.findAllByClientCompanyIdAndAttendanceDateBetween(tenantId, from, to, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Builds the "Mark Attendance" screen's employee list for a given date:
     * every actively-assigned employee (optionally filtered to one site),
     * each paired with their existing attendance record for that date if
     * one already exists (so the UI can show it as locked instead of
     * offering to mark it again).
     */
    @Transactional(readOnly = true)
    public List<EmployeeAttendanceOption> getMarkableEmployees(LocalDate date, Long siteFilter) {
        Long tenantId = tenantContext.requireCurrentTenantId();

        List<EmployeeSiteAssignment> activeAssignments = assignmentRepository
                .findAllByClientCompanyIdAndStatus(tenantId, "ACTIVE");

        Map<Long, Attendance> existingByEmployee = new HashMap<>();
        for (Attendance a : attendanceRepository.findAllByClientCompanyIdAndAttendanceDate(tenantId, date)) {
            existingByEmployee.put(a.getEmployeeId(), a);
        }

        Map<Long, String> siteNames = new HashMap<>();
        List<EmployeeAttendanceOption> options = new ArrayList<>();

        for (EmployeeSiteAssignment assignment : activeAssignments) {
            if (siteFilter != null && !siteFilter.equals(assignment.getSiteId())) continue;

            Employee employee = employeeRepository.findById(assignment.getEmployeeId()).orElse(null);
            if (employee == null || !"ACTIVE".equals(employee.getStatus())) continue;

            String siteName = siteNames.computeIfAbsent(assignment.getSiteId(), id -> {
                try {
                    return siteService.getEntityForCurrentTenant(id).getSiteName();
                } catch (TenantAccessDeniedException ex) {
                    return null;
                }
            });

            Attendance existing = existingByEmployee.get(employee.getId());
            options.add(new EmployeeAttendanceOption(
                    employee.getId(), employee.getEmployeeCode(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    assignment.getSiteId(), siteName,
                    existing != null ? toResponse(existing) : null));
        }

        options.sort((a, b) -> a.getEmployeeCode().compareToIgnoreCase(b.getEmployeeCode()));
        return options;
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException("Status must be one of: " + String.join(", ", VALID_STATUSES));
        }
    }

    private AttendanceResponse toResponse(Attendance a) {
        AttendanceResponse r = new AttendanceResponse();
        r.setId(a.getId());
        r.setEmployeeId(a.getEmployeeId());
        employeeRepository.findById(a.getEmployeeId()).ifPresent(e -> {
            r.setEmployeeCode(e.getEmployeeCode());
            r.setEmployeeName(e.getFirstName() + " " + e.getLastName());
        });
        r.setSiteId(a.getSiteId());
        try {
            Site site = siteService.getEntityForCurrentTenant(a.getSiteId());
            r.setSiteName(site.getSiteName());
        } catch (TenantAccessDeniedException ignored) {
            r.setSiteName(null);
        }
        r.setAttendanceDate(a.getAttendanceDate());
        r.setStatus(a.getStatus());
        r.setRemarks(a.getRemarks());
        if (a.getMarkedBy() != null) {
            userRepository.findById(a.getMarkedBy()).ifPresent(u -> r.setMarkedByUsername(u.getUsername()));
        }
        if (a.getUpdatedBy() != null) {
            userRepository.findById(a.getUpdatedBy()).ifPresent(u -> r.setUpdatedByUsername(u.getUsername()));
        }
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setEditable(tenantContext.currentPermissionNames().contains("ATTENDANCE_UPDATE"));
        return r;
    }
}
