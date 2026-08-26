package com.example.application.payroll_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.payroll_module.dto.PayrollSettingsRequest;
import com.example.application.payroll_module.entity.PayrollSettings;
import com.example.application.payroll_module.repository.PayrollSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Architecture refactor Phase 8: covers TEST 1-3 (month-based resolution
 * picks the configuration whose window actually covers that month, not
 * "today's") and TEST 10/11 (overlap prevention via the "at most one open
 * configuration" invariant, and a future configuration never affecting the
 * currently-open one before its own effective date).
 */
@ExtendWith(MockitoExtension.class)
class PayrollSettingsServiceTest {

    @Mock private PayrollSettingsRepository settingsRepository;
    @Mock private PayrollSettingsResolver settingsResolver;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private PayrollSettingsService service;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
    }

    private PayrollSettingsRequest request(BigDecimal epfPercent) {
        PayrollSettingsRequest r = new PayrollSettingsRequest();
        r.setEpfEnabled(true);
        r.setEpfEmployeePercent(epfPercent);
        r.setEpfEmployerPercent(new BigDecimal("13.00"));
        r.setEsiEnabled(false);
        r.setEsiEmployeePercent(BigDecimal.ZERO);
        r.setEsiEmployerPercent(BigDecimal.ZERO);
        r.setPtEnabled(false);
        r.setProfessionalTax(BigDecimal.ZERO);
        return r;
    }

    /** TEST 1-3: resolving August must use whichever configuration's window covers August, not today's date - this is PayrollSettingsResolver's job, exercised directly here rather than re-mocked. */
    @Test
    void resolverPicksConfigurationCoveringTheRequestedMonthNotToday() {
        PayrollSettingsRepository realRepo = mock(PayrollSettingsRepository.class);
        PayrollSettingsResolver resolver = new PayrollSettingsResolver(realRepo);

        PayrollSettings august = new PayrollSettings();
        august.setEpfEmployeePercent(new BigDecimal("12.00"));
        when(realRepo.findApplicableForDate(TENANT_ID, LocalDate.of(2026, 8, 1))).thenReturn(Optional.of(august));

        PayrollSettings resolved = resolver.resolve(TENANT_ID, 2026, 8);

        assertEquals(0, new BigDecimal("12.00").compareTo(resolved.getEpfEmployeePercent()));
        verify(realRepo).findApplicableForDate(TENANT_ID, LocalDate.of(2026, 8, 1));
    }

    /** A tenant with no configuration at all for the requested month falls back to documented defaults, never throws. */
    @Test
    void resolverFallsBackToDefaultsWhenNoConfigurationExists() {
        PayrollSettingsRepository realRepo = mock(PayrollSettingsRepository.class);
        PayrollSettingsResolver resolver = new PayrollSettingsResolver(realRepo);
        when(realRepo.findApplicableForDate(any(), any())).thenReturn(Optional.empty());

        PayrollSettings resolved = resolver.resolve(TENANT_ID, 2026, 8);

        assertEquals(0, new BigDecimal("12.00").compareTo(resolved.getEpfEmployeePercent())); // entity default
    }

    /** TEST 11: scheduling a new configuration closes the previously open-ended one the day before, rather than mutating its percentages - both rows remain, each with its own correct window. */
    @Test
    void schedulingFutureConfigClosesThePreviousOpenOne() {
        PayrollSettings current = new PayrollSettings();
        current.setId(10L);
        current.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        current.setStatus("ACTIVE");
        when(settingsRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(TENANT_ID, "ACTIVE"))
                .thenReturn(List.of(current));
        when(settingsRepository.save(any(PayrollSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollSettingsRequest request = request(new BigDecimal("10.00"));
        request.setEffectiveFrom(LocalDate.of(2026, 10, 1));

        service.createFutureConfig(request, 9L, null);

        assertEquals(LocalDate.of(2026, 9, 30), current.getEffectiveTo());
        verify(settingsRepository, times(2)).save(any(PayrollSettings.class)); // closes old row + inserts new row
    }

    /** TEST 10: a new configuration cannot start on or before the currently open configuration's own start date - that would be an overlap. */
    @Test
    void schedulingConfigOnOrBeforeCurrentEffectiveDateIsRejected() {
        PayrollSettings current = new PayrollSettings();
        current.setId(10L);
        current.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        current.setStatus("ACTIVE");
        when(settingsRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(TENANT_ID, "ACTIVE"))
                .thenReturn(List.of(current));

        PayrollSettingsRequest request = request(new BigDecimal("10.00"));
        request.setEffectiveFrom(LocalDate.of(2026, 8, 1)); // same date as current - overlap

        assertThrows(BadRequestException.class, () -> service.createFutureConfig(request, 9L, null));
        verify(settingsRepository, never()).save(any());
    }

    /** Percentages outside 0-100 must be rejected before anything is persisted. */
    @Test
    void invalidPercentageIsRejected() {
        PayrollSettingsRequest request = request(new BigDecimal("150.00"));
        request.setEffectiveFrom(LocalDate.of(2026, 10, 1));

        assertThrows(BadRequestException.class, () -> service.createFutureConfig(request, 9L, null));
        verify(settingsRepository, never()).save(any());
    }
}
