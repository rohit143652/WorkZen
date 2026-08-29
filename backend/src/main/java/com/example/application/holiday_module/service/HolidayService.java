package com.example.application.holiday_module.service;

import com.example.application.attendance_module.service.AttendanceService;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.holiday_module.dto.HolidayRequest;
import com.example.application.holiday_module.dto.HolidayResponse;
import com.example.application.holiday_module.entity.Holiday;
import com.example.application.holiday_module.repository.HolidayRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Company Holiday Calendar - Client-Admin-only (HOLIDAY_READ/HOLIDAY_CREATE/HOLIDAY_DELETE are
 * granted only to CLIENT_ADMIN by default - see V85 migration). A holiday can span a range of
 * consecutive days (e.g. a 3-day festival) - adding one immediately auto-marks every currently
 * active employee PRESENT for EVERY date in that range (AttendanceService.
 * markPresentForHoliday(), called once per date). Payable Days then already includes it, since
 * PayrollInputResolver counts PRESENT attendance rows directly with no separate "is this a
 * holiday" concept needed.
 */
@Service
public class HolidayService {

    private static final int MAX_RANGE_DAYS = 31;

    private final HolidayRepository holidayRepository;
    private final AttendanceService attendanceService;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public HolidayService(HolidayRepository holidayRepository, AttendanceService attendanceService,
                           TenantContextService tenantContext, AuditService auditService) {
        this.holidayRepository = holidayRepository;
        this.attendanceService = attendanceService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> findAll() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        // employeesMarkedPresent is only meaningful right at creation time (a "here's what just
        // happened" summary) - it's not a persisted stat, so listing existing holidays doesn't
        // recompute or show it (0 here, distinct from the real count returned by create()).
        return holidayRepository.findAllByClientCompanyIdOrderByStartDateDesc(tenantId).stream()
                .map(h -> toResponse(h, 0))
                .collect(Collectors.toList());
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date.");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot add a holiday for a date in the past - existing holidays on past dates can still be viewed, but new ones can only be added from today onward.");
        }
        long spanDays = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (spanDays > MAX_RANGE_DAYS) {
            throw new BadRequestException("A single holiday can span at most " + MAX_RANGE_DAYS + " days.");
        }

        List<Holiday> overlapping = holidayRepository.findOverlapping(tenantId, request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            Holiday clash = overlapping.get(0);
            throw new DuplicateResourceException(
                    "This range overlaps with an existing holiday: \"" + clash.getName() + "\" (" + clash.getStartDate() + " to " + clash.getEndDate() + ")");
        }

        Holiday holiday = new Holiday();
        holiday.setClientCompanyId(tenantId);
        holiday.setStartDate(request.getStartDate());
        holiday.setEndDate(request.getEndDate());
        holiday.setName(request.getName());
        holiday.setDescription(request.getDescription());
        holiday.setCreatedBy(actorId);
        Holiday saved = holidayRepository.save(holiday);

        int totalMarked = 0;
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            totalMarked += attendanceService.markPresentForHoliday(tenantId, date, "Public Holiday - " + request.getName(), actorId);
        }

        auditService.log(actorId, "HOLIDAY_CREATED",
                "Added holiday \"" + saved.getName() + "\" (" + saved.getStartDate() + " to " + saved.getEndDate()
                        + ") - auto-marked " + totalMarked + " attendance record(s) Present", httpRequest);

        return toResponse(saved, totalMarked);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Holiday holiday = holidayRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
        holidayRepository.delete(holiday);
        auditService.log(actorId, "HOLIDAY_DELETED",
                "Removed holiday \"" + holiday.getName() + "\" (" + holiday.getStartDate() + " to " + holiday.getEndDate() + ")", httpRequest);
        // Deliberately does NOT un-mark the attendance records it created - those are now
        // ordinary attendance history, same as everything else marked "immutable once saved"
        // elsewhere in this app; removing the calendar entry only stops it affecting future runs.
    }

    private HolidayResponse toResponse(Holiday h, int markedCount) {
        return new HolidayResponse(h.getId(), h.getStartDate(), h.getEndDate(), h.getName(), h.getDescription(), markedCount);
    }
}
