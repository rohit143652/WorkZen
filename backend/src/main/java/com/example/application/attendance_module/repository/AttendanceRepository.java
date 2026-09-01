package com.example.application.attendance_module.repository;

import com.example.application.attendance_module.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** Used by "Mark My Attendance" (self-service) to check if today is already marked, so the UI can show the existing status instead of a mark button. */
    Optional<Attendance> findByClientCompanyIdAndEmployeeIdAndAttendanceDate(Long clientCompanyId, Long employeeId, LocalDate attendanceDate);

    boolean existsByClientCompanyIdAndEmployeeIdAndAttendanceDate(Long clientCompanyId, Long employeeId, LocalDate attendanceDate);

    List<Attendance> findAllByClientCompanyIdAndEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long clientCompanyId, Long employeeId, LocalDate from, LocalDate to);

    Page<Attendance> findAllByClientCompanyIdAndAttendanceDateBetween(
            Long clientCompanyId, LocalDate from, LocalDate to, Pageable pageable);

    Page<Attendance> findAllByClientCompanyIdAndSiteIdAndAttendanceDateBetween(
            Long clientCompanyId, Long siteId, LocalDate from, LocalDate to, Pageable pageable);

    /** All attendance rows already marked for a given day, so the "mark attendance" screen can show existing status per employee. */
    List<Attendance> findAllByClientCompanyIdAndAttendanceDate(Long clientCompanyId, LocalDate attendanceDate);

    /** Full (unpaged) month of attendance for the Monthly Attendance & Payment report - needs every row to aggregate per-employee, not a page at a time. */
    List<Attendance> findAllByClientCompanyIdAndAttendanceDateBetweenOrderByEmployeeIdAscAttendanceDateAsc(
            Long clientCompanyId, LocalDate from, LocalDate to);

    /** One employee's ON_LEAVE day count for a month - used to bound the Monthly Report table's manual paid-leave adjustment. */
    long countByClientCompanyIdAndEmployeeIdAndAttendanceDateBetweenAndStatus(
            Long clientCompanyId, Long employeeId, LocalDate from, LocalDate to, String status);
}
