package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.dto.bankrec.CreateAdjustmentRequest;
import com.unionsg.xaccounting.dto.bankrec.SaveReconciliationRequest;
import com.unionsg.xaccounting.dto.deposit.CreateDepositRequest;
import com.unionsg.xaccounting.dto.deposit.ForfeitDepositRequest;
import com.unionsg.xaccounting.dto.deposit.RefundDepositRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseLineRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.settings.SaveBankAccountRequest;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.deposit.DepositTypeRepository;
import com.unionsg.xaccounting.repository.expense.ExpenseRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.banking.BankTransferService;
import com.unionsg.xaccounting.service.bankrec.BankReconciliationService;
import com.unionsg.xaccounting.service.deposit.DepositService;
import com.unionsg.xaccounting.service.downpayment.DownpaymentService;
import com.unionsg.xaccounting.service.expense.ExpenseService;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a small, varied demo for the five modules added after the original investor-demo pass:
 * Expenses, Deposits, Down Payments, Bank Transfers, and Bank Reconciliation. None of these post
 * to the existing USD "Operating Account" - they use two new bank accounts opened in the
 * organization's own base currency instead, so nothing here needs an exchange rate to post
 * cleanly (see the module's own currency-mismatch guards in {@code ExpenseService}/
 * {@code BankTransferCalculator}/{@code DownpaymentService}).
 *
 * <p>Every posted entry is dated between March and August 2026 - inside FY2026, past the two
 * periods {@link DemoDataSeeder} deliberately locks, so none of it needs the accounting calendar
 * reopened. The mix is deliberately varied (an expense split across categories, a deposit
 * refunded, another partially forfeited, a bank transfer with a fee, a reconciliation with both
 * a bank-charge and a bank-interest adjustment) so the Business Intelligence screens
 * (Part 15 of the accounting manual) have more than one flat line to show across Operating
 * Expenses, Other Income, and cash movement.</p>
 *
 * <p>Runs after {@link DemoDataSeeder} (customers, suppliers, financial years) and
 * {@link BankReconciliationSetupSeeder} (the default matching rule reconciliations rely on,
 * though this seeder posts its adjustments without needing a statement import or a match at
 * all). Idempotent and transactional, matching the rest of this package's seeders.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(8)
public class NewModulesDemoSeeder implements ApplicationRunner {

    private static final String KES_OPERATING_GL = "1020";
    private static final String KES_RESERVE_GL = "1030";

    private final ExpenseRepository expenseRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DepositTypeRepository depositTypeRepository;

    private final BankAccountService bankAccountService;
    private final ExpenseService expenseService;
    private final DepositService depositService;
    private final DownpaymentService downpaymentService;
    private final BankTransferService bankTransferService;
    private final BankReconciliationService bankReconciliationService;
    private final JournalService journalService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (expenseRepository.count() > 0) {
            log.info("New-modules demo data already present - skipping NewModulesDemoSeeder.");
            return;
        }

        Customer novaRetail = findCustomer("Nova Retail Group");
        Supplier atlasOffice = findSupplier("Atlas Office Supply Co.");
        Supplier voltageUtilities = findSupplier("Voltage Utilities Inc.");

        Long operatingAccountId = seedBankAccount("KES Operating Account", KES_OPERATING_GL, true);
        Long reserveAccountId = seedBankAccount("KES Reserve Account", KES_RESERVE_GL, false);
        fundNewAccounts();

        seedExpenses(operatingAccountId, atlasOffice, voltageUtilities);
        seedDeposits(operatingAccountId, novaRetail);
        seedDownpayments(operatingAccountId, novaRetail, atlasOffice);
        seedBankTransfers(operatingAccountId, reserveAccountId);
        seedBankReconciliation(operatingAccountId);

        log.info("New-modules demo seeding complete: expenses, deposits, downpayments, bank transfers "
                + "and one bank reconciliation with adjustments have been posted.");
    }

    // ================= Opening funding for the two new accounts =================

    /**
     * A freshly-opened bank account starts at zero, and every module that draws on one checks for
     * sufficient balance before posting. Capitalizes both new accounts with a straightforward
     * owner-contribution entry, dated before anything else in this seeder, instead of quietly
     * enabling overdraft.
     */
    private void fundNewAccounts() {
        CreateJournalLineRequest debitOperating = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf(KES_OPERATING_GL))
                .description("Owner contribution to open the KES operating account")
                .debitAmount(new BigDecimal("20000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();
        CreateJournalLineRequest debitReserve = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf(KES_RESERVE_GL))
                .description("Owner contribution to open the KES reserve account")
                .debitAmount(new BigDecimal("10000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();
        CreateJournalLineRequest credit = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf("3030"))
                .description("Capital contributed to fund the new KES accounts")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("30000.00"))
                .build();

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.of(2026, 3, 1))
                .reference("CAP-KES-ACCOUNTS")
                .description("Capital contribution funding the new KES operating and reserve accounts")
                .journalType(JournalType.GENERAL)
                .lines(List.of(debitOperating, debitReserve, credit))
                .build();

        var created = journalService.create(request);
        journalService.post(created.getId());
        log.info("Funded the two new KES bank accounts with a 30,000 capital contribution.");
    }

    // ================= Expenses =================

    private void seedExpenses(Long paymentAccountId, Supplier atlasOffice, Supplier voltageUtilities) {
        Long officeSupplies = accountId("5000");
        Long otherExpense = accountId("5020");

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2026, 3, 10), PaymentMethod.CARD,
                "Printer paper and toner",
                List.of(line("OFF", officeSupplies, "Printer paper and toner", "245.50")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2026, 4, 14), PaymentMethod.CASH,
                "Regional trade show",
                List.of(
                        line("MKT", officeSupplies, "Trade show banner printing", "300.00"),
                        line("TRV", otherExpense, "Taxi fares for trade show", "150.00")))));

        post(expenseService.create(expense(paymentAccountId, idOf(voltageUtilities), LocalDate.of(2026, 5, 20), PaymentMethod.CARD,
                "Office electricity - May",
                List.of(line("UTL", otherExpense, "Office electricity - May", "410.75")))));

        post(expenseService.create(expense(paymentAccountId, idOf(atlasOffice), LocalDate.of(2026, 6, 25), PaymentMethod.CARD,
                "Replacement desk chairs",
                List.of(line("OFF", officeSupplies, "Replacement desk chairs", "680.00")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2026, 7, 30), PaymentMethod.CASH,
                "Client site visit mileage",
                List.of(line("TRV", otherExpense, "Client site visit mileage", "220.00")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2026, 8, 12), PaymentMethod.MOBILE_MONEY,
                "Social media ad campaign",
                List.of(line("MKT", officeSupplies, "Social media ad campaign", "950.00")))));

        log.info("Seeded 6 expenses across March-August 2026, one split across two categories.");
    }

    private void post(com.unionsg.xaccounting.dto.expense.ExpenseResponse draft) {
        expenseService.post(draft.getId());
    }

    private SaveExpenseRequest expense(Long paymentAccountId, Long supplierId, LocalDate paymentDate,
                                        PaymentMethod method, String memo, List<SaveExpenseLineRequest> lines) {
        return SaveExpenseRequest.builder()
                .supplierId(supplierId)
                .paymentAccountId(paymentAccountId)
                .paymentDate(paymentDate)
                .paymentMethod(method)
                .memo(memo)
                .lines(lines)
                .build();
    }

    private SaveExpenseLineRequest line(String category, Long accountId, String description, String amount) {
        return SaveExpenseLineRequest.builder()
                .category(category)
                .accountId(accountId)
                .description(description)
                .amount(new BigDecimal(amount))
                .build();
    }

    // ================= Deposits =================

    private void seedDeposits(Long bankAccountId, Customer novaRetail) {
        Long securityDepositType = depositTypeId("Security Deposit");
        Long utilityDepositType = depositTypeId("Utility Deposit");
        Long customerSecurityType = depositTypeId("Customer Security Deposit");
        Long tenantDepositType = depositTypeId("Tenant Deposit");

        // A: paid, held, never touched again.
        depositService.create(depositPaid(securityDepositType, "Riverside Business Park (Landlord)",
                "3000.00", bankAccountId, LocalDate.of(2026, 3, 5)), true);

        // B: paid, then refunded in full.
        var utilityDeposit = depositService.create(depositPaid(utilityDepositType, "City Power & Water Co.",
                "500.00", bankAccountId, LocalDate.of(2026, 3, 12)), true);
        RefundDepositRequest refund = new RefundDepositRequest();
        refund.setAmount(new BigDecimal("500.00"));
        refund.setRefundDate(LocalDate.of(2026, 6, 20));
        refund.setNotes("Lease ended; utility account closed in good standing");
        depositService.refund(utilityDeposit.getId(), refund);

        // C: received from a customer, then partially forfeited.
        var customerDeposit = depositService.create(depositReceived(customerSecurityType,
                DepositCounterpartyType.CUSTOMER, novaRetail.getId(), null,
                "2000.00", bankAccountId, LocalDate.of(2026, 4, 2)), true);
        ForfeitDepositRequest forfeit = new ForfeitDepositRequest();
        forfeit.setAmount(new BigDecimal("400.00"));
        forfeit.setForfeitDate(LocalDate.of(2026, 7, 15));
        forfeit.setNotes("Damage to returned equipment");
        depositService.forfeit(customerDeposit.getId(), forfeit);

        // D: received, held, never touched again.
        depositService.create(depositReceived(tenantDepositType, DepositCounterpartyType.OTHER, null,
                "Unit 4B Tenant", "1500.00", bankAccountId, LocalDate.of(2026, 5, 8)), true);

        log.info("Seeded 4 deposits (2 paid, 2 received) - one refunded in full, one partially forfeited.");
    }

    private CreateDepositRequest depositPaid(Long typeId, String counterpartyName, String amount,
                                              Long bankAccountId, LocalDate depositDate) {
        CreateDepositRequest r = new CreateDepositRequest();
        r.setDirection(DepositDirection.DEPOSIT_PAID);
        r.setDepositTypeId(typeId);
        r.setCounterpartyType(DepositCounterpartyType.OTHER);
        r.setCounterpartyName(counterpartyName);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("KES");
        r.setDepositDate(depositDate);
        r.setBankAccountId(bankAccountId);
        return r;
    }

    private CreateDepositRequest depositReceived(Long typeId, DepositCounterpartyType counterpartyType,
                                                  Long customerId, String counterpartyName, String amount,
                                                  Long bankAccountId, LocalDate depositDate) {
        CreateDepositRequest r = new CreateDepositRequest();
        r.setDirection(DepositDirection.DEPOSIT_RECEIVED);
        r.setDepositTypeId(typeId);
        r.setCounterpartyType(counterpartyType);
        r.setCustomerId(customerId);
        r.setCounterpartyName(counterpartyName);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("KES");
        r.setDepositDate(depositDate);
        r.setBankAccountId(bankAccountId);
        return r;
    }

    private Long depositTypeId(String name) {
        return depositTypeRepository.findByDeletedFalse().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .map(DepositType::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Deposit type not found: " + name
                        + " - DatabaseSeeder should have seeded it already"));
    }

    // ================= Down Payments =================

    private void seedDownpayments(Long bankAccountId, Customer novaRetail, Supplier atlasOffice) {
        DownpaymentRequest customerDp = new DownpaymentRequest();
        customerDp.setType(DownpaymentType.CUSTOMER_DOWNPAYMENT);
        customerDp.setCustomerId(novaRetail.getId());
        customerDp.setAmount(new BigDecimal("5000.00"));
        customerDp.setCurrency("KES");
        customerDp.setBankAccountId(bankAccountId);
        customerDp.setPaymentDate(LocalDate.of(2026, 4, 10));
        customerDp.setDescription("Advance against a large custom order, agreed before the order is invoiced");
        customerDp.setPost(true);
        downpaymentService.create(customerDp);

        DownpaymentRequest supplierDp = new DownpaymentRequest();
        supplierDp.setType(DownpaymentType.SUPPLIER_DOWNPAYMENT);
        supplierDp.setSupplierId(atlasOffice.getId());
        supplierDp.setAmount(new BigDecimal("3000.00"));
        supplierDp.setCurrency("KES");
        supplierDp.setBankAccountId(bankAccountId);
        supplierDp.setPaymentDate(LocalDate.of(2026, 5, 15));
        supplierDp.setDescription("Advance to secure production capacity ahead of a bulk order");
        supplierDp.setPost(true);
        downpaymentService.create(supplierDp);

        log.info("Seeded 1 customer downpayment and 1 supplier downpayment, both posted and open.");
    }

    // ================= Bank Transfers =================

    private void seedBankTransfers(Long operatingAccountId, Long reserveAccountId) {
        var t1 = bankTransferService.create(transfer(operatingAccountId, reserveAccountId,
                LocalDate.of(2026, 3, 20), "5000.00", null, null));
        bankTransferService.post(t1.getId());

        var t2 = bankTransferService.create(transfer(reserveAccountId, operatingAccountId,
                LocalDate.of(2026, 5, 5), "2000.00", "15.00", null));
        bankTransferService.post(t2.getId());

        var t3 = bankTransferService.create(transfer(operatingAccountId, reserveAccountId,
                LocalDate.of(2026, 7, 1), "3000.00", null, null));
        bankTransferService.post(t3.getId());

        log.info("Seeded 3 bank transfers between the two KES accounts, one carrying a transfer fee.");
    }

    private SaveBankTransferRequest transfer(Long sourceId, Long destinationId, LocalDate date,
                                              String amount, String feeAmount, String feeAccountCode) {
        return SaveBankTransferRequest.builder()
                .sourceBankAccountId(sourceId)
                .destinationBankAccountId(destinationId)
                .transferDate(date)
                .amount(new BigDecimal(amount))
                .feeAmount(feeAmount != null ? new BigDecimal(feeAmount) : null)
                .feeAccountCode(feeAccountCode)
                .description("Routine transfer between operating and reserve accounts")
                .build();
    }

    // ================= Bank Reconciliation =================

    private void seedBankReconciliation(Long operatingAccountId) {
        SaveReconciliationRequest request = SaveReconciliationRequest.builder()
                .bankAccountId(operatingAccountId)
                .statementDate(LocalDate.of(2026, 3, 31))
                .periodStart(LocalDate.of(2026, 3, 1))
                .periodEnd(LocalDate.of(2026, 3, 31))
                .openingBankBalance(BigDecimal.ZERO)
                .closingBankBalance(BigDecimal.ZERO)
                .tolerance(new BigDecimal("50.00"))
                .notes("March 2026 statement for the KES operating account")
                .build();
        var reconciliation = bankReconciliationService.create(request);

        bankReconciliationService.createAdjustment(reconciliation.getId(), CreateAdjustmentRequest.builder()
                .adjustmentType(ReconciliationAdjustmentType.BANK_CHARGE)
                .transactionDate(LocalDate.of(2026, 3, 31))
                .amount(new BigDecimal("25.00"))
                .description("Monthly account maintenance fee")
                .build());

        bankReconciliationService.createAdjustment(reconciliation.getId(), CreateAdjustmentRequest.builder()
                .adjustmentType(ReconciliationAdjustmentType.BANK_INTEREST)
                .transactionDate(LocalDate.of(2026, 3, 31))
                .amount(new BigDecimal("12.50"))
                .description("Interest earned on balance")
                .build());

        log.info("Seeded one in-progress bank reconciliation for March 2026 with a bank charge and "
                + "a bank interest adjustment posted.");
    }

    // ================= Shared helpers =================

    private Long seedBankAccount(String accountName, String glAccountCode, boolean isDefault) {
        return bankAccountRepository.findAll().stream()
                .filter(a -> accountName.equals(a.getAccountName()))
                .map(BankAccount::getId)
                .findFirst()
                .orElseGet(() -> {
                    SaveBankAccountRequest request = new SaveBankAccountRequest();
                    request.setBankName("Union Trust Bank");
                    request.setAccountName(accountName);
                    request.setCurrency("KES");
                    request.setGlAccountCode(glAccountCode);
                    request.setIsDefault(isDefault);
                    request.setEnableReconciliation(true);
                    return bankAccountService.create(request).getId();
                });
    }

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code)
                .orElseThrow(() -> new IllegalStateException("Chart of accounts entry not found: " + code))
                .getId();
    }

    private Customer findCustomer(String displayName) {
        return customerRepository.findAll().stream()
                .filter(c -> displayName.equals(c.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo customer not found: " + displayName
                        + " - DemoDataSeeder should have seeded it already"));
    }

    private Supplier findSupplier(String displayName) {
        return supplierRepository.findAll().stream()
                .filter(s -> displayName.equals(s.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo supplier not found: " + displayName
                        + " - DemoDataSeeder should have seeded it already"));
    }

    private Long idOf(Supplier supplier) {
        return supplier != null ? supplier.getId() : null;
    }
}
