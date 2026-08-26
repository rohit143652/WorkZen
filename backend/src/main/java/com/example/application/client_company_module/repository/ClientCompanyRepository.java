package com.example.application.client_company_module.repository;

import com.example.application.client_company_module.entity.ClientCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientCompanyRepository extends JpaRepository<ClientCompany, Long>, JpaSpecificationExecutor<ClientCompany> {
    boolean existsByCompanyCode(String companyCode);
    long countByStatus(String status);

    /** Used by CodeGeneratorService to derive the next COMPANY code (global sequence - tenants have no parent scope). */
    java.util.Optional<ClientCompany> findTopByCompanyCodeStartingWithOrderByCompanyCodeDesc(String prefix);
}
