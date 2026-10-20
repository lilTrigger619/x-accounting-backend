-- =============================================================================
-- Manual migration: Fixed assets and depreciation
-- =============================================================================
--
-- Same root cause as 002-014: spring.jpa.hibernate.ddl-auto=update creates the
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only when a table
-- is first created and never widens it. The fixed asset module adds:
--   * JournalType.FIXED_ASSET, DEPRECIATION     (journal_entries.journal_type)
--   * EntityType.FIXED_ASSET                    (files.entity_type)
--   * MappingKey.FIXED_ASSET_*                  (accounting_mappings.mapping_key)
--
-- The new asset_categories, fixed_assets, asset_transactions, depreciation_runs,
-- depreciation_run_lines, asset_estimate_changes and fixed_asset_audit_logs
-- tables are created by Hibernate on startup. The final block adds the
-- constraints that keep their numbers consistent.
--
-- Symptoms without it: activating an asset or posting a depreciation run fails with
--   violates check constraint "journal_entries_journal_type_check"
-- and saving a Fixed Assets mapping fails on accounting_mappings_mapping_key_check.
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 015_fixed_assets.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE', 'CLOSING',
        'REVERSING', 'PREPAYMENT', 'DOWNPAYMENT', 'LOAN', 'BANK_TRANSFER', 'DEPOSIT',
        'EXPENSE', 'FIXED_ASSET', 'DEPRECIATION'
    ));

ALTER TABLE files DROP CONSTRAINT IF EXISTS files_entity_type_check;
ALTER TABLE files ADD CONSTRAINT files_entity_type_check
    CHECK (entity_type IN (
        'INVOICE', 'CUSTOMER', 'SUPPLIER', 'PAYMENT', 'EXPENSE', 'PRODUCT', 'COMPANY', 'USER',
        'EMPLOYEE', 'DEPOSIT', 'GENERATED_DOCUMENT', 'DOCUMENT_TEMPLATE_INVOICE',
        'DOCUMENT_TEMPLATE_QUOTE', 'DOCUMENT_TEMPLATE_PURCHASE_ORDER',
        'DOCUMENT_TEMPLATE_CREDIT_NOTE', 'DOCUMENT_TEMPLATE_DELIVERY_NOTE',
        'DOCUMENT_TEMPLATE_RECEIPT', 'GENERAL_JOURNAL', 'SALES_JOURNAL', 'PURCHASE_JOURNAL',
        'PAYROLL_JOURNAL', 'ADJUSTMENT_JOURNAL', 'OPENING_BALANCE_JOURNAL', 'CLOSING_JOURNAL',
        'REVERSING_JOURNAL', 'BANK_TRANSFER', 'LOAN', 'FIXED_ASSET'
    ));

ALTER TABLE accounting_mappings DROP CONSTRAINT IF EXISTS accounting_mappings_mapping_key_check;
ALTER TABLE accounting_mappings ADD CONSTRAINT accounting_mappings_mapping_key_check
    CHECK (mapping_key IN (
        'PAYMENT_BANK_ACCOUNT', 'PAYMENT_CASH_ACCOUNT', 'PAYMENT_ACCOUNTS_RECEIVABLE',
        'PAYMENT_CUSTOMER_ADVANCES', 'CUSTOMER_DOWNPAYMENT_LIABILITY',
        'INVOICE_ACCOUNTS_RECEIVABLE', 'INVOICE_REVENUE', 'INVOICE_SALES_TAX_PAYABLE',
        'BILL_ACCOUNTS_PAYABLE', 'BILL_DEFAULT_EXPENSE', 'BILL_PURCHASE_TAX_RECEIVABLE',
        'SUPPLIER_PAYMENT_BANK_ACCOUNT', 'SUPPLIER_PAYMENT_CASH_ACCOUNT',
        'SUPPLIER_PAYMENT_ADVANCES', 'SUPPLIER_DOWNPAYMENT_ASSET', 'TAX_WITHHOLDING_PAYABLE',
        'PAYROLL_SALARY_PAYABLE', 'PAYROLL_DEFAULT_SALARY_EXPENSE',
        'PAYROLL_EMPLOYEE_TAX_PAYABLE', 'PAYROLL_LOAN_RECEIVABLE',
        'PAYROLL_ADVANCE_RECEIVABLE', 'PAYROLL_REIMBURSEMENT_PAYABLE',
        'PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE', 'PAYROLL_PAYMENT_BANK_ACCOUNT',
        'CLOSING_RETAINED_EARNINGS', 'PREPAYMENT_DEFAULT_ASSET', 'PREPAYMENT_DEFAULT_EXPENSE',
        'PREPAYMENT_BANK_ACCOUNT', 'LOAN_RECEIVABLE', 'LOAN_PAYABLE', 'LOAN_INTEREST_INCOME',
        'LOAN_INTEREST_EXPENSE', 'LOAN_INTEREST_RECEIVABLE', 'LOAN_INTEREST_PAYABLE',
        'LOAN_FEE_EXPENSE', 'LOAN_BANK_ACCOUNT', 'LOAN_FEE_INCOME', 'LOAN_WRITE_OFF_EXPENSE',
        'BANK_TRANSFER_CHARGES', 'FX_GAIN', 'FX_LOSS', 'DEPOSIT_PAID_ASSET',
        'DEPOSIT_RECEIVED_LIABILITY', 'DEPOSIT_FORFEIT_INCOME', 'DEPOSIT_FORFEIT_EXPENSE',
        'DEPOSIT_BANK_ACCOUNT', 'BANK_CHARGES_EXPENSE', 'BANK_INTEREST_INCOME',
        'BANK_RECONCILIATION_SUSPENSE', 'FIXED_ASSET_COST',
        'FIXED_ASSET_ACCUMULATED_DEPRECIATION', 'FIXED_ASSET_DEPRECIATION_EXPENSE',
        'FIXED_ASSET_ACCUMULATED_IMPAIRMENT', 'FIXED_ASSET_IMPAIRMENT_LOSS',
        'FIXED_ASSET_DISPOSAL_GAIN', 'FIXED_ASSET_DISPOSAL_LOSS', 'FIXED_ASSET_BANK_ACCOUNT'
    ));

DO $$
BEGIN
    IF to_regclass('fixed_assets') IS NOT NULL THEN
        ALTER TABLE fixed_assets DROP CONSTRAINT IF EXISTS ck_fixed_asset_cost_positive;
        ALTER TABLE fixed_assets ADD CONSTRAINT ck_fixed_asset_cost_positive CHECK (acquisition_cost > 0);
        ALTER TABLE fixed_assets DROP CONSTRAINT IF EXISTS ck_fixed_asset_residual_not_negative;
        ALTER TABLE fixed_assets ADD CONSTRAINT ck_fixed_asset_residual_not_negative CHECK (residual_value >= 0);
    END IF;
    IF to_regclass('depreciation_run_lines') IS NOT NULL THEN
        ALTER TABLE depreciation_run_lines DROP CONSTRAINT IF EXISTS ck_depreciation_line_amount;
        ALTER TABLE depreciation_run_lines ADD CONSTRAINT ck_depreciation_line_amount CHECK (depreciation_amount >= 0);
    END IF;
    IF to_regclass('depreciation_runs') IS NOT NULL THEN
        ALTER TABLE depreciation_runs DROP CONSTRAINT IF EXISTS ck_depreciation_run_posted_has_journal;
        ALTER TABLE depreciation_runs ADD CONSTRAINT ck_depreciation_run_posted_has_journal
            CHECK (status NOT IN ('POSTED', 'REVERSED') OR journal_id IS NOT NULL);
    END IF;
END $$;

COMMIT;
