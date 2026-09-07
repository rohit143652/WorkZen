package com.example.application.client_company_module.dto;

import java.util.List;
import java.util.Map;

public class CompanyFeatureResponse {
    private Map<String, Boolean> features;
    private List<CategoryDto> categories;
    private List<String> enforcedCodes;

    public static class CategoryDto {
        private String label;
        private List<String> codes;

        public CategoryDto() {}
        public CategoryDto(String label, List<String> codes) {
            this.label = label;
            this.codes = codes;
        }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<String> getCodes() { return codes; }
        public void setCodes(List<String> codes) { this.codes = codes; }
    }

    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features; }
    public List<CategoryDto> getCategories() { return categories; }
    public void setCategories(List<CategoryDto> categories) { this.categories = categories; }
    public List<String> getEnforcedCodes() { return enforcedCodes; }
    public void setEnforcedCodes(List<String> enforcedCodes) { this.enforcedCodes = enforcedCodes; }
}
