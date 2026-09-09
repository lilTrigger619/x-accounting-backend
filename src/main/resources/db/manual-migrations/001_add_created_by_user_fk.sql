-- =============================================================================
-- Manual migration: turn created_by into a real foreign key to users(id)
-- =============================================================================
--
-- This project uses spring.jpa.hibernate.ddl-auto=update, which does NOT
-- reliably alter an existing column's data type (e.g. varchar -> uuid) on a
-- table that already has rows. Since `created_by` on these 8 tables was
-- previously a loosely-typed varchar column (populated manually in some
-- rows), this migration must be run BY HAND, once, BEFORE deploying the
-- updated application code (BaseEntity.createdBy is now a @ManyToOne User).
--
-- Run this against the target Postgres database:
--   psql -U postgres -d xaccounting -f 001_add_created_by_user_fk.sql
--
-- IMPORTANT: every non-null value currently in a `created_by` column below
-- must already be a valid UUID matching a row in users(id) (as described,
-- these were populated manually). If any table has garbage values (e.g. a
-- username or "system" instead of a UUID), the ALTER on that table will
-- fail — fix or null out those rows first:
--   UPDATE <table> SET created_by = NULL WHERE created_by !~
--     '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
--
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE journal_entries
    ADD CONSTRAINT fk_journal_entries_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE journal_lines
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE journal_lines
    ADD CONSTRAINT fk_journal_lines_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE payments
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE payment_allocations
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE payment_allocations
    ADD CONSTRAINT fk_payment_allocations_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE payment_refunds
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE payment_refunds
    ADD CONSTRAINT fk_payment_refunds_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE supplier_payments
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE supplier_payments
    ADD CONSTRAINT fk_supplier_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE supplier_payment_allocations
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE supplier_payment_allocations
    ADD CONSTRAINT fk_supplier_payment_allocations_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE document_templates
    ALTER COLUMN created_by TYPE uuid USING NULLIF(created_by, '')::uuid;
ALTER TABLE document_templates
    ADD CONSTRAINT fk_document_templates_created_by FOREIGN KEY (created_by) REFERENCES users(id);

COMMIT;
