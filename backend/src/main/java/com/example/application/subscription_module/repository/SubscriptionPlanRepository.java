package com.example.application.subscription_module.repository;

import com.example.application.subscription_module.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByPlanCode(String planCode);
    boolean existsByPlanCode(String planCode);
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAsc();
    List<SubscriptionPlan> findAllByActiveTrueOrderByDisplayOrderAsc();
}
