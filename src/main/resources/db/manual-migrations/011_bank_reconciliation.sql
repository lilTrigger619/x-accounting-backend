-- =============================================================================
-- Manual migration: widen the accounting_mappings.mapping_key CHECK constraint
-- =============================================================================
--
-- Same root cause as 004/005/007/009/010: spring.jpa.hibernate.ddl-auto=update creates a
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only once, when the
-- table is first created. MappingKey gained the Bank Reconciliation keys
-- (BANK_CHARGES_EXPENSE, BANK_INTEREST_INCOME, BANK_RECONCILIATION_SUSPENSE) used
-- when a reconciliation adjustment is posted.
--
-- Symptom: posting a bank reconciliation adjustment fails with:
--   ERROR: new row for relation "accounting_mappings" violates check constraint
--   "accounting_mappings_mapping_key_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 011_bank_reconciliation.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE accounting_mappings DROP CONSTRAINT IF EXISTS accounting_mappings_mapping_key_check;
ALTER TABLE accounting_mappings ADD CONSTRAINT accounting_mappings_mapping_key_check
    CHECK (mapping_key IN (
        'PAYMENT_BANK_ACCOUNT', 'PAYMENT_CASH_ACCOUNT', 'PAYMENT_ACCOUNTS_RECEIVABLE', 'PAYMENT_CUSTOMER_ADVANCES',
        'CUSTOMER_DOWNPAYMENT_LIABILITY', 'INVOICE_ACCOUNTS_RECEIVABLE', 'INVOICE_REVENUE', 'INVOICE_SALES_TAX_PAYABLE',
        'BILL_ACCOUNTS_PAYABLE', 'BILL_DEFAULT_EXPENSE', 'BILL_PURCHASE_TAX_RECEIVABLE', 'SUPPLIER_PAYMENT_BANK_ACCOUNT',
        'SUPPLIER_PAYMENT_CASH_ACCOUNT', 'SUPPLIER_PAYMENT_ADVANCES', 'SUPPLIER_DOWNPAYMENT_ASSET', 'TAX_WITHHOLDING_PAYABLE',
        'PAYROLL_SALARY_PAYABLE', 'PAYROLL_DEFAULT_SALARY_EXPENSE', 'PAYROLL_EMPLOYEE_TAX_PAYABLE', 'PAYROLL_LOAN_RECEIVABLE',
        'PAYROLL_ADVANCE_RECEIVABLE', 'PAYROLL_REIMBURSEMENT_PAYABLE', 'PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE',
        'PAYROLL_PAYMENT_BANK_ACCOUNT', 'CLOSING_RETAINED_EARNINGS', 'PREPAYMENT_DEFAULT_ASSET', 'PREPAYMENT_DEFAULT_EXPENSE',
        'PREPAYMENT_BANK_ACCOUNT', 'LOAN_RECEIVABLE', 'LOAN_PAYABLE', 'LOAN_INTEREST_INCOME', 'LOAN_INTEREST_EXPENSE',
        'LOAN_INTEREST_RECEIVABLE', 'LOAN_INTEREST_PAYABLE', 'LOAN_FEE_EXPENSE', 'LOAN_BANK_ACCOUNT',
        'BANK_TRANSFER_CHARGES', 'FX_GAIN', 'FX_LOSS', 'DEPOSIT_PAID_ASSET', 'DEPOSIT_RECEIVED_LIABILITY',
        'DEPOSIT_FORFEIT_INCOME', 'DEPOSIT_FORFEIT_EXPENSE', 'DEPOSIT_BANK_ACCOUNT', 'BANK_CHARGES_EXPENSE',
        'BANK_INTEREST_INCOME', 'BANK_RECONCILIATION_SUSPENSE'
    ));

COMMIT;
