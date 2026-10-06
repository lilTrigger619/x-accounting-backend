-- =============================================================================
-- Manual migration: Bank Transfers
-- =============================================================================
--
-- Same root cause as 002-007: spring.jpa.hibernate.ddl-auto=update creates the
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only when a table
-- is first created and never widens it. Bank Transfers adds:
--   * JournalType.BANK_TRANSFER       (journal_entries.journal_type)
--   * EntityType.BANK_TRANSFER        (files.entity_type, for attachments)
--   * MappingKey.BANK_TRANSFER_CHARGES, FX_GAIN, FX_LOSS
--                                     (accounting_mappings.mapping_key)
--
-- The new bank_transfers and bank_transfer_activities tables, and their own
-- CHECK/UNIQUE constraints, are created by Hibernate on startup. The final
-- block re-adds those constraints in case the table was created by an older
-- build without them.
--
-- Symptoms without it: posting a transfer fails with
--   violates check constraint "journal_entries_journal_type_check"
-- and attaching a file fails with
--   violates check constraint "files_entity_type_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 009_bank_transfers.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE',
        'CLOSING', 'REVERSING', 'PREPAYMENT', 'LOAN', 'BANK_TRANSFER'
    ));

ALTER TABLE files DROP CONSTRAINT IF EXISTS files_entity_type_check;
ALTER TABLE files ADD CONSTRAINT files_entity_type_check
    CHECK (entity_type IN (
        'INVOICE', 'CUSTOMER', 'SUPPLIER', 'PAYMENT', 'EXPENSE', 'PRODUCT', 'COMPANY', 'USER',
        'EMPLOYEE',
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
        'BANK_TRANSFER_CHARGES', 'FX_GAIN', 'FX_LOSS'
    ));

DO $$
BEGIN
    IF to_regclass('bank_transfers') IS NOT NULL THEN
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_distinct_accounts;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_distinct_accounts
            CHECK (source_bank_account_id <> destination_bank_account_id);
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_amount_positive;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_amount_positive CHECK (amount > 0);
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_fee_non_negative;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_fee_non_negative CHECK (fee_amount >= 0);
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_rate_positive;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_rate_positive CHECK (exchange_rate > 0);
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_posted_has_journal;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_posted_has_journal
            CHECK (status NOT IN ('POSTED', 'REVERSED') OR journal_id IS NOT NULL);
        ALTER TABLE bank_transfers DROP CONSTRAINT IF EXISTS ck_bank_transfer_reversed_has_journal;
        ALTER TABLE bank_transfers ADD CONSTRAINT ck_bank_transfer_reversed_has_journal
            CHECK (status <> 'REVERSED' OR reversal_journal_id IS NOT NULL);
    END IF;
END $$;

COMMIT;
