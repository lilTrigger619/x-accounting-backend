-- =============================================================================
-- Manual migration: widen the files.entity_type CHECK constraint for EMPLOYEE
-- =============================================================================
--
-- Same root cause as 002_fix_invoice_status_check_constraint.sql: this project
-- uses spring.jpa.hibernate.ddl-auto=update, which creates a CHECK constraint
-- for an @Enumerated(EnumType.STRING) column only once, when the table is
-- first created - it does NOT widen that constraint later if the Java enum
-- gains new values. EntityType now has EMPLOYEE (employee photos and
-- documents); if your `files` table was created before that value existed on
-- the enum, its CHECK constraint still only allows the old, narrower list.
--
-- Symptom: uploading an employee photo or document fails with:
--   ERROR: new row for relation "files" violates check constraint
--   "files_entity_type_check"
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 003_add_employee_to_files_entity_type_check.sql
--
-- Safe to run even if the constraint is already up to date (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE files DROP CONSTRAINT IF EXISTS files_entity_type_check;
ALTER TABLE files ADD CONSTRAINT files_entity_type_check
    CHECK (entity_type IN (
        'INVOICE', 'CUSTOMER', 'SUPPLIER', 'PAYMENT', 'EXPENSE', 'PRODUCT', 'COMPANY', 'USER',
        'EMPLOYEE',
        'GENERATED_DOCUMENT',
        'DOCUMENT_TEMPLATE_INVOICE', 'DOCUMENT_TEMPLATE_QUOTE', 'DOCUMENT_TEMPLATE_PURCHASE_ORDER',
        'DOCUMENT_TEMPLATE_CREDIT_NOTE', 'DOCUMENT_TEMPLATE_DELIVERY_NOTE', 'DOCUMENT_TEMPLATE_RECEIPT',
        'GENERAL_JOURNAL', 'SALES_JOURNAL', 'PURCHASE_JOURNAL', 'PAYROLL_JOURNAL',
        'ADJUSTMENT_JOURNAL', 'OPENING_BALANCE_JOURNAL', 'CLOSING_JOURNAL', 'REVERSING_JOURNAL'
    ));

COMMIT;
