-- =============================================================================
-- Manual migration: full Loans module (directions, lifecycle, interest methods,
-- fees, payments, accruals, audit trail)
-- =============================================================================
--
-- The Loans module moved to the spec's vocabulary:
--   * direction            BORROWED/LENT          -> BORROWED_LOAN/LENT_LOAN
--                          (loans.direction and loan_types.default_direction)
--   * status               PARTIALLY_REPAID       -> PARTIALLY_PAID
--                          FULLY_SETTLED          -> FULLY_PAID
--                          WRITTEN_OFF            -> CLOSED
--                          OVERDUE, RESTRUCTURED  -> ACTIVE / PARTIALLY_PAID
--                          (overdue is now worked out per installment)
--   * interest_method      SIMPLE/AMORTIZED + repayment_method
--                          -> SIMPLE / FIXED_INSTALLMENT / REDUCING_BALANCE / CUSTOM_SCHEDULE
--                          (interest-only and balloon loans keep their stored schedule as
--                          CUSTOM_SCHEDULE)
--   * fee_treatment        AMORTIZED              -> CAPITALIZED
--   * interest_type and repayment_method are no longer used; their NOT NULL is dropped.
--
-- New columns (added here if the app has not started since, so the order does not
-- matter) are backfilled so existing loans keep working, and every schedule line keeps
-- a copy of its generated amounts for payment reversal.
--
-- Also widens the CHECK constraints ddl-auto created once and never widens:
--   files.entity_type                 + LOAN (loan attachments)
--   accounting_mappings.mapping_key   + LOAN_FEE_INCOME, LOAN_WRITE_OFF_EXPENSE
--   loan enum columns                 new values above
-- The files and accounting_mappings lists are every EntityType and MappingKey value at the
-- time of writing, including those added by 009 to 011.
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 012_loans_module.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

-- ---- Old enum CHECK constraints -------------------------------------------
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_direction_check;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_status_check;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_interest_method_check;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_interest_type_check;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_repayment_method_check;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_fee_treatment_check;
ALTER TABLE loan_types DROP CONSTRAINT IF EXISTS loan_types_default_direction_check;
ALTER TABLE loan_amortization_lines DROP CONSTRAINT IF EXISTS loan_amortization_lines_status_check;

-- ---- New columns ------------------------------------------------------------
ALTER TABLE loans ALTER COLUMN interest_method TYPE VARCHAR(30);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS grace_period_installments INTEGER;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS accrued_interest_total NUMERIC(19, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS installment_fee NUMERIC(19, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS allow_overpayment BOOLEAN;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS overpayment_balance NUMERIC(19, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS written_off_amount NUMERIC(19, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS collateral_description TEXT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS collateral_value NUMERIC(19, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS write_off_journal_id BIGINT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS defaulted_at TIMESTAMP;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMP;

ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS fees_due NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS fees_paid NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS original_opening_principal NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS original_principal_due NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS original_interest_due NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS original_fees_due NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS original_closing_principal NUMERIC(19, 2);
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS missed BOOLEAN;
ALTER TABLE loan_amortization_lines ADD COLUMN IF NOT EXISTS missed_note VARCHAR(500);

ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS overpayment_amount NUMERIC(19, 2);
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS accrued_interest_applied NUMERIC(19, 2);
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS payment_type VARCHAR(20);
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20);
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS reversal_journal_id BIGINT;
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMP;
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS reversal_reason VARCHAR(500);

-- ---- Retired columns ----------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'loans' AND column_name = 'interest_type') THEN
        ALTER TABLE loans ALTER COLUMN interest_type DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'loans' AND column_name = 'repayment_method') THEN
        ALTER TABLE loans ALTER COLUMN repayment_method DROP NOT NULL;
        UPDATE loans SET interest_method = CASE
                WHEN repayment_method IN ('CUSTOM_SCHEDULE', 'INTEREST_ONLY', 'BALLOON_PAYMENT') THEN 'CUSTOM_SCHEDULE'
                WHEN interest_method = 'SIMPLE' THEN 'SIMPLE'
                WHEN repayment_method = 'EQUAL_PRINCIPAL' THEN 'REDUCING_BALANCE'
                ELSE 'FIXED_INSTALLMENT'
            END
        WHERE interest_method NOT IN ('FIXED_INSTALLMENT', 'REDUCING_BALANCE', 'CUSTOM_SCHEDULE');
    END IF;
END $$;
UPDATE loans SET interest_method = 'FIXED_INSTALLMENT' WHERE interest_method = 'AMORTIZED';

-- ---- Values -------------------------------------------------------------------
UPDATE loans SET direction = 'BORROWED_LOAN' WHERE direction = 'BORROWED';
UPDATE loans SET direction = 'LENT_LOAN' WHERE direction = 'LENT';
UPDATE loan_types SET default_direction = 'BORROWED_LOAN' WHERE default_direction = 'BORROWED';
UPDATE loan_types SET default_direction = 'LENT_LOAN' WHERE default_direction = 'LENT';

UPDATE loans SET status = 'PARTIALLY_PAID' WHERE status = 'PARTIALLY_REPAID';
UPDATE loans SET status = 'FULLY_PAID' WHERE status = 'FULLY_SETTLED';
UPDATE loans SET status = 'CLOSED' WHERE status = 'WRITTEN_OFF';
UPDATE loans l SET status = CASE
        WHEN EXISTS (SELECT 1 FROM loan_repayments r WHERE r.loan_id = l.id) THEN 'PARTIALLY_PAID'
        ELSE 'ACTIVE'
    END
WHERE status IN ('OVERDUE', 'RESTRUCTURED');

UPDATE loans SET fee_treatment = 'CAPITALIZED' WHERE fee_treatment = 'AMORTIZED';

UPDATE loans SET grace_period_installments = 0 WHERE grace_period_installments IS NULL;
UPDATE loans SET accrued_interest_total = COALESCE(outstanding_interest, 0) WHERE accrued_interest_total IS NULL;
UPDATE loans SET installment_fee = 0 WHERE installment_fee IS NULL;
UPDATE loans SET allow_overpayment = FALSE WHERE allow_overpayment IS NULL;
UPDATE loans SET overpayment_balance = 0 WHERE overpayment_balance IS NULL;
UPDATE loans SET written_off_amount = 0 WHERE written_off_amount IS NULL;

UPDATE loan_amortization_lines SET fees_due = 0 WHERE fees_due IS NULL;
UPDATE loan_amortization_lines SET fees_paid = 0 WHERE fees_paid IS NULL;
UPDATE loan_amortization_lines SET missed = FALSE WHERE missed IS NULL;
UPDATE loan_amortization_lines
SET original_opening_principal = opening_principal,
    original_principal_due = principal_due,
    original_interest_due = interest_due,
    original_fees_due = fees_due,
    original_closing_principal = closing_principal
WHERE original_principal_due IS NULL;

UPDATE loan_repayments SET overpayment_amount = 0 WHERE overpayment_amount IS NULL;
UPDATE loan_repayments SET accrued_interest_applied = 0 WHERE accrued_interest_applied IS NULL;
UPDATE loan_repayments SET payment_status = 'POSTED' WHERE payment_status IS NULL;

-- ---- New CHECK constraints ------------------------------------------------------
ALTER TABLE loans ADD CONSTRAINT loans_direction_check
    CHECK (direction IN ('BORROWED_LOAN', 'LENT_LOAN'));
ALTER TABLE loans ADD CONSTRAINT loans_status_check
    CHECK (status IN ('DRAFT', 'APPROVED', 'ACTIVE', 'PARTIALLY_PAID', 'FULLY_PAID', 'DEFAULTED',
                      'CLOSED', 'CANCELLED', 'REVERSED'));
ALTER TABLE loans ADD CONSTRAINT loans_interest_method_check
    CHECK (interest_method IN ('SIMPLE', 'FIXED_INSTALLMENT', 'REDUCING_BALANCE', 'CUSTOM_SCHEDULE'));
ALTER TABLE loans ADD CONSTRAINT loans_fee_treatment_check
    CHECK (fee_treatment IS NULL OR fee_treatment IN ('EXPENSED_IMMEDIATELY', 'CAPITALIZED'));
ALTER TABLE loan_types ADD CONSTRAINT loan_types_default_direction_check
    CHECK (default_direction IS NULL OR default_direction IN ('BORROWED_LOAN', 'LENT_LOAN'));
ALTER TABLE loan_amortization_lines ADD CONSTRAINT loan_amortization_lines_status_check
    CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'MISSED'));

ALTER TABLE files DROP CONSTRAINT IF EXISTS files_entity_type_check;
ALTER TABLE files ADD CONSTRAINT files_entity_type_check
    CHECK (entity_type IN (
        'INVOICE', 'CUSTOMER', 'SUPPLIER', 'PAYMENT', 'EXPENSE', 'PRODUCT', 'COMPANY',
        'USER', 'EMPLOYEE', 'DEPOSIT', 'GENERATED_DOCUMENT', 'DOCUMENT_TEMPLATE_INVOICE',
        'DOCUMENT_TEMPLATE_QUOTE', 'DOCUMENT_TEMPLATE_PURCHASE_ORDER',
        'DOCUMENT_TEMPLATE_CREDIT_NOTE', 'DOCUMENT_TEMPLATE_DELIVERY_NOTE',
        'DOCUMENT_TEMPLATE_RECEIPT', 'GENERAL_JOURNAL', 'SALES_JOURNAL', 'PURCHASE_JOURNAL',
        'PAYROLL_JOURNAL', 'ADJUSTMENT_JOURNAL', 'OPENING_BALANCE_JOURNAL',
        'CLOSING_JOURNAL', 'REVERSING_JOURNAL', 'BANK_TRANSFER', 'LOAN'
    ));

ALTER TABLE accounting_mappings DROP CONSTRAINT IF EXISTS accounting_mappings_mapping_key_check;
ALTER TABLE accounting_mappings ADD CONSTRAINT accounting_mappings_mapping_key_check
    CHECK (mapping_key IN (
        'PAYMENT_BANK_ACCOUNT', 'PAYMENT_CASH_ACCOUNT', 'PAYMENT_ACCOUNTS_RECEIVABLE',
        'PAYMENT_CUSTOMER_ADVANCES', 'CUSTOMER_DOWNPAYMENT_LIABILITY',
        'INVOICE_ACCOUNTS_RECEIVABLE', 'INVOICE_REVENUE', 'INVOICE_SALES_TAX_PAYABLE',
        'BILL_ACCOUNTS_PAYABLE', 'BILL_DEFAULT_EXPENSE', 'BILL_PURCHASE_TAX_RECEIVABLE',
        'SUPPLIER_PAYMENT_BANK_ACCOUNT', 'SUPPLIER_PAYMENT_CASH_ACCOUNT',
        'SUPPLIER_PAYMENT_ADVANCES', 'SUPPLIER_DOWNPAYMENT_ASSET',
        'TAX_WITHHOLDING_PAYABLE', 'PAYROLL_SALARY_PAYABLE',
        'PAYROLL_DEFAULT_SALARY_EXPENSE', 'PAYROLL_EMPLOYEE_TAX_PAYABLE',
        'PAYROLL_LOAN_RECEIVABLE', 'PAYROLL_ADVANCE_RECEIVABLE',
        'PAYROLL_REIMBURSEMENT_PAYABLE', 'PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE',
        'PAYROLL_PAYMENT_BANK_ACCOUNT', 'CLOSING_RETAINED_EARNINGS',
        'PREPAYMENT_DEFAULT_ASSET', 'PREPAYMENT_DEFAULT_EXPENSE', 'PREPAYMENT_BANK_ACCOUNT',
        'LOAN_RECEIVABLE', 'LOAN_PAYABLE', 'LOAN_INTEREST_INCOME', 'LOAN_INTEREST_EXPENSE',
        'LOAN_INTEREST_RECEIVABLE', 'LOAN_INTEREST_PAYABLE', 'LOAN_FEE_EXPENSE',
        'LOAN_BANK_ACCOUNT', 'LOAN_FEE_INCOME', 'LOAN_WRITE_OFF_EXPENSE',
        'BANK_TRANSFER_CHARGES', 'FX_GAIN', 'FX_LOSS', 'DEPOSIT_PAID_ASSET',
        'DEPOSIT_RECEIVED_LIABILITY', 'DEPOSIT_FORFEIT_INCOME', 'DEPOSIT_FORFEIT_EXPENSE',
        'DEPOSIT_BANK_ACCOUNT', 'BANK_CHARGES_EXPENSE', 'BANK_INTEREST_INCOME',
        'BANK_RECONCILIATION_SUSPENSE'
    ));

COMMIT;
