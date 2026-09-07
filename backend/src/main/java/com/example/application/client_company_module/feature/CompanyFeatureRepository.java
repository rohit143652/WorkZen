package com.example.application.client_company_module.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyFeatureRepository extends JpaRepository<CompanyFeature, Long> {
    List<CompanyFeature> findAllByClientCompanyId(Long clientCompanyId);
    Optional<CompanyFeature> findByClientCompanyIdAndFeatureCode(Long clientCompanyId, String featureCode);
}
