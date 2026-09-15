-- =============================================================================
-- Manual migration: refresh the invoices.status CHECK constraint
-- =============================================================================
--
-- This project uses spring.jpa.hibernate.ddl-auto=update, which creates a
-- CHECK constraint for an @Enumerated(EnumType.STRING) column only once, when
-- the table is first created — it does NOT widen that constraint later if the
-- Java enum gains new values. InvoiceStatus now has PARTIALLY_PAID and
-- OVERDUE; if your `invoices` table was created before either value existed
-- on the enum, its CHECK constraint still only allows the old, narrower list.
--
-- Symptom: seeding or normal use fails once an invoice transitions to one of
-- the missing statuses (e.g. a partial payment sets PARTIALLY_PAID), with:
--   ERROR: new row for relation "invoices" violates check constraint
--   "invoices_status_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 002_fix_invoice_status_check_constraint.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check;
ALTER TABLE invoices ADD CONSTRAINT invoices_status_check
    CHECK (status IN ('DRAFT', 'SENT', 'PARTIALLY_PAID', 'PAID', 'CANCELLED', 'OVERDUE'));

COMMIT;
