package com.example.application.employee_module.service;

import com.example.application.employee_module.dto.ProfileCompletionResponse;
import com.example.application.employee_module.entity.Employee;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The ONE authoritative, server-side profile completion calculation - never duplicated in the
 * frontend or any other service (spec sections 19/32). Sections and weights below are chosen by
 * inspecting Employee's ACTUAL fields (see V104/V111 migrations for statutory/bank/emergency
 * contact additions) - nothing here is invented data that doesn't already exist on the entity.
 *
 * Mandatory vs percentage are deliberately separate concepts (spec section 21): a profile can be
 * mandatoryComplete=true (every MANDATORY field filled) while completionPercentage is still <100
 * (optional sections like Bank Details or Photo remain empty). Admin-only fields (department,
 * designation, salary structure, PF/ESI/PT config, joining date) are NEVER counted here - this is
 * purely about what the EMPLOYEE fills in about themselves during onboarding.
 */
@Service
public class EmployeeProfileCompletionService {

    /** Section weights sum to 100 - Personal/Contact/Address are mandatory-heavy (the core of "who is this person"), Emergency Contact is mandatory (safety-critical), Bank/Statutory/Photo are optional (nice to have, often filled in later). */
    private static final int WEIGHT_PERSONAL = 20;
    private static final int WEIGHT_CONTACT = 15;
    private static final int WEIGHT_ADDRESS = 15;
    private static final int WEIGHT_EMERGENCY_CONTACT = 20;
    private static final int WEIGHT_BANK_DETAILS = 15;
    private static final int WEIGHT_STATUTORY = 10;
    private static final int WEIGHT_PHOTO = 5;

    public ProfileCompletionResponse calculate(Employee e) {
        List<String> missingMandatory = new ArrayList<>();
        List<String> incompleteSections = new ArrayList<>();
        int earned = 0;

        boolean hasDob = e.getDateOfBirth() != null;
        boolean personalComplete = hasDob && isFilled(e.getGender());
        if (!hasDob) missingMandatory.add("Date of Birth");
        if (!isFilled(e.getGender())) missingMandatory.add("Gender");
        earned += sectionScore(personalComplete, WEIGHT_PERSONAL, "PERSONAL_INFORMATION", incompleteSections);

        boolean contactComplete = isFilled(e.getMobileNumber());
        if (!contactComplete) missingMandatory.add("Mobile Number");
        earned += sectionScore(contactComplete, WEIGHT_CONTACT, "CONTACT_INFORMATION", incompleteSections);

        boolean addressComplete = isFilled(e.getAddress()) && isFilled(e.getCity()) && isFilled(e.getState()) && isFilled(e.getPincode());
        if (!isFilled(e.getAddress())) missingMandatory.add("Address");
        if (!isFilled(e.getCity())) missingMandatory.add("City");
        if (!isFilled(e.getState())) missingMandatory.add("State");
        if (!isFilled(e.getPincode())) missingMandatory.add("Pincode");
        earned += sectionScore(addressComplete, WEIGHT_ADDRESS, "ADDRESS", incompleteSections);

        boolean emergencyComplete = isFilled(e.getEmergencyContactName()) && isFilled(e.getEmergencyContactRelationship())
                && isFilled(e.getEmergencyContactMobile());
        if (!isFilled(e.getEmergencyContactName())) missingMandatory.add("Emergency Contact Name");
        if (!isFilled(e.getEmergencyContactRelationship())) missingMandatory.add("Emergency Contact Relationship");
        if (!isFilled(e.getEmergencyContactMobile())) missingMandatory.add("Emergency Contact Mobile");
        earned += sectionScore(emergencyComplete, WEIGHT_EMERGENCY_CONTACT, "EMERGENCY_CONTACT", incompleteSections);

        boolean bankComplete = isFilled(e.getBankAccountHolderName()) && isFilled(e.getBankAccountNumber()) && isFilled(e.getBankIfscCode());
        earned += sectionScore(bankComplete, WEIGHT_BANK_DETAILS, "BANK_DETAILS", incompleteSections);

        boolean statutoryComplete = isFilled(e.getUanNumber()) || isFilled(e.getPfMemberId()) || isFilled(e.getEsicNumber());
        earned += sectionScore(statutoryComplete, WEIGHT_STATUTORY, "STATUTORY_INFORMATION", incompleteSections);

        boolean photoComplete = isFilled(e.getPhotoData());
        earned += sectionScore(photoComplete, WEIGHT_PHOTO, "PROFILE_PHOTO", incompleteSections);

        ProfileCompletionResponse response = new ProfileCompletionResponse();
        response.setCompletionPercentage(earned);
        response.setMandatoryComplete(missingMandatory.isEmpty());
        response.setMissingMandatoryFields(missingMandatory);
        response.setIncompleteSections(incompleteSections);
        return response;
    }

    /** Applies a computed onboarding status transition based on the completion result - kept here (not scattered across callers) so "what counts as complete enough to advance status" has one definition. Does NOT save the employee - callers decide when to persist. */
    public void applyOnboardingStatusTransition(Employee employee, ProfileCompletionResponse completion) {
        String current = employee.getOnboardingStatus();
        if ("NOT_STARTED".equals(current) || "INVITED".equals(current)) {
            return;
        }
        if (completion.getCompletionPercentage() >= 100) {
            employee.setOnboardingStatus("PROFILE_COMPLETE");
        } else if (completion.isMandatoryComplete()) {
            employee.setOnboardingStatus("MANDATORY_PROFILE_COMPLETE");
        } else {
            employee.setOnboardingStatus("PROFILE_IN_PROGRESS");
        }
    }

    private int sectionScore(boolean complete, int weight, String sectionCode, List<String> incompleteSections) {
        if (complete) return weight;
        incompleteSections.add(sectionCode);
        return 0;
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }
}
