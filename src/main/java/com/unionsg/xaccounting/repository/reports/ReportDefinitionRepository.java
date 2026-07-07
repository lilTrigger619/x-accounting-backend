package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {

    Optional<ReportDefinition> findByCode(String code);

    boolean existsByCode(String code);
}

