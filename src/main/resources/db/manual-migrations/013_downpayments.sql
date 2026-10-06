-- =============================================================================
-- Manual migration: Downpayments
-- =============================================================================
--
-- Same root cause as 002-012: spring.jpa.hibernate.ddl-auto=update creates the
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only when a table
-- is first created and never widens it. Downpayments adds:
--   * JournalType.DOWNPAYMENT         (journal_entries.journal_type)
-- The mapping keys CUSTOMER_DOWNPAYMENT_LIABILITY and SUPPLIER_DOWNPAYMENT_ASSET
-- are already in the accounting_mappings list from 011/012.
--
-- Symptom without it: posting a downpayment fails with
--   ERROR: new row for relation "journal_entries" violates check constraint
--   "journal_entries_journal_type_check"
--
-- The list below is the full JournalType list as of this migration (it includes
-- 009's BANK_TRANSFER and 010's DEPOSIT). The downpayments,
-- downpayment_allocations and downpayment_refunds tables are created by
-- Hibernate on startup; the final block adds the guard rails Hibernate cannot
-- express.
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 013_downpayments.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE',
        'CLOSING', 'REVERSING', 'PREPAYMENT', 'DOWNPAYMENT', 'LOAN', 'BANK_TRANSFER', 'DEPOSIT'
    ));

DO $$
BEGIN
    IF to_regclass('downpayments') IS NOT NULL THEN
        ALTER TABLE downpayments DROP CONSTRAINT IF EXISTS ck_downpayment_amount_positive;
        ALTER TABLE downpayments ADD CONSTRAINT ck_downpayment_amount_positive CHECK (amount > 0);
        ALTER TABLE downpayments DROP CONSTRAINT IF EXISTS ck_downpayment_balance_in_range;
        ALTER TABLE downpayments ADD CONSTRAINT ck_downpayment_balance_in_range
            CHECK (available_balance >= 0 AND available_balance <= amount);
        ALTER TABLE downpayments DROP CONSTRAINT IF EXISTS ck_downpayment_buckets_balance;
        ALTER TABLE downpayments ADD CONSTRAINT ck_downpayment_buckets_balance
            CHECK (status IN ('DRAFT', 'CANCELLED', 'REVERSED')
                OR applied_amount + refunded_amount + available_balance = amount);
    END IF;
    IF to_regclass('downpayment_allocations') IS NOT NULL THEN
        ALTER TABLE downpayment_allocations DROP CONSTRAINT IF EXISTS ck_downpayment_allocation_amount_positive;
        ALTER TABLE downpayment_allocations ADD CONSTRAINT ck_downpayment_allocation_amount_positive CHECK (amount > 0);
    END IF;
    IF to_regclass('downpayment_refunds') IS NOT NULL THEN
        ALTER TABLE downpayment_refunds DROP CONSTRAINT IF EXISTS ck_downpayment_refund_amount_positive;
        ALTER TABLE downpayment_refunds ADD CONSTRAINT ck_downpayment_refund_amount_positive CHECK (amount > 0);
    END IF;
END $$;

COMMIT;
