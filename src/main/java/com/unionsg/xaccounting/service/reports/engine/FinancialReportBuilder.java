package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeNodeDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinancialReportBuilder {

private final SectionAggregatorView sectionAggregatorView;
    private final FormulaEvaluator formulaEvaluator;
    private final com.unionsg.xaccounting.service.reports.engine.adapter.ReportSectionTreeResolverView treeResolverView;

    // Template data sources
    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportTemplateSectionRepository reportTemplateSectionRepository;
    private final ReportTemplateSectionAccountRepository reportTemplateSectionAccountRepository;

public FinancialReportTreeResponseDto build(FinancialReportEngineRequestDto request) {
        throw new UnsupportedOperationException("Use generateFromTemplate() in Phase 3");
    }

public FinancialReportTreeResponseDto buildFromTemplate(ReportTemplate template,
                                                             java.time.LocalDate fromDate,
                                                             java.time.LocalDate toDate) {

        if (template == null) {
            throw new IllegalArgumentException("ReportTemplate is required");
        }
        System.out.println("template name "+template);
        // Load template sections (visible only)
        List<com.unionsg.xaccounting.entity.reports.ReportTemplateSection> templateSections =
                reportTemplateSectionRepository.findByReportTemplateId(template.getId()).stream()
                        .filter(com.unionsg.xaccounting.entity.reports.ReportTemplateSection::isVisible)
                        .sorted(Comparator.comparingInt(com.unionsg.xaccounting.entity.reports.ReportTemplateSection::getDisplayOrder))
                        .toList();
        
        System.out.println("got the template sections");

        // Load section accounts mapped to the template sections
        java.util.Map<Long, List<com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount>> accountsBySectionId =
                new java.util.HashMap<>();

        for (var s : templateSections) {
            var accounts = reportTemplateSectionAccountRepository.findByReportTemplateSectionIdOrderByDisplayOrderAsc(s.getId());
            accountsBySectionId.put(s.getId(), accounts);
        }
         
        // Convert template -> section views
        List<com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView> sectionViews =
                com.unionsg.xaccounting.service.reports.engine.adapter.TemplateReportAdapter.adapt(
                        template,
                        templateSections,
                        accountsBySectionId
                );

        // Convert template section accounts -> assignment views for view aggregation
        List<com.unionsg.xaccounting.service.reports.engine.view.AccountAssignmentView> assignments = new java.util.ArrayList<>();
        for (var s : templateSections) {
            var accounts = accountsBySectionId.getOrDefault(s.getId(), List.of());
            var sectionCode = s.getSectionCode();
            for (var a : accounts) {
                var av = com.unionsg.xaccounting.service.reports.engine.view.TemplateReportSectionAdapter.adapt(a);
                if (av != null) {
                    assignments.add(av);
                }
            }
        }

        // Aggregate using view-based aggregation
        var balancesBySectionCode = sectionAggregatorView.aggregate(sectionViews, assignments, fromDate, toDate);

        // Evaluate formulas using the existing formula evaluator
        var evaluated = formulaEvaluator.evaluate(balancesBySectionCode, sectionViews);

        // Build tree using view resolver
        FinancialReportTreeNodeDto root = treeResolverView.buildTree(sectionViews,
                (java.util.Map<String, java.math.BigDecimal>) evaluated);

        return new FinancialReportTreeResponseDto(
                template.getTemplateCode(),
                fromDate,
                toDate,
                root
        );
    }
}


