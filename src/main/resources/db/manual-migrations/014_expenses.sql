-- =============================================================================
-- Manual migration: Expenses
-- =============================================================================
--
-- Same root cause as 002-013: spring.jpa.hibernate.ddl-auto=update creates the
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only when a table
-- is first created and never widens it. Expenses adds:
--   * JournalType.EXPENSE             (journal_entries.journal_type)
-- (EntityType.EXPENSE, used for attachments, was already in files.entity_type.)
--
-- The new expenses, expense_lines and expense_activities tables, and their own
-- CHECK/UNIQUE constraints, are created by Hibernate on startup. The final
-- block re-adds those constraints in case a table was created by an older
-- build without them.
--
-- Symptom without it: posting an expense fails with
--   violates check constraint "journal_entries_journal_type_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 014_expenses.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE',
        'CLOSING', 'REVERSING', 'PREPAYMENT', 'DOWNPAYMENT', 'LOAN', 'BANK_TRANSFER', 'DEPOSIT',
        'EXPENSE'
    ));

DO $$
BEGIN
    IF to_regclass('expenses') IS NOT NULL THEN
        ALTER TABLE expenses DROP CONSTRAINT IF EXISTS ck_expense_total_positive;
        ALTER TABLE expenses ADD CONSTRAINT ck_expense_total_positive CHECK (total_amount > 0);
        ALTER TABLE expenses DROP CONSTRAINT IF EXISTS ck_expense_rate_positive;
        ALTER TABLE expenses ADD CONSTRAINT ck_expense_rate_positive CHECK (exchange_rate > 0);
        ALTER TABLE expenses DROP CONSTRAINT IF EXISTS ck_expense_posted_has_journal;
        ALTER TABLE expenses ADD CONSTRAINT ck_expense_posted_has_journal
            CHECK (status NOT IN ('POSTED', 'REVERSED') OR journal_id IS NOT NULL);
        ALTER TABLE expenses DROP CONSTRAINT IF EXISTS ck_expense_reversed_has_journal;
        ALTER TABLE expenses ADD CONSTRAINT ck_expense_reversed_has_journal
            CHECK (status <> 'REVERSED' OR reversal_journal_id IS NOT NULL);
    END IF;
    IF to_regclass('expense_lines') IS NOT NULL THEN
        ALTER TABLE expense_lines DROP CONSTRAINT IF EXISTS ck_expense_line_amount_positive;
        ALTER TABLE expense_lines ADD CONSTRAINT ck_expense_line_amount_positive CHECK (amount > 0);
    END IF;
END $$;

COMMIT;
