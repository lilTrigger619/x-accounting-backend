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
    CUSTOMER_DOWNPAYMENT_LIABILITY(MappingGroup.SALES, "2085",
            "Liability account credited when a customer downpayment is received, and debited as it is applied to invoices or refunded"),

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
    SUPPLIER_DOWNPAYMENT_ASSET(MappingGroup.PURCHASES, "1745",
            "Asset account debited when a downpayment is paid to a supplier, and credited as it is applied to bills or refunded"),
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
            "Equity account that absorbs net income/loss when a financial year is closed"),

    PREPAYMENT_DEFAULT_ASSET(MappingGroup.PREPAYMENTS, "1795",
            "Fallback prepaid-asset account debited when a prepayment is paid and has no account override"),
    PREPAYMENT_DEFAULT_EXPENSE(MappingGroup.PREPAYMENTS, "5000",
            "Fallback expense account debited as each prepayment period is recognized, when the prepayment has no account override"),
    PREPAYMENT_BANK_ACCOUNT(MappingGroup.PREPAYMENTS, "1010",
            "Bank account credited when a prepayment is paid out, when the prepayment has no specific bank account chosen"),

    LOAN_RECEIVABLE(MappingGroup.LOANS, "1780",
            "Asset account debited when the organization disburses a loan it lends out (employee, customer, supplier or other)"),
    LOAN_PAYABLE(MappingGroup.LOANS, "2160",
            "Liability account credited when the organization receives a loan it borrowed (bank, shareholder, director or other lender)"),
    LOAN_INTEREST_INCOME(MappingGroup.LOANS, "4040",
            "Revenue account credited for interest earned on a loan the organization lent out"),
    LOAN_INTEREST_EXPENSE(MappingGroup.LOANS, "5080",
            "Expense account debited for interest owed on a loan the organization borrowed"),
    LOAN_INTEREST_RECEIVABLE(MappingGroup.LOANS, "1790",
            "Asset account debited for interest accrued but not yet received on a loan lent out"),
    LOAN_INTEREST_PAYABLE(MappingGroup.LOANS, "2170",
            "Liability account credited for interest accrued but not yet paid on a loan borrowed"),
    LOAN_FEE_EXPENSE(MappingGroup.LOANS, "5090",
            "Expense account debited for origination/processing/arrangement/penalty fees on a borrowed loan"),
    LOAN_BANK_ACCOUNT(MappingGroup.LOANS, "1010",
            "Bank account used for loan disbursement/repayment when no specific bank account is chosen"),

    BANK_TRANSFER_CHARGES(MappingGroup.BANKING, "5100",
            "Expense account debited for bank charges/fees on a bank transfer, when the transfer has no fee account override"),
    FX_GAIN(MappingGroup.BANKING, "4050",
            "Revenue account credited for a foreign exchange gain realised on a cross-currency bank transfer"),
    FX_LOSS(MappingGroup.BANKING, "5110",
            "Expense account debited for a foreign exchange loss realised on a cross-currency bank transfer"),

    DEPOSIT_PAID_ASSET(MappingGroup.DEPOSITS, "1745",
            "Asset account debited when the organization pays a deposit, when neither the deposit nor its type names an account"),
    DEPOSIT_RECEIVED_LIABILITY(MappingGroup.DEPOSITS, "2085",
            "Liability account credited when a deposit is received from a customer or third party, when neither the deposit nor its type names an account"),
    DEPOSIT_FORFEIT_INCOME(MappingGroup.DEPOSITS, "4060",
            "Income account credited when a deposit the organization received is forfeited to it"),
    DEPOSIT_FORFEIT_EXPENSE(MappingGroup.DEPOSITS, "5095",
            "Expense account debited when a deposit the organization paid is forfeited"),
    DEPOSIT_BANK_ACCOUNT(MappingGroup.DEPOSITS, "1010",
            "Bank account used to pay, receive or refund a deposit when no specific bank account is chosen");

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
