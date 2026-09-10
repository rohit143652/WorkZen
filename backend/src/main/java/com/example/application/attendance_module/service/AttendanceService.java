package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.*;
import com.example.application.attendance_module.entity.Attendance;
import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.feature.FeatureAccessService;
import com.example.application.client_company_module.feature.FeatureCode;
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
import java.time.LocalDateTime;
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
    private final AttendanceRuleConfigService ruleConfigService;
    private final AttendanceRulesEngine ruleEngine;
    private final FeatureAccessService featureAccessService;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
                              EmployeeSiteAssignmentRepository assignmentRepository, SiteService siteService,
                              UserRepository userRepository, TenantContextService tenantContext,
                              AuditService auditService, AttendanceRuleConfigService ruleConfigService,
                              AttendanceRulesEngine ruleEngine, FeatureAccessService featureAccessService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.siteService = siteService;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.ruleConfigService = ruleConfigService;
        this.ruleEngine = ruleEngine;
        this.featureAccessService = featureAccessService;
    }

    /**
     * Company Feature Configuration is the UPPER-LEVEL control (see FeatureAccessService javadoc):
     * company feature enabled AND role permission allowed, both required. This maps the CURRENT
     * actor's role to the right manual-attendance feature code, so a SITE_SUPERVISOR marking
     * attendance is checked against SUPERVISOR_MANUAL_ATTENDANCE specifically, not a generic
     * "admin" bucket - a company can allow supervisors to mark attendance while still disabling
     * it for, say, accountants, without that ever needing a code change.
     */
    private void requireManualAttendanceFeatureEnabled() {
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        java.util.Set<String> roles = tenantContext.currentPrincipal().getRoleNames();
        String featureCode;
        String featureLabel;
        if (roles.contains("HR_ADMIN")) {
            featureCode = FeatureCode.HR_MANUAL_ATTENDANCE;
            featureLabel = "HR manual attendance";
        } else if (roles.contains("SITE_SUPERVISOR")) {
            featureCode = FeatureCode.SUPERVISOR_MANUAL_ATTENDANCE;
            featureLabel = "Supervisor manual attendance";
        } else {
            featureCode = FeatureCode.ADMIN_MANUAL_ATTENDANCE;
            featureLabel = "Admin manual attendance";
        }
        featureAccessService.requireEnabledForCurrentTenant(featureCode, featureLabel);
    }

    /** The ACTOR's role label to store on the record (createdByRole/modifiedByRole) - same mapping used for feature gating, kept in one place. */
    private String currentActorRoleLabel() {
        java.util.Set<String> roles = tenantContext.currentPrincipal().getRoleNames();
        if (roles.contains("HR_ADMIN")) return "HR_ADMIN";
        if (roles.contains("SITE_SUPERVISOR")) return "SITE_SUPERVISOR";
        if (roles.contains("CLIENT_ADMIN")) return "CLIENT_ADMIN";
        if (roles.contains("SITE_ADMIN")) return "SITE_ADMIN";
        return roles.stream().findFirst().orElse("ADMIN");
    }

    @Transactional
    public AttendanceResponse mark(MarkAttendanceRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        requireManualAttendanceFeatureEnabled();
        Attendance saved = markOne(tenantId, request.getEmployeeId(), request.getAttendanceDate(),
                request.getStatus(), request.getRemarks(), actorId, request.getLatitude(), request.getLongitude(),
                request.getCheckInTime(), request.getCheckOutTime(), request.getWorkMode());
        auditService.log(actorId, "ATTENDANCE_MARKED",
                "Marked " + saved.getStatus() + " for employee id " + request.getEmployeeId()
                        + " on " + request.getAttendanceDate()
                        + (saved.getCheckInTime() != null ? " (with check-in/out times)" : " (status only, no times)"),
                httpRequest);
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
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee self attendance");
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));

        Attendance saved = markOne(tenantId, employee.getId(), LocalDate.now(), "PRESENT",
                "Self-marked", actorId, latitude, longitude, null, null, null);

        auditService.log(actorId, "ATTENDANCE_SELF_MARKED",
                employee.getEmployeeCode() + " marked their own attendance for " + saved.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Check-in - the start of the new centralized time-tracking flow (see class javadoc). Same
     * geofence check as markMine()/mark(). If today already has a row from the OLD one-click
     * flow (no check-in time recorded yet), this ADDS check-in time to that SAME row rather than
     * rejecting it or creating a duplicate - there is exactly one attendance record per employee
     * per day no matter which flow touched it. Rejects outright only if a check-in time is
     * already recorded for today and the tenant doesn't allow multiple check-ins.
     */
    @Transactional
    public AttendanceResponse checkIn(Long userId, CheckInRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee self attendance");
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));

        EmployeeSiteAssignment currentAssignment = assignmentRepository
                .findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(employee.getId(), tenantId, "ACTIVE")
                .orElseThrow(() -> new BadRequestException(
                        "no active site assignment - attendance can only be marked for currently assigned employees"));

        AttendanceRuleConfig config = ruleConfigService.getOrCreateForCurrentTenant();
        LocalDate today = LocalDate.now();
        if (ruleEngine.isWeeklyOff(config, today)) {
            throw new BadRequestException("Today is a configured weekly off - check-in is not required.");
        }

        checkGeofence(currentAssignment.getSiteId(), request.getLatitude(), request.getLongitude());

        Attendance attendance = attendanceRepository
                .findByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, employee.getId(), today)
                .orElseGet(Attendance::new);

        boolean isNewRow = attendance.getId() == null;
        if (!isNewRow && attendance.getCheckInTime() != null && !config.isAllowMultipleCheckin()) {
            throw new DuplicateResourceException("Already checked in for today at "
                    + attendance.getCheckInTime().toLocalTime() + " - multiple check-ins are not enabled for this company.");
        }
        // Don't let a check-in silently overwrite a day that's already been finalized some other
        // way (e.g. ON_LEAVE via an approved leave request, or ABSENT/HALF_DAY set directly by an
        // admin) - only a blank day or one already mid check-in (WORKING) can be checked into.
        if (!isNewRow && attendance.getCheckInTime() == null
                && attendance.getStatus() != null && !"WORKING".equals(attendance.getStatus())) {
            throw new BadRequestException("Today is already marked as " + attendance.getStatus()
                    + " - check-in is not available. Contact an admin if this needs to change.");
        }

        if (isNewRow) {
            attendance.setClientCompanyId(tenantId);
            attendance.setEmployeeId(employee.getId());
            attendance.setSiteId(currentAssignment.getSiteId());
            attendance.setAttendanceDate(today);
            attendance.setMarkedBy(actorId);
        }
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setWorkMode(request.getWorkMode() != null ? request.getWorkMode() : "OFFICE");
        attendance.setAttendanceSource("SELF");
        attendance.setStatus("WORKING");
        attendance.setMarkedLatitude(request.getLatitude());
        attendance.setMarkedLongitude(request.getLongitude());

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log(actorId, "CHECK_IN",
                employee.getEmployeeCode() + " checked in at " + saved.getCheckInTime(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Check-out - runs AttendanceRulesEngine.compute() to derive working minutes, late/early-exit,
     * and the final status (PRESENT/HALF_DAY/ABSENT) all in one place, then persists that onto
     * the SAME row check-in created. Rejects check-out without a prior check-in outright (per
     * spec: "prevent check-out before check-in").
     */
    @Transactional
    public AttendanceResponse checkOut(Long userId, CheckOutRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee self attendance");
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));

        Attendance attendance = attendanceRepository
                .findByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, employee.getId(), LocalDate.now())
                .orElseThrow(() -> new BadRequestException("You have not checked in today yet."));

        if (attendance.getCheckInTime() == null) {
            throw new BadRequestException("You have not checked in today yet.");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new DuplicateResourceException("Already checked out for today at " + attendance.getCheckOutTime().toLocalTime());
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        if (!checkOutTime.isAfter(attendance.getCheckInTime())) {
            throw new BadRequestException("Check-out time cannot be before check-in time.");
        }

        AttendanceRuleConfig config = ruleConfigService.getOrCreateForCurrentTenant();
        AttendanceRulesEngine.WorkResult result = ruleEngine.compute(attendance.getCheckInTime(), checkOutTime, config);

        attendance.setCheckOutTime(checkOutTime);
        attendance.setCheckOutLatitude(request.getLatitude());
        attendance.setCheckOutLongitude(request.getLongitude());
        attendance.setGrossWorkMinutes(result.grossWorkMinutes);
        attendance.setBreakMinutes(result.breakMinutes);
        attendance.setNetWorkMinutes(result.netWorkMinutes);
        attendance.setLate(result.late);
        attendance.setLateMinutes(result.lateMinutes);
        attendance.setEarlyExit(result.earlyExit);
        attendance.setEarlyExitMinutes(result.earlyExitMinutes);
        attendance.setStatus(result.status);

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log(actorId, "CHECK_OUT",
                employee.getEmployeeCode() + " checked out at " + checkOutTime + " - net " + result.netWorkMinutes
                        + "m, status " + result.status, httpRequest);
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
        requireManualAttendanceFeatureEnabled();
        List<String> rejected = new ArrayList<>();
        int markedCount = 0;

        for (BulkAttendanceEntry entry : request.getEntries()) {
            try {
                markOne(tenantId, entry.getEmployeeId(), request.getAttendanceDate(),
                        entry.getStatus(), entry.getRemarks(), actorId, request.getLatitude(), request.getLongitude(),
                        entry.getCheckInTime(), entry.getCheckOutTime(), entry.getWorkMode());
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

    /** Shared core of mark() and bulkMark() - see class javadoc for the immutability guarantee this preserves either way. This overload is used ONLY by the internal system callers (holiday auto-marking, leave approval) - always attendanceSource=SYSTEM regardless of which admin happens to be the one who triggered the holiday/leave action, since the attendance itself wasn't a deliberate "mark this person's attendance" act, it's a side effect of an unrelated action. Never has a real device location or check-in/out times to report either - a holiday/leave day was never worked. */
    private Attendance markOne(Long tenantId, Long employeeId, LocalDate date, String status, String remarks, Long actorId) {
        return markOne(tenantId, employeeId, date, status, remarks, actorId, null, null, null, null, null, true);
    }

    private Attendance markOne(Long tenantId, Long employeeId, LocalDate date, String status, String remarks, Long actorId,
                                java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                                LocalDateTime checkInTime, LocalDateTime checkOutTime, String workMode) {
        return markOne(tenantId, employeeId, date, status, remarks, actorId, latitude, longitude,
                checkInTime, checkOutTime, workMode, false);
    }

    /**
     * Full implementation - Case A (spec): both checkInTime and checkOutTime supplied ->
     * computed via the exact same AttendanceRulesEngine a self check-out uses, and the given
     * `status` is IGNORED in favor of the computed one (PRESENT/HALF_DAY/ABSENT), because "the
     * source of attendance must not change the calculation logic" - a manual entry with real
     * times must produce the identical result a self check-out with those same times would.
     * Case B (spec): either time is null -> stored exactly as before, no fake timestamps, no
     * computed hours/late/early, `status` is used as given. attendanceSource/createdByRole are
     * SYSTEM/null when isSystemGenerated is true (holiday/leave auto-mark - see the 6-arg
     * overload above), otherwise derived from whichever admin-tier role actually performed the
     * action (see currentActorRoleLabel()) - SELF is only ever set by checkIn()/checkOut()/
     * markMine(), never from here.
     */
    private Attendance markOne(Long tenantId, Long employeeId, LocalDate date, String status, String remarks, Long actorId,
                                java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                                LocalDateTime checkInTime, LocalDateTime checkOutTime, String workMode,
                                boolean isSystemGenerated) {
        validateStatus(status);
        if (checkInTime != null && checkOutTime != null && !checkOutTime.isAfter(checkInTime)) {
            throw new BadRequestException("Check-out time cannot be before check-in time.");
        }

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

        if (isSystemGenerated) {
            attendance.setAttendanceSource("SYSTEM");
            attendance.setCreatedByRole(null);
        } else {
            String roleLabel = currentActorRoleLabel();
            String source = switch (roleLabel) {
                case "HR_ADMIN" -> "HR_MANUAL";
                case "SITE_SUPERVISOR" -> "SUPERVISOR_MANUAL";
                default -> "ADMIN_MANUAL";
            };
            attendance.setAttendanceSource(source);
            attendance.setCreatedByRole(roleLabel);
        }

        if (checkInTime != null && checkOutTime != null) {
            AttendanceRuleConfig config = ruleConfigService.getOrCreateForCurrentTenant();
            AttendanceRulesEngine.WorkResult result = ruleEngine.compute(checkInTime, checkOutTime, config);
            attendance.setCheckInTime(checkInTime);
            attendance.setCheckOutTime(checkOutTime);
            attendance.setWorkMode(workMode != null ? workMode : "OFFICE");
            attendance.setGrossWorkMinutes(result.grossWorkMinutes);
            attendance.setBreakMinutes(result.breakMinutes);
            attendance.setNetWorkMinutes(result.netWorkMinutes);
            attendance.setLate(result.late);
            attendance.setLateMinutes(result.lateMinutes);
            attendance.setEarlyExit(result.earlyExit);
            attendance.setEarlyExitMinutes(result.earlyExitMinutes);
            attendance.setStatus(result.status);
        } else if (checkInTime != null) {
            // Only a check-in was given (e.g. admin logging a forgotten checkout as still
            // pending) - store it, but never half-compute hours from just one side.
            attendance.setCheckInTime(checkInTime);
            attendance.setWorkMode(workMode != null ? workMode : "OFFICE");
        }
        // Case B (neither time given): checkInTime/checkOutTime/gross/break/net/late/early all
        // stay null, exactly as they were before this change - status is used exactly as given,
        // no fake timestamps, no invented working hours.

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
        LocalDateTime newCheckIn = request.getCheckInTime();
        LocalDateTime newCheckOut = request.getCheckOutTime();
        if (newCheckIn != null && newCheckOut != null && !newCheckOut.isAfter(newCheckIn)) {
            throw new BadRequestException("Check-out time cannot be before check-in time.");
        }

        Attendance attendance = attendanceRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Attendance record does not belong to the current tenant"));

        String previousStatus = attendance.getStatus();
        attendance.setRemarks(request.getRemarks());
        attendance.setUpdatedBy(actorId);
        attendance.setModifiedByRole(currentActorRoleLabel());
        attendance.setModificationReason(request.getModificationReason());

        // Supplying both times recomputes hours/late/early-exit/status through the SAME engine
        // a self check-out uses - a manual correction with real times must produce identical
        // results to what check-out itself would have produced, matching Case A of the spec.
        // Explicitly clearing status only (both times still null/unchanged) keeps the old
        // status-only edit path working exactly as before.
        if (newCheckIn != null && newCheckOut != null) {
            AttendanceRuleConfig config = ruleConfigService.getOrCreateForCurrentTenant();
            AttendanceRulesEngine.WorkResult result = ruleEngine.compute(newCheckIn, newCheckOut, config);
            attendance.setCheckInTime(newCheckIn);
            attendance.setCheckOutTime(newCheckOut);
            attendance.setGrossWorkMinutes(result.grossWorkMinutes);
            attendance.setBreakMinutes(result.breakMinutes);
            attendance.setNetWorkMinutes(result.netWorkMinutes);
            attendance.setLate(result.late);
            attendance.setLateMinutes(result.lateMinutes);
            attendance.setEarlyExit(result.earlyExit);
            attendance.setEarlyExitMinutes(result.earlyExitMinutes);
            attendance.setStatus(result.status);
        } else {
            attendance.setStatus(request.getStatus());
        }

        Attendance saved = attendanceRepository.save(attendance);

        auditService.log(actorId, "ATTENDANCE_UPDATED",
                "Changed attendance on " + attendance.getAttendanceDate() + " from " + previousStatus
                        + " to " + saved.getStatus() + " (employee id " + attendance.getEmployeeId() + ")"
                        + (request.getModificationReason() != null ? " - reason: " + request.getModificationReason() : ""),
                httpRequest);
        return toResponse(saved);
    }

    /** "My Attendance History" for employees who only have ATTENDANCE_SELF_MARK (not the
        broader ATTENDANCE_READ) - the admin-facing findForEmployeeInRange() above requires
        ATTENDANCE_READ and takes an arbitrary employeeId, which is exactly what a self-service
        employee should NOT be able to call (it would let them look up anyone). This resolves
        the employee from the CALLER's own login instead, same pattern as checkIn()/checkOut()/
        findMyTodayStatus() - there is no way to pass in someone else's id here at all. */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findMyHistoryInRange(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("Start date must be on or before end date");
        }
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));
        Long tenantId = tenantContext.requireCurrentTenantId();
        return attendanceRepository
                .findAllByClientCompanyIdAndEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(tenantId, employee.getId(), from, to)
                .stream().map(this::toResponse).toList();
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

    /**
     * Company-wide "who's in today" snapshot for the admin dashboard - not the same thing as
     * getMarkableEmployees() (which is the Mark Attendance screen's full list regardless of
     * check-in state); this is specifically the checked-in/not-yet-checked-in split, like the
     * "At Work / Away" columns in a typical team-presence widget. ON_LEAVE and legacy
     * (no-check-in-time) rows are counted separately rather than lumped into either bucket, so
     * "not checked in" genuinely means "might still show up today", not "confirmed absent".
     */
    @Transactional(readOnly = true)
    public TodayAttendanceOverviewResponse getTodayOverview() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        // This overview is specifically about check-in/check-out data (who's checked in today,
        // who hasn't) - if the company has EMPLOYEE_SELF_ATTENDANCE off, there's nothing
        // meaningful for it to show, same rule the dashboard's canSeeAttendanceOverview() applies
        // on the frontend - enforced here too so hiding the widget isn't the only thing stopping
        // a direct API call from still returning this data.
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee Self Attendance");
        LocalDate today = LocalDate.now();

        List<Employee> activeEmployees = employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE");
        Map<Long, Attendance> todaysAttendanceByEmployee = new HashMap<>();
        for (Attendance a : attendanceRepository.findAllByClientCompanyIdAndAttendanceDate(tenantId, today)) {
            todaysAttendanceByEmployee.put(a.getEmployeeId(), a);
        }

        List<TodayAttendanceOverviewResponse.Entry> checkedIn = new ArrayList<>();
        List<TodayAttendanceOverviewResponse.Entry> notCheckedIn = new ArrayList<>();
        long onLeaveCount = 0;

        for (Employee employee : activeEmployees) {
            Attendance attendanceToday = todaysAttendanceByEmployee.get(employee.getId());
            if (attendanceToday != null && "ON_LEAVE".equals(attendanceToday.getStatus())) {
                onLeaveCount++;
                continue;
            }
            TodayAttendanceOverviewResponse.Entry entry = new TodayAttendanceOverviewResponse.Entry();
            entry.setEmployeeId(employee.getId());
            entry.setEmployeeCode(employee.getEmployeeCode());
            entry.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

            if (attendanceToday != null && attendanceToday.getCheckInTime() != null) {
                entry.setCheckInTime(attendanceToday.getCheckInTime().toString());
                entry.setCheckOutTime(attendanceToday.getCheckOutTime() != null ? attendanceToday.getCheckOutTime().toString() : null);
                entry.setWorkMode(attendanceToday.getWorkMode());
                entry.setLate(attendanceToday.isLate());
                checkedIn.add(entry);
            } else {
                notCheckedIn.add(entry);
            }
        }

        TodayAttendanceOverviewResponse response = new TodayAttendanceOverviewResponse();
        response.setDate(today);
        response.setTotalActiveEmployees(activeEmployees.size());
        response.setCheckedInCount(checkedIn.size());
        response.setNotCheckedInCount(notCheckedIn.size());
        response.setOnLeaveCount(onLeaveCount);
        response.setCheckedIn(checkedIn);
        response.setNotCheckedIn(notCheckedIn);
        return response;
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
        r.setCheckInTime(a.getCheckInTime());
        r.setCheckOutTime(a.getCheckOutTime());
        r.setGrossWorkMinutes(a.getGrossWorkMinutes());
        r.setBreakMinutes(a.getBreakMinutes());
        r.setNetWorkMinutes(a.getNetWorkMinutes());
        r.setWorkMode(a.getWorkMode());
        r.setAttendanceSource(a.getAttendanceSource());
        r.setLate(a.isLate());
        r.setLateMinutes(a.getLateMinutes());
        r.setEarlyExit(a.isEarlyExit());
        r.setEarlyExitMinutes(a.getEarlyExitMinutes());
        r.setCreatedByRole(a.getCreatedByRole());
        r.setModifiedByRole(a.getModifiedByRole());
        r.setModificationReason(a.getModificationReason());
        return r;
    }
}
