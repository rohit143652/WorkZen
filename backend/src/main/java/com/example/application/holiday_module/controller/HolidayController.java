package com.example.application.holiday_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.holiday_module.dto.HolidayRequest;
import com.example.application.holiday_module.dto.HolidayResponse;
import com.example.application.holiday_module.service.HolidayService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Company Holiday Calendar - CLIENT_ADMIN only by default (see V85 migration for the permission grants). */
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOLIDAY_READ')")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", holidayService.findAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOLIDAY_CREATE')")
    public ResponseEntity<ApiResponse<HolidayResponse>> create(@Valid @RequestBody HolidayRequest request,
                                                                 @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                 HttpServletRequest httpRequest) {
        HolidayResponse created = holidayService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Holiday added - " + created.getEmployeesMarkedPresent() + " employee(s) auto-marked Present", created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HOLIDAY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        holidayService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Holiday removed"));
    }
}
