-- =============================================================================
-- Manual migration: Deposits
-- =============================================================================
--
-- Same root cause as 002-009: spring.jpa.hibernate.ddl-auto=update creates the
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only when a table
-- is first created and never widens it. Deposits adds:
--   * JournalType.DEPOSIT             (journal_entries.journal_type)
--   * EntityType.DEPOSIT              (files.entity_type, for attachments)
--   * MappingKey.DEPOSIT_PAID_ASSET, DEPOSIT_RECEIVED_LIABILITY,
--     DEPOSIT_FORFEIT_INCOME, DEPOSIT_FORFEIT_EXPENSE, DEPOSIT_BANK_ACCOUNT
--                                     (accounting_mappings.mapping_key)
-- Each list below is the full list as of this migration (it includes 009's
-- Bank Transfers values), so run 009 first or just run this one.
--
-- The deposit_types, deposits, deposit_allocations and document_settlements
-- tables are created by Hibernate on startup; the final block adds the
-- guard rails Hibernate cannot express.
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 010_deposits.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE',
        'CLOSING', 'REVERSING', 'PREPAYMENT', 'LOAN', 'BANK_TRANSFER', 'DEPOSIT'
    ));

ALTER TABLE files DROP CONSTRAINT IF EXISTS files_entity_type_check;
ALTER TABLE files ADD CONSTRAINT files_entity_type_check
    CHECK (entity_type IN (
        'INVOICE', 'CUSTOMER', 'SUPPLIER', 'PAYMENT', 'EXPENSE', 'PRODUCT', 'COMPANY', 'USER',
        'EMPLOYEE', 'DEPOSIT',
        'GENERATED_DOCUMENT',
        'DOCUMENT_TEMPLATE_INVOICE', 'DOCUMENT_TEMPLATE_QUOTE', 'DOCUMENT_TEMPLATE_PURCHASE_ORDER',
        'DOCUMENT_TEMPLATE_CREDIT_NOTE', 'DOCUMENT_TEMPLATE_DELIVERY_NOTE', 'DOCUMENT_TEMPLATE_RECEIPT',
        'GENERAL_JOURNAL', 'SALES_JOURNAL', 'PURCHASE_JOURNAL', 'PAYROLL_JOURNAL',
        'ADJUSTMENT_JOURNAL', 'OPENING_BALANCE_JOURNAL', 'CLOSING_JOURNAL', 'REVERSING_JOURNAL',
        'BANK_TRANSFER'
    ));

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
        'CLOSING_RETAINED_EARNINGS',
        'PREPAYMENT_DEFAULT_ASSET', 'PREPAYMENT_DEFAULT_EXPENSE', 'PREPAYMENT_BANK_ACCOUNT',
        'LOAN_RECEIVABLE', 'LOAN_PAYABLE', 'LOAN_INTEREST_INCOME', 'LOAN_INTEREST_EXPENSE',
        'LOAN_INTEREST_RECEIVABLE', 'LOAN_INTEREST_PAYABLE', 'LOAN_FEE_EXPENSE', 'LOAN_BANK_ACCOUNT',
        'BANK_TRANSFER_CHARGES', 'FX_GAIN', 'FX_LOSS',
        'DEPOSIT_PAID_ASSET', 'DEPOSIT_RECEIVED_LIABILITY', 'DEPOSIT_FORFEIT_INCOME', 'DEPOSIT_FORFEIT_EXPENSE',
        'DEPOSIT_BANK_ACCOUNT'
    ));

DO $$
BEGIN
    IF to_regclass('deposits') IS NOT NULL THEN
        ALTER TABLE deposits DROP CONSTRAINT IF EXISTS ck_deposit_amount_positive;
        ALTER TABLE deposits ADD CONSTRAINT ck_deposit_amount_positive CHECK (amount > 0);
        ALTER TABLE deposits DROP CONSTRAINT IF EXISTS ck_deposit_balance_in_range;
        ALTER TABLE deposits ADD CONSTRAINT ck_deposit_balance_in_range
            CHECK (available_balance >= 0 AND available_balance <= amount);
        ALTER TABLE deposits DROP CONSTRAINT IF EXISTS ck_deposit_buckets_balance;
        ALTER TABLE deposits ADD CONSTRAINT ck_deposit_buckets_balance
            CHECK (status IN ('DRAFT', 'CANCELLED', 'REVERSED')
                OR applied_amount + refunded_amount + forfeited_amount + transferred_amount + available_balance = amount);
    END IF;
    IF to_regclass('deposit_allocations') IS NOT NULL THEN
        ALTER TABLE deposit_allocations DROP CONSTRAINT IF EXISTS ck_deposit_allocation_amount_positive;
        ALTER TABLE deposit_allocations ADD CONSTRAINT ck_deposit_allocation_amount_positive CHECK (amount > 0);
    END IF;
    IF to_regclass('document_settlements') IS NOT NULL THEN
        DROP INDEX IF EXISTS ux_document_settlement_active_allocation;
        CREATE UNIQUE INDEX ux_document_settlement_active_allocation
            ON document_settlements (source_type, source_allocation_id) WHERE reversed = false;
    END IF;
END $$;

COMMIT;
