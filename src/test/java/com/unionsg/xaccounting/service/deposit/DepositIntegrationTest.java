package com.unionsg.xaccounting.service.deposit;

import com.unionsg.xaccounting.dto.deposit.*;
import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.settlement.SettlementSummaryResponse;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.deposit.*;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.deposit.DepositTypeRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settlement.DocumentSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs deposits against the real database, journal engine and invoice settlement
 * (same Postgres requirement as {@code XaccountingApplicationTests}). Each test rolls back.
 */
@SpringBootTest
@Transactional
class DepositIntegrationTest {

    @Autowired private DepositService service;
    @Autowired private DepositTypeRepository typeRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JournalService journalService;
    @Autowired private DocumentSettlementService settlementService;

    private Invoice openInvoice;

    @BeforeEach
    void setUp() {
        openInvoice = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() != InvoiceStatus.DRAFT && i.getStatus() != InvoiceStatus.CANCELLED
                        && i.getBalance() != null && i.getBalance().compareTo(new BigDecimal("100")) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo data has no open invoice"));
    }

    @Test
    void receivingADepositPostsBankAgainstALiabilityNotIncome() {
        DepositResponse deposit = service.create(received("1000"), true);

        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.ACTIVE);
        assertThat(deposit.getDepositNumber()).startsWith("DEP");
        assertThat(deposit.getAvailableBalance()).isEqualByComparingTo("1000");
        assertThat(deposit.getClassification()).isEqualTo(DepositClassification.CURRENT_LIABILITY);
        assertThat(deposit.getEffectiveAccountCode()).isEqualTo("2085");

        JournalResponse journal = journalService.getById(deposit.getJournalId());
        assertThat(journal.getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(journal.getJournalType()).isEqualTo(JournalType.DEPOSIT);
        assertThat(debitOn(journal.getLines(), "1010")).isEqualByComparingTo("1000");
        assertThat(creditOn(journal.getLines(), "2085")).isEqualByComparingTo("1000");
    }

    @Test
    void applyingToAnInvoiceReducesItsBalanceAndClearsReceivable() {
        DepositResponse deposit = service.create(received("1000"), true);
        BigDecimal before = openInvoice.getBalance();

        DepositResponse applied = service.applyToDocuments(deposit.getId(), apply(openInvoice.getId(), "60"));

        assertThat(applied.getStatus()).isEqualTo(DepositStatus.PARTIALLY_APPLIED);
        assertThat(applied.getAppliedAmount()).isEqualByComparingTo("60");
        assertThat(applied.getAvailableBalance()).isEqualByComparingTo("940");
        assertThat(invoiceRepository.findById(openInvoice.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(before.subtract(new BigDecimal("60")));

        SettlementSummaryResponse summary = settlementService.invoiceSummary(openInvoice.getId());
        assertThat(summary.getAppliedBySource().get("DEPOSIT")).isEqualByComparingTo("60");
        assertThat(summary.getAmountDue()).isEqualByComparingTo(before.subtract(new BigDecimal("60")));

        DepositAllocationResponse allocation = applied.getAllocations().get(0);
        JournalResponse journal = journalService.getById(allocation.getJournalId());
        assertThat(debitOn(journal.getLines(), "2085")).isEqualByComparingTo("60");
        assertThat(creditOn(journal.getLines(), "6220")).isEqualByComparingTo("60");
    }

    @Test
    void nothingCanTakeMoreThanTheAvailableBalance() {
        DepositResponse deposit = service.create(received("50"), true);

        assertThatThrownBy(() -> service.applyToDocuments(deposit.getId(), apply(openInvoice.getId(), "60")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("available deposit balance");
        assertThatThrownBy(() -> service.refund(deposit.getId(), refund("50.01")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("available deposit balance");

        ApplyDepositRequest twice = apply(openInvoice.getId(), "10");
        ApplyDepositLineRequest again = new ApplyDepositLineRequest();
        again.setInvoiceId(openInvoice.getId());
        again.setAmount(new BigDecimal("10"));
        twice.getLines().add(again);
        assertThatThrownBy(() -> service.applyToDocuments(deposit.getId(), twice))
                .isInstanceOf(BusinessException.class).hasMessageContaining("listed twice");
    }

    @Test
    void refundingTheWholeBalanceMarksItRefundedAndReversalRestoresIt() {
        DepositResponse deposit = service.create(received("300"), true);

        DepositResponse refunded = service.refund(deposit.getId(), refund("300"));
        assertThat(refunded.getStatus()).isEqualTo(DepositStatus.REFUNDED);
        assertThat(refunded.getAvailableBalance()).isEqualByComparingTo("0");

        Long allocationId = refunded.getAllocations().get(0).getId();
        DepositResponse restored = service.reverseAllocation(deposit.getId(), allocationId, "Refund bounced");
        assertThat(restored.getStatus()).isEqualTo(DepositStatus.ACTIVE);
        assertThat(restored.getAvailableBalance()).isEqualByComparingTo("300");
        assertThat(restored.getAllocations().get(0).getReversed()).isTrue();
        assertThat(restored.getAllocations().get(0).getReversalJournalId()).isNotNull();
    }

    @Test
    void reversingAnApplicationPutsTheAmountBackOnTheInvoice() {
        DepositResponse deposit = service.create(received("500"), true);
        BigDecimal before = openInvoice.getBalance();
        DepositResponse applied = service.applyToDocuments(deposit.getId(), apply(openInvoice.getId(), "40"));

        service.reverseAllocation(deposit.getId(), applied.getAllocations().get(0).getId(), "Wrong invoice");

        assertThat(invoiceRepository.findById(openInvoice.getId()).orElseThrow().getBalance()).isEqualByComparingTo(before);
        assertThat(settlementService.invoiceSummary(openInvoice.getId()).getAppliedBySource().get("DEPOSIT")).isEqualByComparingTo("0");
        assertThat(service.get(deposit.getId()).getStatus()).isEqualTo(DepositStatus.ACTIVE);
    }

    @Test
    void aDepositPaidIsAnAssetAndForfeitingItIsAnExpense() {
        CreateDepositRequest request = paid("2000");
        request.setExpectedReturnDate(LocalDate.now().plusMonths(18));
        DepositResponse deposit = service.create(request, true);
        assertThat(deposit.getClassification()).isEqualTo(DepositClassification.NON_CURRENT_ASSET);
        JournalResponse activation = journalService.getById(deposit.getJournalId());
        assertThat(debitOn(activation.getLines(), "1745")).isEqualByComparingTo("2000");
        assertThat(creditOn(activation.getLines(), "1010")).isEqualByComparingTo("2000");

        ForfeitDepositRequest forfeit = new ForfeitDepositRequest();
        forfeit.setAmount(new BigDecimal("2000"));
        forfeit.setNotes("Lease broken early");
        DepositResponse forfeited = service.forfeit(deposit.getId(), forfeit);

        assertThat(forfeited.getStatus()).isEqualTo(DepositStatus.FORFEITED);
        JournalResponse journal = journalService.getById(forfeited.getAllocations().get(0).getJournalId());
        assertThat(debitOn(journal.getLines(), "5095")).isEqualByComparingTo("2000");
        assertThat(creditOn(journal.getLines(), "1745")).isEqualByComparingTo("2000");
    }

    @Test
    void aDepositPaidCannotBeHeldInAnIncomeAccount() {
        CreateDepositRequest request = paid("100");
        request.setDepositAccountId(accountRepository.findByAccountId("4020").orElseThrow().getId());

        assertThatThrownBy(() -> service.create(request, false))
                .isInstanceOf(BusinessException.class).hasMessageContaining("must be held in an asset account");
    }

    @Test
    void aNonRefundableDepositCannotBeRefunded() {
        CreateDepositRequest request = received("100");
        request.setRefundable(false);
        DepositResponse deposit = service.create(request, true);

        assertThatThrownBy(() -> service.refund(deposit.getId(), refund("10")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("non-refundable");
    }

    @Test
    void transferMovesBalanceToANewDepositAndCanBeUndone() {
        DepositResponse source = service.create(received("800"), true);
        TransferDepositRequest transfer = new TransferDepositRequest();
        transfer.setAmount(new BigDecimal("300"));
        transfer.setCounterpartyType(DepositCounterpartyType.OTHER);
        transfer.setCounterpartyName("New Tenant Ltd");

        DepositResponse after = service.transfer(source.getId(), transfer);
        assertThat(after.getTransferredAmount()).isEqualByComparingTo("300");
        assertThat(after.getAvailableBalance()).isEqualByComparingTo("500");
        Long targetId = after.getAllocations().get(0).getTargetDepositId();
        DepositResponse target = service.get(targetId);
        assertThat(target.getStatus()).isEqualTo(DepositStatus.ACTIVE);
        assertThat(target.getCounterpartyName()).isEqualTo("New Tenant Ltd");
        assertThat(target.getTransferredFromId()).isEqualTo(source.getId());

        service.reverse(targetId, "Transferred by mistake");
        assertThat(service.get(targetId).getStatus()).isEqualTo(DepositStatus.REVERSED);
        assertThat(service.get(source.getId()).getAvailableBalance()).isEqualByComparingTo("800");
    }

    @Test
    void onlyDraftsCanBeEditedOrDeletedAndAnActiveDepositReverses() {
        DepositResponse draft = service.create(received("100"), false);
        assertThat(draft.getStatus()).isEqualTo(DepositStatus.DRAFT);
        service.update(draft.getId(), received("150"));
        assertThat(service.get(draft.getId()).getAmount()).isEqualByComparingTo("150");

        service.activate(draft.getId());
        assertThatThrownBy(() -> service.update(draft.getId(), received("200")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Only a draft");
        assertThatThrownBy(() -> service.delete(draft.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Reverse it instead");

        DepositResponse reversed = service.reverse(draft.getId(), "Recorded twice");
        assertThat(reversed.getStatus()).isEqualTo(DepositStatus.REVERSED);
        assertThat(journalService.getById(reversed.getJournalId()).getStatus()).isEqualTo(JournalStatus.REVERSED);
    }

    @Test
    void statementRunsTheBalanceThroughEveryMovement() {
        DepositResponse deposit = service.create(received("1000"), true);
        service.applyToDocuments(deposit.getId(), apply(openInvoice.getId(), "100"));
        service.refund(deposit.getId(), refund("200"));

        DepositStatementResponse statement = service.statement(deposit.getId(), null, null, null, null, null, null, null, null, null);
        assertThat(statement.getLines()).hasSize(3);
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("700");
        assertThat(statement.getTotalIncrease()).isEqualByComparingTo("1000");
        assertThat(statement.getTotalDecrease()).isEqualByComparingTo("300");
    }

    private CreateDepositRequest received(String amount) {
        CreateDepositRequest request = new CreateDepositRequest();
        request.setDirection(DepositDirection.DEPOSIT_RECEIVED);
        request.setDepositTypeId(type(DepositDirection.DEPOSIT_RECEIVED).getId());
        request.setCounterpartyType(DepositCounterpartyType.CUSTOMER);
        request.setCustomerId(openInvoice.getCustomer().getId());
        request.setAmount(new BigDecimal(amount));
        request.setCurrency(openInvoice.getCurrency());
        request.setDepositDate(LocalDate.now());
        request.setRefundable(true);
        return request;
    }

    private CreateDepositRequest paid(String amount) {
        CreateDepositRequest request = new CreateDepositRequest();
        request.setDirection(DepositDirection.DEPOSIT_PAID);
        request.setDepositTypeId(type(DepositDirection.DEPOSIT_PAID).getId());
        request.setCounterpartyType(DepositCounterpartyType.SUPPLIER);
        request.setSupplierId(supplierRepository.findAll().get(0).getId());
        request.setAmount(new BigDecimal(amount));
        request.setCurrency(openInvoice.getCurrency());
        request.setDepositDate(LocalDate.now());
        return request;
    }

    private DepositType type(DepositDirection direction) {
        return typeRepository.findByActiveTrueAndDeletedFalse().stream()
                .filter(t -> t.getDirection() == direction).findFirst().orElseThrow();
    }

    private static ApplyDepositRequest apply(Long invoiceId, String amount) {
        ApplyDepositLineRequest line = new ApplyDepositLineRequest();
        line.setInvoiceId(invoiceId);
        line.setAmount(new BigDecimal(amount));
        ApplyDepositRequest request = new ApplyDepositRequest();
        request.setLines(new java.util.ArrayList<>(List.of(line)));
        return request;
    }

    private static RefundDepositRequest refund(String amount) {
        RefundDepositRequest request = new RefundDepositRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private static BigDecimal debitOn(List<JournalLineResponse> lines, String code) {
        return lines.stream().filter(l -> code.equals(l.getAccountCode()))
                .map(JournalLineResponse::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal creditOn(List<JournalLineResponse> lines, String code) {
        return lines.stream().filter(l -> code.equals(l.getAccountCode()))
                .map(JournalLineResponse::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
