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
    private final FormulaEvaluator formulaEvaluator;
    private final ReportSectionTreeResolver treeResolver;
    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportSectionRepository reportSectionRepository;

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
        var evaluated = formulaEvaluator.evaluate(sectionBalances, sections);

        // Build tree for frontend rendering
        FinancialReportTreeNodeDto root = treeResolver.buildTree(sections, (java.util.Map<String, java.math.BigDecimal>) evaluated);


        return new FinancialReportTreeResponseDto(
                reportDefinition.getCode(),
                request.from(),
                request.to(),
                root
        );
    }
}

