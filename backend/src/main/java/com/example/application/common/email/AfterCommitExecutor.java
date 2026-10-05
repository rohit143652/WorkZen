package com.example.application.common.email;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Runs a task on a small background "mail" thread pool, but only AFTER the surrounding database
 * transaction has actually committed.
 *
 * Two problems this solves compared with sending the email inline:
 *  1. Speed - the request no longer waits for the mail server (an SMTP round-trip is typically
 *     1-4 s, far more when the server is slow), and no longer holds its database transaction
 *     (and a pooled DB connection) open for the whole SMTP exchange.
 *  2. Correctness - if the transaction later rolls back (e.g. a later step of "create employee"
 *     fails), the email is never sent. Sent inline, an invitation could go out for an employee
 *     that was never saved, with a link that can never work.
 *
 * With no transaction active (e.g. a plain unit test) the task is simply handed to the pool.
 *
 * The pool is owned by this class on purpose, NOT exposed as a Spring bean: Spring Boot's own
 * default task executor ("applicationTaskExecutor") is only created when NO other Executor bean
 * exists, so registering one here would silently switch off that application-wide default.
 *
 * Deliberately tiny - the work is I/O-bound waiting on a remote server, and email volume is a
 * handful per admin action. Two threads cover normal use; the queue absorbs a bulk import. If the
 * queue ever fills, CallerRunsPolicy makes the submitting request send the mail itself - slower
 * for that one request, but no email is silently dropped.
 */
@Component
public class AfterCommitExecutor {

    private final ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();

    @PostConstruct
    void start() {
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(4);
        pool.setQueueCapacity(200);
        pool.setThreadNamePrefix("mail-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // On shutdown, let mails already queued finish instead of discarding them mid-flight.
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(15);
        pool.initialize();
    }

    @PreDestroy
    void stop() {
        pool.shutdown();
    }

    public void run(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pool.execute(task);
                }
            });
        } else {
            pool.execute(task);
        }
    }
}
