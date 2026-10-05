package com.example.application.mail_module.repository;

import com.example.application.mail_module.entity.MailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailSettingsRepository extends JpaRepository<MailSettings, Long> {
    /** A client company's own sender (at most one - UNIQUE in V122). */
    Optional<MailSettings> findByClientCompanyId(Long clientCompanyId);

    /** The platform default: the row with no company. Kept to a single row by the service. */
    Optional<MailSettings> findFirstByClientCompanyIdIsNullOrderByIdAsc();
}
