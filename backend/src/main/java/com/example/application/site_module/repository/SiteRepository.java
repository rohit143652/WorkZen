package com.example.application.site_module.repository;

import com.example.application.site_module.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    Page<Site> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);
    List<Site> findAllByClientCompanyId(Long clientCompanyId);
    boolean existsByClientCompanyIdAndSiteCode(Long clientCompanyId, String siteCode);
    long countByClientCompanyId(Long clientCompanyId);

    /** Used by CodeGeneratorService to derive the next SITE code, scoped per tenant. */
    Optional<Site> findTopByClientCompanyIdAndSiteCodeStartingWithOrderBySiteCodeDesc(Long clientCompanyId, String prefix);
}
