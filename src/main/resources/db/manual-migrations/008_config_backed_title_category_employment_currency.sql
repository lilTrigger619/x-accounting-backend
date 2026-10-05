-- =============================================================================
-- Manual migration: let title, supplier category, employment type, currency and
-- country hold user-maintained configuration values
-- =============================================================================
--
-- Customer.title, Supplier.category, Employee.employmentType and every currency
-- column used to be Java enums. They are now plain codes checked against the
-- "titles", "supplier-categories", "employment-types" and "currencies"
-- configurations (Settings > Configurations), so users can add values.
--
-- spring.jpa.hibernate.ddl-auto=update created a CHECK constraint for each of
-- those enum columns and never drops it, so any newly added value (for example
-- the GHS currency) is rejected with:
--   ERROR: new row for relation "payments" violates check constraint
--   "payments_currency_check"
--
-- This also moves old stored values onto the seeded configuration codes:
--   currency GHC (the retired Ghana cedi code the enum used) -> GHS
--   country gh/ng/us/ca/uk/cn (old form values) and USA (demo data) -> ISO codes
--   supplier payment method bank_transfer/check/credit_card/cash (old form
--   values) -> the PaymentMethod codes BANK_TRANSFER/CHEQUE/CARD/CASH
--
-- Run this once against the target Postgres database:
--   psql -U postgres -d xaccounting -f 008_config_backed_title_category_employment_currency.sql
--
-- Safe to run more than once.
-- =============================================================================

BEGIN;

ALTER TABLE customers              DROP CONSTRAINT IF EXISTS customers_title_check;
ALTER TABLE suppliers              DROP CONSTRAINT IF EXISTS suppliers_category_check;
ALTER TABLE employees              DROP CONSTRAINT IF EXISTS employees_employment_type_check;
ALTER TABLE payment_terms          DROP CONSTRAINT IF EXISTS payment_terms_currency_check;
ALTER TABLE supplier_payment_terms DROP CONSTRAINT IF EXISTS supplier_payment_terms_currency_check;
ALTER TABLE payments               DROP CONSTRAINT IF EXISTS payments_currency_check;
ALTER TABLE supplier_payments      DROP CONSTRAINT IF EXISTS supplier_payments_currency_check;

UPDATE payment_terms          SET currency = 'GHS' WHERE currency = 'GHC';
UPDATE supplier_payment_terms SET currency = 'GHS' WHERE currency = 'GHC';
UPDATE payments               SET currency = 'GHS' WHERE currency = 'GHC';
UPDATE supplier_payments      SET currency = 'GHS' WHERE currency = 'GHC';

UPDATE addresses SET country = CASE country
        WHEN 'gh'  THEN 'GH'
        WHEN 'ng'  THEN 'NG'
        WHEN 'us'  THEN 'US'
        WHEN 'USA' THEN 'US'
        WHEN 'ca'  THEN 'CA'
        WHEN 'uk'  THEN 'GB'
        WHEN 'cn'  THEN 'CN'
    END
WHERE country IN ('gh', 'ng', 'us', 'USA', 'ca', 'uk', 'cn');

UPDATE supplier_payment_terms SET payment_method = CASE payment_method
        WHEN 'bank_transfer' THEN 'BANK_TRANSFER'
        WHEN 'check'         THEN 'CHEQUE'
        WHEN 'credit_card'   THEN 'CARD'
        WHEN 'cash'          THEN 'CASH'
    END
WHERE payment_method IN ('bank_transfer', 'check', 'credit_card', 'cash');

COMMIT;
