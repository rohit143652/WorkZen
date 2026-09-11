package com.example.application.employee_module.service;

import com.example.application.employee_module.dto.ProfileCompletionResponse;
import com.example.application.employee_module.entity.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers spec scenarios 10 (partial completion returns the correct percentage), 11 (mandatory
 * complete but optional incomplete -> mandatoryComplete=true, percentage<100), and 12 (fully
 * complete -> percentage=100). No mocks needed - this service is pure calculation over an
 * Employee entity, exactly the "deterministic, centralized" design the spec asked for.
 */
class EmployeeProfileCompletionServiceTest {

    private final EmployeeProfileCompletionService service = new EmployeeProfileCompletionService();

    private Employee blankEmployee() {
        return new Employee();
    }

    /** An entirely empty profile: every mandatory field missing, 0% complete. */
    @Test
    void emptyProfileHasZeroPercentAndListsEveryMandatoryFieldMissing() {
        ProfileCompletionResponse result = service.calculate(blankEmployee());

        assertEquals(0, result.getCompletionPercentage());
        assertFalse(result.isMandatoryComplete());
        assertTrue(result.getMissingMandatoryFields().contains("Mobile Number"));
        assertTrue(result.getMissingMandatoryFields().contains("Date of Birth"));
        assertTrue(result.getMissingMandatoryFields().contains("Emergency Contact Name"));
    }

    /** SCENARIO 10: a partially completed profile (Personal + Contact done, nothing else) returns exactly the weight of those two sections. */
    @Test
    void partiallyCompletedProfileReturnsTheCorrectPercentage() {
        Employee e = blankEmployee();
        e.setDateOfBirth(LocalDate.of(1995, 5, 20));
        e.setGender("FEMALE");
        e.setMobileNumber("9876543210");

        ProfileCompletionResponse result = service.calculate(e);

        // Personal (20) + Contact (15) = 35, matching EmployeeProfileCompletionService's own weights.
        assertEquals(35, result.getCompletionPercentage());
        assertFalse(result.isMandatoryComplete());
        assertFalse(result.getMissingMandatoryFields().contains("Mobile Number"));
        assertTrue(result.getMissingMandatoryFields().contains("Address"));
    }

    /** SCENARIO 11: every MANDATORY field filled, but optional sections (Bank, Statutory, Photo) are not - mandatoryComplete=true, percentage stays below 100. */
    @Test
    void mandatoryCompleteButOptionalSectionsIncompleteYieldsBelow100Percent() {
        Employee e = fullyMandatoryEmployee();

        ProfileCompletionResponse result = service.calculate(e);

        assertTrue(result.isMandatoryComplete());
        assertTrue(result.getMissingMandatoryFields().isEmpty());
        assertTrue(result.getCompletionPercentage() < 100);
        assertTrue(result.getIncompleteSections().contains("BANK_DETAILS"));
        assertTrue(result.getIncompleteSections().contains("PROFILE_PHOTO"));
    }

    /** SCENARIO 12: every section (mandatory AND optional) filled -> exactly 100%. */
    @Test
    void fullyCompletedProfileReturnsExactly100Percent() {
        Employee e = fullyMandatoryEmployee();
        e.setBankAccountHolderName("Asha Patil");
        e.setBankAccountNumber("1234567890");
        e.setBankIfscCode("HDFC0001234");
        e.setUanNumber("UAN12345678901");
        e.setPhotoData("data:image/png;base64,abc123");

        ProfileCompletionResponse result = service.calculate(e);

        assertEquals(100, result.getCompletionPercentage());
        assertTrue(result.isMandatoryComplete());
        assertTrue(result.getIncompleteSections().isEmpty());
    }

    /** SCENARIO 13 (data-model side): admin-only fields have no place in this calculation at all - filling them can never move the percentage, confirming an employee editing only what they're allowed to still reaches 100%. */
    @Test
    void adminOnlyFieldsPlayNoRoleInTheCalculation() {
        Employee withoutAdminFields = fullyMandatoryEmployee();
        withoutAdminFields.setBankAccountHolderName("Asha Patil");
        withoutAdminFields.setBankAccountNumber("1234567890");
        withoutAdminFields.setBankIfscCode("HDFC0001234");
        withoutAdminFields.setUanNumber("UAN12345678901");
        withoutAdminFields.setPhotoData("data:image/png;base64,abc123");
        int percentWithoutAdminFields = service.calculate(withoutAdminFields).getCompletionPercentage();

        // Department/designation/salary are admin-only - setting them must not change the score.
        withoutAdminFields.setDepartment("Engineering");
        withoutAdminFields.setDesignation("Senior Engineer");
        int percentAfterAdminFieldsSet = service.calculate(withoutAdminFields).getCompletionPercentage();

        assertEquals(percentWithoutAdminFields, percentAfterAdminFieldsSet);
        assertEquals(100, percentAfterAdminFieldsSet);
    }

    private Employee fullyMandatoryEmployee() {
        Employee e = blankEmployee();
        e.setDateOfBirth(LocalDate.of(1995, 5, 20));
        e.setGender("FEMALE");
        e.setMobileNumber("9876543210");
        e.setAddress("123 MG Road");
        e.setCity("Pune");
        e.setState("Maharashtra");
        e.setPincode("411001");
        e.setEmergencyContactName("Rahul Patil");
        e.setEmergencyContactRelationship("Spouse");
        e.setEmergencyContactMobile("9123456780");
        return e;
    }
}
