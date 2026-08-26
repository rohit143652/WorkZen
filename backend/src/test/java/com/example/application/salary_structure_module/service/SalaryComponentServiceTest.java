package com.example.application.salary_structure_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.salary_structure_module.dto.SalaryComponentRequest;
import com.example.application.salary_structure_module.repository.SalaryComponentRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Architecture refactor Phase 3: Salary Structure must represent Gross
 * Earnings only, so creating a new DEDUCTION or EMPLOYER_CONTRIBUTION
 * component (PF, ESI, PT, Tax, ...) must be rejected - those belong to
 * Payroll Settings + PayrollCalculationService instead (see TEST 9 of the
 * Phase 3 spec: "PF/ESI/PT SalaryComponent must not be used by
 * PayrollCalculationService" - this test covers the entry point that
 * would otherwise let a new one be created in the first place).
 */
@ExtendWith(MockitoExtension.class)
class SalaryComponentServiceTest {

    @Mock private SalaryComponentRepository componentRepository;
    @Mock private SalaryStructureComponentRepository structureComponentRepository;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private SalaryComponentService service;

    private static final Long TENANT_ID = 1L;

    @Test
    void createRejectsNewDeductionComponent() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);

        SalaryComponentRequest request = new SalaryComponentRequest();
        request.setComponentName("Loan Recovery");
        request.setComponentType("DEDUCTION");
        request.setCalculationType("FIXED");
        request.setValue(new BigDecimal("500.00"));

        assertThrows(BadRequestException.class, () -> service.create(request, 9L, null));
        verify(componentRepository, never()).save(any());
    }

    @Test
    void createRejectsNewEmployerContributionComponent() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);

        SalaryComponentRequest request = new SalaryComponentRequest();
        request.setComponentName("Employer PF");
        request.setComponentType("EMPLOYER_CONTRIBUTION");
        request.setCalculationType("PERCENTAGE_OF_BASIC");
        request.setPercentage(new BigDecimal("13.00"));

        assertThrows(BadRequestException.class, () -> service.create(request, 9L, null));
        verify(componentRepository, never()).save(any());
    }

    @Test
    void createAllowsNewEarningComponent() {
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(componentRepository.findTopByClientCompanyIdAndComponentCodeStartingWithOrderByComponentCodeDesc(TENANT_ID, "SC"))
                .thenReturn(java.util.Optional.empty());
        when(componentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryComponentRequest request = new SalaryComponentRequest();
        request.setComponentName("Special Allowance");
        request.setComponentType("EARNING");
        request.setCalculationType("FIXED");
        request.setValue(new BigDecimal("1000.00"));

        service.create(request, 9L, null);

        verify(componentRepository).save(any());
    }
}
