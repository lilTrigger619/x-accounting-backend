# Accounting Manual — Union Accounting Platform

This manual explains how this application does accounting: the theory behind each module, and
then — in exact, code-verified detail — how this specific system implements that theory. It is
written for two audiences at once: a bookkeeper or controller who wants to understand what the
software is actually doing to their books, and a developer who needs to know precisely which
account gets debited and which gets credited, under which conditions, before touching any of this
code.

Every figure, account code, enum value, and business rule below was pulled directly from the
backend source (`x-accounting-backend`) and cross-checked against the frontend (`expense-recorder`)
where relevant. Where the system has a real limitation — a feature that looks implemented but
isn't wired up, an edge case nobody's handled yet, two things with confusingly similar names — this
manual says so plainly rather than describing the aspirational version of the software. An
accounting manual that hides what the system can't yet do is more dangerous than no manual at all,
because someone will eventually rely on it.

Two currencies of truth run through every section: the **accounting concept** (what a controller
would call this in any ledger, in any country, on any software) and the **implementation** (what
this codebase specifically does with it — the entity, the enum, the GL account code, the
`MappingKey`). Read the concept first if the terminology is unfamiliar; skip straight to the
implementation if you already know what a control account or an amortization schedule is and just
need the account codes.

**This is a large, actively-developed system, and this manual covers all seventeen of its parts as
of this writing.** Part 1 through Part 6 cover the accounting backbone and the original five
transaction-posting modules (AR, AP, Prepayments, Loans). Parts 7 through 11 cover five modules that
were added to the system after the first version of this manual was written — Expenses, Deposits,
Down Payments, Bank Transfers, and Bank Reconciliation — and Part 15 covers a Business Intelligence
reporting layer added at the same time. If you read an earlier version of this document, be aware
that the Loans module (Part 6) was substantially rebuilt alongside these additions — its interest
methods, status lifecycle, and several of its GL accounts changed — so re-read that Part even if
you already know the rest.

---

# Part 1 — Foundations: How Double-Entry Works Here

## 1.1 The accounting equation, and why nothing can ever just "add money"

Every transaction this system records obeys one rule, inherited from double-entry bookkeeping as a
discipline going back five hundred years: **Assets = Liabilities + Equity**. Nothing that happens to
the books is allowed to break that equation. If you deposit cash, something else on the other side
of the ledger has to explain where it came from — a customer paid you (an asset moved from
"receivable" to "cash"), or an owner contributed capital (equity went up), or you borrowed it (a
liability went up). There is no such thing in this system, or in real accounting, as money
appearing from nowhere. Every entry has at least one debit and at least one credit, and the sum of
debits must equal the sum of credits, every single time.

**Debit and credit are not "subtract" and "add."** They are positional words — left side and right
side of a ledger account — whose actual effect on a balance depends on what kind of account you're
looking at. This is the single most common point of confusion for anyone new to accounting, so it's
worth explaining carefully:

- **Asset** and **Expense** accounts have a **normal debit balance** — a debit increases them, a
  credit decreases them. Cash, Accounts Receivable, Prepaid Expenses, Loans Receivable, Office
  Supplies Expense — debiting any of these makes the balance go up.
- **Liability**, **Equity**, and **Income** (the system calls revenue "Income," not "Revenue" — see
  §1.3) accounts have a **normal credit balance** — a credit increases them, a debit decreases
  them. Accounts Payable, Loans Payable, Owners Equity, Sales Revenue — crediting any of these
  makes the balance go up.

This system encodes that rule explicitly, not as a convention everyone has to remember by habit.
Every top-level chart-of-accounts grouping carries a `NormalBalance` value of either `DEBIT` or
`CREDIT` (enum `enums/NormalBalance.java`), and every balance calculation in every report
(`utils/CalculateBalance.java`) reads that flag to decide whether to compute `debit − credit` or
`credit − debit` for a given account. You cannot misread a Liability account's balance as negative
just because its ledger entries happen to be credits — the system already knows which direction is
"up" for that account.

## 1.2 The journal entry: the atomic unit of everything

Every financial event in this system — an invoice sent, a payment received, a payroll run posted, a
loan disbursed — ultimately becomes one thing: a **journal entry** (`JournalEntry`,
`entity/Journals/JournalEntry.java`), made up of two or more **journal lines**
(`JournalLine`, `entity/Journals/JournalLine.java`), each line touching exactly one account with
either a debit amount or a credit amount — never both, never neither.

Before any journal can be posted, the system runs it through `JournalPostingServiceImpl` and
enforces, in order:

1. **It has to have at least two lines.** A single-sided entry isn't a journal entry, it's an error.
2. **Each line is one-sided.** A line with both a debit and a credit, or with neither, is rejected
   outright — `hasDebit == hasCredit` is the literal check, and either both true or both false
   throws.
3. **It has to balance.** `Σ debits` must exactly equal `Σ credits`, to the cent. If they don't,
   posting is refused with "Journal is not balanced." This is the accounting equation, enforced in
   code rather than left to trust.
4. **The date has to be postable.** The journal's date is checked against the accounting calendar
   (§2) — if it falls in a locked or closed period, posting is refused, full stop, regardless of
   who's asking or how important the entry is.

A journal entry carries a `status`: `DRAFT`, `POSTED`, `REVERSED`, or `CANCELLED`
(`JournalStatus`). Only a `DRAFT` journal can be posted, and once posted it is never edited — every
report in this system (Balance Sheet, Trial Balance, aging, everything) only ever reads `POSTED`
journals; `DRAFT`, `REVERSED`, and `CANCELLED` lines are invisible to the ledger. If a posted entry
turns out to be wrong, it is never edited or deleted — it is **reversed**.

Every journal carries a `journalType` (`JournalType`), and the current full list of values is:
`GENERAL`, `SALES`, `PURCHASE`, `PAYROLL`, `ADJUSTMENT`, `OPENING_BALANCE`, `CLOSING`, `REVERSING`,
`PREPAYMENT`, `LOAN`, `DOWNPAYMENT`, `EXPENSE`, `DEPOSIT`, and `BANK_TRANSFER` — fourteen values in
total, each introduced as its owning module was built. **Only five of these fourteen can be chosen
on a manually-entered journal**: `GENERAL`, `SALES`, `PURCHASE`, `PAYROLL`, and `ADJUSTMENT`. Every
other value is reserved for the module that owns it — `JournalType.isManualEntry()` is checked the
moment a manual journal is created or its type is changed, and the system refuses with a message
naming the type and explaining that journals of that type "are posted by their own module and can't
be entered by hand." This exists for the same reason control accounts exist (§1.3): an invoice, a
bill, a loan disbursement, a bank transfer — each already has its own posting engine with its own
validation, and letting a human freehand a journal tagged with that same type would let the ledger
disagree with the very subledger the type is supposed to identify.

A manually-entered journal's currency is validated too: it must be a value already present in the
system's "currencies" configuration list, rather than silently defaulting to a hardcoded currency —
so an organization that has only configured USD and EUR cannot accidentally post a journal tagged
GHS just because that happened to be an old default somewhere in the code.

### Reversal is a mirror, not an undo

`JournalServiceImpl.reverse(id, request)` is the one mechanism every "undo" in this system — a
cancelled payment, a reversed payroll run, a reversed loan write-off — ultimately calls. It does not
touch the original entry at all. Instead it:

- Requires the original to currently be `POSTED` (you can't reverse a draft or an already-reversed
  entry).
- Determines the **reversal date**: the caller can supply an explicit date, or it defaults to
  today. This date is validated two ways — it can never be *before* the original journal's own
  date (you can't reverse something before it happened), and it is checked against the accounting
  calendar (§2.3) exactly like any other posting, so a reversal dated into a locked or closed
  period is refused just as a new transaction would be.
- Creates a brand-new journal entry dated the reversal date, numbered by the normal sequence, with
  a reference the caller can override (defaulting to `"REV-" + <original journal number>`), same
  `journalType` as the original.
- Copies every line from the original, but **swaps debit and credit on each one** — what was a
  debit becomes a credit of the same amount, and vice versa. Because the original balanced, the
  mirror automatically balances too.
- Marks the original `REVERSED` (stamped with a timestamp) — it stays in the database forever,
  exactly as it was posted, for audit purposes. Nothing is ever actually deleted from the general
  ledger.

Because a reversal can now be dated anywhere on or after the original entry (not only today), a
plain journal lookup also now shows the reversal relationship explicitly in both directions: a
reversed journal's response names the entry that reversed it (with that reversal's own date, reason,
and who posted it), and a reversing entry's own response names the journal it reverses. Most module
services that reverse their own journal internally (the Loans module reversing a write-off, for
instance) still use the simpler convenience form that always reverses as of today — only the
journal screen itself, and any module action that explicitly asks for a backdated reversal, supplies
an explicit date.

This is the accounting-textbook-correct way to fix a mistake: you never erase history, you add an
equal-and-opposite entry that cancels it out, so the full story — the mistake and its correction —
remains visible forever. Every module that needs to "undo" something (payment cancellation, payroll
run reversal) is built on top of this one mechanism rather than inventing its own.

## 1.3 The five account types, and the Chart of Accounts

This system recognizes exactly five account types (`enums/AccountType.java`): **`ASSET`**,
**`LIABILITY`**, **`EQUITY`**, **`INCOME`**, and **`EXPENSE`**. Note the naming choice: what most
accountants would call "Revenue," this system calls `INCOME` throughout the code, the database, and
every API response. They mean the same thing — if you're looking for "Revenue" in the code and
can't find it, you're looking for `INCOME`.

The Chart of Accounts itself is a **three-level hierarchy**, not a flat list:

1. **`ChartOfAccount`** (`entity/ChartOfAccount.java`) — the ten top-level groupings, each carrying
   the `AccountType` and `NormalBalance` that every account underneath it inherits. These are the
   ten rows seeded on first boot:

   | Code | Grouping | Type | Normal Balance |
   |---|---|---|---|
   | 1 | Asset | ASSET | DEBIT |
   | 2 | Liability | LIABILITY | CREDIT |
   | 3 | Equity | EQUITY | CREDIT |
   | 4 | Income | INCOME | CREDIT |
   | 5 | Expense | EXPENSE | DEBIT |
   | 6 | Bank Account | ASSET | DEBIT |
   | 7 | Credit Card | LIABILITY | CREDIT |
   | 8 | Cost of Goods Sold | EXPENSE | DEBIT |
   | 9 | Accounts Payable | LIABILITY | CREDIT |
   | 10 | Accounts Receivable | ASSET | DEBIT |

2. **`ChartOfAccountClearTo_ENTITY`** — a sub-grouping belonging to one of the ten rows above (for
   example, "Checking" belongs under "Bank Account"; "Buildings" belongs under "Asset").

3. **`AccountEntity`** (`entity/AccountEntity.java`) — the actual, individual account a transaction
   posts to (e.g. "1010 Checking Account"). It points up to a `ChartOfAccountClearTo_ENTITY`, which
   points up to a `ChartOfAccount` — that's where it gets its type and normal balance from. An
   individual account has no `AccountType`/`NormalBalance` field of its own; it inherits both by
   walking up this chain. The query every report uses to compute balances
   (`LedgerAsOfBalanceRepository.findAsOfBalances`) explicitly joins through this exact chain —
   `account → clearTo → chartOfAccount` — so this is a real, load-bearing relationship, not a
   vestigial one.

Each individual `AccountEntity` also carries two flags worth understanding on sight:

- **`isActive`** — a plain on/off switch for whether the account can still be selected for new
  transactions.
- **`isControlAccount`** — this one matters enormously, and is covered in its own section next.

### Control accounts: why you can't just journal-entry your way around the subledgers

A **control account** is a general-ledger account whose balance is supposed to always equal the sum
of some more detailed record kept elsewhere — a "subledger." Accounts Receivable is the classic
example: the GL's AR balance should always equal the total of every open invoice's remaining
balance. If you could freely post manual journal entries to the AR control account, you could make
the GL say one number while the invoice list says another, and nobody would notice until it was too
late to figure out why.

This system prevents that by flagging certain accounts `isControlAccount = true` and then enforcing,
in `JournalServiceImpl.assertNoControlAccountLines`, that a **manually created journal entry** can
never target one of them. The check runs on every line of every manual journal (both create and
edit); if any line's account is flagged, the whole journal is rejected with a message naming the
account and explaining why: *"it is only ever updated by the transaction that owns it (an invoice,
payment, payroll run, etc.)"*

The accounts flagged this way, as seeded:

| Code | Account | Why it's a control account |
|---|---|---|
| 6220 | Accounts Receivable | Mirrors the sum of open invoice balances |
| 6210 | Accounts Payable | Mirrors the sum of open bill balances |
| 2080 | Customer Deposits | Mirrors unapplied/overpaid customer payments |
| 1740 | Supplier Advances | Mirrors unapplied supplier payments |
| 2090 | Sales Tax Payable | Mirrors tax collected but not yet remitted |
| 2150 | Withholding Tax Payable | Mirrors tax withheld but not yet remitted |
| 1770 | Purchase Tax Receivable | Mirrors recoverable input tax on bills |
| 1795 | Prepaid Expenses | Mirrors the unamortized balance of active prepayments |
| 1780 / 1790 | Loans Receivable / Interest Receivable | Mirror the Loans module's lending ledger |
| 2160 / 2170 | Loans Payable / Interest Payable | Mirror the Loans module's borrowing ledger |
| 1750 / 1760 | Employee Loans Receivable / Salary Advances Receivable | Mirror payroll's loan/advance subledger |
| 2100 / 2110 / 2120 / 2130 / 2140 | Salary Payable / Employee Income Tax Payable / Statutory Payable (employee) / Statutory Payable (employer) / Reimbursements Payable | Mirror the payroll engine's liabilities |

This is a real, enforced rule — every single one of the automated posting engines described in this
manual is allowed to post to these accounts because it goes through the system's own internal
posting path, not the manual-entry screen a human uses. A human at the Journal Entries screen simply
cannot touch these nineteen accounts directly. If the GL and a subledger ever disagree, it is a bug
in the automated posting logic, not a stray manual entry — which is exactly the guarantee a control
account is supposed to provide.

**One gap worth flagging here rather than later:** the five newer modules covered in Parts 7–11
(Expenses, Deposits, Down Payments, Bank Transfers, Bank Reconciliation) each introduced their own
new GL accounts, several of which mirror a subledger in exactly the way the accounts above do (the
Customer/Supplier Downpayment accounts, the Deposit asset/liability accounts, the Bank
Reconciliation suspense account). As of this writing, **none of those newer accounts have been added
to this control-account protection list** — confirmed directly for the Downpayment accounts (2087,
1747), and not found anywhere in the seeder for the others either. A manual journal entry today
*can* still be posted directly to, say, the Customer Downpayments liability account, which would
desync it from the Down Payments module's own running balance in exactly the way this mechanism
exists to prevent. See Part 17 for the consolidated list of gaps like this one.

## 1.4 Document numbering

Every document type in this system — invoices, bills, payments, journal entries, payroll runs, and
so on — gets a sequential, human-readable number, generated centrally by
`DocumentNumberService.generateNextNumber(module)` rather than left to each feature to invent its
own scheme. The format is built as:

```
{prefix}{separator}{year if configured}{separator}{month if configured}{separator}{zero-padded sequence}
```

with configuration (`DocumentNumberConfig`) per document type (`DocumentModule` enum), covering:
prefix text, padding width, whether to include the year and/or month, and whether the sequence resets
at the start of each year and/or month. A module with neither reset flag set simply counts up
forever (e.g. `EMP-0001`, `EMP-0002`, ...).

The modules actually exposed on the Numbering & Sequences settings screen, with their configured or
default format:

| Module | Prefix | Seeded format example |
|---|---|---|
| `INVOICE` | INV | `INV-2026-00001` (resets yearly) |
| `JOURNAL` | JNL | `JNL-2026-00001` (resets yearly) |
| `EMPLOYEE` | EMP | `EMP-0001` (never resets) |
| `PAYROLL_RUN` | PR | `PR-2026-00001` (resets yearly) |
| `BILL` | BILL | `BILL-00001` (default, not yet customized) |
| `SUPPLIER_PAYMENT` | SPMT | `SPMT-00001` |
| `PAYMENT` | RCP | `RCP-00001` |
| `PREPAYMENT` | PPY | `PPY-00001` |
| `LOAN` | LN | `LN-00001` |
| `EXPENSE` | (user-assigned) | `EXP-00001`-style, module-specific |
| `DEPOSIT` | (user-assigned) | deposit number series |
| `DOWNPAYMENT` | (user-assigned) | downpayment number series |
| `BANK_TRANSFER` | (user-assigned) | bank transfer number series |
| `BANK_RECONCILIATION` | (user-assigned) | reconciliation number series |
| 6 × `DOCUMENT_TEMPLATE_*` | TMPL-INV / TMPL-QTE / TMPL-PO / TMPL-CN / TMPL-DN / TMPL-RCT | template-numbering, cosmetic only |

Three `DocumentModule` values exist in the enum (`CREDIT_NOTE`, `QUOTE`, `PURCHASE_ORDER`) but
nothing in the application actually calls `generateNextNumber` for them yet — they're reserved for
features not yet built, and deliberately left off the Numbering settings screen so an admin isn't
shown a setting with nothing behind it.

---

# Part 2 — The Accounting Calendar

## 2.1 Why accounting needs a calendar at all

A set of books that's still "open" forever is a set of books nobody can trust, because anyone could
quietly change last year's numbers at any time. Real accounting practice divides time into **fiscal
years**, and those years into **periods** (usually months), and then **locks** each period once
it's been reviewed and reported on — so that "January's numbers" means the same thing today as it
will mean a year from now, because nothing can be added to or changed in January after it's locked.

This system implements that discipline with two linked entities and one centrally-enforced guard
that every single posting path in the application has to go through.

## 2.2 Financial Years and Accounting Periods

A **`FinancialYear`** (`entity/accounting/FinancialYear.java`) has a start date, an end date, and a
status that moves through `DRAFT → OPEN/ACTIVE → CLOSING → CLOSED` (`FinancialYearStatus`). Exactly
one financial year at a time can be flagged `isCurrent = true` — the one the application treats as
"this year" for dashboards, setup checklists, and anywhere else that needs "today's fiscal year"
without the caller specifying one. Creating a new financial year rejects any date range that
overlaps an existing one — you cannot have two financial years both claiming the same day.

By default, creating a financial year automatically slices it into twelve calendar-month
**`AccountingPeriod`** rows (`entity/accounting/AccountingPeriod.java`), each starting life `OPEN`.
A period's status (`AccountingPeriodStatus`) is one of:

- **`OPEN`** — new transactions dated inside it can be posted freely.
- **`LOCKED`** — a soft stop. No new postings dated inside it are allowed, but it can be unlocked
  again (with a reason recorded) if something legitimately needs correcting.
- **`CLOSED`** — a hard stop, meant to be permanent. Reopening a closed period requires a
  non-blank reason and is treated as an exceptional action, not a routine one.

## 2.3 The Period Lock Guard: one gate for the entire application

`PeriodLockGuard` (`service/accounting/PeriodLockGuard.java`) is, by its own design, **the single
place in the entire codebase** that decides whether a given date can be posted to. Every
GL-affecting action in this application — manual journal entries, invoice/bill posting, payment and
supplier-payment posting, prepayment and loan journals, payroll posting, expense/deposit/downpayment/
bank-transfer/bank-reconciliation postings, and journal reversal itself — calls
`PeriodLockGuard.assertPostable(date)` before it's allowed to create an entry. There is no second,
independent locking mechanism anywhere else; if a date is postable according to this guard, every
module agrees it's postable.

The logic is simple and has one deliberately permissive edge case worth knowing about: if the date
being checked doesn't fall inside *any* defined `AccountingPeriod` at all (for example, nobody's
created periods that far in the future, or that far in the past), the guard **allows it through** —
this is explicit, documented backward-compatible behavior, not an oversight. The guard only refuses
a date when a period that *does* cover it is explicitly `LOCKED` or `CLOSED`. In practice this means:
set up your financial years and periods properly, and the lock actually protects you; leave gaps in
your calendar, and those gaps are wide open by default.

Recurring journal generation uses a non-throwing variant, `isPostable(date)`, so that a scheduled
recurring entry landing on a locked period doesn't crash the nightly job — it just defers that one
occurrence and tries again on the next run (§2.6).

## 2.4 Opening Balances

When you first bring an existing business onto this system, you need a way to record "here's what
every account already looked like the day we started" without that look like ordinary trading
activity. `OpeningBalanceService` provides exactly one such entry **per financial year** — the
service explicitly refuses to create a second opening-balance journal for a year that already has
one.

The opening balance journal is dated the financial year's own start date, tagged `journalType =
OPENING_BALANCE`, referenced `"OB-" + <year name>`, and — unusually for this system — its lines are
built by looking up accounts by their real database ID rather than by account code (every other
manually-assembled journal in this system, including Year-End Closing and Recurring Journals, shares
this same by-ID convention, as opposed to the human-facing code-based lookup the ordinary manual
journal screen uses). It is posted immediately and synchronously — it never sits in `DRAFT`.
There's no restriction on which account *types* can appear on an opening balance line; the only
rules are that debits must equal credits, and only one such journal exists per year.

## 2.5 Year-End Closing: where profit becomes equity

This is the single most conceptually important event in the accounting calendar, and the one most
likely to confuse someone without a formal accounting background, so it's worth explaining the
"why" before the "how."

**Income and Expense accounts are "temporary."** Unlike Assets, Liabilities, and Equity — which
carry a running balance forever, year after year — Income and Expense accounts are meant to measure
*this year's* activity only. "Sales Revenue" shouldn't still be showing last year's total three
years from now; it should reset to zero at the start of each new year so it can measure the new
year's sales cleanly. But that revenue and expense activity isn't simply discarded — the *net* of
it (revenue minus expenses, i.e. profit or loss) becomes a permanent addition to the owners' stake in
the business. That's what "closing the books" means: zero out every temporary account, and sweep
the net result of all of them into **Retained Earnings**, a permanent Equity account.

`YearEndClosingService.closeFinancialYear(id)` does exactly this, mechanically:

1. Runs a Profit & Loss report for the entire financial year (start date to end date).
2. For every Income account with a non-zero amount for the year, builds a line that zeroes it out —
   since Income is normally a credit balance, the zeroing line is a debit (unless that particular
   account happens to carry an unusual contra balance, in which case the direction flips
   accordingly — the code determines this per-account rather than assuming).
3. Does the same for every Expense account (normally debit-balanced, so zeroed with a credit).
4. Nets it all out: if the year was profitable, the difference is credited to **Retained Earnings**
   (account `3010`, resolved via `MappingKey.CLOSING_RETAINED_EARNINGS`); if the year posted a net
   loss, Retained Earnings is debited instead for the loss amount.
5. Posts this all as one journal, `journalType = CLOSING`, dated the financial year's own end date,
   referenced `"CLOSE-" + <year name>`.
6. **Auto-locks every still-open period in that financial year** — closing a year is explicitly also
   the point at which every month inside it becomes locked, whether or not someone remembered to
   lock them individually along the way.
7. Marks the financial year `CLOSED`, stamping who closed it and when, and snapshots the year's
   total revenue, total expense, and net profit/loss directly onto the `FinancialYear` record for
   fast future reference.

Two things worth being explicit about, because they're easy to get wrong when reasoning about this
system: **only Income and Expense accounts are ever touched by the closing journal.** Balance-sheet
accounts (Asset, Liability, Equity) are never zeroed — they simply carry their balances forward into
the new year unchanged, which is standard and correct; there's no separate "carry-forward" entry
needed for them because their balances were never temporary in the first place. And if there's
nothing to close (no revenue, no expenses, or they exactly offset to zero net profit/loss), **no
closing journal is posted at all** — closing isn't forced to produce an entry just for the sake of
having one.

**Reopening a closed year** (`reopenFinancialYear`, requiring a mandatory reason) is a *status-only*
operation. It sets the year back to `OPEN` so further administrative action is possible, but it
does **not** reverse the closing journal, does **not** unlock the periods that closing auto-locked,
and does **not** restore the pre-closing account balances. If you reopen a year, you are not undoing
the close — you're just lifting the year-level flag so you can, for example, lock/unlock individual
periods by hand, while the closing journal and its effect on Retained Earnings remain exactly as
posted. If you genuinely need to undo a close, you would reverse the closing journal itself through
the ordinary journal-reversal mechanism (§1.2) — reopening the year alone does not do that for you.

**The balance sheet doesn't wait for you to close the year.** `BalanceSheetServiceImpl` computes a
"current year earnings" figure on every single balance-sheet run (`computeCurrentYearEarnings`),
which is simply this year's revenue-minus-expenses *so far*, run live from the start of the active
financial year up to the report's as-of date. This is what lets a balance sheet mid-year correctly
show undistributed profit as part of equity, without needing a closing entry to exist yet.

## 2.6 Recurring Journals

Some entries — a monthly rent accrual, a depreciation charge — are identical every period except for
the date. `RecurringJournalTemplate` (`entity/accounting/RecurringJournalTemplate.java`) exists so
you set this up once: a description, a reference, a set of balanced debit/credit lines, a start
date, an optional end date and/or maximum number of occurrences, and a frequency —
`DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `SEMI_ANNUALLY`, or `ANNUALLY`.

A scheduled job (`RecurringJournalGenerationJob`, running by default every day at 01:00) calls
`RecurringJournalService.generateDueOccurrences()`, which finds every `ACTIVE` template whose next
scheduled date has arrived and tries to generate a real posted journal for it. Each attempt is
recorded as a `RecurringJournalOccurrence` row, so the same date can never generate twice (enforced
by a database uniqueness constraint on template+date) and every attempt — successful or not — leaves
a trace:

- If the period that date falls in turns out to be locked or closed, the occurrence is recorded as
  `PENDING` with a note explaining why, and — importantly — the template's "next run date" is **not**
  advanced, so the very next scheduled run will try that same date again rather than silently
  skipping it.
- If posting genuinely fails for some other reason, the occurrence is recorded `FAILED` with the
  error message, again without advancing the schedule.
- On success, the occurrence is marked `GENERATED` with a pointer to the journal it created, the
  template's occurrence counter increments, and the schedule advances to the next due date.

A template can be `PAUSED` (temporarily, resumable) or `STOPPED` (permanently, though a stopped
template can still be `ARCHIVED` afterward for tidiness) — and, consistent with the control-account
rule in §1.3, a recurring template is rejected at creation time if any of its lines target a control
account, exactly like a manual journal would be, and likewise rejected if it tries to use a
module-only `journalType` the way a manual journal would be (§1.2).

---

# Part 3 — Accounts Receivable: The Sales Cycle

## 3.1 What "Accounts Receivable" means

When you deliver goods or services to a customer and don't collect cash on the spot, you've created
an asset — a legal right to collect money later — called a **receivable**. Accounts Receivable (AR)
is the control account that, in aggregate, represents every dollar customers currently owe you. The
AR *cycle* is the whole chain of events this implies: establishing who your customers are, invoicing
them, tracking what's still outstanding, collecting payment, and reporting on how old (and how
collectible) that outstanding balance is.

## 3.2 Customers

A `Customer` (`entity/customer/Customer.java`) carries a unique `customerCode`, contact and billing
details, a `customerType`, a `status` (`CustomerStatus`: just `ACTIVE` or `INACTIVE` — there's no
"on hold" or "blocked" status in this system today), separate billing and shipping addresses, payment
terms, and a tax-exemption flag (`TaxInfo` — note this is about whether the customer is *exempt*
from tax, not a rate; compare this with how suppliers carry a withholding *rate*, §4.2).

Every customer has an **activity log** (`CustomerActivityLog`) — a running, append-only feed of
everything that's happened to them: creation, status changes, every invoice created against them,
every time an invoice is actually sent, every payment received, every allocation of a payment to an
invoice, and every email the system actually delivered. This log is written in its own separate
database transaction (`Propagation.REQUIRES_NEW`) specifically so that if writing the log entry ever
fails for some reason, it can never roll back — and can never be rolled back by — the real business
transaction it's describing. A broken activity feed should never be allowed to break the ability to
record a payment. Note there is no equivalent activity log on the supplier/AP side — this is an AR-only
feature today.

## 3.3 Products & Services

A `Product` (`entity/product/Product.java`) is master data describing something you sell: a name, a
price, an item type (`ProductItemType`: `INVENTORY`, `NON_INVENTORY`, `SERVICE`, or `BUNDLE`), a
category, and — critically for accounting purposes — an `incomeAccount`, the specific GL revenue
account that product's sales should land on.

**This only matters on the sales side.** When an invoice line references a product with its own
income account configured, that line's revenue is routed there instead of the generic default
revenue account (see §3.4's revenue-splitting logic). On the purchase side, bill lines have **no**
product reference at all — you cannot link a bill line to a `Product`, so a product's income
account (or any expense/COGS account, which doesn't exist on `Product` at all) is never consulted
when recording what you bought. There is also no inventory or cost-of-goods-sold posting anywhere
in this system — selling an `INVENTORY`-type product posts revenue exactly the same way as selling a
`SERVICE`-type product; nothing reduces an inventory asset or books a COGS expense. If your business
needs perpetual inventory accounting, this system does not provide it today; it tracks product
*catalog* data (price, category, income account) but not stock quantities or cost layers.

A `Product` can also point to a `TaxCategory` (name, type — `SALES_TAX`/`VAT`/`WITHHOLDING_TAX` —
and rate), but be aware this is catalog metadata only: the backend's invoice and bill calculation
logic reads the tax rate directly off each line item as a plain percentage field the request
supplies — it does not look up `Product.taxCategory.rate` itself to compute that figure. Whatever
process (the frontend, or a human) decides the line's `taxRate`, the backend simply trusts and
applies it.

## 3.4 Invoices: creating the receivable

An `Invoice` (`entity/invoice/Invoice.java`) starts life as `DRAFT` and moves through
`InvoiceStatus` values of `SENT`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, or `OVERDUE`. It's made up
of one or more `InvoiceItem` lines, each with a quantity, a unit price, and a flat tax-rate
percentage.

**Calculation** (`InvoiceCalculationService`) works line by line: `lineSubtotal = quantity ×
unitPrice`; `lineTax = lineSubtotal × taxRate/100`; `lineTotal = lineSubtotal + lineTax`. The
invoice's `subtotal` and `totalTax` are the sums of those across all lines. An optional discount
(flat amount or percentage of subtotal) reduces the total before arriving at `totalDue`. The
invoice's `balance` — what's actually still owed — is `max(0, totalDue − amountPaid)`, recalculated
from scratch every time a draft invoice is edited, and separately kept up to date incrementally as
payments get allocated against it once it's no longer a draft.

**An invoice only affects the General Ledger once — the moment it's actually sent to the customer,**
not when it's created as a draft. (There is a second, older `sendInvoice` method in the codebase
used only by the demo-data seeder that behaves slightly differently; the real, API-reachable send
flow is `InvoiceEmailService.sendInvoice`, called by `POST /api/invoices/{id}/send`, and that's the
behavior described here.) Sending requires the invoice to still be `DRAFT`; it generates the PDF
from the chosen (or default) template, posts the General Ledger entry, flips status to `SENT`, logs
the activity, and queues the actual email delivery asynchronously so a slow mail server never blocks
the HTTP response. There's a separate zero-side-effect preview endpoint
(`/send-preview`) that renders exactly what the recipient and email body will look like without
touching status, the GL, or the activity log at all — useful for checking before committing.

**The journal posted on send** (`InvoiceJournalService.postInvoiceJournal`):

```
Dr  Accounts Receivable (code 6220)                              totalAmount
    Cr  Revenue — split by product's income account, or          net revenue
        the default Sales account (code 4020) for lines with
        no product-specific account configured
    Cr  Sales Tax Payable (code 2090)                             totalTax   [only if any]
```

The revenue side is genuinely split, line by line: every line whose product has its own income
account contributes its share of net revenue (pre-discount subtotal, pro-rated by discount) to that
specific account; everything else falls through to the generic default. Whichever account ends up
absorbing the very last slice of the split picks up any rounding remainder, so the lines always sum
to exactly the net revenue figure, to the cent — never off by a penny due to rounding division
across several accounts.

This journal is tagged `journalType = SALES`, and the system explicitly guards against posting it
twice for the same invoice.

## 3.5 Payments and Receipts: collecting the receivable

A `PaymentEntity` (`entity/payment/PaymentEntity.java`) represents money actually received from a
customer: an amount, a date, a payment method, optionally a specific bank account it landed in, and
a status that moves `DRAFT → RECEIVED → PARTIALLY_ALLOCATED/ALLOCATED → (REFUNDED) → CANCELLED` (with
`CANCELLED` always being the final word regardless of how it got there).

**Here's a subtlety that matters a great deal for understanding this system's AR accounting: a
payment and its allocation to an invoice are two different things.** You can receive $1,000 from a
customer today and not yet know (or not yet decide) which invoice(s) it pays off — maybe it's a
deposit, maybe it's an overpayment, maybe the customer paid before you'd even finished matching it
up. The system handles this by splitting every payment amount into an **allocated** portion (matched
to specific open invoices) and an **unallocated** portion (sitting as a liability called "Customer
Advances" until it's matched to something).

**The journal posted when a payment is first recorded:**

```
Dr  Bank or Cash (the specific bank account chosen, or the         amountReceived
    appropriate default mapping for cash vs. bank method)
    Cr  Accounts Receivable (code 6220)                            allocatedAmount   [if any]
    Cr  Customer Advances (code 2080)                               unallocatedAmount [if any]
```

If more of the same payment gets allocated to an invoice *after* that first posting (say, the
customer called back and told you which invoice the deposit was for), a small follow-up journal
moves the amount from Customer Advances into Accounts Receivable: `Dr Customer Advances / Cr
Accounts Receivable`. Removing an allocation reverses the exact same movement in the other
direction. A refund splits the refunded amount between whatever portion was already allocated
(debited back out of AR) and whatever was still sitting unallocated (debited back out of Customer
Advances), crediting the bank/cash account for the full refund. Cancelling a payment altogether
doesn't construct a new entry by hand — it simply calls the generic journal-reversal mechanism
(§1.2) on the original payment journal, producing a clean mirror-image entry.

**Each time an allocation is applied or removed, the invoice's own status is recomputed from its
resulting balance**: zero balance means `PAID`; a balance equal to the full invoice amount (nothing
allocated yet) means `SENT`; anything in between means `PARTIALLY_PAID`.

One asymmetry worth knowing about if you're troubleshooting a discrepancy: *moving* an allocation
from one invoice to another (`reallocatePayment`) updates both invoices' balances correctly but does
**not** post any adjusting GL entry the way allocating or removing an allocation does — this looks
like a gap in the current implementation rather than an intentional design choice, and is worth
keeping in mind if a reallocation is ever used and the GL and invoice list seem to disagree
afterward.

**A newer, separate mechanism exists for deposits taken before an invoice is even issued** — see
Part 9. It coexists with the "Customer Advances" unallocated-amount mechanism described above
rather than replacing it; see §9.1 for exactly how the two differ and when each applies.

## 3.6 AR Aging and Customer Statements

**Aging** answers the question "how overdue is what customers owe us, and by how much?" — the
central tool for assessing collection risk. The system computes it as of any chosen date (default:
today), looking at every invoice that's neither cancelled nor fully paid and still carrying a
positive balance, and sorts each one's *entire remaining balance* into exactly one of five buckets
based on how many days past its due date the as-of date is:

| Bucket | Days past due |
|---|---|
| Current | 0 or not yet due |
| 1–30 | 1 to 30 days overdue |
| 31–60 | 31 to 60 days overdue |
| 61–90 | 61 to 90 days overdue |
| Over 90 | more than 90 days overdue |

Results are grouped by customer with a grand total row. Worth noting: AR aging, as implemented,
includes draft invoices with a positive balance in its calculation — it does not filter them out the
way AP aging filters out draft bills (§4.5). If a draft invoice happens to have a due date that's
now overdue, it will show up in the aging report even though it was never actually sent to the
customer.

A **Customer Statement** is simply a running account of everything that happened between two dates:
an opening balance (the net of everything before the statement's start date — invoiced amounts minus
payments allocated), then every invoice (as a debit) and every payment allocation (as a credit)
within the window, each shown with the running balance after it, down to a closing balance at the
end.

---

# Part 4 — Accounts Payable: The Purchase Cycle

## 4.1 What "Accounts Payable" means, and how it mirrors AR

Accounts Payable (AP) is the exact mirror image of AR: instead of money *owed to you*, it's a
control account tracking money *you owe* to suppliers for goods and services you've already
received but haven't yet paid for. Nearly every mechanism described in Part 3 has a direct AP
counterpart — suppliers instead of customers, bills instead of invoices, supplier payments instead
of receipts — with the debit/credit direction flipped, since AP is a liability rather than an asset.

**Not every purchase has to go through a Bill.** If you pay for something immediately, in one step,
straight from a bank or cash account — with no separate supplier invoice to track and settle later —
the Expenses module (Part 7) is the more direct path; Bills and Supplier Payments exist specifically
for the "owed now, paid later" case.

## 4.2 Suppliers, and the one real asymmetry worth knowing

A `Supplier` (`entity/supplier/Supplier.java`) looks much like a `Customer` — code, contact details,
address, payment terms, a status reusing the same `ACTIVE`/`INACTIVE` enum. The field that actually
differs in substance is tax information: a supplier carries a `WithholdingTax` record — a flag for
whether withholding applies at all, and an actual percentage **rate** — rather than the exemption
flag customers carry. This single rate is what drives withholding tax calculation on every payment
made to that supplier (§4.5).

There is no activity-log equivalent on the supplier side (§3.2's `CustomerActivityLog` has no AP
counterpart).

## 4.3 Bills: recording the liability

A `Bill` (`entity/bill/Bill.java`) is the AP equivalent of an invoice — lines with quantity, unit
price, and a flat tax-rate percentage, calculated identically to invoice lines (`BillCalculationService`
mirrors `InvoiceCalculationService` exactly: line subtotal/tax/total, optional discount, `balance =
max(0, totalDue − amountPaid)`). Its status (`BillStatus`) moves through `DRAFT`, `OPEN`,
`PARTIALLY_PAID`, `PAID`, `CANCELLED`, `OVERDUE` — note the system calls the "posted, awaiting
payment" state `OPEN` here, where the equivalent AR state is called `SENT`; same concept, different
word, worth knowing when reading either side of the code.

Unlike an invoice line, a bill line **cannot** reference a `Product` at all — there's no such field
on `BillItem`. This has a real consequence for GL posting: there is no per-line expense-account
split on the purchase side. **Approving a bill** (`BillService.approveBill`, the one action that
both finalizes the bill and posts it to the GL in a single step — unlike the invoice side, where
creating and sending/posting are two separate actions) posts:

```
Dr  Expense (the single default expense account, code 5000)        net expense
Dr  Purchase Tax Receivable (code 1770)                             totalTax   [if any]
    Cr  Accounts Payable (code 6210)                                totalAmount
```

Every bill, regardless of what it's actually for, lands its entire net expense on one generic
account unless and until the Accounting Mappings for this key are deliberately reconfigured — there
is no automatic routing by category or line description the way invoices route by product. (The
newer Expenses module, Part 7, does support a distinct GL account per line — if that per-line
routing matters to you, consider whether a given purchase belongs there instead of on a Bill.) Note
also that the tax portion here is booked as an **asset** (recoverable input tax), modeling a
VAT-style system where the tax you pay on purchases can later be reclaimed or offset — not as a
straight expense.

There is no withholding tax or purchase-tax modeling anywhere on the `Bill` entity itself — purchase
tax here is the flat line-level `taxRate` already described, and withholding tax (§4.4) only enters
the picture later, at payment time, driven entirely by the supplier's own configured rate rather than
anything on the bill.

## 4.4 Supplier Payments and Withholding Tax

A `SupplierPaymentEntity` mirrors `PaymentEntity` closely — amount, date, method, optional specific
bank account, allocated/unallocated split against open bills — but adds one field the AR side has no
equivalent for: `withholdingTaxAmount`.

**Withholding tax is a mechanic worth explaining carefully, because it genuinely confuses people the
first time they see it.** In many jurisdictions, when you pay certain kinds of suppliers (often
professional services, or non-resident suppliers), you're legally required to withhold a percentage
of the payment and remit *that portion* directly to the tax authority yourself, on the supplier's
behalf — rather than paying the supplier the full amount and trusting them to handle their own taxes.
The supplier still gets "paid" in full as far as their invoice is concerned (their bill is fully
settled), but the actual cash that leaves your bank account is less than the bill amount, because a
slice of it went to the tax authority instead of to them.

The system computes this automatically at the moment a payment is created, straight off the
supplier's own configured rate: `withholdingTaxAmount = amountPaid × supplier.withholdingTax.rate /
100` — and only if that supplier actually has withholding switched on with a positive rate; otherwise
it's always zero. Once the payment moves past `DRAFT`, this figure is frozen; it's not recalculated
again even if the supplier's rate later changes.

**The journal posted:**

```
Dr  Accounts Payable (code 6210)                                   allocatedAmount    [if any]
Dr  Supplier Advances (code 1740)                                   unallocatedAmount  [if any]
    Cr  Bank or Cash                                                 amountPaid − withholdingTaxAmount
    Cr  Withholding Tax Payable (code 2150)                          withholdingTaxAmount  [if any]
```

Notice what this achieves: the bill's liability is fully cleared (the full `amountPaid` figure is
what clears the AP balance against the bill), but the actual cash leaving your bank is only the net
amount — the withheld portion instead becomes a new liability, Withholding Tax Payable, representing
what you now owe the tax authority. "Supplier Advances" here is the AP-side mirror of AR's "Customer
Advances" — an asset representing money paid to a supplier that hasn't yet been matched to a
specific bill.

Two gaps worth flagging plainly, since a full accounting manual should document what's *missing* as
clearly as what's present: there is **no refund mechanism on the AP side** (the AR side has
`PaymentRefundEntity` and a dedicated reversal journal; supplier payments have neither), and there is
**no cancellation/reversal journal method** in the AP journal service at all — the AR side's
`postCancellationJournal` (which simply calls the generic reversal mechanism) has no AP counterpart.

**A newer, separate mechanism exists for deposits paid to a supplier before a bill is even issued** —
see Part 9, which also explains how it differs from the "Supplier Advances" unallocated-amount
mechanism described above.

## 4.5 AP Aging and Supplier Statements

Identical mechanics to §3.6 — same five buckets, same bucketing logic — just keyed on bills and
suppliers instead of invoices and customers. The one behavioral asymmetry already mentioned: **AP
aging explicitly excludes draft bills**, where AR aging does not exclude draft invoices. This is the
system as actually coded, not a typo in this manual — if you're reconciling the two reports and
wondering why a draft shows up on one side and not the other, that's why.

## 4.6 Tax Categories and the sales-tax remittance gap

A `TaxCategory` (name, type — `SALES_TAX`, `VAT`, or `WITHHOLDING_TAX` — and a rate) is configured
catalog data, consulted today only as an attribute a `Product` can carry (§3.3); it is not
independently referenced by invoice lines, bill lines, customers, or suppliers in the backend.

It's worth being direct about one gap here rather than implying a feature exists that doesn't: this
system tracks sales tax collected as a liability (**Sales Tax Payable**, code 2090) and withholding
tax as a liability (code 2150), and purchase/input tax as a recoverable asset (code 1770) — but there
is **no remittance workflow** anywhere in the system. Nothing models the act of actually paying the
tax authority and clearing that liability. Today, remitting collected sales tax (or paying over
withheld tax) to the relevant authority would have to be done as an ordinary manual payment —
debiting the liability account and crediting cash — exactly the kind of entry the manual Journal
Entry screen is built for, since neither 2090 nor 2150 is flagged as a control account preventing it
(they are flagged as control accounts for *other* reasons — to stop a manual entry from desyncing
them from the automated postings that build them up — but a deliberate manual remittance entry
clearing the liability is exactly the kind of correction a controller legitimately needs to make by
hand here, and nothing stops it).

---

# Part 5 — Prepayments: Paying for Something Before You Use It

## 5.1 The concept: why a prepaid expense is an asset, not an expense, at first

When you pay a year of insurance or rent up front, you have not yet incurred eleven-twelfths of that
expense — you've simply pre-paid for a benefit you'll receive gradually over the coming months. Until
you actually consume that benefit, the accounting-correct way to represent the payment is as an
**asset** ("Prepaid Expenses") — you're owed eleven months of future insurance coverage, which is a
real economic resource — and then, period by period, you move a slice of that asset into the
Expense account as you actually use it up. This process is called **amortization** or
**recognition**, and getting the timing right is the whole point of a prepaid-expense module: it
stops a business from understating this month's true cost just because cash happened to leave the
bank account eleven months ago.

**This module is deliberately scoped to the paid-out side only** — money your organization pays out
in advance. The mirror-image concept on the sales side (a customer paying you in advance) is handled
by the "Customer Advances" mechanism (§3.5) for an unallocated ordinary payment, or by the Down
Payments module (Part 9) for a deliberate, standalone pre-invoice deposit — not by this module.

## 5.2 How a Prepayment moves through its life

A `Prepayment` (`entity/prepayment/Prepayment.java`) is created in `DRAFT` with a total amount, a
counterparty (a supplier, an employee, or free-text "other"), a payment date, a recognition start
date, and — this is the key input — a number of periods and a recognition frequency (`MONTHLY`,
`QUARTERLY`, `SEMI_ANNUALLY`, or `ANNUALLY`).

**Creating it immediately generates the full amortization schedule, before a single dollar has
moved.** The schedule is strictly **straight-line**: the total amount is divided evenly across the
number of periods, rounded down to the cent for every period except the last, which absorbs
whatever rounding remainder is left over — guaranteeing the schedule always sums to exactly the
total, to the cent, with no other amortization method (declining-balance, effective-interest, or
otherwise) available as an option in this module.

**Activating** the prepayment (moving it from `DRAFT` to `ACTIVE`) is the moment the actual cash
outflow is recorded:

```
Dr  Prepaid Expenses (default code 1795, or a specific override account chosen on the record)   totalAmount
    Cr  Bank Account (default code 1010, or the chosen bank account)                             totalAmount
```

From here on, each scheduled period is **recognized** one at a time, in order — there's no "jump
ahead" or "recognize period 7 before period 5"; the system only ever recognizes the lowest-numbered
unrecognized period. Each recognition posts:

```
Dr  Expense (default code 5000, or a specific override account)     period amount
    Cr  Prepaid Expenses (code 1795)                                 period amount
```

As periods get recognized, the prepayment's status moves from `ACTIVE` to `PARTIALLY_RECOGNIZED`,
and finally to `FULLY_RECOGNIZED` once the last scheduled period has been recognized.

## 5.3 Correcting a prepayment: why there's a write-off but no reversal

If a prepayment is cancelled while still a `DRAFT` — nothing has been posted yet, so nothing needs
undoing; it simply moves to `CANCELLED`. But once even one period has been recognized, this module
deliberately does **not** offer a general-purpose "reverse" or "adjust" action. The only correction
available is a **write-off**: it takes whatever balance is still sitting unrecognized and expenses
the entire remainder in one shot, dated today, and marks every remaining scheduled period
`SKIPPED` rather than recognized. This is an intentional, documented design choice rather than a
missing feature — a true reversal of a partially-recognized prepayment would have to unwind each
individual period's recognition journal one at a time (since each one already affected a separate
reporting period's expense figure), and that capability simply isn't built. If you need to correct a
prepayment mistake that's already had periods recognized against it, writing off the remainder and
starting a fresh, corrected prepayment record is the available path today — not a clean reversal.

(The `PrepaymentStatus` enum does include a `REVERSED` value, but no code anywhere in the service
ever sets it — it's a placeholder for a capability that hasn't been built, not evidence that
reversal is actually supported.)

## 5.4 Prepayments accounting-mapping reference

| MappingKey | Default account | Role |
|---|---|---|
| `PREPAYMENT_DEFAULT_ASSET` | 1795 — Prepaid Expenses | Debited on activation; credited on each recognition |
| `PREPAYMENT_DEFAULT_EXPENSE` | 5000 — Operating Expenses | Debited on each recognition and on write-off |
| `PREPAYMENT_BANK_ACCOUNT` | 1010 | Credited on activation, if no specific bank account was chosen |

Every one of these can be overridden on an individual prepayment record (a specific prepaid-asset
account, a specific expense account, a specific bank account) — the mapping is only the fallback
used when the record itself doesn't specify one.

---

# Part 6 — Loans: Both Sides of Borrowing and Lending

## 6.1 The concept, and a crucial naming warning

A loan is, from an accounting standpoint, a straightforward application of the accounting equation:
if your organization **borrows** money, you now have more cash (an asset) and a matching new
obligation to repay it (a liability — "Loans Payable"). If your organization **lends** money to
someone else, you have less cash but a new right to be repaid (an asset — "Loans Receivable"). Every
period that passes, interest accrues, and that interest is itself a cost (if you borrowed) or income
(if you lent) — whether or not any cash has actually changed hands for it yet.

**Before going any further, a warning that matters a great deal if you ever read the source code
directly: there are two completely different "loan" concepts in this codebase, with two different
status enums that happen to share the same short name.** The general-purpose Loans module described
in this Part (`entity/loan/Loan.java`, with its enum at `enums/loan/LoanStatus.java`) is a full
amortization-schedule-driven instrument that can represent a loan to or from *anyone* — an employee,
a customer, a supplier, a bank, a shareholder, a director, or another financial institution. A
completely separate, much simpler feature exists specifically inside Payroll
(`entity/payroll/EmployeeLoan.java`, with its own unrelated enum at the top-level
`enums/LoanStatus.java`, just `ACTIVE`/`CLOSED`/`CANCELLED`) purely to let a payroll run deduct a
fixed installment from an employee's pay each period, with no schedule, no interest compounding, and
a much shorter lifecycle. The two are documented separately — this Part covers the general Loans
module; the payroll-specific one is covered in Part 12's payroll loans/advances section (§12.7) —
and the rest of this Part should be understood as describing only the general module unless stated
otherwise.

**This module was substantially rebuilt after the first version of this manual was written** — its
interest-method enum was renamed and restructured, its status lifecycle changed shape, interest
accrual gained a proper dedicated ledger, custom installment schedules became genuinely supported,
and it gained its own dedicated audit log and its own aging-style dashboard and statement reports. If
you have seen an earlier description of this module (including an earlier draft of this manual),
treat everything below as the current, correct behavior.

## 6.2 Direction: the one field everything else branches on

Every `Loan` record declares a `direction`: **`BORROWED`** (your organization owes someone) or
**`LENT`** (someone owes your organization). Nearly every piece of this module's logic — which
account gets debited at disbursement, which account absorbs interest, which direction a repayment's
cash flows — branches on this single field, because the whole thing is mechanically the mirror image
of itself depending on which side of the transaction your organization is on.

## 6.3 Setting up the loan: interest method and the amortization schedule

A loan declares a principal amount, an annual interest rate (which can be zero — many employee or
shareholder loans genuinely carry no interest), a payment frequency (`WEEKLY` through `ANNUALLY`), a
number of installments, an optional number of **grace-period installments** at the start of the
schedule (during which only interest is due, no principal), and an **interest method**
(`LoanInterestMethod`) — this is where the real complexity lives, since it determines the shape of
the entire schedule. Four methods exist:

- **`SIMPLE`** — flat interest every period, calculated once against the *original* principal (not
  the shrinking balance), paired with equal principal reduction each period. Because the interest
  never declines even as the balance does, this is the most straightforward — and, for the borrower,
  the most expensive relative to the other methods — way to charge interest on a loan.
- **`FIXED_INSTALLMENT`** — the familiar "fixed monthly payment" structure most people recognize from
  a mortgage or car loan. Every installment is the *same total amount*, but the mix between
  principal and interest shifts over time: early installments are mostly interest (since the
  outstanding balance, and therefore the interest charged on it, is still large), and later
  installments are mostly principal. The system computes this with the standard annuity formula —
  `payment = P × r ÷ (1 − (1+r)⁻ⁿ)` — where `r` is the periodic interest rate (the annual rate
  divided by however many periods fall in a year for the chosen frequency) and `n` is the number of
  amortizing installments (i.e. excluding any grace-period installments). Each period's interest is
  the opening balance times that periodic rate; the principal portion is whatever's left of the fixed
  payment after interest.
- **`REDUCING_BALANCE`** — a true declining-balance schedule. Instead of a fixed total payment, the
  *principal* portion is fixed every period (the total principal divided evenly across the
  amortizing installments), while the *interest* portion genuinely declines each period because it's
  always calculated on the shrinking opening balance. This produces a schedule where the total
  payment amount gets smaller over time, front-loaded with the highest payments.
- **`CUSTOM_SCHEDULE`** — the user enters each installment's own due date and principal amount by
  hand (and, optionally, its own interest and fee amounts too). This is now genuinely supported, not
  a placeholder: the system validates that every due date falls after the loan's start date, that no
  two installments share the same date, and — critically — that the principal amounts entered across
  every installment add up to exactly the loan's financed principal, rejecting the whole schedule by
  name if they don't. Where an installment's interest isn't explicitly given, it's computed on the
  reducing balance using actual day-count simple interest; where a fee isn't given, it defaults to
  the loan's flat per-installment fee. A custom schedule also behaves differently when someone later
  pays down extra principal early: rather than re-amortizing every remaining installment the way the
  two formula-driven methods do, the extra principal is removed from the *last* installments first —
  since a custom schedule's dates and amounts represent deliberate human intent, not a formula the
  system is free to recompute.

For every method except `CUSTOM_SCHEDULE`, whichever installment is numerically last in the
schedule is forced to absorb the outstanding balance exactly, so rounding across the whole schedule
never leaves a few cents unaccounted for at the end. During any grace-period installment, principal
due is forced to zero regardless of method — interest-only, no principal reduction — which is how
this module now represents what used to be called an "interest-only" or "balloon" loan: set the
grace period to cover every installment but the last, and the entire principal balance comes due in
one lump sum on that final installment, on top of whichever interest method you've chosen.

## 6.4 The lifecycle: draft, approve, disburse, and what can go wrong afterward

A loan's status (`LoanStatus`) moves through a specific, limited set of values: `DRAFT`, `APPROVED`,
`ACTIVE`, `PARTIALLY_PAID`, `FULLY_PAID`, `DEFAULTED`, `CLOSED`, `CANCELLED`, `REVERSED`.

A loan starts `DRAFT`, moves to `APPROVED` (a pure status change, no journal — this is the point
where someone has signed off that the loan should actually happen), and then to `ACTIVE` on
**disbursement**, which is the first moment real money moves and the first journal posts.

Upfront fees are settled one of two ways, declared on the loan as `LoanFeeTreatment`:
**`EXPENSED_IMMEDIATELY`** (netted straight off the cash that changes hands at disbursement, while
the liability/receivable is still booked at the full gross principal) or **`CAPITALIZED`** (added to
the balance being tracked going forward and repaid through the schedule, with the cash received
equal to the full principal rather than net of the fee). Either way — and this is a genuine
improvement over an earlier version of this module — **the fee is always recognized in the P&L
immediately at disbursement**, whether borrowed or lent; "capitalized" only changes how the cash is
netted and how much balance is carried forward, not whether the fee hits an income or expense
account right away.

**Disbursing a BORROWED loan** (your organization receiving money):

```
Dr  Bank Account                                                       net cash received
Dr  Loan Fees Expense (code 5090)                                      fees   [if any]
    Cr  Loans Payable (code 2160, booked at principal, or principal+fees
        if the fee treatment is CAPITALIZED)
```

**Disbursing a LENT loan** (your organization paying money out) is the mirror image, with its fee
leg landing on income rather than expense — a genuine improvement over an earlier version of this
module, which had no dedicated account for fee income earned on money lent out:

```
Dr  Loans Receivable (code 1780, booked at principal, or principal+fees
    if the fee treatment is CAPITALIZED)
    Cr  Bank Account                                                    net cash disbursed
    Cr  Loan Fee Income (code 4042)                                     fees   [if any]
```

## 6.5 Repayment: splitting principal, interest, and fees

Recording a repayment requires the caller to explicitly state how much of the payment is principal,
how much is interest, and how much is fees — the system validates the total is positive and that the
principal portion doesn't exceed what's still outstanding, and classifies the payment after the fact
as `SCHEDULED`, `EARLY`, `PARTIAL`, or `OVERPAYMENT` (`LoanPaymentType`) purely as a reporting label,
by comparing what was paid against what the schedule actually had due on that date — this
classification has no effect on the journal posted; it only changes how the payment is described in
reports and statements. The stated split is then applied against the schedule's oldest unpaid
installments (a line is marked fully `PAID` once both its principal and interest portions are
covered, or `PARTIALLY_PAID` otherwise).

**If any of this repayment's interest is settling interest that was already formally accrued**
(§6.6) rather than interest that simply came due on schedule, the system now correctly tracks that
split and reduces the loan's outstanding-accrued-interest balance by exactly that amount — fixing
what used to be a real tracking gap in an earlier version of this module, where accrued interest
only ever went up and ordinary repayments never brought it back down.

**Repaying a BORROWED loan:**

```
Dr  Loans Payable (code 2160)                                          principal portion     [if any]
Dr  Interest Expense (code 5080)                                       interest not yet accrued [if any]
Dr  Interest Payable (code 2170)                                       previously-accrued interest now settled [if any]
Dr  Loan Fees Expense (code 5090)                                      fees portion          [if any]
    Cr  Bank Account                                                    total
```

**Repaying a LENT loan** — and here, unlike in an earlier version of this module, fee income now has
its own dedicated account, separate from interest income:

```
Dr  Bank Account                                                        total
    Cr  Loans Receivable (code 1780)                                    principal portion     [if any]
    Cr  Interest Income (code 4040)                                     interest not yet accrued [if any]
    Cr  Interest Receivable (code 1790)                                 previously-accrued interest now settled [if any]
    Cr  Loan Fee Income (code 4042)                                     fees portion          [if any]
```

**Settling a loan in full** is simply a convenience wrapper around ordinary repayment — it
automatically fills in the outstanding principal and outstanding interest as the repayment amounts
and runs the exact same repayment logic described above. There's no separate "settlement journal
type" — it produces precisely the same kind of entry a manually-entered final repayment would.

## 6.6 Accruing interest: now a real, auditable ledger of its own

**Accruing interest** (a standalone action, available but not automatically scheduled — nothing in
this codebase runs it on a timer) lets you recognize interest as owed even before any cash actually
changes hands, which is the correct accrual-basis treatment. Unlike an earlier version of this
module, where "outstanding interest" was just a running number on the loan with no record of how it
got there, every accrual is now its own permanent, individually-dated ledger row (whether it was
later reversed, and which journal it posted), so the full history of how a loan's accrued-interest
balance built up over time is always reconstructable.

```
BORROWED:  Dr Interest Expense (5080)       / Cr Interest Payable (2170)
LENT:      Dr Interest Receivable (1790)    / Cr Interest Income (4040)
```

As described in §6.5, this balance is correctly drawn back down the moment a repayment actually
settles previously-accrued interest — it is no longer a figure that only ever increases.

## 6.7 Defaulting, closing, and write-off: what happens when a loan goes bad

A loan that's `ACTIVE` or `PARTIALLY_PAID` can be flagged **`DEFAULTED`** — a pure status change with
a required reason, marking that the counterparty has stopped servicing it. Defaulting a loan does
not itself stop anything or post any journal; a defaulted loan can still take payments exactly like
an active one, exactly as the status's own purpose implies — "defaulted" is a flag for attention and
reporting, not an automatic write-off.

**Closing** a loan (`close`) behaves differently depending on how the loan got there:

- A loan that's reached `FULLY_PAID` simply closes — nothing left to do, no journal needed.
- A `BORROWED` loan that still has a balance **cannot** be closed at all through this action; the
  system refuses outright and tells you exactly how much is still owed. A debt your organization
  itself owes cannot simply be written off as a matter of bookkeeping convenience — forgiving it
  would be a formal debt-restructuring event with its own distinct accounting, which this module
  isn't built to represent.
- A `LENT` loan that's `ACTIVE`, `PARTIALLY_PAID`, or `DEFAULTED` and still has a balance can only be
  closed by explicitly asking for a **write-off**, with a required reason. Before that's accepted,
  the system also insists any outstanding overpayment balance on the loan be reversed and corrected
  first — you can't write off a loan while it's simultaneously sitting on money it was overpaid.

**The write-off journal** — and this, too, is a genuine improvement over an earlier version of this
module, which had no dedicated write-off account and instead co-mingled write-offs with ordinary
loan fees — now writes off *both* the unpaid principal and any unpaid accrued interest, to a
dedicated expense account of its own:

```
Dr  Loan Write-off Expense (code 5085)        remaining principal + remaining accrued interest
    Cr  Loans Receivable (code 1780)            remaining principal          [if any]
    Cr  Interest Receivable (code 1790)         remaining accrued interest   [if any]
```

A loan can also simply be **cancelled** (only while still `DRAFT` or `APPROVED`, before any money has
actually moved) with no journal needed, since nothing was ever posted. And a disbursed loan can be
**reversed** outright — undoing the disbursement and every repayment and accrual ever posted against
it, each via the ordinary journal-reversal mechanism (§1.2) — for the rare case where a loan was
disbursed in error and needs to be unwound in its entirety rather than corrected transaction by
transaction.

## 6.8 A dedicated audit trail, and loan-specific reports

Every meaningful thing that happens to a loan — created, updated, deleted, approved, disbursed, a
payment recorded or reversed, an installment missed, interest accrued, defaulted, written off,
closed, cancelled, reversed — is written to its own dedicated, append-only loan audit log, entirely
separate from both the generic created-by/updated-at fields every entity carries and the
system-wide Settings Audit Log (§16.2). This mirrors the dedicated audit log Payroll already keeps
for its own run lifecycle (§12.8) — a pattern this system now uses in more than one place for a
module whose lifecycle is important enough to deserve its own narrative trail, distinct from the
general-purpose one.

Two reports exist specifically for this module, deliberately modeled on patterns already established
elsewhere in this manual:

- A **loan dashboard**, by currency, in the same spirit as AR/AP aging (§3.6/§4.5): how much is
  currently borrowed and lent in total and outstanding, how much interest is currently due but
  unpaid on each side, and which installments are overdue versus due in the next 30 days.
- A **loan statement**, per loan, in the same running-balance shape as a customer or supplier
  statement (§3.6/§4.5): disbursement, each charge, each payment (and any reversal of one), and any
  write-off, each shown against a running balance down to a closing figure.

## 6.9 Loans accounting-mapping reference

| MappingKey | Default account | Role |
|---|---|---|
| `LOAN_RECEIVABLE` | 1780 | Debited disbursing a LENT loan; credited on its repayment or write-off |
| `LOAN_PAYABLE` | 2160 | Credited disbursing a BORROWED loan; debited on its repayment |
| `LOAN_INTEREST_INCOME` | 4040 | Credited for interest earned (not yet accrued) on a LENT loan |
| `LOAN_INTEREST_EXPENSE` | 5080 | Debited for interest owed (not yet accrued) on a BORROWED loan |
| `LOAN_INTEREST_RECEIVABLE` | 1790 | Debited on accrual, credited when settled or written off — LENT |
| `LOAN_INTEREST_PAYABLE` | 2170 | Credited on accrual, debited when settled — BORROWED |
| `LOAN_FEE_EXPENSE` | 5090 | Debited for fees on a BORROWED loan |
| `LOAN_FEE_INCOME` | 4042 | Credited for fees earned on a LENT loan |
| `LOAN_WRITE_OFF_EXPENSE` | 5085 | Debited for the unpaid balance of a defaulted LENT loan being written off |
| `LOAN_BANK_ACCOUNT` | 1010 | Used for disbursement/repayment cash movement by default |

As before, every record can override the bank account and the principal/interest accounts
individually — the mapping only supplies the fallback.

---

# Part 7 — Expenses: Paying for Something Already, in One Step

## 7.1 The concept, and how this differs from a Bill

An expense, in the everyday sense, is money your organization spends. The Accounts Payable cycle
(Part 4) models the common business case of that spending: a supplier bills you, you owe it for a
while, and you pay it later, possibly in several installments. But a great deal of real spending
doesn't work that way at all — a card swipe for office supplies, cash handed over for a taxi, a
one-off purchase where there was never a separate "bill" to track because the money left the bank
account the moment the purchase happened. Forcing every one of those through the full Bill → approve
→ Supplier Payment cycle would be needless overhead for a transaction that is, from the moment it
happens, already fully paid.

The Expenses module exists for exactly this case: **an expense paid straight from one of the
organization's own bank or cash accounts, with no bill and no separate payment step.** Posting one
creates exactly one balanced journal, in one action, and that's the whole lifecycle.

## 7.2 How an Expense is built, and what makes it different from a Bill line by line

An `Expense` (`entity/expense/Expense.java`) has a payment date, a payment method, the specific
`BankAccount` the money actually came out of (required — there's no mapping-key fallback here; you
must say which account paid it), an optional supplier (a pure cash/miscellaneous expense can have no
supplier at all, unlike a Bill, which always requires one), and one or more `ExpenseLine` rows.

**Here is the one structural difference from a Bill worth remembering: each `ExpenseLine` carries
its own specific GL expense account.** Where a Bill's entire net amount lands on one generic default
expense account regardless of what each line was actually for (§4.3), an Expense can split a single
transaction across several different expense accounts in one entry — a single card statement line
that was really "60% office supplies, 40% software," say, can be recorded as two lines on one
Expense, each debited to its own account. Each line also carries a free-text expense category (drawn
from a configurable list — Office Supplies, Travel, Utilities, Marketing, Salaries, and so on) purely
for reporting purposes; the category tag and the GL account are two separate things, and the category
has no effect on where the line posts.

An expense's lifecycle has only three states (`ExpenseStatus`): `DRAFT`, `POSTED`, `REVERSED`. There
is no approval workflow, no "submitted for review" step — a draft can be edited or deleted freely,
and posting it is a single action available to anyone with the right permission.

## 7.3 The journal: one line per expense category, one line for the cash

```
Dr  Expense account of each ExpenseLine                             each line's own amount
    Cr  the chosen payment (bank/cash) account's own GL code         total of all lines
```

Notice this posts **without going through any `MappingKey` at all** — unlike nearly every other
module in this manual. Both sides are resolved directly from records the user themselves picked: the
expense account is whichever `AccountEntity` was chosen on the line, and the credit side is whichever
specific `BankAccount` was chosen on the expense. This is a deliberate structural difference from
Deposits (Part 8), which does fall back to configurable mapping keys when no specific account is
chosen — Expenses never falls back to anything, because every expense line and every expense's
payment account are both required fields with no default to fall back to.

Before posting, the system re-validates that every chosen expense-line account genuinely resolves to
an `EXPENSE`-type account, is active, and is **not** a control account — so an expense can never be
used as a backdoor way to post to an account the rest of this manual says should only ever be
touched by its own automated engine. It also locks the chosen payment account's row and checks it
actually has enough available balance before posting (unless overdrafting is explicitly allowed),
exactly the same serialized-balance-check pattern Bank Transfers use (Part 10).

**Reversing a posted expense** works exactly like every other reversal in this manual (§1.2): a new,
equal-and-opposite journal is posted, the original is marked `REVERSED` and left untouched forever. A
`DRAFT` expense, having never posted anything, is simply deleted outright rather than reversed —
reversal is only ever available for something that actually posted a journal in the first place.

---

# Part 8 — Deposits: Money Held, Not Yet Earned or Spent

## 8.1 The concept, and why direction matters so much here

A deposit — a security deposit on a lease, a utility deposit, a refundable deposit for returnable
equipment — is money that changes hands without anyone actually earning or spending it yet. If a
tenant hands a landlord a security deposit, the landlord hasn't earned a dollar of income; they're
simply holding money that, in the ordinary course of events, they expect to hand straight back. The
accounting-correct treatment reflects exactly that: **receiving or paying a deposit never touches
income or expense.** It only becomes income or expense later, and only in the one specific
circumstance where the deposit is **forfeited** — the tenant doesn't get it back, say, because of
damage — at which point, and only at that point, the organization genuinely keeps (or loses) money it
didn't previously have (or still have a claim to), and that is when, and only when, a P&L account is
touched.

This module is explicitly **bidirectional**, and which direction a given deposit runs changes
everything about how it's booked:

- **`DEPOSIT_PAID`** — your organization hands money to someone else and expects it back (a security
  deposit paid to a landlord, a utility deposit paid to a provider). Held as an **asset** until it
  comes back, is applied against a bill, or is forfeited.
- **`DEPOSIT_RECEIVED`** — your organization holds money that belongs, in principle, to someone else
  (a security deposit received from a tenant, an equipment deposit received from a customer). Held
  as a **liability** until it's refunded, applied against an invoice, or forfeited.

A `DepositType` (a configurable, named kind of deposit — "Security Deposit," "Tenant Deposit,"
"Supplier Advance Deposit," and so on) is **fixed to one direction** and can optionally carry its own
specific holding account and forfeiture account, overriding the direction's own mapped default for
every deposit created under that type — the same override-then-fallback pattern used throughout this
manual, just one level deeper than usual (a request-level override beats the type's own override,
which beats the mapped default).

## 8.2 The five things that can happen to a deposit's balance

Every deposit moves through a status (`DepositStatus`: `DRAFT`, `ACTIVE`, `PARTIALLY_APPLIED`,
`FULLY_APPLIED`, `REFUNDED`, `FORFEITED`, `CANCELLED`, `REVERSED`) derived automatically from how much
of its balance remains, has been applied, been refunded, or been forfeited — never set by hand.
There are exactly four ways a deposit's balance can leave it (`DepositAllocationType`):

**Activation** — the moment the deposit is actually posted, for the full amount:

```
DEPOSIT_PAID:      Dr  Deposit asset (default code 1745, or the type's/record's own override)   amount
                       Cr  Bank Account
DEPOSIT_RECEIVED:  Dr  Bank Account
                       Cr  Deposit liability (default code 2085, or override)                    amount
```

**Application** — using the deposit against an actual invoice or bill, the moment the lease ends or
the order is fulfilled and there's something concrete to apply it to:

```
DEPOSIT_RECEIVED (applied to an invoice):   Dr  Deposit liability   / Cr  Accounts Receivable (6220)
DEPOSIT_PAID (applied to a bill):           Dr  Accounts Payable (6210)  / Cr  Deposit asset
```

**Refund** — the money is physically returned through a bank account, with the deposit's balance
simply reversing out the way it came in:

```
DEPOSIT_PAID:      Dr  Bank Account        / Cr  Deposit asset
DEPOSIT_RECEIVED:  Dr  Deposit liability    / Cr  Bank Account
```

A non-refundable deposit (`refundable = false`) cannot be refunded at all — the system insists it be
applied or forfeited instead.

**Forfeiture** — the only one of the four that touches income or expense, and the only one where
direction genuinely changes which side of the P&L is hit, not just which account:

```
DEPOSIT_PAID (the organization loses money it paid out):      Dr  Deposit Forfeit Expense (5095)  / Cr  Deposit asset
DEPOSIT_RECEIVED (the organization keeps money it was holding): Dr  Deposit liability  / Cr  Deposit Forfeit Income (4060)
```

A forfeiture always requires a reason to be recorded — this is money genuinely changing hands for
good, not a routine entry.

There is also a **transfer** action, letting part of a deposit's balance move into a brand-new,
already-active deposit (optionally under a different type, counterparty, or account), which can be
useful when one deposit genuinely needs to be split or repurposed without unwinding and re-entering
it from scratch.

**None of these four allocation rows is ever edited or deleted.** Correcting one means reversing it —
which posts its own offsetting journal and flags the row, leaving the full history of what happened
and what was later undone permanently visible, the same discipline every other reversal in this
manual follows.

## 8.3 Deposits accounting-mapping reference

| MappingKey | Default account | Role |
|---|---|---|
| `DEPOSIT_PAID_ASSET` | 1745 | Debited paying a deposit, absent a type/record override |
| `DEPOSIT_RECEIVED_LIABILITY` | 2085 | Credited receiving a deposit, absent a type/record override |
| `DEPOSIT_FORFEIT_EXPENSE` | 5095 | Debited when a deposit the organization paid is forfeited |
| `DEPOSIT_FORFEIT_INCOME` | 4060 | Credited when a deposit the organization received is forfeited to it |
| `DEPOSIT_BANK_ACCOUNT` | 1010 | Used for paying, receiving, or refunding a deposit by default |

---

# Part 9 — Down Payments, and the Shared Settlement Ledger

## 9.1 The concept, and how this differs from "Customer Advances"

A down payment — a deposit on a large custom order, an advance paid to secure a supplier's capacity —
is money that changes hands *before* the invoice or bill it will eventually settle even exists.
Conceptually, this is close to the "Customer Advances" and "Supplier Advances" mechanism already
described in Parts 3 and 4: both represent money received or paid that hasn't yet been matched to a
specific document. **The two are genuinely separate mechanisms in this system, not one superseding
the other, and knowing which one a given transaction went through matters for finding and
understanding it later.**

- **Customer/Supplier Advances** (§3.5 / §4.4) is not its own record at all — it is simply whatever
  portion of an ordinary `Payment` or `SupplierPayment` hasn't yet been allocated to an invoice or
  bill. It lives on accounts 2080 and 1740.
- **Down Payments** (`Downpayment`, `entity/downpayment/Downpayment.java`) is its own standalone
  entity with its own number series, its own status lifecycle, and its own distinct GL accounts
  (2087 for customer downpayments, 1747 for supplier downpayments) — deliberately separate from 2080
  and 1740. It's the more formal path for a deliberate, planned advance taken or given specifically
  in anticipation of a future invoice or bill, as opposed to a payment that simply happened to arrive
  before it was fully matched up.

Nothing in the code treats one as superseding or migrating into the other — they coexist, and an
organization may well use both: Customer Advances for the everyday "received more than was
allocated" case, and Down Payments for a deliberate, tracked advance negotiated up front.

## 9.2 How a Down Payment moves, and the journal at each step

A `Downpayment` carries a type (`CUSTOMER_DOWNPAYMENT` or `SUPPLIER_DOWNPAYMENT`) and moves through a
status (`DRAFT`, `OPEN`, `PARTIALLY_APPLIED`, `FULLY_APPLIED`, `REFUNDED`, `CLOSED`, `CANCELLED`,
`REVERSED`) derived, like Deposits, purely from how much of its amount remains available versus
applied versus refunded.

**Posting** (moving it from `DRAFT` to `OPEN`) books the receipt or payment:

```
Customer downpayment received:   Dr  Bank Account   / Cr  Customer Downpayments liability (2087)
Supplier downpayment paid:       Dr  Supplier Downpayments asset (1747)   / Cr  Bank Account
```

**Applying** it against an actual invoice or bill once one finally exists:

```
Customer (applied to an invoice):   Dr  Customer Downpayments liability (2087)   / Cr  Accounts Receivable (6220)
Supplier (applied to a bill):       Dr  Accounts Payable (6210)   / Cr  Supplier Downpayments asset (1747)
```

**Refunding** unused downpayment balance back to the counterparty reverses the receipt/payment
direction exactly as a Deposit refund does (§8.2).

Two distinct correction actions exist, and they mean different things: a **refund** returns money
while the downpayment record stays usable and trackable; a full **reversal** of the downpayment
itself undoes the original posting entirely, but — deliberately — is only allowed while absolutely
nothing has yet been applied or refunded against it. If anything has already moved, you reverse that
specific application or refund individually instead of the downpayment as a whole.

**One gap worth flagging plainly, since it directly concerns the integrity mechanism described in
§1.3: the two new control accounts this module introduced — 2087 and 1747 — have not been added to
the control-account protection list.** A manual journal entry can currently still be posted directly
to either one, which the equivalent accounts for every older module (2080, 1740, and the rest listed
in §1.3) are specifically protected against.

## 9.3 The shared settlement ledger

Down Payments and Deposits both apply against the exact same kinds of target document — an invoice or
a bill — and a controller naturally wants one place to see "what, in total, has settled this
invoice?" without having to separately check the Deposits module and the Down Payments module (and,
down the line, whatever else might someday settle an invoice, like a credit note). `DocumentSettlement`
(`entity/settlement/DocumentSettlement.java`) is exactly that one place: a shared, cross-module
record of "this amount, from this source, settled this document," covering both source types that
exist today (`SettlementSourceType`: `DEPOSIT`, `DOWNPAYMENT`).

**It is important to understand what this module is, and what it deliberately is not: it posts no
journal entries of its own.** Every journal described in §8.2 and §9.2 is posted by the Deposits or
Down Payments service itself — `DocumentSettlement` is purely a read-side tracking record the owning
module writes to alongside its own posting, not a posting engine in its own right. Querying an
invoice or bill's settlement summary through this shared ledger returns exactly how much cash
payment it's received (ordinary allocated `Payment`/`SupplierPayment` amounts, which deliberately
keep their own separate allocation tables and are not tracked here) alongside how much has come from
each non-cash source, and a running total due.

---

# Part 10 — Bank Transfers: Moving Money Between Your Own Accounts

## 10.1 The concept

Not every bank-account movement is a transaction with the outside world. Moving money from your
Checking account to your Savings account doesn't change what your organization owns in total — it's
still exactly as much cash as before, just sitting in a different place. The accounting-correct
treatment is simply to debit the account that gained money and credit the account that lost it, for
the same amount; the only real accounting complexity arises when the two accounts are in different
currencies, where "the same amount" stops being a single well-defined number.

## 10.2 The journal, and what happens with fees and currency conversion

A `BankTransfer` names a source `BankAccount` and a destination `BankAccount` (which the system
enforces must actually be different accounts, with different GL codes — a "transfer" into the same
account would have no accounting effect and is rejected outright), a transfer amount, an optional
transaction fee, and, for a cross-currency transfer, the exchange rate between the two accounts'
currencies and each side's own rate back to the organization's base accounting currency.

```
Dr  Destination bank account's own GL code             base-currency value received
    Cr  Source bank account's own GL code                base-currency value sent
Dr  Bank Transfer Charges (default code 5100, or a       fee amount    [only if a fee was charged]
    specific account chosen on the transfer)
    Cr  Source bank account's own GL code                 fee amount    [only if a fee was charged]
Dr  FX Loss (default code 5110)  or                       the difference  [cross-currency only, and
    Cr  FX Gain (default code 4050)                                       only if there is one]
```

Both bank legs post directly to whichever GL code each `BankAccount` itself carries — there's no
mapping key involved for the accounts actually moving the money, only for the fee and for any
foreign-exchange gain or loss. The exchange-gain-or-loss figure is simply the base-currency value
received by the destination minus the base-currency value taken from the source (excluding any fee) —
positive means the organization effectively gained value in its own base currency purely from how the
rates moved between the two sides of the transfer, and negative means it lost some.

Like Expenses, a transfer locks the source account's row and checks for sufficient available balance
before posting (unless overdrafting is explicitly allowed), and like every other module in this
manual, reversing a posted transfer (only from `POSTED`, requiring a reason) posts a brand-new,
equal-and-opposite journal rather than touching the original — the original stays exactly as posted,
forever.

## 10.3 Bank Transfers accounting-mapping reference

| MappingKey | Default account | Role |
|---|---|---|
| `BANK_TRANSFER_CHARGES` | 5100 | Debited for bank fees on a transfer, absent a specific override |
| `FX_GAIN` | 4050 | Credited for a foreign-exchange gain realized on a cross-currency transfer |
| `FX_LOSS` | 5110 | Debited for a foreign-exchange loss realized on a cross-currency transfer |

---

# Part 11 — Bank Reconciliation: Proving the Books Agree With the Bank

## 11.1 The concept: why reconciliation exists at all

Your own books and the bank's own records of your account are two independently-maintained stories
of the same cash, and they will not always agree on any given day — not because either is wrong, but
because of timing. You might have written a check that hasn't been cashed yet (it's in your books,
not yet on the bank's statement); the bank might have charged a fee you didn't know about until the
statement arrived (it's on the bank's statement, not yet in your books). **Bank reconciliation is the
disciplined process of comparing the two, explaining every difference, and arriving at a balance both
sides agree on** — and it's one of the single most important internal controls in all of accounting,
because an account nobody reconciles is an account where errors, and worse, can hide indefinitely.

This system models the process faithfully: it imports a copy of the bank's own statement, tries to
automatically match each statement line against the corresponding entry already posted in your own
GL, lets a human confirm or manually fix whatever the automatic matching couldn't resolve on its own,
and provides a dedicated mechanism for posting the one kind of entry a reconciliation might reveal
you're actually missing — a bank fee, interest earned, a direct debit you hadn't recorded yet.

## 11.2 Importing a bank statement

A `BankStatementImportProfile` is a saved, reusable description of one bank's particular CSV export
format — which column holds the date, which holds the description, whether debits and credits are
two separate columns or one signed "amount" column, and, if the latter, whether positive numbers in
that column mean money coming in or money going out (`AmountSignConvention`) — since different banks
genuinely disagree on this convention. A default, generic profile ships out of the box for the common
shape (separate Date/Description/Reference/Debit/Credit/Balance columns), and an organization can
save as many bank-specific profiles as it needs.

Importing a file parses every row, tolerating malformed individual rows without failing the whole
file, and — critically — **deduplicates** against anything already imported: if the bank supplies its
own transaction ID, that alone identifies a row uniquely; otherwise the system fingerprints a row
from its date, signed amount, reference, description, and running balance, so re-importing the same
statement (or one that overlaps a previous import) doesn't create duplicate statement lines. Imported
lines never post anything by themselves — they sit alongside the GL, waiting to be matched against
it.

## 11.3 One reconciliation = one bank account, one statement period

A `BankReconciliation` ties together exactly one `BankAccount` and one statement period (an opening
balance, a closing balance, a statement date). Only one reconciliation can be open for a given bank
account at a time, and each new one must start strictly after the previous *completed*
reconciliation's period ended — reconciliations for one account happen in strict chronological
sequence, the same discipline an accounting period lock enforces for the calendar as a whole (§2.3),
scoped here to one bank account's own history rather than the whole ledger.

## 11.4 The matching engine: how statement lines and book lines get paired up

For each unmatched statement line and each unmatched posted GL line on the bank account, the
matching engine scores how likely a pair is to actually be the same real-world transaction, based on:
an exact amount match (required before any pair is even considered — amounts must agree to the cent
and run in the same direction), how close the two dates are, whether either side's reference or
transaction number appears in the other side's text, whether a shared cheque or reference number
appears on both, how similar the two descriptions are in plain-language terms, and whether a known
customer's or supplier's name or payment reference appears in the statement text. A pair scoring high
enough, and not tied with any other equally-plausible pair, is **auto-confirmed** outright; a pair
scoring more modestly is **proposed** for a human to confirm or reject; anything scoring too low is
never suggested at all. These thresholds are themselves configurable per matching rule, and an
organization can define several rules — generally ordered, applied to specific bank accounts or to
every account — rather than being stuck with one fixed sensitivity for every situation.

Where the automatic engine can't confidently resolve something, a human can match manually — one
statement line to one book line, one to many, or many to one (never many-to-many in a single match),
with the matched amount always being whichever side's total is smaller, so a partial match correctly
leaves the leftover on the larger side still open for a later match or for an adjustment. Several
manual matches can also be submitted together in one bulk action.

## 11.5 Adjustments: the only part of this module that posts a journal

Everything described so far — importing, matching, confirming — only ever *compares* data; none of it
touches the General Ledger. **Adjustments are the one exception**, and they exist for exactly the
case reconciliation is meant to surface: something real happened to the bank account that simply
isn't in your books yet.

Six fixed adjustment types exist, each with its own built-in cash direction and its own default
offsetting account:

| Type | Money in or out? | Default offset account |
|---|---|---|
| Bank charges | Out | Bank Charges Expense (5100) |
| Bank interest | In | Bank Interest Income (4040) |
| Direct debit | Out | Bank Reconciliation Suspense (1799) |
| Direct credit | In | Bank Reconciliation Suspense (1799) |
| Unknown bank debit | Out | Bank Reconciliation Suspense (1799) |
| Unknown bank credit | In | Bank Reconciliation Suspense (1799) |

```
Money in  (interest, direct credit, unknown credit):  Dr  Bank Account      / Cr  offset account
Money out (bank charge, direct debit, unknown debit): Dr  offset account     / Cr  Bank Account
```

The "Suspense" account exists specifically for the direct-debit, direct-credit, and unknown-item
types — a temporary holding account for something the bank statement proved happened but that hasn't
yet been properly classified to its real expense or income account; a controller would typically
investigate and reclassify a suspense balance out to its correct account in due course, rather than
leaving it there indefinitely. If an adjustment is tied to a specific statement line, posting it
immediately and automatically clears that line against the new journal entry too — the adjustment
both fills the gap in the books and resolves the reconciling item in one action. Reversing a posted
adjustment, as with everything else in this manual, posts a fresh equal-and-opposite journal rather
than touching the original.

## 11.6 Completing and reopening a reconciliation

Completing a reconciliation requires every proposed match to have first been confirmed or rejected —
nothing is allowed to complete with an unresolved question mark still sitting on it — and freezes the
reconciliation's figures (the book balance, outstanding deposits and withdrawals, charges, interest,
and the adjustments posted within it) as a permanent record rather than something that keeps
recalculating after the fact. If the difference between the books and the statement falls outside an
acceptable tolerance, completing still requires an explicit reason and a specific permission to
override it — a reconciliation is not supposed to "complete" with an unexplained gap by default.

Only the single most recently completed reconciliation for a given bank account can ever be reopened,
and only if nothing else is currently open for that account — consistent with the strict chronological
sequencing described in §11.3. Reopening does not touch any posted adjustment journal; it only
reinstates the ability to keep matching and adjusting that period's own lines.

Every lifecycle action in this module — created, statement imported, auto-matched, matched,
confirmed, rejected, unmatched, adjustment posted or reversed, reviewed, completed, reopened,
cancelled, deleted — is written to its own dedicated, append-only audit log, the same pattern already
described for Loans (§6.8) and Payroll (§12.8): a module whose integrity matters enough to deserve
its own narrative trail, distinct from the general Settings Audit Log (§16.2).

## 11.7 Bank Reconciliation accounting-mapping reference

| MappingKey | Default account | Role |
|---|---|---|
| `BANK_CHARGES_EXPENSE` | 5100 | Debited for bank charges brought in via a reconciliation adjustment |
| `BANK_INTEREST_INCOME` | 4040 | Credited for bank interest brought in via a reconciliation adjustment |
| `BANK_RECONCILIATION_SUSPENSE` | 1799 | Holds direct debits/credits and unknown bank items pending reclassification |

Two of these defaults are worth flagging explicitly rather than letting a reader assume a
coincidence is a bug: `BANK_CHARGES_EXPENSE` (5100) shares its default code with Bank Transfers' own
`BANK_TRANSFER_CHARGES` (§10.3), and `BANK_INTEREST_INCOME` (4040) shares its default code with the
Loans module's `LOAN_INTEREST_INCOME` (§6.9) — in both cases these are separately-configurable
mapping keys that simply happen to ship pointed at the same account by default. An organization that
wants bank-fee or bank-interest activity kept visibly separate from transfer fees or loan interest in
its reports should retarget one of each pair under Settings.

---

# Part 12 — Payroll

## 12.1 Why payroll is its own accounting discipline

Payroll is, mechanically, just another set of journal entries — but it's unusually dense with moving
parts, because a single pay run has to simultaneously: expense the gross cost of employing people;
record exactly what's owed to each employee net of deductions; record exactly what's owed to the
government and to benefit/statutory schemes on the employee's behalf; record the employer's *own*
matching costs on top of what the employee sees; and then, separately, actually move cash. Getting
any one of these wrong either understates the true cost of a workforce or misstates what's actually
owed to a third party — which is why payroll accounting is usually treated as a specialism in its
own right rather than "just another expense."

## 12.2 Master data: the shape of an organization

Before any pay can be calculated, four simple structures establish how employees are grouped:
**Department** (with a cost-center code for departmental cost reporting), **Position** (belonging to
a department), **Work Location**, and **Payroll Group** — the latter being the one that actually
matters mechanically, since it declares a pay frequency (`WEEKLY`, `BIWEEKLY`, `SEMI_MONTHLY`, or
`MONTHLY`) and every Payroll Calendar Period and Payroll Run belongs to exactly one group.

An `Employee` (deliberately a separate record from the login `User` entity used for system access —
an employee doesn't need a login, and a login doesn't need to be an employee) carries personal and
employment details, a required link to exactly one Payroll Group, an employment type (`FULL_TIME`,
`PART_TIME`, `CONTRACT`, `TEMPORARY`, `INTERN`), and an employment status (`ACTIVE`, `INACTIVE`,
`SUSPENDED`, `TERMINATED`). **Only employees flagged exactly `ACTIVE` are ever picked up by a payroll
run** — inactive, suspended, and terminated employees are all excluded identically; there's no
special distinction in payroll logic between "suspended" and "terminated" beyond the status label
itself.

## 12.3 Compensation: why it's effective-dated history, not a single field

An employee's pay is not simply a number sitting on the `Employee` record. It's resolved through a
separate `EmployeeSalaryStructure` record — a join between the employee and a reusable
`SalaryStructure` template (itself just a named bundle of pay components, like "Standard Monthly") —
that carries a `basicSalary` figure and an `effectiveFrom` date.

**This is deliberately an append-only history, never an overwrite.** When an employee gets a raise or
a promotion, the system doesn't edit the existing compensation record — it closes the old one (giving
it an `effectiveTo` date and flagging it no longer current) and inserts a brand-new one starting the
day the change takes effect. A new assignment's start date must be strictly after the current one's,
and the system refuses to let you backdate a change ahead of an already-recorded one. This matters
enormously for correctness: if you run (or re-run) payroll for a period three months ago, the
calculation engine resolves "what was this employee's pay *as of that period's end date*," which
correctly returns whatever rate was actually in force back then — not today's rate — even if several
raises have happened since. Every payroll record permanently stores exactly which compensation
assignment it used, so even if rates change again later, you can always trace back precisely what
was paid and why.

## 12.4 Pay Components: the building blocks of a payslip

A `PayComponent` is the atomic unit every earning, deduction, or contribution is built from — a
named line like "Housing Allowance" or "Union Dues" — and it's classified along three independent
dimensions that together determine exactly how it behaves:

- **Category** (`PayComponentCategory`) — what *kind* of thing it is: `BASIC`, `ALLOWANCE`,
  `OVERTIME`, `BONUS`, `COMMISSION`, `REIMBURSEMENT`, `BENEFIT`, `EMPLOYEE_STATUTORY_DEDUCTION`,
  `EMPLOYER_STATUTORY_CONTRIBUTION`, `LOAN_REPAYMENT`, `ADVANCE_REPAYMENT`, `OTHER_EARNING`,
  `OTHER_DEDUCTION`. This is mostly descriptive/organizational.
- **Side** (`PayComponentSide`) — the one that actually drives accounting treatment: `EARNING`
  (increases gross pay and employer cost), `EMPLOYEE_DEDUCTION` (reduces what the employee actually
  takes home), or `EMPLOYER_CONTRIBUTION` (an employer cost that the employee never sees reflected in
  their own net pay at all — a cost the company bears on top of, not instead of, what it pays the
  employee).
- **Calculation method** (`PayComponentCalculationMethod`) — `FIXED_AMOUNT`, `PERCENTAGE_OF_BASIC`,
  `PERCENTAGE_OF_GROSS`, or `HOURS_TIMES_RATE`. There is no free-form "formula" method — every
  component resolves through exactly one of these four mechanical calculations.

Crucially, **each `PayComponent` carries its own two GL account codes directly on the record** — a
debit account (used when the component behaves as an expense: every earning, and the employer-cost
leg of an employer contribution) and a credit account (used when it behaves as a liability being
credited: every employee deduction, and the liability leg of an employer contribution). This is a
different mechanism from the `MappingKey` approach used everywhere else in this system — payroll
components are self-describing about where they post, rather than resolving through a central
mapping table (though the three "built-in," non-component lines every run always includes — Basic
Salary, Income Tax, and loan/advance repayments — do still fall back to `MappingKey` resolution,
since they have no `PayComponent` of their own to carry account codes).

## 12.5 Statutory contributions and income tax: configurable, not hard-coded

This system deliberately contains **no hard-coded country-specific payroll scheme** — no built-in
CPF, no built-in Social Security formula, nothing tied to a specific jurisdiction's rules. Instead it
provides two fully admin-configurable shapes:

A **`StatutoryScheme`** declares a code, a name, a calculation basis (`BASIC_SALARY`, `GROSS_PAY`, or
`PENSIONABLE_EARNINGS`), separate employee and employer contribution rates (either of which can be
left unset, meaning that side doesn't contribute at all to that particular scheme), and three
distinct GL account codes — one for the employee's liability, one for the employer's own expense, and
one for the employer's liability. Schemes are themselves effective-dated, so a rate change takes
effect going forward without retroactively altering an already-posted run.

For each scheme effective as of a given pay period, the employee's share is computed as a percentage
of whichever basis the scheme specifies and posted as an `EMPLOYEE_DEDUCTION`; the employer's share
is computed the same way and posted as an `EMPLOYER_CONTRIBUTION` — meaning an employer contribution
genuinely posts *two* legs: an expense to the employer, and a liability for what the employer now
owes the scheme, distinct from what's deducted from the employee.

**Income tax** is computed through a separate, standard progressive-bracket mechanism
(`TaxConfiguration` and its ordered `TaxBracket` lines — each bracket a minimum, an optional maximum,
and a rate). Taxable income for the calculation is each earning flagged individually as taxable
(Basic Salary is always taxable), **minus** the employee's own statutory contributions — i.e.
statutory deductions are treated as pre-tax, which the system's own documentation is careful to
describe as "common but not universal practice," an acknowledged simplification rather than a
universal rule. Tax is then calculated bracket by bracket in the ordinary progressive way: each
bracket taxes only the slice of income that falls within its own range at its own rate. **A payroll
run cannot be calculated at all if no tax configuration is currently effective** — there's no
"skip tax" fallback.

One inconsistency worth flagging directly: `TaxConfiguration` carries its own
`taxPayableAccountCode` field, but the actual posting engine does not read it — Income Tax always
posts to the fixed `PAYROLL_EMPLOYEE_TAX_PAYABLE` mapping key regardless of what's configured on the
tax configuration record itself. This looks like an unused field rather than a deliberate design
choice.

## 12.6 The calculation engine: exactly what it does, and what it deliberately doesn't

For each active employee in a run, the calculation proceeds in a fixed sequence: resolve the
effective compensation assignment; take Basic Salary as the starting earning; resolve every other
structural earning line (fixed amounts and percentage-of-basic lines first, then percentage-of-gross
lines calculated against the running total of everything already resolved — an explicitly
acknowledged simplification rather than a true circular gross-up); layer in any approved one-off
variable inputs for the period (overtime hours, a one-time bonus — each marked "applied" the moment
it's used, so the same overtime can never accidentally be paid twice across two different runs);
resolve statutory contributions and income tax as described above; resolve any loan or salary-advance
installment due (§12.7); and finally total everything into gross pay, total deductions, and net pay.

**If the resulting net pay would be negative — deductions exceeding what the employee earned that
period — the system does not attempt to carry a negative balance forward or bill the employee for
the shortfall.** It clamps net pay to zero and flags the record for human review. This is a
deliberate simplification, not a configurable policy; a genuinely negative-net-pay situation always
needs a person to look at it.

**The single most important limitation to understand about this calculation engine: it does not
pro-rate for anything.** There is no logic anywhere that adjusts pay for a partial period — not for
an employee who joined mid-period, not for one who was terminated mid-period (as long as they're
still flagged `ACTIVE` at calculation time), not for unpaid leave. Every active employee is paid a
full period's Basic Salary and structure lines regardless of how much of that period they were
actually employed for. If your organization needs proration, it has to be handled manually today —
by adjusting the compensation assignment's effective date carefully, or by a manual adjustment
outside the calculation engine — rather than something the engine computes for you.

## 12.7 Loans and advances inside payroll: the simple cousin of the Loans module

As flagged in §6.1, `EmployeeLoan` and `SalaryAdvance` are deliberately much simpler instruments than
the general Loans module — a flat principal, a single fixed installment or deduction amount, no
amortization schedule, no compounding interest — that exist specifically to feed a fixed deduction
into the payroll calculation engine every period, until recovered.

**Issuing** either one books it immediately as an asset, never as an upfront expense:

```
Dr  Employee Loans Receivable (1750) or Salary Advances Receivable (1760)     amount
    Cr  Bank Account (1010)                                                    amount
```

**During calculation**, for every active loan or outstanding advance the employee has, the engine
adds a fixed `EMPLOYEE_DEDUCTION` line for that period's installment (capped at whatever balance
actually remains, so the very last installment doesn't overshoot). A deliberate, documented
simplification: the *entire* installment is applied to principal — this mechanism does not split an
installment between principal and interest the way the general Loans module does.

Crucially, **the actual subledger balance (how much is still outstanding) is only reduced after the
payroll journal itself has actually posted** — not at calculation time, and not while a run merely
sits `CALCULATED` or `UNDER_REVIEW`. A loan automatically closes once both outstanding principal and
interest hit zero; an advance flips to fully `RECOVERED` the same way.

**Reimbursements** (`ReimbursementClaim`) are a genuinely separate workflow from payroll runs
entirely — an employee submits a claim, it's approved (which books `Dr Reimbursement Expense (5070)
/ Cr Reimbursement Payable (2140)`, unless the claim is flagged as already recorded elsewhere, e.g.
already captured via an ordinary bill), and then paid (`Dr Reimbursement Payable / Cr Bank`). None of
this ever touches a `PayrollRun` or appears on a payslip — despite a `payrollRun` field existing on
the claim entity, nothing in the codebase ever actually links a claim to a run.

## 12.8 The Payroll Run lifecycle

A `PayrollRun` belongs to exactly one `PayrollCalendarPeriod` and moves through a deliberately
strict, auditable sequence:

```
DRAFT → (calculate) → CALCULATED → (submit for review) → UNDER_REVIEW
      → (approve) → APPROVED → (post) → POSTED → (pay) → PAID
(POSTED or PAID) → (reverse) → REVERSED
(DRAFT / CALCULATED / UNDER_REVIEW) → (cancel) → CANCELLED
```

A `PayrollCalendarPeriod`'s own start/end/pay/accounting dates are still entered by hand for each
period rather than auto-generated from a group's frequency, but the periods themselves now carry
real guardrails that an earlier version of this module lacked: creating or editing one validates that
its date range doesn't overlap any other period already defined for the same payroll group, and a
period can only be edited or soft-deleted while it's still `OPEN` **and** has no payroll run (other
than a cancelled one) already recorded against it — once a real run exists for a period, that
period's own dates are locked in place by that fact alone, separately from whatever the run itself
later does. (A status-transition mechanism exists on the period itself for marking it `PROCESSING` or
`CLOSED`, but — as before — nothing in the payroll run lifecycle actually calls it; a period's own
status field doesn't move on its own just because a run against it did.)

**A genuine segregation-of-duties control is enforced in code, not just left to policy:** the person
who approves a run cannot be the same person who prepared it or the same person who submitted it for
review. This is checked at the moment of approval and will outright reject the action if the
approver is the same user. Every single transition — created, calculated, submitted, approved,
posted, paid, reversed, cancelled — writes its own entry to a dedicated payroll audit log, separate
from the general Settings Audit Log described in Part 16.

Calculating a run that's already been calculated once (allowed from `DRAFT` or `CALCULATED`, not
beyond) is a genuine full recalculation — every existing record for the run is deleted, every
variable input that had been marked "applied" to this run is released back to available, and every
active employee is calculated completely fresh from scratch. It is not an incremental patch.

## 12.9 Posting payroll to the General Ledger

**Posting a run produces exactly one balanced journal for the entire run — never one journal per
employee.** `PayrollJournalService` walks every component of every employee's record and
accumulates a running total, per GL account, of everything that needs to be debited and everything
that needs to be credited, so the final journal carries one net line per account regardless of how
many employees or how many components contributed to it. In full:

- **Debits**: the default Salary Expense account for aggregate Basic Salary, each earning
  component's own configured debit account (allowances, overtime, bonuses — wherever one's
  configured), and each statutory scheme's employer-expense account for the employer's own
  contribution cost.
- **Credits**: the Employee Income Tax Payable account for the aggregate income tax; each statutory
  scheme's employee-liability account for the employee's own withheld contribution; each scheme's
  employer-liability account for what the employer itself now owes that scheme; the Employee Loans
  Receivable or Salary Advances Receivable account for any repayment lines; any other deduction
  component's own configured credit account; and, finally, a single aggregate **Salary Payable**
  line for the sum of every employee's net pay — the one figure representing what's actually owed to
  the workforce as a group once this journal posts.

This happens only after the period-lock guard confirms the run's accounting date is actually
postable — the same central gate every other module goes through. **Immediately after** the journal
successfully posts (not before, and not as part of the same atomic step as the calculation), any
loan or advance repayment lines actually reduce their subledger balances — timed deliberately so a
run that fails to post never leaves a loan balance reduced with nothing to show for it in the ledger.

**Paying the run** is a deliberately separate, later action, producing its own separate two-line
journal — `Dr Salary Payable (2100) / Cr Bank Account (1010)` for the total net pay — precisely
because the moment wages are *earned and owed* (posting) and the moment cash actually *leaves the
bank* (payment) are genuinely two different accounting events that shouldn't be conflated into one
entry. The payment journal's own documentation is explicit that it must never re-debit a salary
expense account, since doing so would double the recorded cost of payroll.

## 12.10 Payslips, reports, and reversal

A **payslip** is never recomputed on demand — it's exactly the stored record the calculation engine
produced at the time, served read-only, so a payslip always shows precisely what was calculated and
posted, even if master data (a pay component's name, say) changes afterward. The **Payroll
Register** is simply the full employee-by-employee detail of one run. A **GL reconciliation** report
compares the running total of net pay across every posted run against the actual current balance of
the Salary Payable account in the GL, surfacing any difference — the closest thing this system has to
"does the subledger match the ledger" for payroll specifically. There is no separate,
dedicated statutory-contributions report; that detail is only visible inside the register's own
component breakdown.

**Reversing a posted or paid run** reverses its GL journal(s) — the payment journal first if the run
had reached `PAID`, then the main posting journal — using the exact same generic equal-and-opposite
reversal mechanism (§1.2) every other module relies on, which is then its own fully audited event.
This is the single most important limitation in the entire payroll module to understand clearly,
because it's stated directly and honestly in the system's own code comments: **reversing a run does
not restore the outstanding balance of any employee loan or salary advance that run's posting had
already reduced or closed.** The General Ledger is correctly put back to where it was before the run
posted — but the loan/advance subledger is not; it's left exactly as the (now-reversed) posting had
left it. If you reverse a payroll run that happened to pay down someone's salary advance, that
advance's balance stays reduced even though the GL no longer reflects the payroll expense that
funded the reduction. Correcting that mismatch today requires a manual adjustment to the loan or
advance record — this is an acknowledged gap, not a subtlety to be reasoned around.

## 12.11 Payroll Chart of Accounts reference

| Code | Account | Role |
|---|---|---|
| 1750 | Employee Loans Receivable | Asset — payroll-specific employee loan balances |
| 1760 | Salary Advances Receivable | Asset — payroll-specific salary advance balances |
| 2100 | Salary Payable | Liability — aggregate net pay owed, credited at posting, debited at payment |
| 2110 | Employee Income Tax Payable | Liability — income tax withheld |
| 2120 | Statutory Contributions Payable (Employee) | Liability — employee statutory withholdings |
| 2130 | Statutory Contributions Payable (Employer) | Liability — employer's own statutory obligation |
| 2140 | Employee Reimbursements Payable | Liability — approved, unpaid reimbursement claims |
| 5040 | Salaries and Wages Expense | Expense — default earnings expense |
| 5050 | Employer Statutory Contributions Expense | Expense — employer's statutory cost |
| 5060 | Employee Benefits Expense | Expense — reserved for benefit-category components |
| 5070 | Employee Reimbursement Expense | Expense — reimbursement cost |

All eleven are flagged control accounts, for the same reason every other module's subledger-mirroring
accounts are: so a manual journal entry can never silently desynchronize the GL from what the payroll
engine itself is tracking.

---

# Part 13 — Settings: The Configuration Layer Everything Else Depends On

## 13.1 Accounting Mappings: the single lookup table everything resolves through

Every automated posting engine described in this manual — invoices, payments, bills, supplier
payments, prepayments, loans, payroll, expenses, deposits, down payments, bank transfers, bank
reconciliation, year-end closing — resolves *which specific GL account* to post to through exactly
one mechanism: a `MappingKey` enum value, looked up through `AccountingMappingService.resolve(key)`.
This is deliberately a thin, simple lookup table (`AccountingMapping`, one row per key, just the key
and the account code it currently points to) — not a rules engine, not a decision tree, just "this
key currently means this account."

**It is self-healing.** The very first time any key is ever looked up — even if an administrator has
never visited the Accounting Mappings settings screen at all — the system seeds a row for it using a
sensible hardcoded default (the ones listed throughout this manual: `INVOICE_REVENUE` defaults to
4020, `PAYROLL_SALARY_PAYABLE` defaults to 2100, and so on). This means the system behaves
identically to a version with no settings screen at all until an administrator deliberately decides
to retarget a mapping — there is no "unconfigured" state that blocks anything. **Posting only ever
fails if the account code a mapping currently points to doesn't actually exist in the Chart of
Accounts** (for instance, if it had been deleted) — at that point, the responsible posting engine
throws a clear, specific error naming the mapping and the missing code, directing the user to go fix
it under Settings.

Every change to a mapping (and only changes that actually alter the stored value — a no-op update
doesn't generate a log entry) is recorded to the Settings Audit Log (§16.2), including who changed
it, from what, to what, and why.

## 13.2 Organization profile

A single, deliberately singleton record (this is a single-tenant application — there's only ever one
organization) holding legal name, trading name, registration and tax identifiers, business
classification, contact details, and three separate address blocks (primary, billing, shipping).
Based on everything traced through the posting engines described in this manual, **this data is
purely informational** — it appears on outbound documents (invoices, statements, payslips) but is
never read by any GL-posting calculation. Only three of its fields (legal name, tax ID, registration
number) are actually written to the Settings Audit Log when changed; address and contact-detail
changes are not logged.

## 13.3 Bank Accounts

A `BankAccount` record ties a human-facing name to a specific Chart of Accounts code
(`glAccountCode`) — a simple validated string match, not a formal foreign key — plus a currency, a
default flag, and a "reconciliation enabled" flag. **The reconciliation flag is no longer a dead
field** — it is now the switch that determines whether a bank account can be targeted at all by the
Bank Reconciliation module (Part 11): a bank account must have it enabled before a statement import
or a reconciliation can be created against it.

One honest gap still worth stating plainly: **`isDefault` is stored and returned by the API, but
nothing anywhere in the posting logic actually consults it** to pick a fallback bank account — every
posting engine either uses a specific bank account explicitly chosen on the transaction itself, or
falls back directly to a `MappingKey`, never to "whichever bank account is flagged default."

What *does* actually work: whenever a transaction (a payment, a supplier payment, a prepayment, a
loan, an expense, a deposit, a down payment, one side of a bank transfer) has a specific
`BankAccount` chosen on it, that record's GL account always wins over whatever the general mapping
key would have resolved to — the mapping key is purely the fallback used when no specific bank
account was chosen for that particular transaction.

## 13.4 Numbering & Sequences

Covered in full in §1.4 — this is the settings surface that lets an administrator customize prefix,
padding, separator, and year/month reset behavior per document type. One asymmetry worth noting: unlike
Accounting Mapping, Organization, and Bank Account changes, **changes to numbering configuration are
not written to the Settings Audit Log** — there's no record of who changed a numbering format or
when.

## 13.5 Setup Completeness: a readiness checklist, not a gate

A ten-item checklist (organization profile populated, a current financial year exists, that year has
at least one open period, the chart of accounts has at least one account, a default base currency is
set, at least one tax category exists, at least one bank account exists, invoice numbering is
configured, a default invoice template exists, and — the one check that goes deeper than "does a
record exist" — every single Payroll-group mapping key actually resolves to a real account) is
surfaced on a "Setup Completeness" view, with a straightforward complete/total count and a
"ready to operate" flag.

**It is important to understand that this checklist is purely informational.** No posting path in
this system — not invoices, not bills, not prepayments, not loans, not any of the five newer modules
in Parts 7–11 — checks this flag or calls this service before deciding whether to let a transaction
through. An organization flagged "not ready to operate" can still fully use every feature described
in this manual; the checklist exists to help an administrator notice what they haven't set up yet,
not to prevent anything. Notably, Prepayments and Loans mapping groups have no equivalent "all
configured" check item at all, and neither do any of the five newer modules — only Payroll gets that
deeper validation today.

---

# Part 14 — Financial Statements and Reports

## 14.1 Balance Sheet

A Balance Sheet is a snapshot — "as of this date, what do we own, what do we owe, and what's left
over for the owners?" This system's `BalanceSheetServiceImpl` computes it by summing **every posted
journal line from the very beginning of the ledger** through the chosen as-of date, for every Asset,
Liability, and Equity account (never Income or Expense — those belong on a different report
entirely), converting each account's debit/credit totals into a single signed balance according to
its own normal-balance direction (§1.1), and skipping any account that nets to exactly zero.

Because Income and Expense accounts are deliberately excluded from this cumulative calculation, the
report separately computes a **"current year earnings"** figure — this year's revenue minus this
year's expenses, from the start of whichever financial year is currently flagged active through the
as-of date — and adds it into the equity total. This is what lets a balance sheet produced mid-year,
before any year-end closing has happened, still correctly reflect the profit or loss accumulated so
far this year as part of the owners' equity, exactly as accounting theory requires, without needing a
closing entry to exist yet.

The report always includes a **balance check** — total assets minus (total liabilities plus total
equity plus current-year earnings) — which should always equal exactly zero if the books genuinely
balance. If it doesn't, something in the ledger is genuinely wrong, since the system enforces
balanced journals at the point of posting (§1.2); a non-zero balance check here would point to a data
integrity problem, not a normal reporting nuance.

## 14.2 Trial Balance

Where the Balance Sheet only shows three of the five account types, the Trial Balance is the
complete picture — every Asset, Liability, Equity, Income, and Expense account, same cumulative
"from the beginning of time through this date" logic, but presented the traditional
accounting-ledger way: as two columns, Debit and Credit, rather than a single signed figure. A
positive balance shows up on the account's own normal side; a balance that happens to run the
"wrong" way (a contra balance) shows up, in absolute value, on the opposite side instead — exactly as
a real trial balance would present an account that's gone unexpectedly negative. The report's own
balance check is simply total debits minus total credits across every row, which — again — should
always land on exactly zero.

## 14.3 Dashboard

The operational dashboard pulls together, as of any date (defaulting to today): a cash balance
(summed across whichever accounts are configured as "cash accounts" for this purpose); the current
Accounts Receivable and Accounts Payable balances (resolved through the very same mapping keys the
posting engines themselves use, so the dashboard figure and the posting-engine figure can never
disagree about which account they mean); year-to-date net profit (a Profit & Loss report run from
the start of the active financial year to today); a twelve-month revenue/expense trend built by
running that same Profit & Loss calculation once per trailing month; the eight most recent posted
journal entries across the whole system, for a quick "what just happened" view; and a small
"accounting health" summary — the current financial year's name and status, how many periods are
currently locked, how many journal entries are still sitting in draft, and whether the current year
has an opening balance recorded yet.

---

# Part 15 — Business Intelligence and Analytics

## 15.1 What this module is, and — just as importantly — what it is not

Everything described in this Part is a **reporting layer only**. It reads the exact same posted
journal lines and the same Chart of Accounts every other report in this manual reads; it writes
nothing to the General Ledger, maintains no subledger of its own, and has no posting engine anywhere
inside it. Its one persisted table is purely a UI-configuration record (which alerts are enabled, and
at what threshold), not financial data. Think of this Part as a more analytically-minded cousin of
the Dashboard (§14.3) — same underlying data, built for a different kind of question.

## 15.2 Reclassifying accounts for analysis

Reports elsewhere in this manual group accounts by the five base account types (§1.3). For analysis,
this module regroups every account into one of fourteen more business-meaningful categories — cash
and bank, receivables, other current assets, non-current assets; payables, loan liabilities, other
current liabilities, non-current liabilities; equity; operating revenue, other income; cost of sales,
operating expense, other expense — derived from each account's chart-of-accounts grouping, its role
as a resolved control account (an AR/AP control account, a loan payable account, and so on), and a
handful of category-name heuristics (a category literally named "Other income" or "Other expense," a
category suggesting a long-term/non-current asset or liability). This reclassification exists purely
to make BI figures read naturally to a business audience — it never changes how an account posts or
how any other report in this manual treats it.

## 15.3 The Executive Dashboard and Revenue Analytics

The **Executive Dashboard** presents thirteen headline metrics — Revenue, Gross Profit, Gross Profit
Margin, Operating Expenses, Net Profit, Net Profit Margin, Cash & Bank Balance, Accounts Receivable,
Accounts Payable, Working Capital, Outstanding Loans, Current Assets, Current Liabilities — each
compared against a prior period with a direction and a plain-language explanation, alongside five
trend lines (Revenue, Expenses, Gross Profit, Net Profit, Cash & Bank) plotted over a chosen
granularity.

**Revenue Analytics** breaks revenue down by whichever dimension is meaningful and actually
available in the data today — by account, by category, by customer, by product, by whether the
underlying item is a product or a service, by invoice status, by invoice currency — while being
explicit, dimension by dimension, about which breakdowns the data doesn't yet support (branch,
department, and salesperson are all recognized as potentially useful breakdowns that nothing in the
system currently records, and the report says so plainly rather than silently omitting them).

## 15.4 Drill-down: from a headline figure to the individual journal line

Every KPI, trend point, and breakdown in this module carries enough information to be **drilled
into** — from the summary figure, down to the specific accounts that make it up, and from any one of
those accounts, down to the individual posted journal lines behind it, each showing its journal
number, date, status, and source module. This is the module's own answer to "where did this number
actually come from" — a controller questioning a BI figure can always trace it back to the real,
underlying ledger entries, the same entries every other report in this manual is built from.

## 15.5 Alerts

Seven specific conditions can be configured to flag attention: a significant revenue decline or
expense increase against a comparison period, a low cash position (less than a configurable number
of months of average expenses covered by cash on hand), a high proportion of receivables overdue, a
heavy burden of bills due soon relative to cash on hand, a significant decline in profit margin, and
a budget-variance alert that is always reported as unavailable today, since the system has no budget
data source to compare against. Each is independently enabled or disabled, with its own threshold and
comparison window, editable from Settings.

---

# Part 16 — Governance and Audit

## 16.1 Who did what, and when: automatic auditing on every record

Every business entity in this system — invoices, payments, bills, loans, prepayments, journal
entries, and more — automatically tracks who created it and when, through Spring's standard JPA
auditing infrastructure: the moment any such record is saved, the system reads whoever is currently
authenticated and stamps it as the creator, permanently, in a field that can never be edited
afterward. If a record was created by an automated process with no logged-in user behind it (a
seeder populating demo data, for instance), that field is simply left blank rather than guessed at.
Every record also carries an optimistic-locking version number (preventing two people from
silently overwriting each other's concurrent edits) and supports soft deletion — a "deleted" record
is flagged and hidden, never actually erased from the database.

## 16.2 The Settings Audit Log: a narrower, deliberately scoped trail

It's worth being precise about something that could otherwise cause confusion: the application's
general "Audit Trail" settings screen and the backend's `SettingsAuditLog` are **the exact same
data** — there is no separate, broader "every entity's every change" audit feature anywhere in this
system. The log is deliberately scoped to exactly three categories of change: **Accounting
Mappings**, the **Organization** profile, and **Bank Accounts** — the screen's own description is
explicit that "cosmetic settings aren't logged here since they carry no financial risk." Each entry
records what changed, from what value to what value, who changed it, when, and (optionally) why.

This means a great many things described elsewhere in this manual — a prepayment being activated, a
loan being disbursed, a payroll run being approved, a numbering format being changed, an expense
being posted, a bank reconciliation being completed — leave **no** trace in this particular log.
Their own trail exists instead through the posted journal entries they generate (which, recall, are
never edited or deleted, only reversed) and through the ordinary created-by/updated-by/updated-at
fields on the records themselves — just not through this specifically-scoped settings log.

## 16.3 Module-specific audit logs: a pattern now used in three places

Three modules in this system have grown their own dedicated, append-only audit log, entirely
separate from both the generic per-record auditing (§16.1) and the Settings Audit Log (§16.2):
**Payroll** (every run-lifecycle transition, §12.8), **Loans** (every loan-lifecycle action, §6.8),
and **Bank Reconciliation** (every reconciliation and matching action, §11.6). Each exists for the
same reason: these are modules whose lifecycle is intricate and consequential enough — a multi-step
approval chain, a loan's full history of disbursement and repayment, a bank account's reconciled
history — that a narrative, purpose-built trail scoped to that one module is more useful than relying
on the generic mechanisms alone. If you're looking for "what happened to this loan" or "what happened
to this payroll run" or "what happened during this reconciliation," look at that module's own log
first, not the Settings Audit Log, which was never meant to cover any of them.

## 16.4 Control accounts and the period lock, revisited as governance tools

Two mechanisms already described in detail (§1.3 and §2.3) are worth naming explicitly as the
system's primary *governance* controls, because that's really what they are: the control-account
restriction stops anyone from manually overriding what an automated subledger says is true, and the
period lock stops anyone from altering a period's figures after that period's books have been
reviewed and closed out. Together, they are this system's answer to "how do we know the numbers we
reported last month can't quietly change this month" — which is, at bottom, the entire reason
accounting periods and control accounts exist as concepts in the first place. As noted in §1.3, this
protection has not yet been extended to cover the newest modules' own subledger-mirroring accounts —
worth remembering as those modules see heavier use.

---

# Part 17 — Known Limitations and Simplifications, Consolidated

Everything below has already been flagged in context, in the section describing the relevant
module. This section exists purely as a single, scannable list for anyone who wants the "what to be
careful about" picture without reading the whole manual end to end.

- **No remittance workflow for any collected or withheld tax.** Sales Tax Payable (2090) and
  Withholding Tax Payable (2150) accumulate correctly as the system automatically posts to them, but
  actually paying the tax authority and clearing the liability has to be done as an ordinary manual
  journal entry — there is no dedicated "remit tax" feature.
- **No inventory or cost-of-goods-sold accounting.** Selling an `INVENTORY`-type product posts
  revenue exactly like any other product type; nothing reduces a stock asset or books a COGS
  expense anywhere in this system.
- **Bills have no per-line expense-account routing.** Every bill's entire net expense lands on one
  generic default expense account, unlike invoices, which do split revenue by each line's product's
  configured income account — and unlike the newer Expenses module (Part 7), which does support a
  distinct GL account per line.
- **Reallocating a payment to a different invoice doesn't post an adjusting GL entry**, unlike
  allocating or removing an allocation, which both do.
- **Supplier payments have no refund mechanism and no cancellation/reversal journal method**, unlike
  customer payments, which have both.
- **AR aging includes draft invoices with a positive balance; AP aging excludes draft bills.** This
  asymmetry is in the code as written, not a documentation error.
- **A `TaxCategory`'s rate is not automatically applied to invoice or bill lines by the backend** —
  the line-level tax rate is whatever value the request explicitly supplies; `TaxCategory` is
  consulted only as descriptive metadata on a `Product`.
- **Prepayments have no reversal action once any period has been recognized** — only a write-off of
  the entire remaining balance. A `REVERSED` status value exists on the entity but is never actually
  set by any code.
- **The payroll calculation engine performs no proration of any kind** — not for mid-period hires,
  terminations, or unpaid leave. Every active employee is paid a full period's compensation
  regardless of how much of the period they actually worked.
- **Reversing a posted or paid payroll run correctly reverses its GL journal, but does not restore
  any employee loan or salary advance balance that run's posting had already reduced or closed.**
  This is a genuine, acknowledged subledger/GL mismatch after a payroll reversal, requiring a manual
  correction to the loan or advance record.
- **`TaxConfiguration.taxPayableAccountCode` is stored but never actually read** by the payroll
  posting engine, which always uses the fixed `PAYROLL_EMPLOYEE_TAX_PAYABLE` mapping key regardless.
- **`BankAccount.isDefault` is a stored flag with no behavior behind it** — no posting logic picks a
  "default" bank account automatically; a specific account must always be chosen on the transaction
  or resolved through a mapping key.
- **Numbering configuration changes are not recorded to the Settings Audit Log**, unlike Accounting
  Mapping, Organization, and Bank Account changes.
- **The Setup Completeness checklist is purely informational and blocks nothing** — an organization
  can transact fully regardless of its completeness score, and the checklist has no dedicated
  "all configured" check for the Prepayments, Loans, or any of the five newer modules' mapping
  groups specifically (only Payroll gets that deeper check).
- **Two different, unrelated "loan status" enums exist** with the same short name —
  `enums/loan/LoanStatus.java` for the general Loans module, and the simpler `enums/LoanStatus.java`
  used only by Payroll's `EmployeeLoan`. Don't conflate them when reading the code.
- **The Down Payments module's two new control accounts (2087, 1747) have not been added to the
  control-account protection list described in §1.3** — a manual journal entry can currently still be
  posted directly to either one, unlike the equivalent accounts in every other module.
- **`BANK_CHARGES_EXPENSE` (Bank Reconciliation) defaults to the same account code as
  `BANK_TRANSFER_CHARGES` (Bank Transfers), and `BANK_INTEREST_INCOME` (Bank Reconciliation) defaults
  to the same code as `LOAN_INTEREST_INCOME` (Loans).** Each pair is separately configurable; they
  simply ship pointed at the same account by default, which an organization wanting the activity kept
  visibly separate should retarget under Settings.
- **The Business Intelligence module's budget-variance alert is always reported as unavailable** —
  the system has no budget data source anywhere to compare actuals against.
- **A `PayrollCalendarPeriod`'s own `PROCESSING`/`CLOSED` status-transition methods exist but are
  never called by anything in the payroll run lifecycle** — a period's own status field does not
  move automatically just because a run against it was calculated, posted, or paid.

---

# Appendix A — Complete Chart of Accounts (as seeded)

| Code | Account | Type |
|---|---|---|
| 1000 | Cash on Hand | Asset |
| 1010 | Checking Account | Asset |
| 1020 | Savings Account | Asset |
| 1030 | Money Market Account | Asset |
| 1500 | Buildings | Asset |
| 1510 | Land | Asset |
| 1520 | Machinery & Equipment | Asset |
| 1530 | Furniture & Fixtures | Asset |
| 1600 | Vehicles | Asset |
| 1610 | Computers | Asset |
| 1620 | Software | Asset |
| 1630 | Leasehold Improvements | Asset |
| 1700 | Intangible Assets | Asset |
| 1710 | Accumulated Depreciation | Asset |
| 1720 | Accumulated Amortization | Asset |
| 1730 | Other Fixed Assets | Asset |
| 1740 | Supplier Advances ★ | Asset |
| 1745 | Deposits Paid | Asset |
| 1747 | Supplier Downpayments | Asset |
| 1750 | Employee Loans Receivable ★ | Asset |
| 1760 | Salary Advances Receivable ★ | Asset |
| 1770 | Purchase Tax Receivable ★ | Asset |
| 1780 | Loans Receivable ★ | Asset |
| 1790 | Interest Receivable ★ | Asset |
| 1795 | Prepaid Expenses ★ | Asset |
| 1799 | Bank Reconciliation Suspense | Asset |
| 2000 | Long Term Liability | Liability |
| 2010 | Current Liability | Liability |
| 2020 | Other Creditors | Liability |
| 2030 | Credit Card Liability | Liability |
| 2040 | Other Liability | Liability |
| 2050 | Trust Account Liability | Liability |
| 2060 | Rents Held in Trust | Liability |
| 2070 | Merchant Account Fees Payable | Liability |
| 2080 | Customer Deposits ★ | Liability |
| 2085 | Deposits Received | Liability |
| 2087 | Customer Downpayments | Liability |
| 2090 | Sales Tax Payable ★ | Liability |
| 2100 | Salary Payable ★ | Liability |
| 2110 | Employee Income Tax Payable ★ | Liability |
| 2120 | Statutory Contributions Payable — Employee ★ | Liability |
| 2130 | Statutory Contributions Payable — Employer ★ | Liability |
| 2140 | Employee Reimbursements Payable ★ | Liability |
| 2150 | Withholding Tax Payable ★ | Liability |
| 2160 | Loans Payable ★ | Liability |
| 2170 | Interest Payable ★ | Liability |
| 3000 | Owners Equity | Equity |
| 3010 | Retained Earnings | Equity |
| 3020 | Other Equity | Equity |
| 3030 | Capital Contributions | Equity |
| 4000 | Operating Revenue | Income |
| 4010 | Other Income | Income |
| 4020 | Sales | Income |
| 4030 | Sales Revenue | Income |
| 4040 | Interest Income | Income |
| 4042 | Loan Fee Income | Income |
| 4050 | FX Gain | Income |
| 4060 | Forfeited Deposits Income | Income |
| 5000 | Operating Expenses | Expense |
| 5010 | Payroll Expense | Expense |
| 5020 | Other Expense | Expense |
| 5030 | Shipping / Freight | Expense |
| 5040 | Salaries and Wages Expense | Expense |
| 5050 | Employer Statutory Contributions Expense | Expense |
| 5060 | Employee Benefits Expense | Expense |
| 5070 | Employee Reimbursement Expense | Expense |
| 5080 | Interest Expense | Expense |
| 5085 | Loan Write-off Expense | Expense |
| 5090 | Loan Fees Expense | Expense |
| 5095 | Forfeited Deposits Expense | Expense |
| 5100 | Bank Charges Expense | Expense |
| 5110 | FX Loss | Expense |
| 6000 | Cost of Goods Sold | COGS |
| 6010 | Job Materials | COGS |
| 6020 | Equipment Rental | COGS |
| 6030 | Subcontractor Costs | COGS |
| 6100 | Direct Cost | Direct Cost |
| 6110 | Direct Labor | Direct Cost |
| 6120 | Other Direct Costs | Direct Cost |
| 6130 | Credit Card Fees | Direct Cost |
| 6200 | Credit Card Account | Liability |
| 6210 | Accounts Payable ★ | Liability |
| 6220 | Accounts Receivable ★ | Asset |

★ = flagged as a control account; cannot be posted to from a manual journal entry. Note that the
Down Payments (1747, 2087), Deposits (1745, 2085, 4060, 5095), Bank Transfer (5100, 4050, 5110), and
Bank Reconciliation (1799) accounts are **not** on this protected list, per the gap noted in §1.3 and
Part 17.

---

# Appendix B — Complete MappingKey Reference

| Group | MappingKey | Default Account |
|---|---|---|
| Sales | `PAYMENT_BANK_ACCOUNT` | 1010 |
| Sales | `PAYMENT_CASH_ACCOUNT` | 1000 |
| Sales | `PAYMENT_ACCOUNTS_RECEIVABLE` | 6220 |
| Sales | `PAYMENT_CUSTOMER_ADVANCES` | 2080 |
| Sales | `INVOICE_ACCOUNTS_RECEIVABLE` | 6220 |
| Sales | `INVOICE_REVENUE` | 4020 |
| Sales | `INVOICE_SALES_TAX_PAYABLE` | 2090 |
| Sales | `CUSTOMER_DOWNPAYMENT_LIABILITY` | 2087 |
| Purchases | `BILL_ACCOUNTS_PAYABLE` | 6210 |
| Purchases | `BILL_DEFAULT_EXPENSE` | 5000 |
| Purchases | `BILL_PURCHASE_TAX_RECEIVABLE` | 1770 |
| Purchases | `SUPPLIER_PAYMENT_BANK_ACCOUNT` | 1010 |
| Purchases | `SUPPLIER_PAYMENT_CASH_ACCOUNT` | 1000 |
| Purchases | `SUPPLIER_PAYMENT_ADVANCES` | 1740 |
| Purchases | `TAX_WITHHOLDING_PAYABLE` | 2150 |
| Purchases | `SUPPLIER_DOWNPAYMENT_ASSET` | 1747 |
| Payroll | `PAYROLL_SALARY_PAYABLE` | 2100 |
| Payroll | `PAYROLL_DEFAULT_SALARY_EXPENSE` | 5040 |
| Payroll | `PAYROLL_EMPLOYEE_TAX_PAYABLE` | 2110 |
| Payroll | `PAYROLL_LOAN_RECEIVABLE` | 1750 |
| Payroll | `PAYROLL_ADVANCE_RECEIVABLE` | 1760 |
| Payroll | `PAYROLL_REIMBURSEMENT_PAYABLE` | 2140 |
| Payroll | `PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE` | 5070 |
| Payroll | `PAYROLL_PAYMENT_BANK_ACCOUNT` | 1010 |
| Closing | `CLOSING_RETAINED_EARNINGS` | 3010 |
| Prepayments | `PREPAYMENT_DEFAULT_ASSET` | 1795 |
| Prepayments | `PREPAYMENT_DEFAULT_EXPENSE` | 5000 |
| Prepayments | `PREPAYMENT_BANK_ACCOUNT` | 1010 |
| Loans | `LOAN_RECEIVABLE` | 1780 |
| Loans | `LOAN_PAYABLE` | 2160 |
| Loans | `LOAN_INTEREST_INCOME` | 4040 |
| Loans | `LOAN_INTEREST_EXPENSE` | 5080 |
| Loans | `LOAN_INTEREST_RECEIVABLE` | 1790 |
| Loans | `LOAN_INTEREST_PAYABLE` | 2170 |
| Loans | `LOAN_FEE_EXPENSE` | 5090 |
| Loans | `LOAN_FEE_INCOME` | 4042 |
| Loans | `LOAN_WRITE_OFF_EXPENSE` | 5085 |
| Loans | `LOAN_BANK_ACCOUNT` | 1010 |
| Banking | `BANK_TRANSFER_CHARGES` | 5100 |
| Banking | `FX_GAIN` | 4050 |
| Banking | `FX_LOSS` | 5110 |
| Banking | `BANK_CHARGES_EXPENSE` | 5100 |
| Banking | `BANK_INTEREST_INCOME` | 4040 |
| Banking | `BANK_RECONCILIATION_SUSPENSE` | 1799 |
| Deposits | `DEPOSIT_PAID_ASSET` | 1745 |
| Deposits | `DEPOSIT_RECEIVED_LIABILITY` | 2085 |
| Deposits | `DEPOSIT_FORFEIT_INCOME` | 4060 |
| Deposits | `DEPOSIT_FORFEIT_EXPENSE` | 5095 |
| Deposits | `DEPOSIT_BANK_ACCOUNT` | 1010 |

Expenses posts with no `MappingKey` at all — both sides of its journal resolve directly from
user-chosen records (the line's own account, the expense's own payment account), never from a
configurable mapping. Document Settlement has no `MappingKey` of its own because it posts no
journal at all (§9.3).

---

# Appendix C — Document Numbering Reference

| Module | Prefix | Seeded/Default Format |
|---|---|---|
| Invoice | INV | `INV-YYYY-00001`, resets yearly |
| Journal Entry | JNL | `JNL-YYYY-00001`, resets yearly |
| Employee | EMP | `EMP-0001`, never resets |
| Payroll Run | PR | `PR-YYYY-00001`, resets yearly |
| Bill | BILL | `BILL-00001` |
| Supplier Payment | SPMT | `SPMT-00001` |
| Customer Payment / Receipt | RCP | `RCP-00001` |
| Prepayment | PPY | `PPY-00001` |
| Loan | LN | `LN-00001` |
| Expense | (admin-configured) | no built-in default prefix shipped |
| Deposit | (admin-configured) | no built-in default prefix shipped |
| Down Payment | (admin-configured) | no built-in default prefix shipped |
| Bank Transfer | (admin-configured) | no built-in default prefix shipped |
| Bank Reconciliation | (admin-configured) | no built-in default prefix shipped |
| Document Template (Invoice/Quote/PO/Credit Note/Delivery Note/Receipt) | TMPL-INV / TMPL-QTE / TMPL-PO / TMPL-CN / TMPL-DN / TMPL-RCT | cosmetic numbering only |

---

*This manual reflects the system as implemented at the time of writing. It will drift out of date
the moment new modules are added or existing ones change — treat it as a snapshot of ground truth to
be kept current, not a specification to build toward.*
