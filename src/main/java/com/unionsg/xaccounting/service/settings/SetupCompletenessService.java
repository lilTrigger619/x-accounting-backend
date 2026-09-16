package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.AccountingMappingResponse;
import com.unionsg.xaccounting.dto.settings.SetupChecklistItem;
import com.unionsg.xaccounting.dto.settings.SetupCompletenessResponse;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.entity.settings.Organization;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.settings.MappingGroup;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.repository.DocumentNumberConfigRepository;
import com.unionsg.xaccounting.service.ConfigService;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * "Am I ready to operate?" (Settings & Setup §37). Rather than a dedicated Setup Wizard
 * duplicating every module's own data-entry screens, this checks what's actually configured
 * right now and links straight to the real settings screen for anything missing.
 */
@Service
@RequiredArgsConstructor
public class SetupCompletenessService {

    private final OrganizationService organizationService;
    private final FinancialYearRepository financialYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountRepository accountRepository;
    private final ConfigService configService;
    private final TaxCategoryRepository taxCategoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DocumentNumberConfigRepository documentNumberConfigRepository;
    private final DocumentTemplateRepository documentTemplateRepository;
    private final AccountingMappingService accountingMappingService;

    /**
     * Not read-only: {@link AccountingMappingService#list()} self-heals by inserting a default
     * row for any mapping key nobody has touched yet, which a read-only transaction can't do.
     */
    @Transactional
    public SetupCompletenessResponse check() {
        List<SetupChecklistItem> items = new ArrayList<>();

        Organization org = organizationService.getOrCreate();
        boolean orgDone = org.getLegalName() != null && !org.getLegalName().isBlank();
        items.add(item("organization", "Organization profile", orgDone,
                orgDone ? "Configured" : "Legal name not set", "/settings/organization"));

        FinancialYear currentFy = financialYearRepository.findByIsCurrentTrue().orElse(null);
        items.add(item("financial_year", "Financial year", currentFy != null,
                currentFy != null ? "Current year: " + currentFy.getName() : "No active financial year",
                "/financial-years"));

        boolean hasOpenPeriod = currentFy != null && accountingPeriodRepository
                .findByFinancialYearIdOrderByPeriodNumberAsc(currentFy.getId()).stream()
                .anyMatch(p -> p.getStatus() == AccountingPeriodStatus.OPEN);
        items.add(item("accounting_period", "Open accounting period", hasOpenPeriod,
                hasOpenPeriod ? "At least one period is open for posting" : "No open accounting period",
                "/financial-years"));

        long accountCount = accountRepository.count();
        items.add(item("chart_of_accounts", "Chart of accounts", accountCount > 0,
                accountCount + " accounts configured", "/chart-of-accounts"));

        boolean baseCurrencySet = hasDefaultConfigItem("currencies");
        items.add(item("base_currency", "Base currency", baseCurrencySet,
                baseCurrencySet ? "Default currency set" : "No default currency marked",
                "/settings/configs/currencies"));

        long taxCount = taxCategoryRepository.findByDeletedFalse().size();
        items.add(item("tax_configuration", "Tax configuration", taxCount > 0,
                taxCount + " tax categories configured", "/taxes"));

        long bankAccountCount = bankAccountRepository.count();
        items.add(item("bank_account", "Bank account", bankAccountCount > 0,
                bankAccountCount + " bank account(s) configured", "/settings/bank-accounts"));

        boolean invoiceNumbering = documentNumberConfigRepository
                .existsByModuleAndCompanyIdAndBranchId("INVOICE", 0L, 0L);
        items.add(item("invoice_numbering", "Invoice numbering", invoiceNumbering,
                invoiceNumbering ? "Configured" : "Not configured", "/settings/numbering"));

        boolean invoiceTemplate = documentTemplateRepository
                .existsByDocumentTypeAndIsDefaultTrue(DocumentType.INVOICE);
        items.add(item("invoice_template", "Default invoice template", invoiceTemplate,
                invoiceTemplate ? "Configured" : "No default invoice template set",
                "/settings/document-templates/invoices"));

        List<AccountingMappingResponse> payrollMappings = accountingMappingService.list().stream()
                .filter(m -> m.getGroup() == MappingGroup.PAYROLL).toList();
        boolean payrollMappingsDone = !payrollMappings.isEmpty() && payrollMappings.stream().allMatch(AccountingMappingResponse::isConfigured);
        long payrollConfiguredCount = payrollMappings.stream().filter(AccountingMappingResponse::isConfigured).count();
        items.add(item("payroll_mappings", "Payroll accounting mappings", payrollMappingsDone,
                payrollConfiguredCount + " of " + payrollMappings.size() + " payroll mappings resolve to a real account",
                "/settings/accounting-mappings"));

        int completeCount = (int) items.stream().filter(SetupChecklistItem::isComplete).count();
        return SetupCompletenessResponse.builder()
                .completeCount(completeCount)
                .totalCount(items.size())
                .readyToOperate(completeCount == items.size())
                .items(items)
                .build();
    }

    private boolean hasDefaultConfigItem(String configKey) {
        try {
            var config = configService.getConfigByKey(configKey);
            return config != null && config.getItems() != null
                    && config.getItems().stream().anyMatch(i -> Boolean.TRUE.equals(i.getIsDefault()));
        } catch (Exception e) {
            return false;
        }
    }

    private SetupChecklistItem item(String key, String label, boolean complete, String detail, String settingsPath) {
        return SetupChecklistItem.builder()
                .key(key).label(label).complete(complete).detail(detail).settingsPath(settingsPath)
                .build();
    }
}
