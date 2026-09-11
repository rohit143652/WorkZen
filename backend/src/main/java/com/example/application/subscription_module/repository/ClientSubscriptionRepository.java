package com.example.application.subscription_module.repository;

import com.example.application.subscription_module.entity.ClientSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientSubscriptionRepository extends JpaRepository<ClientSubscription, Long> {
    Optional<ClientSubscription> findByClientCompanyId(Long clientCompanyId);
    List<ClientSubscription> findAllByPlanId(Long planId);
    long countByPlanId(Long planId);

    /** For the daily auto-expiry job - anything with a past end date that isn't already EXPIRED/SUSPENDED/CANCELLED (a terminal or already-restricted state doesn't need re-processing). */
    List<ClientSubscription> findAllByEndDateBeforeAndStatusNotIn(java.time.LocalDate date, List<String> excludedStatuses);

    /** For the Super Admin dashboard's "expiring soon" widget - anything ending within the given window that isn't already terminal (EXPIRED/SUSPENDED/CANCELLED - those are either already over or already flagged, not "coming up"). */
    List<ClientSubscription> findAllByEndDateBetweenAndStatusNotInOrderByEndDateAsc(
            java.time.LocalDate from, java.time.LocalDate to, List<String> excludedStatuses);
}
