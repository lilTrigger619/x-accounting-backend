package com.unionsg.xaccounting.communication.repository;

import com.unionsg.xaccounting.communication.domain.MailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MailConfigurationRepository extends JpaRepository<MailConfiguration, Long> {

    Optional<MailConfiguration> findByCompanyIdAndActiveTrue(String companyId);

    Optional<MailConfiguration> findByActiveTrue();
}

