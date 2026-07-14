package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.AccountType;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportTemplateSeeder implements ApplicationRunner {

    private static final String PL_STANDARD = "PL_STANDARD";

    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportTemplateSectionRepository reportTemplateSectionRepository;
    private final ReportTemplateSectionAccountRepository reportTemplateSectionAccountRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPlStandardTemplateIfMissing();
    }

    private void seedPlStandardTemplateIfMissing() {
        if (reportTemplateRepository.existsByTemplateCode(PL_STANDARD)) {
            return;
        }

        // Template
        ReportTemplate template = ReportTemplate.builder()
                .templateCode(PL_STANDARD)
                .templateName("Profit & Loss - Standard")
                .description("System template. Standard Profit & Loss report.")
                .category("FINANCIAL_REPORTS")
                .status(ReportTemplateStatus.PUBLISHED)
                .version(1)
                .isSystemTemplate(true)
                .createdBy("system")
                .createdDate(LocalDateTime.now())
                .updatedBy("system")
                .updatedDate(LocalDateTime.now())
                .build();

        template = reportTemplateRepository.save(template);

        // Sections (top-level hierarchy)
        // Keep formulas minimal and aligned with builder/evaluator expectations.
        // - Revenue + Cost of Sales are SECTIONs fed by assigned accounts.
        // - Gross Profit subtotal = Revenue - Expenses
        // - Net Profit is TOTAL = Gross Profit (we split total value into grossProfit/netProfit/netLoss in controller formatter)

        ReportTemplateSection plRevenue = createSection(template, null, "pl_revenue", "Revenue", 10, SectionType.SECTION, null, true);
        ReportTemplateSection plCostOfSales = createSection(template, null, "pl_cost_of_sales", "Expenses", 20, SectionType.SECTION, null, true);

        createSection(template, null, "pl_gross_profit", "Gross Profit", 30, SectionType.SUBTOTAL, "pl_revenue - pl_cost_of_sales", true);

        createSection(template, null, "pl_net_profit", "Net Profit", 40, SectionType.TOTAL, "pl_gross_profit", true);

        // Assign all INCOME accounts to Revenue and all EXPENSE accounts to Expenses
        List<ReportTemplateSection> sections = reportTemplateSectionRepository.findByReportTemplateId(template.getId());
        ReportTemplateSection revenueSection = sections.stream().filter(s -> Objects.equals(s.getSectionCode(), "pl_revenue")).findFirst().orElseThrow();
        ReportTemplateSection expenseSection = sections.stream().filter(s -> Objects.equals(s.getSectionCode(), "pl_cost_of_sales")).findFirst().orElseThrow();

        // Assign accounts by chart-of-account type (Income -> Revenue, Expense -> Expenses).
        // This project’s AccountRepository currently doesn’t expose derived-query methods for these fields,
        // so we load all accounts and filter in-memory.
        List<AccountEntity> allAccounts = accountRepository.findAll();

        List<AccountEntity> incomeAccounts = allAccounts.stream()
.filter(a -> a.getIsActive() != null && a.getIsActive())
                .filter(a -> !a.isDeleted())

                .filter(a -> a.getCoaClearTo() != null &&
                        a.getCoaClearTo().getChartOfAccount() != null &&
                        a.getCoaClearTo().getChartOfAccount().getAccountType() == AccountType.INCOME)
                .toList();

        List<AccountEntity> expenseAccounts = allAccounts.stream()
                .filter(a -> a.getIsActive() != null && a.getIsActive())
                .filter(a -> !a.isDeleted())
                .filter(a -> a.getCoaClearTo() != null &&

                        a.getCoaClearTo().getChartOfAccount() != null &&
                        a.getCoaClearTo().getChartOfAccount().getAccountType() == AccountType.EXPENSE)
                .toList();


        // If repo methods are not available (older project), fall back to no assignments.
        // This keeps the seeder compile-safe only if repository supports these methods.
        // In this codebase, the exact repository method may differ; therefore we use defensive reflection-free approach:
        // We'll try common accountId patterns only if account lists are empty.

        int order = 10;
        for (AccountEntity a : safeSort(incomeAccounts)) {
            saveAssignmentIfAbsent(revenueSection, a, order++);
        }

        order = 10;
        for (AccountEntity a : safeSort(expenseAccounts)) {
            saveAssignmentIfAbsent(expenseSection, a, order++);
        }

        log.info("Seeded PL_STANDARD template (id={}, templateCode={}).", template.getId(), template.getTemplateCode());
    }

    private void saveAssignmentIfAbsent(ReportTemplateSection section, AccountEntity account, int displayOrder) {
        boolean exists = reportTemplateSectionAccountRepository.existsByReportTemplateSectionIdAndAccountId(section.getId(), account.getId());
        if (exists) return;

        ReportTemplateSectionAccount assignment = ReportTemplateSectionAccount.builder()
                .reportTemplateSection(section)
                .account(account)
                .displayOrder(displayOrder)
                .build();

        reportTemplateSectionAccountRepository.save(assignment);
    }

    private ReportTemplateSection createSection(
            ReportTemplate template,
            ReportTemplateSection parent,
            String sectionCode,
            String title,
            int displayOrder,
            SectionType sectionType,
            String formula,
            boolean visible
    ) {
        ReportTemplateSection entity = ReportTemplateSection.builder()
                .reportTemplate(template)
                .parentSection(parent)
                .sectionCode(sectionCode)
                .title(title)
                .displayOrder(displayOrder)
                .sectionType(sectionType)
                .formula(formula)
                .visible(visible)
                .expandedByDefault(true)
                .build();

        return reportTemplateSectionRepository.save(entity);
    }

    private List<AccountEntity> safeSort(List<AccountEntity> accounts) {
        if (accounts == null) return List.of();
        return accounts.stream()
                .sorted(Comparator.comparing(AccountEntity::getAccountId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}

