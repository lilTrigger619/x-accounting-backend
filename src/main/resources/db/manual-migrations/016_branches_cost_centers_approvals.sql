-- =============================================================================
-- Manual migration: branches, cost centers, scoped access and journal approvals
-- =============================================================================
--
-- The application now does all of this itself on every start (BranchBackfillRunner), so
-- you only need this file if that runner logged an error, or if you prefer to migrate by
-- hand before deploying.
--
-- 1. JournalStatus gained SUBMITTED, APPROVED and REJECTED (approval workflow). As in
--    002-015, ddl-auto=update never widens an existing enum CHECK constraint, so posting
--    a submitted journal would fail with:
--      ERROR: new row for relation "journal_entries" violates check constraint
--      "journal_entries_status_check"
-- 2. Every journal and journal line now carries a branch and cost center. Hibernate adds
--    the columns; this file creates the Head Office branch (HO) and its default cost
--    center (HO-GEN) and moves all existing journals and lines into it.
--
-- Run after starting the new version once (so Hibernate has created the new tables and
-- columns):
--   psql -U postgres -d xaccounting -f 016_branches_cost_centers_approvals.sql
--
-- Safe to run more than once (idempotent).
-- =============================================================================

BEGIN;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_status_check;
ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_status_check
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'POSTED', 'REVERSED', 'CANCELLED'));

-- Head Office branch
INSERT INTO branches (code, name, description, is_head_office, active, is_deleted, created_at, version)
SELECT 'HO', 'Head Office',
       'Main office. Everything recorded before branches were introduced belongs here.',
       TRUE, TRUE, FALSE, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM branches WHERE is_head_office = TRUE AND is_deleted = FALSE);

-- Its default cost center
INSERT INTO cost_centers (code, name, description, branch_id, is_branch_default, active, is_deleted, created_at, version)
SELECT b.code || '-GEN', b.name || ' - General',
       'Branch-level costs that aren''t charged to a department',
       b.id, TRUE, TRUE, FALSE, NOW(), 0
FROM branches b
WHERE b.is_head_office = TRUE AND b.is_deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM cost_centers c
                  WHERE c.branch_id = b.id AND c.is_branch_default = TRUE AND c.is_deleted = FALSE);

-- Existing journals and lines go to the Head Office
UPDATE journal_lines l
SET cost_center_id = c.id, branch_id = c.branch_id
FROM cost_centers c JOIN branches b ON b.id = c.branch_id
WHERE l.cost_center_id IS NULL
  AND b.is_head_office = TRUE AND b.is_deleted = FALSE
  AND c.is_branch_default = TRUE AND c.is_deleted = FALSE;

UPDATE journal_entries j
SET cost_center_id = c.id, branch_id = c.branch_id
FROM cost_centers c JOIN branches b ON b.id = c.branch_id
WHERE j.cost_center_id IS NULL
  AND b.is_head_office = TRUE AND b.is_deleted = FALSE
  AND c.is_branch_default = TRUE AND c.is_deleted = FALSE;

COMMIT;

-- Existing users are given Global scope once by the application on its first start
-- (branch_accounting_settings.scope_backfill_done). Users created later get Head Office
-- access until an admin sets their scope in Users > Access scope.
