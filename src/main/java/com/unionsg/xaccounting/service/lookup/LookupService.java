package com.unionsg.xaccounting.service.lookup;

import com.unionsg.xaccounting.dto.lookup.LookupDefinitionDto;
import com.unionsg.xaccounting.dto.lookup.LookupOptionDto;
import com.unionsg.xaccounting.enums.*;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import com.unionsg.xaccounting.enums.deposit.*;
import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import com.unionsg.xaccounting.enums.bankrec.BookTransactionStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import com.unionsg.xaccounting.enums.loan.*;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Serves system-controlled values (statuses, types and calculation options backed by Java
 * enums) so screens never hardcode them. Values users can add to live in Configurations
 * ({@code /api/configs}) instead.
 */
@Service
public class LookupService {

    private record Entry(String title, Class<? extends Enum<?>> type, Predicate<Enum<?>> include) {}

    private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();

    static {
        register("account-types", "Account Types", AccountType.class);
        register("bank-transfer-statuses", "Bank Transfer Statuses", BankTransferStatus.class);
        register("amount-sign-conventions", "Amount Sign Conventions", AmountSignConvention.class);
        register("bank-match-statuses", "Match Statuses", MatchStatus.class);
        register("bank-match-types", "Match Types", MatchType.class);
        register("bank-reconciliation-adjustment-types", "Adjustment Types", ReconciliationAdjustmentType.class);
        register("bank-reconciliation-statuses", "Reconciliation Statuses", ReconciliationStatus.class);
        register("bank-transaction-match-statuses", "Transaction Match Statuses", StatementTransactionStatus.class);
        register("bill-statuses", "Bill Statuses", BillStatus.class);
        register("customer-statuses", "Customer Statuses", CustomerStatus.class);
        register("customer-types", "Customer Types", CustomerType.class);
        register("deposit-allocation-types", "Deposit Allocation Types", DepositAllocationType.class);
        register("deposit-classifications", "Deposit Classifications", DepositClassification.class);
        register("deposit-counterparty-types", "Deposit Counterparty Types", DepositCounterpartyType.class);
        register("deposit-directions", "Deposit Directions", DepositDirection.class);
        register("deposit-statuses", "Deposit Statuses", DepositStatus.class);
        register("discount-types", "Discount Types", DiscountType.class);
        register("downpayment-statuses", "Downpayment Statuses", DownpaymentStatus.class);
        register("downpayment-types", "Downpayment Types", DownpaymentType.class);
        register("employment-statuses", "Employment Statuses", EmploymentStatus.class);
        register("invoice-statuses", "Invoice Statuses", InvoiceStatus.class);
        register("journal-statuses", "Journal Statuses", JournalStatus.class);
        register("journal-types", "Journal Types", JournalType.class);
        register("loan-counterparty-types", "Loan Counterparty Types", LoanCounterpartyType.class);
        register("loan-directions", "Loan Directions", LoanDirection.class);
        register("loan-frequencies", "Loan Payment Frequencies", LoanFrequency.class);
        register("loan-fee-treatments", "Loan Fee Treatments", LoanFeeTreatment.class);
        register("loan-installment-statuses", "Installment Statuses", LoanInstallmentStatus.class);
        register("loan-interest-methods", "Interest Calculation Methods", LoanInterestMethod.class);
        register("loan-payment-statuses", "Loan Payment Statuses", LoanPaymentStatus.class);
        register("loan-payment-types", "Loan Payment Types", LoanPaymentType.class);
        register("loan-statuses", "Loan Statuses", com.unionsg.xaccounting.enums.loan.LoanStatus.class);
        register("manual-journal-types", "Journal Types", JournalType.class, t -> ((JournalType) t).isManualEntry());
        register("pay-component-calculation-methods", "Calculation Methods", PayComponentCalculationMethod.class);
        register("pay-component-categories", "Pay Component Categories", PayComponentCategory.class);
        register("pay-component-sides", "Pay Component Sides", PayComponentSide.class);
        register("pay-frequencies", "Pay Frequencies", PayFrequency.class);
        register("payment-methods", "Payment Methods", PaymentMethod.class);
        register("payroll-period-statuses", "Payroll Period Statuses", PayrollPeriodStatus.class);
        register("payment-statuses", "Payment Statuses", PaymentStatus.class);
        register("payment-term-types", "Payment Terms", PaymentTermType.class);
        register("prepayment-counterparty-types", "Prepayment Counterparty Types", PrepaymentCounterpartyType.class);
        register("prepayment-frequencies", "Recognition Frequencies", PrepaymentFrequency.class);
        register("product-item-types", "Item Types", ProductItemType.class);
        register("recurring-journal-frequencies", "Recurring Frequencies", RecurringJournalFrequency.class);
        register("report-template-statuses", "Report Template Statuses", ReportTemplateStatus.class);
        register("section-types", "Report Section Types", SectionType.class);
        register("supplier-payment-statuses", "Supplier Payment Statuses", SupplierPaymentStatus.class);
        register("tax-category-types", "Tax Types", TaxCategoryType.class);
    }

    private static void register(String key, String title, Class<? extends Enum<?>> type) {
        register(key, title, type, c -> true);
    }

    /** Registers only the constants {@code include} accepts, e.g. the journal types a person may enter by hand. */
    private static void register(String key, String title, Class<? extends Enum<?>> type, Predicate<Enum<?>> include) {
        REGISTRY.put(key, new Entry(title, type, include));
    }

    public List<LookupDefinitionDto> getAll() {
        return REGISTRY.keySet().stream().map(this::get).toList();
    }

    public LookupDefinitionDto get(String key) {
        Entry entry = REGISTRY.get(key);
        if (entry == null) {
            throw new ResourceNotFoundException("Unknown lookup: " + key);
        }
        List<LookupOptionDto> options = Arrays.stream(entry.type().getEnumConstants())
                .filter(entry.include())
                .map(c -> new LookupOptionDto(c.name(), labelOf(c)))
                .toList();
        return new LookupDefinitionDto(key, entry.title(), options);
    }

    private static String labelOf(Enum<?> constant) {
        if (constant instanceof LabeledEnum labeled) {
            return labeled.getLabel();
        }
        String[] words = constant.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }
}
