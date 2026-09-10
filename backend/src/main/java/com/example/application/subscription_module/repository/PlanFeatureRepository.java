package com.example.application.subscription_module.repository;

import com.example.application.subscription_module.entity.PlanFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanFeatureRepository extends JpaRepository<PlanFeature, Long> {
    List<PlanFeature> findAllByPlanId(Long planId);
    Optional<PlanFeature> findByPlanIdAndFeatureCode(Long planId, String featureCode);
    void deleteAllByPlanId(Long planId);
}
