package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.entity.settings.AccountingMapping;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.repository.analytics.AnalyticsAccountRow;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import com.unionsg.xaccounting.repository.settings.AccountingMappingRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sorts every ledger account into an {@link AccountClass}. The rules, in order:
 * <ul>
 *   <li>Cash &amp; bank: the "Bank Account" chart group, the dashboard cash account codes, every
 *       bank account's GL account, and the accounts mapped for bank and cash payments.</li>
 *   <li>Receivable / payable: the "Accounts receivable" / "Accounts payable" chart groups and the
 *       accounts mapped as the receivable / payable control accounts.</li>
 *   <li>Loans payable: the account mapped for loans payable and the principal account of every
 *       borrowed loan.</li>
 *   <li>Cost of sales: the "Cost of Goods Sold" chart group.</li>
 *   <li>Other income / other expenses: a category named "Other income" / "Other expense", plus
 *       foreign exchange and loan write-off accounts on the expense side.</li>
 *   <li>Non-current assets: the fixed asset categories (buildings, land, equipment, vehicles,
 *       software, depreciation and so on). Non-current liabilities: categories named long term.</li>
 *   <li>Everything else falls to the obvious class for its account type.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AccountClassifier {

    static final long CHART_BANK_ACCOUNT = 6L;
    static final long CHART_COST_OF_GOODS_SOLD = 8L;
    static final long CHART_ACCOUNTS_PAYABLE = 9L;
    static final long CHART_ACCOUNTS_RECEIVABLE = 10L;

    private static final Set<String> FIXED_ASSET_CATEGORIES = Set.of(
            "buildings", "land", "machinery & equipment", "furniture & fixtures", "vehicles", "computers",
            "software", "leasehold improvements", "intangible assets", "accumulated depreciation",
            "accumulated amortization", "other fixed assets", "fixed asset", "fixed assets");

    private final AnalyticsLedgerRepository ledgerRepository;
    private final AccountingMappingRepository mappingRepository;
    private final BankAccountRepository bankAccountRepository;

    @Value("${dashboard.cash-account-codes:}")
    private String cashAccountCodesCsv;

    /** All accounts, keyed by account id, in code order. */
    public Map<Long, ClassifiedAccount> classifyAll() {
        Set<String> cashCodes = new HashSet<>();
        for (String code : (cashAccountCodesCsv == null ? "" : cashAccountCodesCsv).split("\\s*,\\s*")) {
            if (!code.isBlank()) cashCodes.add(code.trim());
        }
        bankAccountRepository.findAll().stream()
                .map(BankAccount::getGlAccountCode)
                .filter(Objects::nonNull)
                .forEach(cashCodes::add);
        cashCodes.addAll(mapped(MappingKey.PAYMENT_BANK_ACCOUNT, MappingKey.PAYMENT_CASH_ACCOUNT));

        Set<String> receivableCodes = mapped(MappingKey.INVOICE_ACCOUNTS_RECEIVABLE, MappingKey.PAYMENT_ACCOUNTS_RECEIVABLE);
        Set<String> payableCodes = mapped(MappingKey.BILL_ACCOUNTS_PAYABLE);
        Set<String> loanPayableCodes = mapped(MappingKey.LOAN_PAYABLE);
        Set<Long> borrowedPrincipalIds = new HashSet<>(ledgerRepository.findBorrowedLoanPrincipalAccountIds());
        Set<String> otherExpenseCodes = mapped(MappingKey.FX_LOSS, MappingKey.LOAN_WRITE_OFF_EXPENSE);
        Set<String> otherIncomeCodes = mapped(MappingKey.FX_GAIN);

        Map<Long, ClassifiedAccount> out = new LinkedHashMap<>();
        ledgerRepository.findAllAccounts().stream()
                .sorted((a, b) -> String.valueOf(a.getAccountCode()).compareTo(String.valueOf(b.getAccountCode())))
                .forEach(row -> out.put(row.getAccountId(), new ClassifiedAccount(
                        row.getAccountId(), row.getAccountCode(), row.getAccountName(), row.getAccountType(),
                        category(row),
                        classify(row, cashCodes, receivableCodes, payableCodes, loanPayableCodes,
                                borrowedPrincipalIds, otherIncomeCodes, otherExpenseCodes))));
        return out;
    }

    static AccountClass classify(AnalyticsAccountRow row, Set<String> cashCodes, Set<String> receivableCodes,
                                 Set<String> payableCodes, Set<String> loanPayableCodes, Set<Long> borrowedPrincipalIds,
                                 Set<String> otherIncomeCodes, Set<String> otherExpenseCodes) {
        String code = row.getAccountCode();
        long chart = row.getChartCode() == null ? -1L : row.getChartCode();
        String category = normalize(row.getClearToName());
        AccountType type = row.getAccountType() == null ? AccountType.ASSET : row.getAccountType();

        switch (type) {
            case ASSET -> {
                if (chart == CHART_BANK_ACCOUNT || cashCodes.contains(code)) return AccountClass.CASH_BANK;
                if (chart == CHART_ACCOUNTS_RECEIVABLE || receivableCodes.contains(code)) return AccountClass.RECEIVABLE;
                if (FIXED_ASSET_CATEGORIES.contains(category) || category.contains("fixed asset")) {
                    return AccountClass.NON_CURRENT_ASSET;
                }
                return AccountClass.OTHER_CURRENT_ASSET;
            }
            case LIABILITY -> {
                if (chart == CHART_ACCOUNTS_PAYABLE || payableCodes.contains(code)) return AccountClass.PAYABLE;
                if (loanPayableCodes.contains(code) || borrowedPrincipalIds.contains(row.getAccountId())) {
                    return AccountClass.LOAN_LIABILITY;
                }
                if (category.contains("long term") || category.contains("long-term") || category.contains("non-current")) {
                    return AccountClass.NON_CURRENT_LIABILITY;
                }
                return AccountClass.OTHER_CURRENT_LIABILITY;
            }
            case EQUITY -> {
                return AccountClass.EQUITY;
            }
            case INCOME -> {
                if (category.equals("other income") || otherIncomeCodes.contains(code)) return AccountClass.OTHER_INCOME;
                return AccountClass.OPERATING_REVENUE;
            }
            case EXPENSE -> {
                if (chart == CHART_COST_OF_GOODS_SOLD) return AccountClass.COST_OF_SALES;
                if (category.equals("other expense") || otherExpenseCodes.contains(code)) return AccountClass.OTHER_EXPENSE;
                return AccountClass.OPERATING_EXPENSE;
            }
            default -> {
                return AccountClass.OTHER_CURRENT_ASSET;
            }
        }
    }

    private static String category(AnalyticsAccountRow row) {
        if (row.getClearToName() != null && !row.getClearToName().isBlank()) return row.getClearToName().trim();
        if (row.getChartName() != null && !row.getChartName().isBlank()) return row.getChartName().trim();
        return "Uncategorised";
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> mapped(MappingKey... keys) {
        Set<String> codes = new HashSet<>();
        for (MappingKey key : keys) {
            mappingRepository.findByMappingKey(key)
                    .map(AccountingMapping::getAccountCode)
                    .filter(Objects::nonNull)
                    .ifPresent(codes::add);
        }
        return codes;
    }

    public static Set<Long> idsOf(Map<Long, ClassifiedAccount> accounts, Collection<AccountClass> classes) {
        Set<Long> ids = new HashSet<>();
        accounts.values().forEach(a -> {
            if (classes.contains(a.accountClass())) ids.add(a.id());
        });
        return ids;
    }

    public static List<AccountClass> incomeStatementClasses() {
        return List.of(AccountClass.OPERATING_REVENUE, AccountClass.OTHER_INCOME, AccountClass.COST_OF_SALES,
                AccountClass.OPERATING_EXPENSE, AccountClass.OTHER_EXPENSE);
    }
}
