package com.example.application.salary_structure_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.salary_structure_module.entity.SalaryComponent;
import com.example.application.salary_structure_module.entity.SalaryStructureComponent;
import com.example.application.salary_structure_module.repository.EmployeeSalaryStructureRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureComponentRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Covers spec section 57's exact worked example (Basic 15000 + HRA 20% of
 * Basic + Conveyance 1500 + Special 1000 = Gross 20500) plus the
 * tenant-isolation guarantee (section 37/45) that every lookup is scoped
 * by the current tenant, never a bare findById.
 */
@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTest {

    @Mock private SalaryStructureRepository structureRepository;
    @Mock private SalaryStructureComponentRepository structureComponentRepository;
    @Mock private SalaryComponentService salaryComponentService;
    @Mock private EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private SalaryStructureService service;

    private static final Long TENANT_ID = 1L;
    private static final Long STRUCTURE_ID = 10L;

    private SalaryComponent basic;
    private SalaryComponent hra;
    private SalaryComponent conveyance;
    private SalaryComponent special;

    @BeforeEach
    void setUp() {
        basic = component(1L, "BASIC", "EARNING");
        hra = component(2L, "HRA", "EARNING");
        conveyance = component(3L, "CONVEYANCE", "EARNING");
        special = component(4L, "SPECIAL_ALLOWANCE", "EARNING");
    }

    private SalaryComponent component(Long id, String code, String type) {
        SalaryComponent c = new SalaryComponent();
        c.setId(id);
        c.setComponentCode(code);
        c.setComponentName(code);
        c.setComponentType(type);
        c.setActive(true);
        return c;
    }

    private SalaryStructureComponent lineItem(Long id, Long componentId, String calcType, BigDecimal amount, BigDecimal percentage) {
        SalaryStructureComponent sc = new SalaryStructureComponent();
        sc.setId(id);
        sc.setSalaryComponentId(componentId);
        sc.setCalculationType(calcType);
        sc.setAmount(amount);
        sc.setPercentage(percentage);
        sc.setActive(true);
        return sc;
    }

    /** Spec section 57: Basic=15000, HRA=20% of Basic (=3000), Conveyance=1500, Special=1000 -> Gross=20500. */
    @Test
    void calculatesGrossSalaryPerSpecWorkedExample() {
        List<SalaryStructureComponent> lineItems = List.of(
                lineItem(101L, basic.getId(), "FIXED", new BigDecimal("15000.00"), null),
                lineItem(102L, hra.getId(), "PERCENTAGE_OF_BASIC", null, new BigDecimal("20.00")),
                lineItem(103L, conveyance.getId(), "FIXED", new BigDecimal("1500.00"), null),
                lineItem(104L, special.getId(), "FIXED", new BigDecimal("1000.00"), null)
        );
        when(structureComponentRepository.findAllBySalaryStructureIdOrderByDisplayOrderAsc(STRUCTURE_ID)).thenReturn(lineItems);
        when(salaryComponentService.getEntityForCurrentTenant(basic.getId())).thenReturn(basic);
        when(salaryComponentService.getEntityForCurrentTenant(hra.getId())).thenReturn(hra);
        when(salaryComponentService.getEntityForCurrentTenant(conveyance.getId())).thenReturn(conveyance);
        when(salaryComponentService.getEntityForCurrentTenant(special.getId())).thenReturn(special);

        SalaryStructureService.CalculationResult result = service.calculateForStructure(STRUCTURE_ID);

        assertEquals(0, new BigDecimal("20500.00").compareTo(result.grossEarnings));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalDeductions));
        assertEquals(0, new BigDecimal("20500.00").compareTo(result.netSalary));

        BigDecimal resolvedHra = result.componentResponses.stream()
                .filter(r -> r.getSalaryComponentId().equals(hra.getId()))
                .findFirst().orElseThrow().getResolvedAmount();
        assertEquals(0, new BigDecimal("3000.00").compareTo(resolvedHra));
    }

    /** Spec section 37/45: a structure belonging to a different tenant must never be returned - not even by ID. */
    @Test
    void findByIdRejectsStructureFromAnotherTenant() {
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(structureRepository.findByIdAndClientCompanyId(STRUCTURE_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(TenantAccessDeniedException.class, () -> service.findById(STRUCTURE_ID));
        verify(structureRepository, never()).findById(anyLong());
    }
}
