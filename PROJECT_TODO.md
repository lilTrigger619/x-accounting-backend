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

---

## 1. ACCOUNTING CORE

- [x] Chart of Accounts (BE: `ChartOfAccount`, `AccountController`; FE: `ChartOfAccountsPage`)
- [x] Journal Entries (BE: `JournalEntry`, `JournalLine`, `JournalService`; FE: `JournalForm`, `JournalsListPage`)
- [x] Journal Posting (BE: `JournalPostingService`; FE: `JournalPostingPage`)
- [x] Journal Reversal (BE: `ReverseJournalRequest`; FE: `ReverseJournalDialog`, `JournalReversalCard`)
- [ ] Accounting Periods (define financial years/periods) — no entity in BE
- [ ] Period Locking (block writes to closed periods) — not implemented
- [ ] Fiscal Year Management (create/rollover) — not implemented
- [ ] Opening Balances (setup-time balance entry) — enum value exists (`OPENING_BALANCE_JOURNAL`), no logic
- [ ] Year-End Closing (close temp accounts, roll to next FY) — not implemented
- [ ] Retained Earnings (auto-transfer of P&L) — not implemented
- [ ] Recurring Journal Entries (scheduled generation) — not implemented (no `Recurring*` code found)
- [ ] Journal Templates (save/reuse structures) — not implemented
- [ ] Journal Approval (review/approve before posting) — not implemented (no approval workflow exists anywhere)
- [ ] Adjusting Entries (period-end adjustments) — enum value exists (`ADJUSTMENT_JOURNAL`), no dedicated logic
- [ ] Suspense Accounts — not implemented
- [ ] Multi-Currency Accounting — only a `Currency` enum exists; no multi-currency ledger logic
- [ ] Exchange Rate Management — not implemented
- [ ] Foreign Exchange Gain/Loss — not implemented

## 2. SALES / ACCOUNTS RECEIVABLE

- [x] Customers (BE: `Customer`, `CustomerController`; FE: `CustomersPage`, `CustomerForm`)
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

- [x] Dashboard (FE: `DashboardPage`, `Dashboard.tsx`)
- [x] General Ledger concepts (BE: `FinancialReportEngine`, GL data feeding reports)
- [x] Trial Balance (BE + FE `ReportsController` / `/api/reports/trial-balance`)
- [x] Profit & Loss (BE: `ProfitAndLossService`/`ProfitAndLossController`)
- [x] Balance Sheet (BE: `/api/reports/balance-sheet`)
- [x] Cash Flow Statement (BE: `/api/reports/cash-flow`)
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
- [ ] Complete Audit Trail (creation/modification/approval/posting/reversal/deletion log) — no dedicated audit log entity/service
- [x] Immutable Accounting History (posted journals protected from edit) — enforced via journal status + edit guards
- [ ] User Activity Tracking — not implemented
- [ ] Transaction History (lifecycle view per record) — partially via `InvoiceActivityTimeline`/`ActivityResponse` for invoices/payments only; not general-purpose
- [ ] Change History (field-level diffs) — not implemented
- [x] Approval History — implemented only for report templates (`ReportTemplateHistory`), not for business transactions (no approvals exist yet elsewhere)
- [ ] Period Controls — not implemented (no accounting periods exist yet)
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
- [x] Automatic Accounting — **fixed**: `PaymentServiceImpl.createPayment` now calls `PaymentJournalService.postPaymentJournal`, and `PaymentAllocationServiceImpl` calls `postAdditionalAllocationJournal`/`postRemoveAllocationJournal` on allocate (incl. auto-allocate), remove, and clear — matching the pattern the new AP module (`APJournalService`) already used. Customer *invoices* still don't post a journal on their own (no `InvoiceJournalService` exists — an invoice only affects the AR subledger fields on the `Invoice` entity itself, not the GL, until a payment against it posts); that's a separate, smaller gap than the payment-side one that's now closed.
- [x] Source Traceability (entries reference originating transaction) — present for AP (bills/supplier payments via `sourceModule`/`sourceEntityId`) and now for AR payments too (`sourceModule="PAYMENT"`); invoice creation still isn't source-traceable to a journal since none is posted for it (see above)
- [x] No Silent Financial Changes (draft-vs-posted edit protection)
- [x] Reversal Rather Than Destruction (Journal Reversal implemented)
- [~] Balance Consistency across subledgers — holds for customer/GL today; will need re-validation once supplier bills, banking, and inventory modules are added
- [ ] Period Integrity — not enforceable yet (no accounting periods)
- [ ] Currency Integrity (multi-currency dual values) — not implemented (no multi-currency)
- [~] Rounding Controls — `BigDecimal` used consistently in money fields; explicit rounding-mode policy not confirmed

## 18. AUTOMATION

- [ ] Recurring Invoices — not implemented
- [ ] Recurring Bills — not implemented
- [ ] Recurring Journals — not implemented
- [ ] Payment Reminders — not implemented (template exists, no trigger)
- [ ] Overdue Notifications — not implemented
- [ ] Scheduled Reports — not implemented (`SchedulingConfig` exists for infra but no scheduled report job found)
- [ ] Automatic Reconciliation Suggestions — not implemented (no banking module)
- [ ] Automatic Tax Calculations — not implemented (no tax engine)
- [x] Automatic Accounting Entries — implemented for AR payments and AP (bills + supplier payments); invoice creation itself still doesn't post a journal (see §17)

## 19. BUSINESS INTELLIGENCE

- [~] Executive Dashboard — FE `Dashboard.tsx` exists; confirm data depth (revenue/expense/cash/AR/AP indicators) vs. placeholder
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
- [~] Activity Timeline — implemented for invoices (`InvoiceActivityTimeline`) only, not system-wide
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

1. **Accounting Periods + Period Locking** — everything else (closing, budgets) depends on periods existing.
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

**Just closed:** AR Payment GL posting — `PaymentServiceImpl`/`PaymentAllocationServiceImpl` now
call `PaymentJournalService` on create/allocate/remove/clear, matching the AP module's pattern
(`APJournalService`). Real GL posting now covers both AR payments and AP (bills + supplier
payments); the one remaining posting gap is that *invoices* themselves don't post a journal on
creation (only the payment against them does) — a smaller, separate item from what was tracked
here before.
