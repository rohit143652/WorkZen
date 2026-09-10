package com.example.application.subscription_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.subscription_module.entity.ClientSubscription;
import com.example.application.subscription_module.entity.SubscriptionHistory;
import com.example.application.subscription_module.repository.ClientSubscriptionRepository;
import com.example.application.subscription_module.repository.SubscriptionHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs once a day and automatically moves any subscription whose end date has passed into
 * EXPIRED - "auto deactivate after the end date" per the client's request. Deliberately sets
 * EXPIRED rather than inventing a separate "DEACTIVATED" status: EXPIRED already means exactly
 * this in the existing status model (see ClientSubscription.status javadoc / spec section 10) -
 * a company past its subscription window, access restricted per SubscriptionAccessPolicy,
 * distinct from SUSPENDED (an admin action) or CANCELLED (permanently ended by choice).
 *
 * Only touches subscriptions with a real end date (Enterprise/no-fixed-term subscriptions have
 * endDate=null and are correctly never auto-expired by this job) and skips anything already in
 * a terminal/restricted state (EXPIRED, SUSPENDED, CANCELLED) so re-running this daily never
 * re-processes the same row or overwrites a status an admin set deliberately.
 */
@Service
public class SubscriptionExpiryJob {

    private static final List<String> SKIP_STATUSES = List.of("EXPIRED", "SUSPENDED", "CANCELLED");

    private final ClientSubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final AuditService auditService;

    public SubscriptionExpiryJob(ClientSubscriptionRepository subscriptionRepository,
                                  SubscriptionHistoryRepository historyRepository, AuditService auditService) {
        this.subscriptionRepository = subscriptionRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
    }

    /** Runs once daily at 00:15 server time - late enough past midnight that "endDate < today" unambiguously means the day has fully passed everywhere the app cares about. */
    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void expireOverdueSubscriptions() {
        List<ClientSubscription> overdue = subscriptionRepository.findAllByEndDateBeforeAndStatusNotIn(LocalDate.now(), SKIP_STATUSES);
        for (ClientSubscription subscription : overdue) {
            String previousStatus = subscription.getStatus();
            subscription.setStatus("EXPIRED");
            subscriptionRepository.save(subscription);

            SubscriptionHistory h = new SubscriptionHistory();
            h.setClientCompanyId(subscription.getClientCompanyId());
            h.setPreviousStatus(previousStatus);
            h.setNewStatus("EXPIRED");
            h.setReason("Automatically expired - subscription end date (" + subscription.getEndDate() + ") has passed");
            historyRepository.save(h);

            auditService.log(null, "CLIENT_SUBSCRIPTION_AUTO_EXPIRED",
                    "Client company #" + subscription.getClientCompanyId() + "'s subscription auto-expired (end date "
                            + subscription.getEndDate() + " has passed)", null);
        }
    }
}
