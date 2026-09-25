package com.unionsg.xaccounting.enums.settings;

/**
 * Every business-event-to-GL-account mapping the automated posting engines resolve at
 * transaction time (Settings & Setup §8/§46). Each key carries the account code the
 * application shipped with, seeded into {@code AccountingMapping} on first boot so an
 * admin can retarget it from the Settings UI without a code change or restart.
 */
public enum MappingKey {

    PAYMENT_BANK_ACCOUNT(MappingGroup.SALES, "1010",
            "Bank account debited when a customer payment is received by bank transfer, cheque, card or mobile money"),
    PAYMENT_CASH_ACCOUNT(MappingGroup.SALES, "1000",
            "Cash account debited when a customer payment is received in cash"),
    PAYMENT_ACCOUNTS_RECEIVABLE(MappingGroup.SALES, "6220",
            "Accounts Receivable control account credited when a customer payment is applied"),
    PAYMENT_CUSTOMER_ADVANCES(MappingGroup.SALES, "2080",
            "Liability account credited for unapplied customer payments (advances/overpayments)"),

    INVOICE_ACCOUNTS_RECEIVABLE(MappingGroup.SALES, "6220",
            "Accounts Receivable control account debited when an invoice is posted"),
    INVOICE_REVENUE(MappingGroup.SALES, "4020",
            "Default revenue account credited when an invoice is posted"),
    INVOICE_SALES_TAX_PAYABLE(MappingGroup.SALES, "2090",
            "Liability account credited for sales tax collected on invoices"),

    BILL_ACCOUNTS_PAYABLE(MappingGroup.PURCHASES, "6210",
            "Accounts Payable control account credited when a supplier bill is recorded"),
    BILL_DEFAULT_EXPENSE(MappingGroup.PURCHASES, "5000",
            "Fallback expense account debited when a bill line has no specific account"),
    BILL_PURCHASE_TAX_RECEIVABLE(MappingGroup.PURCHASES, "1770",
            "Asset account debited for recoverable purchase tax (input VAT) charged on supplier bills"),
    SUPPLIER_PAYMENT_BANK_ACCOUNT(MappingGroup.PURCHASES, "1010",
            "Bank account credited when a supplier payment is made by bank transfer, cheque, card or mobile money"),
    SUPPLIER_PAYMENT_CASH_ACCOUNT(MappingGroup.PURCHASES, "1000",
            "Cash account credited when a supplier payment is made in cash"),
    SUPPLIER_PAYMENT_ADVANCES(MappingGroup.PURCHASES, "1740",
            "Asset account debited for unapplied supplier payments (advances)"),
    TAX_WITHHOLDING_PAYABLE(MappingGroup.PURCHASES, "2150",
            "Liability account credited for tax withheld from a supplier payment, payable to the tax authority"),

    PAYROLL_SALARY_PAYABLE(MappingGroup.PAYROLL, "2100",
            "Liability account credited for each employee's net pay in a payroll run"),
    PAYROLL_DEFAULT_SALARY_EXPENSE(MappingGroup.PAYROLL, "5040",
            "Fallback expense account for earnings without a component-level GL mapping"),
    PAYROLL_EMPLOYEE_TAX_PAYABLE(MappingGroup.PAYROLL, "2110",
            "Liability account credited for employee tax withheld"),
    PAYROLL_LOAN_RECEIVABLE(MappingGroup.PAYROLL, "1750",
            "Asset account debited when an employee loan is disbursed"),
    PAYROLL_ADVANCE_RECEIVABLE(MappingGroup.PAYROLL, "1760",
            "Asset account debited when a salary advance is issued"),
    PAYROLL_REIMBURSEMENT_PAYABLE(MappingGroup.PAYROLL, "2140",
            "Liability account credited for approved employee reimbursement claims"),
    PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE(MappingGroup.PAYROLL, "5070",
            "Fallback expense account for reimbursement claims without a category mapping"),
    PAYROLL_PAYMENT_BANK_ACCOUNT(MappingGroup.PAYROLL, "1010",
            "Bank account credited when payroll, loans, advances or reimbursements are paid out"),

    CLOSING_RETAINED_EARNINGS(MappingGroup.CLOSING, "3010",
            "Equity account that absorbs net income/loss when a financial year is closed");

    private final MappingGroup group;
    private final String defaultAccountCode;
    private final String description;

    MappingKey(MappingGroup group, String defaultAccountCode, String description) {
        this.group = group;
        this.defaultAccountCode = defaultAccountCode;
        this.description = description;
    }

    public MappingGroup getGroup() {
        return group;
    }

    public String getDefaultAccountCode() {
        return defaultAccountCode;
    }

    public String getDescription() {
        return description;
    }
}
