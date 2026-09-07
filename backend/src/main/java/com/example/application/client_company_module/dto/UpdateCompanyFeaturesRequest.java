package com.example.application.client_company_module.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public class UpdateCompanyFeaturesRequest {
    @NotEmpty(message = "At least one feature update is required")
    private Map<String, Boolean> features;

    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features; }
}
