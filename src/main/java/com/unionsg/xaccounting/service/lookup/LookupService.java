package com.unionsg.xaccounting.service.lookup;

import com.unionsg.xaccounting.dto.lookup.LookupDefinitionDto;
import com.unionsg.xaccounting.dto.lookup.LookupOptionDto;
import com.unionsg.xaccounting.enums.*;
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

/**
 * Serves system-controlled values (statuses, types and calculation options backed by Java
 * enums) so screens never hardcode them. Values users can add to live in Configurations
 * ({@code /api/configs}) instead.
 */
@Service
public class LookupService {

    private record Entry(String title, Class<? extends Enum<?>> type) {}

    private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();

    static {
        register("account-types", "Account Types", AccountType.class);
        register("bill-statuses", "Bill Statuses", BillStatus.class);
        register("customer-statuses", "Customer Statuses", CustomerStatus.class);
        register("customer-types", "Customer Types", CustomerType.class);
        register("discount-types", "Discount Types", DiscountType.class);
        register("employment-statuses", "Employment Statuses", EmploymentStatus.class);
        register("invoice-statuses", "Invoice Statuses", InvoiceStatus.class);
        register("journal-statuses", "Journal Statuses", JournalStatus.class);
        register("journal-types", "Journal Types", JournalType.class);
        register("loan-counterparty-types", "Loan Counterparty Types", LoanCounterpartyType.class);
        register("loan-directions", "Loan Directions", LoanDirection.class);
        register("loan-frequencies", "Loan Payment Frequencies", LoanFrequency.class);
        register("loan-interest-methods", "Loan Interest Methods", LoanInterestMethod.class);
        register("loan-interest-types", "Loan Interest Types", LoanInterestType.class);
        register("loan-repayment-methods", "Loan Repayment Methods", LoanRepaymentMethod.class);
        register("pay-component-calculation-methods", "Calculation Methods", PayComponentCalculationMethod.class);
        register("pay-component-categories", "Pay Component Categories", PayComponentCategory.class);
        register("pay-component-sides", "Pay Component Sides", PayComponentSide.class);
        register("pay-frequencies", "Pay Frequencies", PayFrequency.class);
        register("payment-methods", "Payment Methods", PaymentMethod.class);
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
        REGISTRY.put(key, new Entry(title, type));
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
