package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateSectionRepository extends JpaRepository<ReportTemplateSection, Long> {

    List<ReportTemplateSection> findByReportTemplateId(Long reportTemplateId);

    Optional<ReportTemplateSection> findByReportTemplateIdAndSectionCode(Long reportTemplateId, String sectionCode);

    List<ReportTemplateSection> findByReportTemplateIdAndParentSectionId(Long reportTemplateId, Long parentSectionId);

    List<ReportTemplateSection> findByReportTemplateIdAndParentSectionIdIsNull(Long reportTemplateId);
}

