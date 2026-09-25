-- =============================================================================
-- Manual migration: widen the journal_entries.journal_type CHECK constraint
-- =============================================================================
--
-- Same root cause as 002/003/004/005: spring.jpa.hibernate.ddl-auto=update creates a
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only once, when the table
-- is first created - it does NOT widen that constraint later when the Java enum gains
-- new values. JournalType gained PREPAYMENT and LOAN (Phase 3's Prepayments/Loans
-- subsystems each post through their own journal type, mirroring how PURCHASE/SALES/
-- PAYROLL already have their own); if your journal_entries table was created before
-- those existed on the enum, its CHECK constraint still only allows the old, narrower
-- list.
--
-- Symptom: creating a Prepayment or Loan journal fails with:
--   ERROR: new row for relation "journal_entries" violates check constraint
--   "journal_entries_journal_type_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 006_widen_journal_type_check_phase3.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_journal_type_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_journal_type_check
    CHECK (journal_type IN (
        'GENERAL', 'SALES', 'PURCHASE', 'PAYROLL', 'ADJUSTMENT', 'OPENING_BALANCE',
        'CLOSING', 'REVERSING', 'PREPAYMENT', 'LOAN'
    ));

COMMIT;
