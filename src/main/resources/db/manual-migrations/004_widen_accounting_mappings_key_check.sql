-- =============================================================================
-- Manual migration: widen the accounting_mappings.mapping_key CHECK constraint
-- =============================================================================
--
-- Same root cause as 002_fix_invoice_status_check_constraint.sql and
-- 003_add_employee_to_files_entity_type_check.sql: this project uses
-- spring.jpa.hibernate.ddl-auto=update, which creates a CHECK constraint for an
-- @Enumerated(EnumType.STRING) column only once, when the table is first
-- created - it does NOT widen that constraint later if the Java enum gains new
-- values. MappingKey gained PAYMENT_CASH_ACCOUNT and SUPPLIER_PAYMENT_CASH_ACCOUNT
-- (cash-vs-bank posting, Settings & Setup §8); if your accounting_mappings table
-- was created before those existed on the enum, its CHECK constraint still only
-- allows the old, narrower list.
--
-- Symptom: resolving or saving either mapping fails with:
--   ERROR: new row for relation "accounting_mappings" violates check constraint
--   "accounting_mappings_mapping_key_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 004_widen_accounting_mappings_key_check.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE accounting_mappings DROP CONSTRAINT IF EXISTS accounting_mappings_mapping_key_check;
ALTER TABLE accounting_mappings ADD CONSTRAINT accounting_mappings_mapping_key_check
    CHECK (mapping_key IN (
        'PAYMENT_BANK_ACCOUNT', 'PAYMENT_CASH_ACCOUNT', 'PAYMENT_ACCOUNTS_RECEIVABLE', 'PAYMENT_CUSTOMER_ADVANCES',
        'INVOICE_ACCOUNTS_RECEIVABLE', 'INVOICE_REVENUE', 'INVOICE_SALES_TAX_PAYABLE',
        'BILL_ACCOUNTS_PAYABLE', 'BILL_DEFAULT_EXPENSE',
        'SUPPLIER_PAYMENT_BANK_ACCOUNT', 'SUPPLIER_PAYMENT_CASH_ACCOUNT', 'SUPPLIER_PAYMENT_ADVANCES',
        'PAYROLL_SALARY_PAYABLE', 'PAYROLL_DEFAULT_SALARY_EXPENSE', 'PAYROLL_EMPLOYEE_TAX_PAYABLE',
        'PAYROLL_LOAN_RECEIVABLE', 'PAYROLL_ADVANCE_RECEIVABLE', 'PAYROLL_REIMBURSEMENT_PAYABLE',
        'PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE', 'PAYROLL_PAYMENT_BANK_ACCOUNT',
        'CLOSING_RETAINED_EARNINGS'
    ));

COMMIT;
