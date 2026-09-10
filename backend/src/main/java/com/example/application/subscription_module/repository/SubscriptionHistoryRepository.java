package com.example.application.subscription_module.repository;

import com.example.application.subscription_module.entity.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {
    List<SubscriptionHistory> findAllByClientCompanyIdOrderByChangeDateDesc(Long clientCompanyId);
}
