package com.example.application.employee_module.dto;

import java.util.List;

public class ProfileCompletionResponse {
    private int completionPercentage;
    private boolean mandatoryComplete;
    private List<String> missingMandatoryFields;
    private List<String> incompleteSections;

    public int getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(int completionPercentage) { this.completionPercentage = completionPercentage; }
    public boolean isMandatoryComplete() { return mandatoryComplete; }
    public void setMandatoryComplete(boolean mandatoryComplete) { this.mandatoryComplete = mandatoryComplete; }
    public List<String> getMissingMandatoryFields() { return missingMandatoryFields; }
    public void setMissingMandatoryFields(List<String> missingMandatoryFields) { this.missingMandatoryFields = missingMandatoryFields; }
    public List<String> getIncompleteSections() { return incompleteSections; }
    public void setIncompleteSections(List<String> incompleteSections) { this.incompleteSections = incompleteSections; }
}
