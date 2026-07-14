package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.reports.ReportDefinition;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportDefinitionRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * Seeds production-ready financial report metadata.
 * <p>
 * Idempotent by using report definition codes for existence checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMetadataSeeder implements ApplicationRunner {

    // Report definition codes
    private static final String TRIAL_BALANCE = "TRIAL_BALANCE";
    private static final String PROFIT_AND_LOSS = "PROFIT_AND_LOSS";
    private static final String BALANCE_SHEET = "BALANCE_SHEET";
    private static final String CASH_FLOW = "CASH_FLOW";

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportSectionRepository reportSectionRepository;
    private final ReportSectionAccountRepository reportSectionAccountRepository;

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // DISABLED (Phase 5 migration): legacy runtime seeding for ReportDefinition/ReportSection/ReportSectionAccount.
        // Keep code for rollback/reference, but prevent execution.
        // (No early return to avoid unreachable code compilation issues.)
        if (true) {
            return;
        }

        // Seed definitions + sections
        seedDefinitionIfMissing(TRIAL_BALANCE, "Trial Balance", "Trial balance report definition");
        seedDefinitionIfMissing(PROFIT_AND_LOSS, "Profit and Loss", "Profit and Loss report definition");
        seedDefinitionIfMissing(BALANCE_SHEET, "Balance Sheet", "Balance Sheet report definition");
        seedDefinitionIfMissing(CASH_FLOW, "Cash Flow", "Cash Flow report definition");

        // Now resolve IDs
        Map<String, ReportDefinition> defs = Map.of(
                TRIAL_BALANCE, reportDefinitionRepository.findByCode(TRIAL_BALANCE).orElseThrow(),
                PROFIT_AND_LOSS, reportDefinitionRepository.findByCode(PROFIT_AND_LOSS).orElseThrow(),
                BALANCE_SHEET, reportDefinitionRepository.findByCode(BALANCE_SHEET).orElseThrow(),
                CASH_FLOW, reportDefinitionRepository.findByCode(CASH_FLOW).orElseThrow()
        );

        // Seed sections & parent hierarchy using reportDefinitionId + parentSectionId.
        seedSectionsForTrialBalance(defs.get(TRIAL_BALANCE));
        seedSectionsForProfitAndLoss(defs.get(PROFIT_AND_LOSS));
        seedSectionsForBalanceSheet(defs.get(BALANCE_SHEET));
        seedSectionsForCashFlow(defs.get(CASH_FLOW));


        log.info("Report metadata seeding completed.");
    }

    private void seedDefinitionIfMissing(String code, String name, String description) {
        if (reportDefinitionRepository.existsByCode(code)) {
            return;
        }

        ReportDefinition def = ReportDefinition.builder()
                .code(code)
                .name(name)
                .description(description)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reportDefinitionRepository.save(def);
        log.info("Seeded report definition {}", code);
    }

    private void seedSectionsForTrialBalance(ReportDefinition def) {
        seedSections(def, List.of(


                // Top-level categories
                section("trial_assets", "Assets", 10, null, SectionType.SECTION, null),
                section("trial_liabilities", "Liabilities", 20, null, SectionType.SECTION, null),
                section("trial_equity", "Equity", 30, null, SectionType.SECTION, null),
                section("trial_income", "Income", 40, null, SectionType.SECTION, null),
                section("trial_expenses", "Expenses", 50, null, SectionType.SECTION, null)
        ), List.of(
                // Subsections under Assets/ Liabilities (placeholders without formula)
                section("trial_current_assets", "Assets", 11, "trial_assets", SectionType.SECTION, null),
                section("trial_current_liabilities", "Liabilities", 21, "trial_liabilities", SectionType.SECTION, null)
        ));
    }


    private void seedSectionsForProfitAndLoss(ReportDefinition def) {
        // Required list (using SECTION + SUBTOTAL/TOTAL styles)
        List<SeedSection> sections = List.of(
                section("pl_revenue", "Revenue", 10, null, SectionType.SECTION, null),
                section("pl_cost_of_sales", "Cost of Sales", 20, null, SectionType.SECTION, null),
                section("pl_gross_profit", "Gross Profit", 30, null, SectionType.SUBTOTAL, null),
                section("pl_other_income", "Other Income", 40, null, SectionType.SECTION, null),
                section("pl_operating_expenses", "Operating Expenses", 50, null, SectionType.SECTION, null),
                section("pl_operating_profit", "Operating Profit", 60, null, SectionType.SUBTOTAL, null),
                section("pl_finance_costs", "Finance Costs", 70, null, SectionType.SECTION, null),
                section("pl_profit_before_tax", "Profit Before Tax", 80, null, SectionType.SUBTOTAL, null),
                section("pl_tax", "Tax", 90, null, SectionType.SECTION, null),
                section("pl_net_profit", "Net Profit", 100, null, SectionType.TOTAL, null)
        );

        seedSections(def, sections, List.of());
    }

    private void seedSectionsForBalanceSheet(ReportDefinition def) {

        List<SeedSection> top = List.of(
                section("bs_assets", "Assets", 10, null, SectionType.SECTION, null),
                section("bs_current_assets", "Current Assets", 20, "bs_assets", SectionType.SECTION, null),
                section("bs_non_current_assets", "Non Current Assets", 30, "bs_assets", SectionType.SECTION, null),
                section("bs_total_assets", "Total Assets", 40, null, SectionType.SUBTOTAL, null),

                section("bs_liabilities", "Liabilities", 50, null, SectionType.SECTION, null),
                section("bs_current_liabilities", "Current Liabilities", 60, "bs_liabilities", SectionType.SECTION, null),
                section("bs_non_current_liabilities", "Non Current Liabilities", 70, "bs_liabilities", SectionType.SECTION, null),
                section("bs_total_liabilities", "Total Liabilities", 80, null, SectionType.SUBTOTAL, null),

                section("bs_equity", "Equity", 90, null, SectionType.SECTION, null),
                section("bs_retained_earnings", "Retained Earnings", 100, "bs_equity", SectionType.SECTION, null),
                section("bs_total_equity", "Total Equity", 110, null, SectionType.SUBTOTAL, null),

                section("bs_total_liabilities_and_equity", "Total Liabilities and Equity", 120, null, SectionType.TOTAL, null)
        );

        seedSections(def, top, List.of());
    }

    private void seedSectionsForCashFlow(ReportDefinition def) {
        List<SeedSection> top = List.of(
                section("cf_operating_activities", "Operating Activities", 10, null, SectionType.SECTION, null),
                section("cf_investing_activities", "Investing Activities", 20, null, SectionType.SECTION, null),
                section("cf_financing_activities", "Financing Activities", 30, null, SectionType.SECTION, null),
                section("cf_net_cash_movement", "Net Cash Movement", 40, null, SectionType.SUBTOTAL, null),
                section("cf_opening_cash", "Opening Cash", 50, null, SectionType.SECTION, null),
                section("cf_closing_cash", "Closing Cash", 60, null, SectionType.TOTAL, null)
        );

        seedSections(def, top, List.of());
    }

    /**
     * Seed sections in an idempotent way.
     *
     * Note: entity IDs are generated, so we seed by unique (reportDefinitionId + code).
     * If code uniqueness isn’t enforced in DB, we still guard by existence checks.
     */
    private void seedSections(ReportDefinition def, List<SeedSection> topSections, List<SeedSection> extraSections) {
        // Merge lists
        List<SeedSection> all = new java.util.ArrayList<>();
        all.addAll(topSections);
        all.addAll(extraSections);

        // Existing sections in this definition
        // Since ReportSectionRepository only has findByReportDefinitionId, we load all and match by code
        List<ReportSection> existing = reportSectionRepository.findByReportDefinitionId(def.getId());
        Map<String, ReportSection> existingByCode = existing.stream()
                .collect(java.util.stream.Collectors.toMap(ReportSection::getCode, s -> s, (a,b)->a));

        // Seed top-level first (no parent)
        // Then seed children referencing parent by code.
        // Pass 1: create any missing with parent already known or null.
        for (SeedSection ss : all.stream().sorted(Comparator.comparingInt(SeedSection::displayOrder)).toList()) {
            if (existingByCode.containsKey(ss.code)) {
                continue;
            }

            ReportSection parent = null;
            if (ss.parentCode != null) {
                parent = existingByCode.get(ss.parentCode);
                if (parent == null) {
                    // parent not created yet; attempt to find from DB
                    parent = reportSectionRepository.findByReportDefinitionId(def.getId()).stream()
                            .filter(s -> Objects.equals(s.getCode(), ss.parentCode))
                            .findFirst()
                            .orElse(null);
                }
            }

            ReportSection created = ReportSection.builder()
                    .reportDefinition(def)
                    .parentSection(parent)
                    .title(ss.title)
                    .code(ss.code)
                    .displayOrder(ss.displayOrder)
                    .sectionType(ss.sectionType)
                    .formula(ss.formula)
                    .active(true)
                    .build();

            created = reportSectionRepository.save(created);
            existingByCode.put(ss.code, created);
        }

        // Finally, seed report_section_accounts if formulas define accounts.
        // Currently, project doesn’t have formula parsing wired, so we don’t attach accounts here.
    }

    private static SeedSection section(String code,
                                        String title,
                                        int displayOrder,
                                        String parentCode,
                                        SectionType sectionType,
                                        String formula) {
        return new SeedSection(code, title, displayOrder, parentCode, sectionType, formula);
    }

    private static class SeedSection {

        private final String code;
        private final String title;
        private final int displayOrder;
        private final String parentCode;
        private final SectionType sectionType;
        private final String formula;

        private SeedSection(String code, String title, int displayOrder, String parentCode, SectionType sectionType, String formula) {
            this.code = code;
            this.title = title;
            this.displayOrder = displayOrder;
            this.parentCode = parentCode;
            this.sectionType = sectionType;
            this.formula = formula;
        }

        public int displayOrder() {
            return displayOrder;
        }
    }
}

