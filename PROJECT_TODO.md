# Enterprise Accounting Application — Master TODO

Consolidated status across both repositories:
- **Frontend (FE):** `expense-recorder` (React + Vite + TS + shadcn/Tailwind)
- **Backend (BE):** `x-accounting-backend` (Java Spring Boot + Gradle)

> **Update 1:** Accounts Payable core (Bills, Supplier Payments, AR/AP Aging, Customer/Supplier
> Statements) was built end-to-end — see §2/§3/§10 below and the "AP Build" note at the top of
> §0 for exactly what shipped and what's still open.
>
> **Update 2:** The AR Payment Receipts screens and the Suppliers list — flagged in Update 1 as
> still running on mock data — are now wired to the real backend too. See "AR Wiring" note below.
> AR payments also now post real GL journals (`PaymentJournalService` wired into
> `PaymentServiceImpl`/`PaymentAllocationServiceImpl`), matching AP.
>
> **Update 3:** Customers list is wired to the real backend, and there's a new `CustomerViewPage`
> detail screen with a real cross-entity activity log (customer created, status changed,
> invoices created/sent, payments received/allocated, emails sent) — each entry deep-links to
> its source record. See "Customer Wiring" note below.
>
> **Update 4:** New Products/Services module (BE: `Product` entity/`ProductController` at
> `/api/products` with image upload via the existing generic file-attachment system; `TaxCategory`
> entity/`TaxCategoryController` at `/api/tax-categories`, seeded with Sales Tax, VAT and
> Withholding Tax rows; FE: `ProductsPage`/`ProductsList`, `ProductForm` with "Basic Info" (image,
> name, item type, category, cost group) and "Sales" (description, price/rate, income account via
> a new `SelectAccountModal` defaulting to the seeded "Sales" account, tax category dropdown)
> sections). Invoice line items (`AddInvoiceLineModal`) can now search and select a product/
> service, which prefills description/price/tax rate while leaving them editable. See "Products
> & Services" note below.
>
> **Update 5:** The "Send Invoice" dialog can now preview exactly what will be sent — the
> rendered email (recipient/subject/body) and the invoice PDF, using the actual invoice and the
> currently-selected template — before you click Send. This surfaced (and fixed) a real bug: the
> send endpoint was silently ignoring the template/email/subject/message the dialog collected.
> See "Invoice Send Preview" note below.
>
> **Update 6:** `created_by` now has real referential integrity to `users` (it was a
> never-populated varchar column on `journal_entries`, `journal_lines`, `payments`,
> `payment_allocations`, `payment_refunds`, `supplier_payments`,
> `supplier_payment_allocations`, and `document_templates`) — see "Created By Audit Trail"
> note below, **including a manual DB migration you must run once**.
>
> **Update 7:** The invoice preview modal in the Document Template Designer ("Invoice preview",
> opened via the toolbar's Preview button) now has a "Download PDF" button that renders the
> current unsaved designer configuration as a real PDF (new `POST
> /api/document-templates/{id}/sample-preview-pdf`, reusing the same sample-data rendering
> pipeline as the existing HTML preview).

This file is the single source of truth for what exists vs. what remains, verified directly
against the code (routes, controllers, entities, services) rather than assumed. It mirrors the
structure of the master project prompt. Update the checkboxes as work lands — do not remove
completed items, mark them `[x]`.

Legend: `[x]` Done · `[~]` Partial / one side only · `[ ]` Not started

---

## 0. Reality Check vs. "Completed" List in the Master Prompt

### AP Build (this pass) — what shipped and what's still open

Built a full Accounts Payable core, mirroring the existing AR (Invoice/Payment) architecture
exactly, plus the two AR reports that were missing (aging, statements):

- **Bills** (BE: `Bill`/`BillItem` entities, `BillController` at `/api/bills`, `BillService`,
  `BillCalculationService`, `BillNumberGenerator`; FE: `BillsPage`/`BillsList`, `BillNewPage`
  create+edit, `BillViewPage`) — draft → approve → paid lifecycle. Approving a bill posts a real
  GL journal (Dr default expense account, Cr Accounts Payable) via `APJournalService`.
- **Supplier Payments** (BE: `SupplierPaymentEntity`/`SupplierPaymentAllocationEntity`,
  `SupplierPaymentController` at `/api/supplier-payments`, `SupplierPaymentService`,
  `SupplierPaymentAllocationService` with allocate/auto-allocate-oldest/auto-allocate-largest/
  remove/clear, `SupplierPaymentNumberGenerator`; FE: `SupplierPaymentsPage`,
  `RecordSupplierPaymentPage`, `SupplierPaymentDetailsPage`, `AllocateSupplierPaymentPage`) —
  creating a payment posts a real GL journal (Dr AP/Advances, Cr Bank) and each allocation change
  posts its own adjusting journal. **This is more complete than the existing AR Payment module**:
  `PaymentJournalService` (AR side) exists but was never actually invoked by `PaymentServiceImpl`
  — a pre-existing gap, not touched here, but worth fixing for parity (see priority list).
- **AR/AP Aging** (BE: `AgingReportService`, `AgingController` at `/api/reports/ar-aging` and
  `/api/reports/ap-aging` — current/1-30/31-60/61-90/90+ buckets computed from open Invoice/Bill
  balances; FE: `ArAgingPage`, `ApAgingPage`).
- **Customer/Supplier Statements** (BE: `StatementService`, `StatementController` at
  `/api/customers/{id}/statement` and `/api/suppliers/{id}/statement` — running-balance
  statement combining invoices/bills with payment allocations over a date range; FE:
  `CustomerStatementPage`, `SupplierStatementPage`, printable).
- Small additive BE changes made to support the above: `GET /api/bills/outstanding?supplierId=`
  (bills still owing a balance, for the payment-allocation screen), `PaymentAllocationRepository
  .findByInvoice_CustomerId`, `SupplierPaymentAllocationRepository.findByBill_SupplierId`.

**Deliberately deferred (not in this pass)** — flagged rather than silently skipped:
- **Supplier Credits/Refunds** — AP mirror of `PaymentRefundEntity` wasn't built; only
  payment *creation* and *allocation* exist for AP, no refund workflow.
- **Purchase Orders / Purchase-to-Bill** — still just a document-template category, no entity.
- **Recurring Bills** — not built (depends on a recurrence engine that doesn't exist yet).
- **Bill/Supplier-payment attachments, email, PDF** — Bills don't have a document-template
  renderer yet (Invoices do); attachments infra (`FileService`) wasn't wired in.
- **Bill numbering / GL account IDs are config-driven placeholders**, same pattern the existing
  AR module already uses (`payment.journal.*` in `application.properties`) — the new
  `bill.journal.*` / `supplierpayment.journal.*` keys point at account IDs `4`/`5`/`6` that need
  to actually exist in the `account` table for GL posting to succeed at runtime; update these
  once real Chart of Accounts IDs for Accounts Payable / Expense / Supplier Advances are known.

### AR Wiring (this pass) — Payment Receipts + Suppliers list now real

Previously `services/payment.service.ts` (backing `PaymentReceiptsListPage`, `ReceivePaymentPage`,
`AllocatePaymentPage`, `PaymentDetailsPage`) and `SuppliersList.tsx` read from
`mock/payments.mock.ts` / a hardcoded array — the pages were fully built but not connected. Fixed:

- **`services/payment.service.ts` rewritten** to call the real `/api/payments`, `/api/customers`,
  `/api/invoices`, `/api/chart-of-accounts` endpoints, adapting backend DTOs to the exact same
  `Payment`/`Customer`/`OutstandingInvoice`/`BankAccount` shapes the pages already expect — no
  page component needed to change. Notable adapter details:
  - `createPayment` creates the payment first (without allocations, to avoid a latent bug in
    `PaymentServiceImpl.initializePayment` where allocations passed at creation time don't get
    their running totals recalculated), then calls the separate `/allocate` endpoint — same
    pattern used for the new AP `SupplierPaymentService`.
  - `updateAllocations` (used by the "replace the whole allocation set" AR allocate screen)
    clears existing allocations via `DELETE /allocation` then re-applies the desired set, since
    the real backend models allocation as incremental add/remove rather than "set full state."
  - Backend `Currency` enum has `GHC`, not `GHS` (which the AR screens hardcode as the default);
    translated transparently at the service boundary (`toBackendCurrency`/`fromBackendCurrency`)
    rather than changing the UI's currency labels.
  - `Customer.outstandingBalance` / `creditBalance` in the picker list default to `0` — the thin
    `CustomerResponseDTO` used by the list/search endpoint has no phone number or balance
    aggregate, and computing a live balance per row in a typeahead would mean an expensive
    per-customer fetch. Honest `0` rather than fabricated numbers; a real aggregate would need a
    new backend endpoint (same shape as the AP `Aging` work) if this needs to be accurate.
  - `nextReceiptNumber()` returns a cosmetic placeholder — the backend always generates the real
    receipt number server-side on save and ignores whatever the client sends.
- **Small additive BE change**: `GET /api/invoices/outstanding?customerId=` (mirrors the AP
  `/api/bills/outstanding` endpoint added earlier), used to populate the receive-payment
  allocation table with real open invoices instead of mock ones.
- **`SuppliersList.tsx` rewritten** to call `SupplierRequests.getSuppliers` (already real, just
  unused by this screen) with server-side pagination/search, plus a "Balance Owed" column
  computed from the new `GET /api/reports/ap-aging` endpoint instead of a fabricated number.

**Still not wired / worth knowing:**
- Payment receipt PDF generation, email, attachments, and activity-timeline endpoints on the AR
  side are pre-existing backend stubs (`PaymentController` returns empty bytes / empty lists for
  `/receipt`, `/attachments`, `/activities`, `/emails`) — the adapter surfaces whatever those
  stubs give back (empty), it doesn't fabricate content, but those features aren't functional
  end-to-end yet.
- `Payment.customerCode`/`phone` on **list rows** (not detail) are blank — `PaymentListItemResponse`
  doesn't carry them; only the single-payment detail endpoint does.

### Customer Wiring (this pass) — real list, detail screen, cross-entity activity log

`CustomersList.tsx` was reading from a hardcoded array like Suppliers was; fixed the same way
(real `CustomerRequests.getCustomers` with pagination/search, balance column from AR aging).
Beyond that, built a genuinely new capability: a customer detail screen with a real activity feed.

- **`CustomerResponseDTO` enriched** (BE: `CustomerMapper.toResponse`) — now also returns phone,
  mobile, website, createdAt, billingAddress, shippingAddress, paymentTerms, and taxInfo (all
  already eagerly loaded on the `Customer` entity via default `@OneToOne` fetch, so no extra
  queries). The list endpoint got these too since it reuses the same mapper — harmless, ignored
  by callers that only read the thin fields.
- **`CustomerActivityLog`** (BE: new entity + `CustomerActivityLogRepository` +
  `CustomerActivityLogService`, `GET /api/customers/{id}/activity`) — a per-customer, append-only
  feed. `CustomerActivityLogService.record(...)` runs in its own `REQUIRES_NEW` transaction and
  swallows its own exceptions, so a broken log write can never roll back the real business
  transaction (payment creation, invoice send, etc.) it's attached to.
- **Wired into real triggers**, not just a stub table: `CustomerServiceImpl.createCustomer`
  (CREATED), a new `PATCH /api/customers/{id}/status` endpoint (STATUS_CHANGED — this endpoint
  didn't exist before; customers had no update path at all), `InvoiceService.createInvoice`
  (INVOICE_CREATED), `InvoiceEmailService.sendInvoice` (INVOICE_SENT),
  `InvoiceEmailRequestedEventListener` (EMAIL_SENT, only on confirmed delivery),
  `PaymentServiceImpl.createPayment` (PAYMENT_RECEIVED), and `PaymentAllocationServiceImpl
  .allocatePayment` (one consolidated PAYMENT_ALLOCATED entry per allocate call, not one per line,
  to avoid a noisy feed).
- **FE: `CustomerViewPage`** — contact info, billing/shipping address, payment terms & tax info,
  an Activate/Deactivate action (using the new status endpoint), outstanding balance (AR aging)
  with a link to the existing `CustomerStatementPage`, and the activity timeline itself. Each
  entry is clickable when it has a `referenceType`/`referenceId`: `INVOICE` and `EMAIL` route to
  `/invoices/:id`, `PAYMENT` routes to `/payments/:id` (the real payment detail page, now that AR
  payments are wired to the backend); `CUSTOMER`-referenced entries (created, status changed)
  aren't clickable — there's nowhere more specific to send them.

**Worth knowing:** this is a *customer-scoped* activity log, not a system-wide audit trail —
suppliers, bills, journals, users/roles, and report templates still have no equivalent (report
templates have their own separate `ReportTemplateHistory`, unrelated to this). A general-purpose
audit log covering every entity is still the bigger, unbuilt item in §14.

Most claimed-complete items check out. A few corrections found during this audit:

- **Banking / Transfers** — claimed "started" in the master prompt, but **no banking code exists
  in either repo** (no `BankAccount` entity/controller in BE, no banking routes/pages in FE;
  "bank" only appears in FE/BE as a payment *method* enum value). Treat as not started.
- **Employees** — FE has full CRUD UI (`EmployeeForm.tsx`, `EmployeesList.tsx`,
  `EmployeesPage.tsx`, `EmployeeNewPage.tsx`) but **BE has no `Employee` entity, controller, or
  service at all.** The FE screens are not backed by a real API.
- **Expenses** — FE has full CRUD UI (`ExpenseForm.tsx`, `ExpensesList.tsx`,
  `AddExpenseLineModal.tsx`, `ExpensesListPage.tsx`) but **BE has no `Expense` entity/controller**
  — `EXPENSE` only exists as an `AccountType`/`EntityType` enum value, not a transaction module.
- **Tax Configuration** — FE has a Taxes screen; BE has no standalone `Tax`/`TaxRate` entity —
  only `TaxInfo` (customer) and `WithholdingTax` (supplier) sub-records exist. No tax engine.
- **Credit Limits** — actually **already implemented**: `PaymentTerms.creditLimit` (BE entity)
  and surfaced via `CustomerMapper`/`PaymentTermsDTO`. Not called out in the master prompt's
  completed list but it's done.
- BE enums (`EntityType`, `DocumentModule`, `documenttemplate.enums.DocumentType`) already
  anticipate `QUOTE`, `PURCHASE_ORDER`, `CREDIT_NOTE`, `DELIVERY_NOTE`, `RECEIPT`,
  `PAYROLL_JOURNAL`, `ADJUSTMENT_JOURNAL`, `OPENING_BALANCE_JOURNAL`, `CLOSING_JOURNAL` as
  document/journal *categories* for the document-template system — but there is **no actual
  transactional logic** behind any of them except `REVERSING_JOURNAL` (Journal Reversal, which
  is fully implemented).

### Products & Services (this pass) — new module, wired into invoice line items

There was no catalog of sellable items anywhere in either repo — every invoice line was
free-typed from scratch. Built a Products/Services module per spec and wired it into invoice
line entry:

- **`Product`** (BE: new entity/`ProductRepository`/`ProductService`/`ProductController` at
  `/api/products`, multipart create/update mirroring `InvoiceController`'s pattern — a `request`
  JSON part plus an optional `image` file part). Fields split into the two sections asked for:
  - *Basic Info*: `name`, `itemType` (`INVENTORY`/`NON_INVENTORY`/`SERVICE`/`BUNDLE`), `category`
    and `costGroup` (kept as free-text fields rather than their own managed reference tables —
    a deliberate scope cut to keep this pass focused; revisit if the business needs enforced
    category/cost-group lists). The image reuses the existing generic file-attachment system
    (`EntityType.PRODUCT`) — create the product first, then upload the image referencing its id,
    same as `InvoiceService` does for invoice attachments; the resulting file id is stored
    directly on `Product.imageFileId` (not re-queried per row) so the product list can show
    thumbnails without an extra request per item.
  - *Sales*: `description`, `price`, `incomeAccount` (FK to the existing `AccountEntity`/`account`
    table — the one with real named accounts like "4020 Sales", not the coarser `ChartOfAccount`
    grouping table), `taxCategory` (FK to the new `TaxCategory` table below).
- **`TaxCategory`** (BE: new entity/`TaxCategoryRepository`/`TaxCategoryService`/
  `TaxCategoryController` at `/api/tax-categories`) — `type` (`SALES_TAX`/`VAT`/
  `WITHHOLDING_TAX`) + `name` + `rate`. Seeded via `DatabaseSeeder` (guarded by
  `taxCategoryRepository.count() == 0`, same pattern as the rest of the seeder) with two rows
  each: Sales Tax (5%, 2.5%), VAT (15% standard, 0% zero-rated), Withholding Tax (5%, 10%).
- **FE**: `ProductsPage`/`ProductsList` (search, pagination, image thumbnail, delete), `ProductForm`
  (`ProductNewPage`, used for both create and edit) laid out as "Basic Info" + "Sales" cards
  matching the spec, a new reusable `SelectAccountModal` (Command-based search dialog over
  `GET /api/accounts`, highlights the "4020 Sales" account as the default) for the income-account
  field, and a tax-category `Select` populated from `/api/tax-categories`.
- **Invoice integration**: `AddInvoiceLineModal` gained a product/service search combobox at the
  top. Selecting an item prefills `description`, `unitPrice`, and `taxRate` from the product's
  tax category rate — all three remain plain editable fields afterward, nothing is locked.

### Invoice Send Preview (this pass) — preview before send, and a real bug fixed underneath

The "Send Invoice" dialog (`SendInvoiceDialog`) already had Template/Email/Subject/Message
fields, but they were cosmetic: `InvoiceController./send` never had a `@RequestBody` parameter
at all, so whatever the dialog collected was silently discarded on every send — the invoice PDF
always used the *default* template (`InvoiceDocumentService.generateInvoicePdf` hardcoded it) and
the email always went to `customer.getEmail()`, ignoring any override. Fixed that properly rather
than just bolting a preview onto broken plumbing:

- **BE — template selection now actually applies**: `InvoiceDocumentService` gained
  `resolveInvoiceTemplate(templateId)` (id if given, else the default INVOICE template) and a
  `generateInvoicePdf(invoiceId, templateId)` overload; the old `generateInvoicePdf(invoiceId)`
  delegates to it with `null`.
- **BE — new `SendInvoiceRequest` DTO** (`templateId`, `email`, `subject`, `message`, all
  optional) is now actually read by `POST /api/invoices/{id}/send`.
- **BE — new `InvoiceEmailContentService`**, extracted from what used to be dead-on-arrival logic
  inside `InvoiceEmailRequestedEventListener` (which picked `templates.get(0)` — the first
  template found, never the one actually selected). It resolves the subject/body either from the
  chosen template's `DocumentTemplateEmail` (STANDARD type) or from the caller's subject/message
  overrides, so the same resolution is shared by the real send and the preview.
- **BE — `InvoiceEmailService.sendInvoice`** now resolves the template, recipient email
  (override → else customer email), and email content *synchronously* at send time, then passes
  the already-resolved `subject`/`bodyHtml` on `InvoiceEmailRequestedEvent` to the async listener
  — the listener's only remaining job is delivery, so it no longer needs its own (buggy) template
  lookup.
- **BE — new `POST /api/invoices/{id}/send-preview`** (same `SendInvoiceRequest` body) returns
  `{toEmail, subject, bodyHtml}` with zero side effects — no PDF generated/saved, no status
  change, no email sent.
- **FE — `SendInvoiceDialog`** gained a "Preview" step: it calls the new `send-preview` endpoint
  for the email content and the existing (already-built, previously unused for this)
  `POST /api/document-templates/{templateId}/preview` for the actual invoice PDF — rendered
  inline via an iframe — using the real invoice and whichever template is currently selected in
  the dialog. The recipient email field is pre-populated (from `invoice.billingInfo.billingEmail`
  when the caller has it, else silently resolved via one `send-preview` call using the customer's
  email on file) but stays a plain editable input the whole time.

### Created By Audit Trail (this pass) — real FK, not a never-populated string

`created_by` on `journal_entries`, `journal_lines`, `payments`, `payment_allocations`,
`payment_refunds`, `supplier_payments`, `supplier_payment_allocations`, and `document_templates`
(every `BaseEntity` subclass — that's the complete list, no other table was affected) was a plain
`varchar(100)` column that **no create flow ever populated** — posting a journal, recording a
payment, etc. left it null. The user had manually backfilled real values into it directly in the
database for existing rows.

- **BE — `BaseEntity.createdBy`** changed from `String` to a real `@ManyToOne User`
  (`@JoinColumn(name = "created_by")`), populated automatically via `@CreatedBy` +
  `@EntityListeners(AuditingEntityListener.class)` — reusing the `@EnableJpaAuditing` /
  `AuditorAware<User>` bean that was already configured (and already used by `AuditableBase`/
  `User`) but that `BaseEntity` simply wasn't wired into. This fixes recording for all 8 tables at
  once with no per-service code changes needed. `updated_by` (still a plain string) now also
  gets auto-populated on create/update via a small helper, matching a concurrent fix that landed
  on this branch from elsewhere.
- **BE — new `CreatedByDTO`/`CreatedByMapper`**, wired into `JournalResponse`/`JournalMapper` and
  `PaymentDetailsResponse`/`PaymentMapper` so the API returns `{id, fullName}` instead of nothing.
  (Supplier payments and document templates weren't touched at the DTO/frontend level since no
  screen currently displays their creator — the FK/recording fix still covers them.)
- **⚠️ BE — manual migration required**: `spring.jpa.hibernate.ddl-auto=update` will not safely
  convert an already-populated `varchar` column to `uuid` + add a FK constraint. Run
  `src/main/resources/db/manual-migrations/001_add_created_by_user_fk.sql` against the database
  **once, before deploying this change** — it converts all 8 `created_by` columns and adds the FK
  to `users(id)`. The script's header explains how to handle any pre-existing non-UUID values.
- **FE — new `/settings/users/:id` `UserViewPage`** (name, email, status, roles, direct
  permissions) plus `UserRequests.getUser(id)`, since only an edit form existed before and it
  relied on router state rather than fetching by id.
- **FE — new `CreatedByLink`** (name + a small icon button opening the user's page, or a plain
  "System" label when there's no creator) now renders everywhere `created_by` was already shown:
  `JournalDetailsPage`, `JournalsListPage`, `JournalPostingPage`, `PaymentDetailsPage`.

### Enterprise Accounting — Period, Fiscal Year and Closing Management (this pass) — periods, opening balances, year-end closing, retained earnings, recurring journals

A large, from-scratch module implementing the full 23-section spec covering Accounting Periods,
Financial Year Management, Period Locking, Opening Balances, Year-End Closing, Retained Earnings
and Recurring Journal Entries.

- **BE — `FinancialYear` / `AccountingPeriod`** entities plus `FinancialYearService`
  (create with optional auto-generated monthly periods, overlap validation, `activate()` that
  demotes the previous current year) and `AccountingPeriodService` (`lock`/`unlock`/`close`/
  `reopen`, the last requiring a documented reason; `lockAllForFinancialYear` used by closing).
- **BE — `FinancialPeriodAuditLog`**, a shared audit trail (`FinancialPeriodAuditLogService`,
  its own `REQUIRES_NEW` transaction so a logging failure can never roll back the action it
  records) covering every status change across Financial Years, Accounting Periods, Opening
  Balances, Year-End Closing and Recurring Journals.
- **BE — `PeriodLockGuard`**, the single enforcement point for period locking. Rather than
  threading a period check through every module that posts to the GL, it hooks into the one
  choke point all of them already share: `JournalPostingServiceImpl.validatePostingPeriod`
  (previously an empty stub) — which manual journals, AR payment/refund postings, and AP
  bill/supplier-payment postings all funnel through via `JournalService.create()`+`post()`.
  `JournalServiceImpl.reverse()` bypassed that pipeline (it posts a reversal directly), so it
  got its own explicit guard call. A date outside any defined period is allowed through, so
  the feature doesn't retroactively block existing data the moment it ships.
- **BE — `OpeningBalanceService`**, a one-time balanced journal per Financial Year
  (`journalType=OPENING_BALANCE`, dated at the FY start date, `sourceModule`/`sourceEntityId`
  linked back to the FY using the existing generic linkage fields AR/AP already use — no schema
  change to `journal_entries`). Duplicate posting is blocked via `FinancialYear.hasOpeningBalance`.
- **BE — `YearEndClosingService`**: `getClosingPreview` derives total revenue/expense and net
  profit or loss for the FY's date range by reusing the existing `ProfitAndLossService`/
  `ProfitAndLossRepository` query (no stored account balances exist anywhere in this codebase —
  everything is derived from summed `JournalLine`s). `closeFinancialYear` posts a real `CLOSING`
  journal that zeroes every INCOME/EXPENSE account with a non-zero period balance and transfers
  the net result to a configurable Retained Earnings account
  (`accounting.closing.retained-earnings-account-id`, default the seeded `3010` account), stores
  a denormalized totalRevenue/totalExpense/netProfitLoss/retainedEarningsMovement snapshot on
  `FinancialYear` so historical reporting for a closed year doesn't depend on re-deriving it later,
  and auto-locks every remaining open period. `reopenFinancialYear` is a controlled, reason-required
  reopen back to OPEN status.
- **BE — Recurring Journals**: `RecurringJournalTemplate`/`RecurringJournalTemplateLine`/
  `RecurringJournalOccurrence` plus `RecurringJournalService` (template CRUD,
  pause/resume/stop/archive lifecycle, `generateDueOccurrences()`). A due date that falls in a
  locked/closed period is recorded as a PENDING occurrence and retried on the next run rather
  than skipped or forced through, using the same `PeriodLockGuard`. A daily
  `@Scheduled` job (`RecurringJournalGenerationJob`) and an on-demand `/generate-due` endpoint
  both drive it. The unique `(template, scheduled_date)` constraint prevents double-generation.
- **BE — new controllers**: `/api/financial-years` (+ `/activate`), `/api/accounting-periods`
  (+ `/lock`, `/unlock`, `/close`, `/reopen`), `/api/opening-balances`,
  `/api/financial-years/{id}/closing` (`/preview`, `/close`, `/reopen`), `/api/recurring-journals`
  (+ `/pause`, `/resume`, `/stop`, `/archive`, `/occurrences`, `/generate-due`).
- **FE — `FinancialYearsPage`** (list, create with optional auto-generated monthly periods,
  activate) and **`FinancialYearDetailPage`** with three tabs — **Accounting Periods**
  (lock/unlock/close/reopen with a reason dialog), **Opening Balance** (a reusable
  `JournalLineEditor` balanced multi-line entry form, becomes read-only once posted), and
  **Year-End Closing** (revenue/expense/net P&L preview, warnings, confirm-to-close, and a
  reason-required reopen once closed).
- **FE — `RecurringJournalsPage`**: template list with status badges, create dialog (reusing
  `JournalLineEditor`), pause/resume/stop/archive actions, a "Generate Due Now" button, and an
  occurrence-history side sheet showing generated/pending/failed dates.
- **FE — new `src/components/accounting/JournalLineEditor`**, a reusable balanced multi-line
  account/description/debit/credit editor (built on the existing `SelectAccountModal`) shared by
  the Opening Balance and Recurring Journal forms.
- Wired into the sidebar's Accounting section and `App.tsx` routing (`/financial-years`,
  `/financial-years/:id`, `/recurring-journals`).
- **Not done in this pass**: manual browser QA against a live backend (would need a running DB +
  authenticated session); typecheck (`tsc -p tsconfig.app.json --noEmit`) and `npm run build`
  both pass cleanly, and the backend compiles cleanly under the usual temporary JDK 21 swap.

---

## 1. ACCOUNTING CORE

- [x] Chart of Accounts (BE: `ChartOfAccount`, `AccountController`; FE: `ChartOfAccountsPage`)
- [x] Journal Entries (BE: `JournalEntry`, `JournalLine`, `JournalService`; FE: `JournalForm`, `JournalsListPage`)
- [x] Journal Posting (BE: `JournalPostingService`; FE: `JournalPostingPage`)
- [x] Journal Reversal (BE: `ReverseJournalRequest`; FE: `ReverseJournalDialog`, `JournalReversalCard`)
- [x] Accounting Periods (define financial years/periods) (BE: `FinancialYear`, `AccountingPeriod`, `FinancialYearService`, `AccountingPeriodService`; FE: `FinancialYearsPage`, `FinancialYearDetailPage` → Accounting Periods tab)
- [x] Period Locking (block writes to closed periods) (BE: `PeriodLockGuard`, wired into the single GL choke point `JournalPostingServiceImpl.validatePostingPeriod` plus `JournalServiceImpl.reverse()`, so manual journals, AR payments, AP bills/supplier payments, reversals and recurring journal generation all respect lock/close state)
- [x] Fiscal Year Management (create/rollover) (BE: create with auto-generated monthly periods, `activate()` demotes the previous current year; a "rollover" is create-next-year + close-previous rather than a single button, matching how the rest of the module is built)
- [x] Opening Balances (setup-time balance entry) (BE: `OpeningBalanceService`, one-time balanced journal dated at FY start, `journalType=OPENING_BALANCE`; FE: `OpeningBalanceTab`)
- [x] Year-End Closing (close temp accounts, roll to next FY) (BE: `YearEndClosingService.closeFinancialYear` — zeroes every INCOME/EXPENSE account with a non-zero balance in the FY's date range via a posted `CLOSING` journal, then auto-locks remaining open periods; FE: `YearEndClosingTab` preview + confirm)
- [x] Retained Earnings (auto-transfer of P&L) (BE: the `CLOSING` journal's balancing line posts the net profit/loss to a configurable Retained Earnings account, `accounting.closing.retained-earnings-account-id`, default seeded account `3010`; a denormalized snapshot — totalRevenue/totalExpense/netProfitLoss/retainedEarningsMovement — is stored on `FinancialYear` for reporting that survives future query changes)
- [x] Recurring Journal Entries (scheduled generation) (BE: `RecurringJournalTemplate`/`RecurringJournalTemplateLine`/`RecurringJournalOccurrence`, `RecurringJournalService.generateDueOccurrences()` run by a daily `@Scheduled` job (`RecurringJournalGenerationJob`) and an on-demand endpoint; a due date inside a locked/closed period is recorded PENDING and retried rather than skipped or forced through; FE: `RecurringJournalsPage`)
- [ ] Journal Templates (save/reuse structures) — not implemented
- [ ] Journal Approval (review/approve before posting) — not implemented (no approval workflow exists anywhere)
- [ ] Adjusting Entries (period-end adjustments) — enum value exists (`ADJUSTMENT_JOURNAL`), no dedicated logic
- [ ] Suspense Accounts — not implemented
- [ ] Multi-Currency Accounting — only a `Currency` enum exists; no multi-currency ledger logic
- [ ] Exchange Rate Management — not implemented
- [ ] Foreign Exchange Gain/Loss — not implemented

## 2. SALES / ACCOUNTS RECEIVABLE

- [x] Customers (BE: `Customer`, `CustomerController`; FE: `CustomersPage`/`CustomersList` — real list+pagination+search, `CustomerForm`, `CustomerViewPage` detail screen with activity log)
- [x] Invoices — create/edit/status lifecycle (BE: `Invoice`, `InvoiceController`, `InvoiceService`; FE: `InvoicesPage`, `InvoiceNewPage`, `InvoiceViewPage`)
- [x] Invoice Editing Controls (draft editable, finalized protected)
- [x] Invoice PDF Generation (BE: `OpenHtmlPdfGenerationService`, invoice renderers)
- [x] Invoice Templates (design/branding/layout) (BE: `documenttemplate` module; FE: `document-template` feature)
- [x] Invoice Email Configuration (subject/body/variables) (BE: `DocumentTemplateEmail`; FE: `email/EmailDesignerDialog`)
- [x] Invoice Preview (FE: `InvoiceTemplatePreview`, `ReportPreview`-style flows; BE sample preview endpoint)
- [x] Receive Payments (BE: `PaymentController`, `PaymentService`; FE: `ReceivePaymentPage`)
- [x] Payment Allocation (BE: `PaymentAllocationService`; FE: `AllocatePaymentPage`, `OutstandingInvoiceTable`)
- [x] Credit Limits (BE: `PaymentTerms.creditLimit`)
- [~] Partial Payments — allocation model supports partial amounts; needs explicit UX/reporting confirmation
- [~] Customer Refunds — `PaymentRefundEntity`/`RefundPaymentRequest` exist generically; verify full accounting impact (GL entries) is wired
- [x] Customer Statements (BE: `StatementService.getCustomerStatement`, `GET /api/customers/{id}/statement`; FE: `CustomerStatementPage`)
- [ ] Credit Notes (customer credit/invoice adjustment) — only a document-template category exists, no transaction entity/workflow
- [ ] Customer Deposits (money received pre-invoice) — not implemented
- [ ] Customer Credits (track/allocate unapplied credits) — not implemented
- [x] Customer Aging (BE: `AgingReportService.getArAging`, `GET /api/reports/ar-aging`; FE: `ArAgingPage`)
- [ ] Collections (overdue follow-up tooling) — not implemented
- [ ] Recurring Invoices (auto-generate on schedule) — not implemented
- [ ] Invoice Reminders (auto notify on due/overdue) — an `invoice-reminder.html` email template exists but no scheduling/trigger logic found
- [ ] Overpayments handling — not confirmed/implemented
- [ ] Unapplied Payments tracking (surfaced view) — draft payment concept exists (`CreateDraftPaymentRequest`) but no "unapplied" balance report
- [ ] Sales Quotes — not implemented (document-template category only)
- [ ] Quote-to-Invoice conversion — not implemented
- [ ] Sales Orders — not implemented
- [ ] Delivery / Fulfillment Tracking — not implemented (document-template category only)

## 3. PURCHASES / ACCOUNTS PAYABLE

- [x] Suppliers (BE: `Supplier`, `SupplierController`; FE: `SupplierForm`, `SuppliersList.tsx` — both real now, list view wired to `/api/suppliers` with an AP-aging-derived balance column)
- [x] Supplier Bills (BE: `Bill`/`BillItem`, `BillController` at `/api/bills`, `BillService`; FE: `BillsPage`/`BillsList`, `BillNewPage`, `BillViewPage`) — draft/open/partially-paid/paid/cancelled lifecycle, approve posts GL journal
- [x] Supplier Payments (BE: `SupplierPaymentEntity`, `SupplierPaymentController` at `/api/supplier-payments`, `SupplierPaymentService`; FE: `SupplierPaymentsPage`, `RecordSupplierPaymentPage`, `SupplierPaymentDetailsPage`) — posts GL journal on creation
- [x] Supplier Statements (BE: `StatementService.getSupplierStatement`, `GET /api/suppliers/{id}/statement`; FE: `SupplierStatementPage`)
- [x] Supplier Aging / AP Aging (BE: `AgingReportService.getApAging`, `GET /api/reports/ap-aging`; FE: `ApAgingPage`)
- [ ] Supplier Credits — not implemented
- [ ] Supplier Refunds — not implemented (AP mirror of `PaymentRefundEntity` wasn't built; see §0)
- [ ] Purchase Orders — not implemented (document-template category only)
- [ ] Purchase-to-Bill conversion — not implemented
- [ ] Recurring Bills — not implemented
- [x] Partial Supplier Payments — supported via `SupplierPaymentAllocationService` (partial bill allocation, same model as AR)
- [x] Unallocated Supplier Payments — tracked via `SupplierPaymentEntity.unallocatedAmount`, visible on list/detail screens
- [ ] Expense Management — **FE only** (`ExpenseForm`, `ExpensesList`); **no BE `Expense` entity/controller/service**
- [ ] Employee Expenses (claims/reimbursement) — not implemented

## 4. BANKING

- [ ] Bank Accounts — not implemented anywhere (no entity, no BE/FE code)
- [ ] Bank Transactions — not implemented
- [ ] Bank Reconciliation — not implemented
- [ ] Bank Statement Import — not implemented
- [ ] Transaction Matching (auto-suggest) — not implemented
- [ ] Reconciliation Adjustments — not implemented
- [ ] Reconciliation History — not implemented
- [ ] Outstanding Transactions view — not implemented
- [ ] Bank Transfers — not implemented (only a `BANK_TRANSFER`-style payment method value exists)
- [ ] Cash Accounts as part of banking/reconciliation — not implemented

## 5. INVENTORY

- [ ] Inventory Items (stock-controlled products) — not implemented anywhere
- [ ] Warehouses / Locations — not implemented
- [ ] Stock Receipts — not implemented
- [ ] Stock Issues — not implemented
- [ ] Stock Transfers — not implemented
- [ ] Inventory Adjustments — not implemented
- [ ] Stock Counts — not implemented
- [ ] Inventory Valuation — not implemented
- [ ] Cost of Goods Sold — not implemented
- [ ] Low Stock Alerts — not implemented
- [ ] Inventory History — not implemented

## 6. TAXATION

- [~] Basic tax fields exist per-customer (`TaxInfo`) and per-supplier (`WithholdingTax`); FE has a Taxes screen (`TaxesPage`, `TaxForm`) — but there is no standalone `TaxRate`/`TaxCode` BE entity or controller, so FE tax screens are likely not backed by real persistence yet (verify)
- [ ] Tax Rates (configurable) — no dedicated entity
- [ ] Tax Codes (different treatments) — not implemented
- [ ] Tax-Inclusive vs Tax-Exclusive transaction handling — not implemented
- [ ] Sales Tax / VAT Reporting — not implemented
- [ ] Input and Output Tax distinction — not implemented
- [ ] Tax Adjustments — not implemented
- [ ] Tax Exemptions — not implemented
- [ ] Tax Periods — not implemented
- [ ] Tax Audit Trail — not implemented

## 7. EXPENSES

- [~] Expense Recording — **FE UI only**, no BE persistence/API
- [ ] Expense Categories (tie to chart of accounts) — not implemented
- [ ] Expense Attachments — not implemented (generic `FileService` exists in BE and could be reused)
- [ ] Billable Expenses — not implemented
- [ ] Expense Reimbursement — not implemented
- [ ] Recurring Expenses — not implemented
- [ ] Expense Approval — not implemented

## 8. FIXED ASSETS

- [ ] Fixed Asset Register — not implemented
- [ ] Asset Categories — not implemented
- [ ] Asset Acquisition — not implemented
- [ ] Asset Disposal — not implemented
- [ ] Depreciation (calculation) — not implemented
- [ ] Depreciation Schedules — not implemented
- [ ] Accumulated Depreciation — not implemented
- [ ] Asset Transfers — not implemented
- [ ] Asset Revaluation — not implemented
- [ ] Asset Reporting — not implemented

## 9. PAYROLL / EMPLOYEE ACCOUNTING

- [~] Employee Management — **FE UI only** (`EmployeeForm`, `EmployeesList`); **no BE `Employee` entity/controller**
- [ ] Payroll processing — not implemented (only a `PAYROLL_JOURNAL` enum value exists)
- [ ] Salary Components (earnings/deductions/benefits) — not implemented
- [ ] Payroll Journal (auto-post to GL) — not implemented
- [ ] Employee Advances — not implemented
- [ ] Employee Reimbursements — not implemented

## 10. FINANCIAL REPORTING

- [x] Dashboard (BE: `DashboardService`/`DashboardController` — cash balance, AR/AP totals, YTD net profit, 12-month revenue/expense trend, recent transactions, accounting health; FE: `Dashboard.tsx`) — **fixed**: this previously rendered 100% hardcoded 2021/2022 mock data (a fake "$20,700.00 net profit", fake transactions) with no backend call at all; now wired to live data.
- [x] General Ledger concepts (BE: `FinancialReportEngine`, GL data feeding reports)
- [x] Trial Balance (BE: `TrialBalanceService`/`TrialBalanceController` at `/api/reports/trial-balance`; FE: `TrialBalancePage`) — **fixed**: the line previously here (`ReportsController` / same URL) pointed at a dead stub that routed through the generic report-template engine for a `"TRIAL_BALANCE"` template that was never seeded, and used a from/to date range rather than true as-of-date cumulative balances — it could never have returned a real trial balance. Removed; replaced with a dedicated implementation whose totals are verified to balance to zero against live seeded data.
- [x] Profit & Loss (BE: `ProfitAndLossService`/`ProfitAndLossController`; FE: `ProfitAndLossPage`) — the backend endpoint existed with no frontend page consuming it at all; added one.
- [x] Balance Sheet (BE: `BalanceSheetService`/`BalanceSheetController` at `/api/reports/balance-sheet`; FE: `BalanceSheetPage`) — **fixed**: same dead-stub situation as Trial Balance above (wrong report-engine semantics, no seeded template, would never have returned real data). The real implementation uses true as-of-date cumulative balances (not the P&L engine's period-activity semantics) and includes a Current Year Earnings line computed from the active Financial Year's year-to-date P&L, so Assets = Liabilities + Equity holds true mid-year, before the next Year-End Closing sweeps it into Retained Earnings. Verified to balance to zero against live seeded data.
- [ ] Cash Flow Statement — `ReportsController#cashFlow` exists at `/api/reports/cash-flow` but is the same class of non-functional stub as the old Trial Balance/Balance Sheet endpoints were (routes through the generic report engine for a `"CASH_FLOW"` template that was never seeded); left as-is this pass since a correct cash flow statement (indirect method reconciling net income to operating cash) is a larger, separate piece of work.
- [x] Report Engine (reusable templates, not hard-coded) (BE: full `ReportTemplate` designer subsystem — sections, formulas, draft locks, versioning/history, publish lifecycle; FE: `ReportDesigner`, `FormulaBuilder`, `SectionTree`, wizard, versions)
- [ ] Account Statements (single-account activity view for end users) — GL data exists but no dedicated statement endpoint/page (customer/supplier statements now exist, see §2/§3; a per-GL-account statement is still open)
- [x] Accounts Receivable Aging (see §2)
- [x] Accounts Payable Aging (see §3)
- [ ] Tax Reports — not implemented
- [ ] Sales Reports (by customer/product/salesperson/period) — not implemented
- [ ] Purchase Reports — not implemented
- [ ] Expense Reports — not implemented (no expense data source in BE yet)
- [ ] Inventory Reports — not implemented (no inventory module)
- [ ] Budget vs Actual — not implemented
- [ ] Comparative Reports (period over period) — not implemented
- [ ] Management Reports (summarized) — not implemented
- [ ] Drill-Down Reporting (summary → transactions) — not implemented
- [ ] Exportable Reports (CSV/Excel/PDF export of reports) — not implemented (PDF exists only for documents, not reports)
- [x] Print-Ready Reports — PDF generation infra exists and is reused across the document system

## 11. BUDGETING & FORECASTING

- [ ] Budgets (annual/periodic) — not implemented at all
- [ ] Budget by Account — not implemented
- [ ] Budget by Department — not implemented
- [ ] Budget vs Actual — not implemented
- [ ] Forecasting — not implemented
- [ ] Budget Revision — not implemented
- [ ] Budget Approval — not implemented

## 12. WORKFLOW & APPROVALS

- [ ] Approval Rules (configurable) — not implemented; no approval engine exists anywhere in BE
- [ ] Invoice Approval — not implemented
- [ ] Bill Approval — not implemented (no bills module either)
- [ ] Expense Approval — not implemented
- [ ] Payment Approval — not implemented
- [ ] Journal Approval — not implemented
- [ ] Purchase Approval — not implemented (no PO module either)
- [ ] Role-Based Approval (vary by role/amount) — not implemented
- [ ] Approval History — not implemented

## 13. DOCUMENT MANAGEMENT

- [x] Document Attachments infra (BE: `FileEntity`/`FileService`/`FileStorageService`) — generic upload exists; confirm it's wired into invoice/expense/supplier flows
- [x] Document Templates (BE `documenttemplate` module; FE `document-template` feature — content/design/email panels, invoice designer canvas)
- [x] Business Branding (logo/colors/fonts/layout) (FE: `ColorSettings`, `TypographySettings`, `LayoutSettings`)
- [x] PDF Generation (BE: `OpenHtmlPdfGenerationService`, per-style renderers: classic/modern/professional)
- [x] Email Delivery (BE: `MailService`/`SmtpMailService`, `InvoiceEmailService`)
- [x] Email History (BE: `EmailLog`, `EmailLogController`; FE: `InvoiceEmailHistory`)
- [ ] Receipt Management (store receipts against expenses) — not implemented (no expense module)
- [ ] Supplier Document Storage — not implemented
- [ ] Document Templates beyond invoices (quote/PO/credit-note/delivery-note/receipt) — enum categories exist, but only invoice templates are actually implemented (renderers, CSS, seed data all invoice-only)
- [ ] Document Versioning (template version history) — `ReportTemplateHistory`/`VersionHistoryService` exists for **report** templates, not for **document** templates — needs equivalent for document templates

## 14. AUDIT & INTERNAL CONTROLS

- [~] `AuditableBase` (createdBy/updatedBy/timestamps) exists on entities — baseline traceability only
- [~] Complete Audit Trail — a real, customer-scoped activity log now exists (`CustomerActivityLog`, see §2/§15 note below) covering customer creation, status changes, invoices, payments, and emails; still not a system-wide audit trail (no equivalent for suppliers, bills, journals, users, roles, etc.)
- [x] Immutable Accounting History (posted journals protected from edit) — enforced via journal status + edit guards
- [ ] User Activity Tracking — not implemented
- [~] Transaction History (lifecycle view per record) — invoices/payments have `InvoiceActivityTimeline`/`ActivityResponse`; customers now have a real cross-entity feed (`CustomerActivityLog`) surfaced on `CustomerViewPage`; still not general-purpose across every entity
- [ ] Change History (field-level diffs) — not implemented
- [x] Approval History — implemented only for report templates (`ReportTemplateHistory`), not for business transactions (no approvals exist yet elsewhere)
- [x] Period Controls (BE: `FinancialPeriodAuditLog` records every CREATED/ACTIVATED/LOCKED/UNLOCKED/CLOSED/REOPENED/GENERATED action against a Financial Year, Accounting Period, Opening Balance, Year-End Closing or Recurring Journal, in its own `REQUIRES_NEW` transaction so an audit-write failure can't roll back the business action it documents; see §1)
- [x] Permission Controls (RBAC) (BE: `Role`, `Permission`, `RequirePermission`, `PermissionInterceptor`, `PermissionScanner`)
- [ ] Segregation of Duties enforcement — not implemented
- [ ] Audit Reports (dedicated audit/compliance report views) — not implemented

## 15. USER & BUSINESS ADMINISTRATION

- [x] Multiple Users (BE: `UserController`, `User`; FE: `UsersPage`, `UserForm`)
- [x] Roles & Permissions (BE: `RoleController`, `PermissionController`; FE: `RolesPage`, `RoleForm`)
- [x] Number Sequences (invoices/receipts/journals numbering) (BE: `DocumentNumberConfig`, `DocumentSequence`, `DocumentNumberGeneratorService`)
- [x] Business Preferences (generic config system) (BE: `Config`/`ConfigItem`/`ConfigController`; FE: `ConfigsPage`, `ConfigDetailPage`)
- [ ] Business Setup Wizard (guided onboarding) — not implemented
- [~] Company Profile — `CompanyInfo`/`CompanyInfoResolver`/`CompanyConfigSeeder` exist but are used internally for document rendering only; no dedicated company-profile settings screen/CRUD confirmed
- [ ] User Invitations (invite flow, vs. direct admin creation) — not confirmed; `UserController` supports creation, invitation flow (email + accept) not found
- [ ] User Deactivation (soft-disable without deleting history) — `UserStatus` enum exists; confirm deactivate endpoint/UX
- [ ] Departments — not implemented
- [ ] Locations / Branches — not implemented
- [ ] Currency Preferences (primary/supported currencies at business level) — only a `Currency` enum used per-record; no business-level currency settings
- [ ] Date / Number Formats (business-configurable) — not implemented

## 16. SECURITY

- [x] Secure Authentication (BE: `AuthController`, `JwtService`, `JwtAuthenticationFilter`)
- [x] Role-Based Access (see §14/§15)
- [~] Session Management — JWT + `RefreshToken` exist; confirm session revocation/expiry policy is complete
- [~] Password Management — creation/login exist; confirm reset/change-password flow
- [ ] Multi-Factor Authentication — not implemented
- [ ] Login History — not implemented
- [ ] Sensitive Action Protection (extra auth for high-risk actions) — not implemented
- [~] Data Protection — encryption exists for mail config secrets (`EncryptionService`); broader data protection policy not confirmed

## 17. ACCOUNTING DATA INTEGRITY

- [x] Double-Entry Enforcement (BE: journal line balancing on post)
- [x] Automatic Accounting — **fixed further**: `PaymentServiceImpl.createPayment` calls `PaymentJournalService.postPaymentJournal`, `PaymentAllocationServiceImpl` calls `postAdditionalAllocationJournal`/`postRemoveAllocationJournal` on allocate/remove/clear, and `APJournalService` covers bills and supplier payments. The remaining gap noted here previously — customer invoices never posted a journal at all, so the Accounts Receivable control account was only ever credited (by payments) and never debited (by the sales that created the receivable), driving it permanently negative — is now closed: `InvoiceJournalService` posts Dr Accounts Receivable / Cr Revenue / Cr Sales Tax Payable when an invoice is first sent. Verified against live data: the Balance Sheet's AR figure and `balanceCheck` are both now correct (previously confirmed negative and non-balancing when this was traced end-to-end against a real database for the first time).
- [x] Source Traceability (entries reference originating transaction) — present for AP (bills/supplier payments), AR payments (`sourceModule="PAYMENT"`), and now invoices too (`sourceModule="INVOICE"`)
- [x] No Silent Financial Changes (draft-vs-posted edit protection)
- [x] Reversal Rather Than Destruction (Journal Reversal implemented)
- [~] Balance Consistency across subledgers — holds for customer/GL today; will need re-validation once supplier bills, banking, and inventory modules are added
- [x] Period Integrity (BE: `PeriodLockGuard.assertPostable`/`isPostable` enforced at the single GL posting choke point plus journal reversal; see §1)
- [ ] Currency Integrity (multi-currency dual values) — not implemented (no multi-currency)
- [~] Rounding Controls — `BigDecimal` used consistently in money fields; explicit rounding-mode policy not confirmed

## 18. AUTOMATION

- [ ] Recurring Invoices — not implemented
- [ ] Recurring Bills — not implemented
- [x] Recurring Journals — implemented (see §1)
- [ ] Payment Reminders — not implemented (template exists, no trigger)
- [ ] Overdue Notifications — not implemented
- [ ] Scheduled Reports — not implemented (`SchedulingConfig` exists for infra but no scheduled report job found)
- [ ] Automatic Reconciliation Suggestions — not implemented (no banking module)
- [ ] Automatic Tax Calculations — not implemented (no tax engine)
- [x] Automatic Accounting Entries — implemented for AR payments, AP (bills + supplier payments), and now invoice sending too (see §17)

## 19. BUSINESS INTELLIGENCE

- [x] Executive Dashboard — confirmed and fixed: it was 100% hardcoded 2021/2022 placeholder data with no backend call. Now backed by `DashboardService` (cash, AR/AP, YTD net profit, 12-month revenue/expense trend, recent transactions, accounting health).
- [ ] Revenue Trends — not implemented
- [ ] Expense Trends — not implemented
- [ ] Profitability Analysis (beyond raw P&L) — not implemented
- [ ] Cash Position widget — not implemented
- [ ] Receivables Analysis — not implemented
- [ ] Payables Analysis — not implemented
- [ ] Top Customers — not implemented
- [ ] Top Products / Services — not implemented
- [ ] Expense Analysis — not implemented
- [ ] Financial KPIs (configurable) — not implemented

## 20. DATA IMPORT & EXPORT

- [ ] Customer Import — not implemented
- [ ] Supplier Import — not implemented
- [ ] Product Import — not implemented
- [ ] Chart of Accounts Import — not implemented
- [ ] Opening Balance Import — not implemented
- [ ] Transaction Import — not implemented
- [ ] Bank Statement Import — not implemented
- [ ] Data Export — not implemented
- [ ] Migration Tools — not implemented

## 21. LOCALIZATION & COMPLIANCE

- [~] Multi-Currency — enum only, no real multi-currency accounting (see §1)
- [ ] Multi-Language Readiness — not implemented (no i18n framework in FE)
- [ ] Regional Tax Support — not implemented (no tax engine)
- [ ] Local Accounting Requirements — not implemented
- [~] Financial Compliance (records/audit trail sufficiency) — baseline exists (`AuditableBase`, permission system) but no dedicated audit trail module (see §14)

## 22. CUSTOMER EXPERIENCE

- [ ] Global Search (cross-entity: customers/suppliers/invoices/payments/accounts/products) — not implemented
- [ ] Notifications Center — not implemented
- [~] Activity Timeline — implemented for invoices (`InvoiceActivityTimeline`) and now customers (`CustomerViewPage`'s activity log, clickable through to the source invoice/payment/email), not system-wide
- [~] Contextual Actions — present ad hoc per page (e.g., invoice actions); not a formalized pattern
- [ ] Bulk Actions — not implemented
- [~] Responsive Design — Tailwind-based UI, mobile hook exists (`use-mobile.tsx`); full responsive QA not confirmed
- [~] Empty States — some exist (e.g., `EmptyReportState.tsx` for reports); not confirmed across all modules
- [~] Error Handling — `NotFound.tsx`, toast system (`use-toast`, `sonner`) exist; global API-error UX not confirmed
- [ ] Confirmation Controls (guard irreversible actions) — partially present (`ArchiveDialog`, `DeleteTemplateDialog`, `PostConfirmationDialog`) for reports/journals only; not a system-wide pattern

## 23. ENTERPRISE-LEVEL REQUIREMENTS

These are cross-cutting qualities, not features to "complete" once — re-validate each as new
modules (banking, inventory, payroll, budgeting, etc.) are added:

- [ ] Scalable Architecture — revisit as data volume/module count grows
- [ ] Financial Accuracy — maintained by double-entry + `BigDecimal`; re-verify per new module
- [ ] Auditability — currently weak (see §14); needs a real audit-trail module
- [ ] Reliability (no lost/duplicated/partial transactions) — needs explicit transactional-boundary review as modules grow
- [ ] Consistency (shared accounting rules across modules) — currently fine because few modules exist; will need enforcement as AP/banking/inventory land
- [ ] Extensibility — report-template engine is a good precedent; other modules should follow a similar pluggable pattern
- [ ] Configurability — `Config`/`ConfigItem` system is a good foundation, underused elsewhere
- [ ] Security — see §16 gaps (MFA, login history, sensitive-action protection)
- [ ] Performance at scale — not yet tested against large datasets
- [ ] Data Integrity across subledgers — see §17
- [ ] Recoverability (backup/restore posture) — not addressed in either repo
- [ ] Observability (ops-facing tracing/logging) — not addressed beyond default Spring logging

---

## Suggested Priority Order (highest leverage first)

1. ~~**Accounting Periods + Period Locking**~~ — done; see §1 and the note below.
2. **Expense module backend** — FE already built; wire it to a real `Expense` entity/controller/service and post it to the GL.
3. **Employee module backend** — same situation as Expenses.
4. **Supplier Credits/Refunds + Purchase Orders/Purchase-to-Bill** — completes the AP lifecycle to the same depth as AR.
5. **Banking (Bank Accounts, Transactions, Reconciliation)** — currently zero coverage despite being claimed as "started."
6. **Tax engine (Tax Rates/Codes, VAT reporting)** — FE has a screen with no real backend model.
7. **Credit Notes, Customer Deposits/Credits** — completes the AR lifecycle.
8. **Payment receipt PDF/email/attachments/activity** — pre-existing AR backend stubs, now visibly empty end-to-end via the wired-up frontend rather than hidden behind mock data.
9. **Recurring Invoices/Bills/Journals + reminders** — automation layer, depends on 1–3 existing first.
10. **Approval workflow engine** — generic enough to apply to journals, invoices, bills, expenses, payments at once.
11. **Inventory, Fixed Assets, Payroll, Budgeting** — large standalone modules, tackle after the above core gaps are closed.

**Just closed:** Enterprise Accounting — Period, Fiscal Year and Closing Management. Added
`FinancialYear`/`AccountingPeriod` with lock/unlock/close/reopen and a shared
`FinancialPeriodAuditLog` for every status change; a single `PeriodLockGuard` enforces period
locking at the one GL posting choke point (`JournalPostingServiceImpl.validatePostingPeriod`)
plus `JournalServiceImpl.reverse()`, so manual journals, AR payments, AP bills/supplier payments
and reversals all respect a locked or permanently closed period without touching each module
individually. Added a one-time `OpeningBalanceService` per Financial Year, a
`YearEndClosingService` that zeroes INCOME/EXPENSE accounts into a configurable Retained Earnings
account via a posted `CLOSING` journal and auto-locks remaining open periods (with a controlled,
reason-required reopen), and a `RecurringJournalTemplate`/`RecurringJournalOccurrence` system with
a daily scheduled generation job that defers rather than skips occurrences whose date falls in a
locked period. Frontend: `FinancialYearsPage` (list/create/activate), `FinancialYearDetailPage`
(tabbed Accounting Periods / Opening Balance / Year-End Closing), and `RecurringJournalsPage`
(create/pause/resume/stop/archive/occurrence history/generate-due-now), all wired into the
Accounting section of the sidebar. Manual browser QA against a live backend was not performed in
this session (typecheck + production build both pass); the earlier AR Payment GL posting note
this replaced remains true — invoice creation itself still doesn't post its own journal.

**Just closed (investor-demo pass):** A full backend-up, real-Postgres verification pass (this
project's data had never previously been exercised against a genuinely empty database end to end)
surfaced and fixed five previously-undiscovered, cascading GL-posting bugs — each one had been
masking the next, so none had ever been reached before: (1) `application.properties`'s AR/AP
journal account-id settings held raw placeholder integers (1-6) instead of real chart-of-account
codes; (2) a fresh database crashed on boot because a chart-of-account "clear to" row referenced by
`DatabaseSeeder` had been left commented out; (3) `JournalEntry.status` defaulted to `POSTED`
instead of `DRAFT`, so every journal creation failed with "Only draft journals can be posted"; (4)
`journal_entries.reference` has a DB-level unique constraint, but `PaymentJournalServiceImpl` and
`APJournalService` both hardcoded the same reference for every journal a single payment could post
(primary + allocation + unallocation + refund), so a second journal on the same payment always
violated it; (5) `Invoice.balance` was never set on creation (unlike the equivalent, correct
`Bill.balance` logic), which made `PaymentAllocationServiceImpl` reject allocations against
brand-new invoices as "already fully paid." All five are fixed at the root cause, not worked
around. Closing the architectural gap flagged in the paragraph above, `InvoiceJournalService` now
posts Dr Accounts Receivable / Cr Revenue / Cr Sales Tax Payable when an invoice is first sent —
previously invoices never touched the GL at all, so the AR control account was only ever credited
(by payments) and never debited (by the sales that created the receivable), driving it permanently
negative. Also removed two dead `ReportsController` stub endpoints (`trialBalance()`,
`balanceSheet()`) that collided with, and were shadowed by, real `TrialBalanceService`/
`BalanceSheetService` implementations built this pass — the stubs routed through the generic
report-template engine for templates that were never seeded and used the wrong date-range
semantics, so they could never have returned correct figures; `cashFlow()` is the same class of
dead stub and is documented as still broken in §10 rather than silently left as "done." Fixed a
Jackson serialization bug where `FinancialYearResponse.isCurrent`/`AccountingPeriodResponse.isActive`
were being emitted as `"current"`/`"active"` (Lombok's `is`-getter naming vs. Jackson's default
bean introspection stripped the prefix) via `@Getter(AccessLevel.NONE)` plus a hand-written
`@JsonProperty`-annotated getter. Built a comprehensive, idempotent `DemoDataSeeder` (8 customers,
5 suppliers, 6 products, an opening balance, a full year of monthly invoices/bills with a realistic
mix of paid/partially-paid/unpaid outcomes, two recurring journal templates backfilled across their
whole history, a manual adjusting journal, a completed year-end close into a second, partially
locked financial year) so the app now demos with a full, realistic operating history instead of an
empty database. Verified end-to-end against the live seeded data: Balance Sheet `balanceCheck: 0.0`
and Trial Balance `difference: 0.0` (both exactly zero), Dashboard AR/net-profit/revenue figures
all correct and positive where they had previously been negative or zero.
