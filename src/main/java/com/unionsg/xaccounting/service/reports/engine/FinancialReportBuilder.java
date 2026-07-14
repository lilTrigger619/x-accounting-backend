package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeNodeDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportDefinition;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.repository.reports.ReportDefinitionRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinancialReportBuilder {

    private final SectionAggregator sectionAggregator;
    private final SectionAggregatorView sectionAggregatorView;
    private final FormulaEvaluator formulaEvaluator;
    private final ReportSectionTreeResolver treeResolver;
    private final com.unionsg.xaccounting.service.reports.engine.adapter.ReportSectionTreeResolverView treeResolverView;
    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportSectionRepository reportSectionRepository;

    // Template data sources
    private final com.unionsg.xaccounting.repository.reports.ReportTemplateRepository reportTemplateRepository;
    private final com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository reportTemplateSectionRepository;
    private final com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository reportTemplateSectionAccountRepository;

    public FinancialReportTreeResponseDto build(FinancialReportEngineRequestDto request) {
        ReportDefinition reportDefinition = reportDefinitionRepository.findByCode(request.reportCode())
                .orElseThrow(() -> new IllegalArgumentException("Report definition not found for code: " + request.reportCode()));

        List<ReportSection> sections = reportSectionRepository.findByReportDefinitionId(reportDefinition.getId()).stream()
                .filter(ReportSection::isActive)
                .sorted(Comparator.comparingInt(ReportSection::getDisplayOrder))
                .toList();

        // Aggregate leaf/assigned balances (DB aggregation)
        var sectionBalances = sectionAggregator.aggregate(reportDefinition.getId(), request.from(), request.to());

        // Evaluate formulas/subtotals/totals
        List<com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView> sectionViews = sections.stream()
                .map(com.unionsg.xaccounting.service.reports.engine.view.RuntimeReportSectionAdapter::adapt)
                .toList();

        var evaluated = formulaEvaluator.evaluate(sectionBalances, sectionViews);

        // Build tree for frontend rendering
        FinancialReportTreeNodeDto root = treeResolver.buildTree(sections, (java.util.Map<String, java.math.BigDecimal>) evaluated);

        return new FinancialReportTreeResponseDto(
                reportDefinition.getCode(),
                request.from(),
                request.to(),
                root
        );
    }

    public FinancialReportTreeResponseDto buildFromTemplate(com.unionsg.xaccounting.entity.reports.ReportTemplate template,
                                                             java.time.LocalDate fromDate,
                                                             java.time.LocalDate toDate) {

        if (template == null) {
            throw new IllegalArgumentException("ReportTemplate is required");
        }

        // Load template sections (visible only)
        List<com.unionsg.xaccounting.entity.reports.ReportTemplateSection> templateSections =
                reportTemplateSectionRepository.findByReportTemplateId(template.getId()).stream()
                        .filter(com.unionsg.xaccounting.entity.reports.ReportTemplateSection::isVisible)
                        .sorted(Comparator.comparingInt(com.unionsg.xaccounting.entity.reports.ReportTemplateSection::getDisplayOrder))
                        .toList();

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


