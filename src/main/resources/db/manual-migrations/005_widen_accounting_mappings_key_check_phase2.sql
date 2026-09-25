-- =============================================================================
-- Manual migration: widen the accounting_mappings.mapping_key CHECK constraint
-- =============================================================================
--
-- Same root cause as 002/003/004: spring.jpa.hibernate.ddl-auto=update creates a
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only once, when the
-- table is first created - it does NOT widen that constraint later when the Java
-- enum gains new values. MappingKey gained BILL_PURCHASE_TAX_RECEIVABLE (input VAT
-- on supplier bills) and TAX_WITHHOLDING_PAYABLE (tax withheld from a supplier
-- payment); if your accounting_mappings table was created before those existed on
-- the enum, its CHECK constraint still only allows the old, narrower list.
--
-- Symptom: resolving or saving either mapping fails with:
--   ERROR: new row for relation "accounting_mappings" violates check constraint
--   "accounting_mappings_mapping_key_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 005_widen_accounting_mappings_key_check_phase2.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE accounting_mappings DROP CONSTRAINT IF EXISTS accounting_mappings_mapping_key_check;
ALTER TABLE accounting_mappings ADD CONSTRAINT accounting_mappings_mapping_key_check
    CHECK (mapping_key IN (
        'PAYMENT_BANK_ACCOUNT', 'PAYMENT_CASH_ACCOUNT', 'PAYMENT_ACCOUNTS_RECEIVABLE', 'PAYMENT_CUSTOMER_ADVANCES',
        'INVOICE_ACCOUNTS_RECEIVABLE', 'INVOICE_REVENUE', 'INVOICE_SALES_TAX_PAYABLE',
        'BILL_ACCOUNTS_PAYABLE', 'BILL_DEFAULT_EXPENSE', 'BILL_PURCHASE_TAX_RECEIVABLE',
        'SUPPLIER_PAYMENT_BANK_ACCOUNT', 'SUPPLIER_PAYMENT_CASH_ACCOUNT', 'SUPPLIER_PAYMENT_ADVANCES',
        'TAX_WITHHOLDING_PAYABLE',
        'PAYROLL_SALARY_PAYABLE', 'PAYROLL_DEFAULT_SALARY_EXPENSE', 'PAYROLL_EMPLOYEE_TAX_PAYABLE',
        'PAYROLL_LOAN_RECEIVABLE', 'PAYROLL_ADVANCE_RECEIVABLE', 'PAYROLL_REIMBURSEMENT_PAYABLE',
        'PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE', 'PAYROLL_PAYMENT_BANK_ACCOUNT',
        'CLOSING_RETAINED_EARNINGS'
    ));

COMMIT;
