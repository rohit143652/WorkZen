package com.example.application.employee_module.service;

import com.example.application.common.time.AppTime;
import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.email.EmailService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.dto.OnboardingSetPasswordRequest;
import com.example.application.employee_module.dto.OnboardingValidateResponse;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.entity.EmployeeOnboardingInvitation;
import com.example.application.employee_module.repository.EmployeeOnboardingInvitationRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers spec scenarios 3 (validate token), 4 (wrong code), 5 (correct code -> password set),
 * 6 (used invitation cannot be reused), 7 (expired invitation), 8 (resend invalidates old
 * invitation), and 14 (cannot access/resend another tenant's invitation).
 *
 * Uses a REAL BCryptPasswordEncoder (not mocked) since the verification-code check specifically
 * exercises passwordEncoder.matches() against an actual bcrypt hash - mocking it would test
 * nothing real about the one piece of logic these scenarios care about most.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeOnboardingServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final String RAW_CODE = "654321";
    private static final String TOKEN = "test-token-abc123";

    @Mock private EmployeeOnboardingInvitationRepository invitationRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientCompanyRepository clientCompanyRepository;
    @Mock private EmailService emailService;
    @Mock private com.example.application.common.email.EmailTemplateService emailTemplateService;
    @Mock private AuditService auditService;
    @Mock private TenantContextService tenantContext;
    @Mock private com.example.application.common.email.AfterCommitExecutor afterCommitExecutor;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private EmployeeOnboardingService service;

    private Employee employee;
    private User user;

    @BeforeEach
    void setUp() {
        service = new EmployeeOnboardingService(invitationRepository, employeeRepository, userRepository,
                clientCompanyRepository, passwordEncoder, emailService, emailTemplateService, auditService, tenantContext, afterCommitExecutor);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(service, "invitationExpiryHours", 24L);
        ReflectionTestUtils.setField(service, "maxCodeAttempts", 5);
        ReflectionTestUtils.setField(service, "passwordMinLength", 8);
        // A bare mock returns null, and Mockito's anyString() does NOT match null - so without this
        // every verify(emailService).sendHtml(..., anyString(), anyString()) could never succeed.
        lenient().when(emailTemplateService.render(anyString(), anyMap())).thenReturn("<html>rendered</html>");

        employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        employee.setClientCompanyId(TENANT_ID);
        employee.setFirstName("Asha");
        employee.setLastName("Patil");
        employee.setEmployeeCode("EMP0010");
        employee.setEmail("asha@example.com");
        employee.setOnboardingStatus("INVITED");

        user = new User();
        user.setId(USER_ID);
        user.setActive(false);
        user.setUsername("asha.patil");
    }

    private EmployeeOnboardingInvitation invitationWith(String status, LocalDateTime expiresAt, int attemptCount) {
        EmployeeOnboardingInvitation inv = new EmployeeOnboardingInvitation();
        inv.setId(99L);
        inv.setClientCompanyId(TENANT_ID);
        inv.setEmployeeId(EMPLOYEE_ID);
        inv.setUserId(USER_ID);
        inv.setTokenHash(sha256(TOKEN));
        inv.setVerificationCodeHash(passwordEncoder.encode(RAW_CODE));
        inv.setExpiresAt(expiresAt);
        inv.setStatus(status);
        inv.setAttemptCount(attemptCount);
        return inv;
    }

    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.Base64.getEncoder().encodeToString(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** SCENARIO 3: a valid, non-expired invitation returns safe employee/company info. */
    @Test
    void validateTokenReturnsEmployeeAndCompanyInfoForAValidInvitation() {
        EmployeeOnboardingInvitation invitation = invitationWith("PENDING", AppTime.now().plusHours(20), 0);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        ClientCompany company = new ClientCompany();
        company.setCompanyName("Acme Corp");
        when(clientCompanyRepository.findById(TENANT_ID)).thenReturn(Optional.of(company));

        OnboardingValidateResponse response = service.validateToken(TOKEN);

        assertEquals("Asha Patil", response.getEmployeeName());
        assertEquals("EMP0010", response.getEmployeeCode());
        assertEquals("Acme Corp", response.getCompanyName());
        assertFalse(response.isExpired());
        assertFalse(response.isAlreadyUsed());
    }

    /** SCENARIO 4: wrong verification code is rejected and does not set a password. */
    @Test
    void verifyCodeRejectsWrongCode() {
        EmployeeOnboardingInvitation invitation = invitationWith("PENDING", AppTime.now().plusHours(20), 0);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.verifyCode(TOKEN, "000000"));
        assertEquals("Incorrect verification code.", ex.getMessage());
        ArgumentCaptor<EmployeeOnboardingInvitation> captor = ArgumentCaptor.forClass(EmployeeOnboardingInvitation.class);
        verify(invitationRepository, atLeastOnce()).save(captor.capture());
        assertEquals(1, captor.getValue().getAttemptCount());
        verify(userRepository, never()).save(any());
    }

    /** SCENARIO 5: correct code lets the employee set their own password successfully. */
    @Test
    void setPasswordSucceedsWithCorrectCodeAndActivatesTheAccount() {
        EmployeeOnboardingInvitation invitation = invitationWith("PENDING", AppTime.now().plusHours(20), 0);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        OnboardingSetPasswordRequest request = new OnboardingSetPasswordRequest();
        request.setToken(TOKEN);
        request.setVerificationCode(RAW_CODE);
        request.setPassword("BrandNewPass123");
        request.setConfirmPassword("BrandNewPass123");

        service.setPassword(request);

        assertTrue(user.isActive());
        assertNotNull(user.getPassword());
        assertTrue(passwordEncoder.matches("BrandNewPass123", user.getPassword()));
        assertEquals("USED", invitation.getStatus());
        assertNotNull(invitation.getUsedAt());
        assertEquals("PROFILE_IN_PROGRESS", employee.getOnboardingStatus());
    }

    /** SCENARIO 6: an already-USED invitation can never be reused, even with the right code. */
    @Test
    void setPasswordRejectsAnAlreadyUsedInvitation() {
        EmployeeOnboardingInvitation invitation = invitationWith("USED", AppTime.now().plusHours(20), 0);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));

        OnboardingSetPasswordRequest request = new OnboardingSetPasswordRequest();
        request.setToken(TOKEN);
        request.setVerificationCode(RAW_CODE);
        request.setPassword("BrandNewPass123");
        request.setConfirmPassword("BrandNewPass123");

        assertThrows(BadRequestException.class, () -> service.setPassword(request));
        verify(userRepository, never()).save(any());
    }

    /** SCENARIO 7: an expired invitation cannot be used to set a password, and gets marked EXPIRED. */
    @Test
    void setPasswordRejectsAnExpiredInvitation() {
        EmployeeOnboardingInvitation invitation = invitationWith("PENDING", AppTime.now().minusHours(1), 0);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));

        OnboardingSetPasswordRequest request = new OnboardingSetPasswordRequest();
        request.setToken(TOKEN);
        request.setVerificationCode(RAW_CODE);
        request.setPassword("BrandNewPass123");
        request.setConfirmPassword("BrandNewPass123");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.setPassword(request));
        assertTrue(ex.getMessage().contains("expired"));
        assertEquals("EXPIRED", invitation.getStatus());
        verify(userRepository, never()).save(any());
    }

    /** SCENARIO 8: resending supersedes the old invitation and issues a fresh one - the old one stops working. */
    @Test
    void resendInvitationSupersedesThePreviousOneAndSendsANewEmail() {
        EmployeeOnboardingInvitation oldInvitation = invitationWith("PENDING", AppTime.now().plusHours(20), 2);
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT_ID)).thenReturn(Optional.of(employee));
        employee.setUser(user);
        when(invitationRepository.findAllByEmployeeIdAndStatus(EMPLOYEE_ID, "PENDING")).thenReturn(List.of(oldInvitation));
        when(invitationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID)).thenReturn(Optional.of(oldInvitation));
        when(clientCompanyRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        service.resendInvitation(EMPLOYEE_ID, 1L, null);

        assertEquals("SUPERSEDED", oldInvitation.getStatus());
        verify(emailService, times(1)).sendHtml(eq(TENANT_ID), eq("asha@example.com"), anyString(), anyString());
        verify(invitationRepository, atLeast(2)).save(any(EmployeeOnboardingInvitation.class));
    }

    /** SCENARIO 14: cannot resend an invitation for an employee belonging to a DIFFERENT tenant. */
    @Test
    void resendInvitationRejectsAnEmployeeFromAnotherTenant() {
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resendInvitation(EMPLOYEE_ID, 1L, null));
        verify(invitationRepository, never()).save(any());
        verify(emailService, never()).sendHtml(any(), anyString(), anyString(), anyString());
    }

    /** A LOCKED invitation (too many failed attempts) is refused even with the correct code. */
    @Test
    void verifyCodeRejectsALockedInvitationEvenWithCorrectCode() {
        EmployeeOnboardingInvitation invitation = invitationWith("PENDING", AppTime.now().plusHours(20), 5);
        when(invitationRepository.findAllByTokenHash(anyString())).thenReturn(List.of(invitation));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.verifyCode(TOKEN, RAW_CODE));
        assertTrue(ex.getMessage().contains("Too many"));
        assertEquals("LOCKED", invitation.getStatus());
    }

    /** Saving must NOT wait on the mail server: the invitation is created and the email only QUEUED for after commit. */
    @Test
    void createInvitationQueuesTheEmailInsteadOfSendingItInsideTheRequest() {
        String status = service.createInvitation(employee, user, 1L, null);

        assertEquals("PENDING", status);
        verify(emailService, never()).sendHtml(any(), anyString(), anyString(), anyString());
        verify(afterCommitExecutor, times(1)).run(any(Runnable.class));
        verify(invitationRepository, atLeast(1)).save(any(EmployeeOnboardingInvitation.class));
    }

    /** The employee needs the login name to sign in after setting a password - it must be in the email. */
    @Test
    void theInvitationEmailContainsTheUsername() {
        service.createInvitation(employee, user, 1L, null);

        verify(emailTemplateService).render(eq("employee-invitation"),
                argThat((java.util.Map<String, String> vars) -> "asha.patil".equals(vars.get("username"))));
    }

    /** The queued task is what really sends, and it records SENT on the invitation. */
    @Test
    void theQueuedTaskSendsTheEmailAndRecordsSent() {
        when(emailService.sendHtml(any(), anyString(), anyString(), anyString())).thenReturn(true);
        service.createInvitation(employee, user, 1L, null);
        org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor).run(task.capture());

        task.getValue().run();

        verify(emailService, times(1)).sendHtml(eq(TENANT_ID), eq("asha@example.com"), anyString(), anyString());
        verify(invitationRepository).updateEmailStatus(any(), eq("SENT"), any());
    }

    /** A failed background send must be RECORDED as FAILED - the admin can only act on it if it is visible. */
    @Test
    void aFailedBackgroundSendIsRecordedAsFailedNotLost() {
        when(emailService.sendHtml(any(), anyString(), anyString(), anyString())).thenReturn(false);
        service.createInvitation(employee, user, 1L, null);
        org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor).run(task.capture());

        task.getValue().run();

        verify(invitationRepository).updateEmailStatus(any(), eq("FAILED"), isNull());
    }

    /** Even an unexpected exception in the mail thread must end as FAILED, never leave the row PENDING forever. */
    @Test
    void anUnexpectedErrorInTheMailThreadIsStillRecordedAsFailed() {
        when(emailService.sendHtml(any(), anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("boom"));
        service.createInvitation(employee, user, 1L, null);
        org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor).run(task.capture());

        assertDoesNotThrow(() -> task.getValue().run());

        verify(invitationRepository).updateEmailStatus(any(), eq("FAILED"), isNull());
    }

    /** Resend is an explicit admin click: it WAITS for the mail server and reports the real outcome, never queues. */
    @Test
    void resendWaitsForTheMailServerAndReportsTheRealOutcome() {
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT_ID)).thenReturn(Optional.of(employee));
        employee.setUser(user);
        when(emailService.sendHtml(any(), anyString(), anyString(), anyString())).thenReturn(true);

        assertTrue(service.resendInvitation(EMPLOYEE_ID, 1L, null));
        verify(afterCommitExecutor, never()).run(any(Runnable.class));
    }

    /** PENDING for over 5 minutes means the mail thread never finished (e.g. app restarted) - report FAILED so the admin resends. */
    @Test
    void anInvitationStuckPendingForOverFiveMinutesIsReportedAsFailed() {
        EmployeeOnboardingInvitation inv = invitationWith("PENDING", AppTime.now().plusHours(20), 0);
        inv.setEmailStatus("PENDING");
        when(invitationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID)).thenReturn(Optional.of(inv));

        inv.setCreatedAt(AppTime.now().minusMinutes(6));
        assertEquals("FAILED", service.latestEmailStatus(EMPLOYEE_ID));

        inv.setCreatedAt(AppTime.now().minusSeconds(20));
        assertEquals("PENDING", service.latestEmailStatus(EMPLOYEE_ID));
    }

    /** Resend would only confuse someone who already activated (and regress their onboarding status) - refused. */
    @Test
    void resendIsRefusedForAnEmployeeWhoHasAlreadyActivatedTheirAccount() {
        user.setActive(true);
        employee.setUser(user);
        when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT_ID)).thenReturn(Optional.of(employee));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.resendInvitation(EMPLOYEE_ID, 1L, null));

        assertTrue(ex.getMessage().contains("already activated"));
        verify(emailService, never()).sendHtml(any(), anyString(), anyString(), anyString());
    }
}
