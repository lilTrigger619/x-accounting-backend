package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    Optional<ReportTemplate> findByTemplateCode(String templateCode);

    boolean existsByTemplateCode(String templateCode);

    Optional<ReportTemplate> findTopByTemplateCodeAndStatusOrderByVersionDesc(
            String templateCode,
            com.unionsg.xaccounting.enums.ReportTemplateStatus status
    );

}


