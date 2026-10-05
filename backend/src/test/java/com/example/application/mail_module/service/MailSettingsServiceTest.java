package com.example.application.mail_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.mail_module.dto.MailSettingsOverview;
import com.example.application.mail_module.dto.MailSettingsOverviewRow;
import com.example.application.mail_module.dto.MailSettingsRequest;
import com.example.application.mail_module.dto.MailSettingsResponse;
import com.example.application.mail_module.entity.MailSettings;
import com.example.application.mail_module.repository.MailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailSettingsServiceTest {

    private static final Long ACME = 5L;
    private static final Long GLOBEX = 6L;

    @Mock private MailSettingsRepository repository;
    @Mock private AuditService auditService;
    @Mock private ClientCompanyRepository clientCompanyRepository;

    private MailSettingsService service;

    @BeforeEach
    void setUp() {
        service = newService();
        lenient().when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        lenient().when(repository.findByClientCompanyId(any())).thenReturn(Optional.empty());
        lenient().when(clientCompanyRepository.findById(ACME)).thenReturn(Optional.of(company(ACME, "Acme Ltd")));
        lenient().when(clientCompanyRepository.findById(GLOBEX)).thenReturn(Optional.of(company(GLOBEX, "Globex")));
    }

    private MailSettingsService newService() {
        MailSettingsService s = new MailSettingsService(repository, auditService, clientCompanyRepository);
        ReflectionTestUtils.setField(s, "envHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(s, "envPort", 587);
        ReflectionTestUtils.setField(s, "envUsername", "env@example.com");
        ReflectionTestUtils.setField(s, "envPassword", "env-password");
        ReflectionTestUtils.setField(s, "envFrom", "env@example.com");
        return s;
    }

    private static ClientCompany company(Long id, String name) {
        ClientCompany c = new ClientCompany();
        c.setId(id);
        c.setCompanyName(name);
        return c;
    }

    /** A saved sender. companyId null = the platform default. */
    private MailSettings row(Long companyId, String from, boolean enabled) {
        MailSettings m = new MailSettings();
        m.setClientCompanyId(companyId);
        m.setHost("smtp.example.com");
        m.setPort(587);
        m.setUsername(from);
        m.setFromAddress(from);
        m.setPassword("pw-for-" + from);
        m.setEnabled(enabled);
        return m;
    }

    private MailSettingsRequest request(String password) {
        return new MailSettingsRequest("smtp.gmail.com", 587, "me@gmail.com", "me@gmail.com", password, true);
    }

    // ================= which sender an email uses =================

    @Test
    void aCompanyWithItsOwnSenderSendsFromIt() {
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(row(ACME, "hr@acme.com", true)));
        when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.of(row(null, "platform@x.com", true)));

        EffectiveMailConfig cfg = service.resolveEffective(ACME);

        assertEquals("COMPANY", cfg.source());
        assertEquals("hr@acme.com", cfg.fromAddress());
        assertEquals("pw-for-hr@acme.com", cfg.password());
    }

    @Test
    void twoCompaniesNeverShareASender() {
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(row(ACME, "hr@acme.com", true)));
        when(repository.findByClientCompanyId(GLOBEX)).thenReturn(Optional.of(row(GLOBEX, "hr@globex.com", true)));

        assertEquals("hr@acme.com", service.resolveEffective(ACME).fromAddress());
        assertEquals("hr@globex.com", service.resolveEffective(GLOBEX).fromAddress());
    }

    @Test
    void aCompanyWithoutItsOwnSenderFallsBackToThePlatformDefault() {
        when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.of(row(null, "platform@x.com", true)));

        EffectiveMailConfig cfg = service.resolveEffective(ACME);

        assertEquals("PLATFORM", cfg.source());
        assertEquals("platform@x.com", cfg.fromAddress());
    }

    @Test
    void withNothingSavedAnywhereTheEnvironmentIsUsed() {
        EffectiveMailConfig cfg = service.resolveEffective(ACME);
        assertEquals("ENVIRONMENT", cfg.source());
        assertTrue(cfg.usable());
        assertEquals("env@example.com", cfg.username());
    }

    @Test
    void aDisabledCompanySenderIsSkippedInFavourOfThePlatformDefault() {
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(row(ACME, "hr@acme.com", false)));
        when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.of(row(null, "platform@x.com", true)));

        assertEquals("platform@x.com", service.resolveEffective(ACME).fromAddress());
    }

    @Test
    void aPasswordSavedByTheOldEncryptedVersionIsNotSentAsAPasswordAndAsksForReEntry() {
        MailSettings legacy = row(ACME, "hr@acme.com", true);
        legacy.setPassword("v1:AbCdEf:ZZZZZZZZ");
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(legacy));
        when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.of(row(null, "platform@x.com", true)));

        EffectiveMailConfig cfg = service.resolveEffective(ACME);

        // Fails closed: never falls through to ANOTHER sender's address, never sends the blob as a password.
        assertEquals("COMPANY", cfg.source());
        assertFalse(cfg.usable());
        assertTrue(cfg.problem().contains("Re-enter the password"));
        assertNull(cfg.password());
    }

    @Test
    void anIncompleteEnvironmentIsNotUsableAndNamesWhatIsMissing() {
        ReflectionTestUtils.setField(service, "envPassword", "");
        EffectiveMailConfig cfg = service.resolveEffective(ACME);
        assertFalse(cfg.usable());
        assertTrue(cfg.problem().contains("MAIL_PASSWORD"));
        assertFalse(cfg.problem().contains("MAIL_USERNAME"));
    }

    // ================= saving =================

    @Test
    void savingForACompanyStoresItAgainstThatCompanyWithTheUsernameAndPasswordAsEntered() {
        service.save(ACME, request("my-s3cret-pw"), 1L, null);

        ArgumentCaptor<MailSettings> saved = ArgumentCaptor.forClass(MailSettings.class);
        verify(repository).save(saved.capture());
        assertEquals(ACME, saved.getValue().getClientCompanyId());
        assertEquals("me@gmail.com", saved.getValue().getUsername());
        assertEquals("my-s3cret-pw", saved.getValue().getPassword());
    }

    @Test
    void whatWasSavedIsExactlyWhatIsReadBackForSending() {
        service.save(ACME, request("my-s3cret-pw"), 1L, null);
        ArgumentCaptor<MailSettings> saved = ArgumentCaptor.forClass(MailSettings.class);
        verify(repository).save(saved.capture());
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(saved.getValue()));

        EffectiveMailConfig cfg = service.resolveEffective(ACME);

        assertTrue(cfg.usable());
        assertEquals("me@gmail.com", cfg.username());
        assertEquals("my-s3cret-pw", cfg.password());
    }

    @Test
    void savingWithNoCompanyStoresThePlatformDefault() {
        service.save(null, request("my-s3cret-pw"), 1L, null);

        ArgumentCaptor<MailSettings> saved = ArgumentCaptor.forClass(MailSettings.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getClientCompanyId());
    }

    @Test
    void savingForAnUnknownCompanyIsRejectedNotSilentlyStoredAsTheDefault() {
        when(clientCompanyRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.save(999L, request("pw12345678"), 1L, null));
        verify(repository, never()).save(any());
    }

    @Test
    void theAuditLogNamesTheCompanyButNeverThePassword() {
        service.save(ACME, request("my-s3cret-pw"), 1L, null);

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(auditService).log(eq(1L), eq("MAIL_SETTINGS_UPDATED"), description.capture(), any());
        assertFalse(description.getValue().contains("my-s3cret-pw"));
        assertTrue(description.getValue().contains("Acme Ltd"));
        assertTrue(description.getValue().contains("password changed"));
    }

    @Test
    void theFirstSaveRequiresAPassword() {
        assertThrows(BadRequestException.class, () -> service.save(ACME, request(null), 1L, null));
        assertThrows(BadRequestException.class, () -> service.save(ACME, request("   "), 1L, null));
        verify(repository, never()).save(any());
    }

    @Test
    void aBlankPasswordOnUpdateKeepsTheSavedPassword() {
        MailSettings existing = row(ACME, "hr@acme.com", true);
        String before = existing.getPassword();
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(existing));

        service.save(ACME, request(null), 1L, null);

        assertEquals(before, existing.getPassword());
        assertEquals("me@gmail.com", existing.getUsername());
    }

    @Test
    void aGmailAppPasswordPastedWithSpacesIsCleanedOthersAreOnlyTrimmed() {
        assertEquals("abcdefghijklmnop", MailSettingsService.normalizePassword("smtp.gmail.com", "abcd efgh ijkl mnop"));
        assertEquals("a b", MailSettingsService.normalizePassword("mail.example.com", "  a b  "));
        assertNull(MailSettingsService.normalizePassword("smtp.gmail.com", "   "));
    }

    @Test
    void anInvalidPortOrFromAddressIsRejected() {
        assertThrows(BadRequestException.class, () ->
                service.save(ACME, new MailSettingsRequest("smtp.gmail.com", 0, "u@x.com", "u@x.com", "pw12345678", true), 1L, null));
        assertThrows(BadRequestException.class, () ->
                service.save(ACME, new MailSettingsRequest("smtp.gmail.com", 70000, "u@x.com", "u@x.com", "pw12345678", true), 1L, null));
        assertThrows(BadRequestException.class, () ->
                service.save(ACME, new MailSettingsRequest("smtp.gmail.com", 587, "u@x.com", "not-an-email", "pw12345678", true), 1L, null));
        assertThrows(BadRequestException.class, () ->
                service.save(ACME, new MailSettingsRequest("", 587, "u@x.com", "u@x.com", "pw12345678", true), 1L, null));
        verify(repository, never()).save(any());
    }

    // ================= what the API can reveal / deleting / overview =================

    @Test
    void theResponseCanNeverCarryAPassword() {
        boolean hasPasswordComponent = Arrays.stream(MailSettingsResponse.class.getRecordComponents())
                .anyMatch(c -> c.getName().toLowerCase().contains("password") && !c.getName().equals("passwordSet"));
        assertFalse(hasPasswordComponent, "MailSettingsResponse must only expose passwordSet, never a password");
    }

    @Test
    void deletingACompanySenderSendsThatCompanyBackToThePlatformDefault() {
        MailSettings existing = row(ACME, "hr@acme.com", true);
        MailSettings platform = row(null, "platform@x.com", true);
        when(repository.findByClientCompanyId(ACME)).thenReturn(Optional.of(existing), Optional.empty());
        when(repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()).thenReturn(Optional.of(platform));

        MailSettingsResponse after = service.delete(ACME, 1L, null);

        verify(repository).delete(existing);
        verify(auditService).log(eq(1L), eq("MAIL_SETTINGS_DELETED"), contains("Acme Ltd"), any());
        assertEquals("PLATFORM", after.effectiveSource());
        assertEquals("platform@x.com", after.effectiveFromAddress());
    }

    @Test
    void theOverviewShowsEachCompanyWhichSenderItUses() {
        when(repository.findAll()).thenReturn(List.of(row(null, "platform@x.com", true), row(ACME, "hr@acme.com", true)));
        when(clientCompanyRepository.findAll()).thenReturn(List.of(company(GLOBEX, "Globex"), company(ACME, "Acme Ltd")));

        MailSettingsOverview overview = service.overview();

        assertEquals(List.of("Acme Ltd", "Globex"), overview.companies().stream().map(MailSettingsOverviewRow::companyName).toList());
        MailSettingsOverviewRow acme = overview.companies().get(0);
        assertTrue(acme.hasOwnSettings());
        assertEquals("COMPANY", acme.effectiveSource());
        assertEquals("hr@acme.com", acme.effectiveFromAddress());
        MailSettingsOverviewRow globex = overview.companies().get(1);
        assertFalse(globex.hasOwnSettings());
        assertEquals("PLATFORM", globex.effectiveSource());
        assertEquals("platform@x.com", globex.effectiveFromAddress());
        assertTrue(overview.platform().hasOwnSettings());
    }
}
