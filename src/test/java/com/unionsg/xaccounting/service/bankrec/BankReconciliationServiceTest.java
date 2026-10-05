package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.CompleteReconciliationRequest;
import com.unionsg.xaccounting.dto.bankrec.CreateAdjustmentRequest;
import com.unionsg.xaccounting.dto.bankrec.ManualMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAdjustment;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatch;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatchItem;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.bankrec.AdjustmentStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchItemSide;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.bankrec.BankLedgerRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAdjustmentRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAuditLogRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchItemRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationMatchRepository;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementTransactionRepository;
import com.unionsg.xaccounting.repository.payment.PaymentRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.service.PermissionService;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankReconciliationServiceTest {

    private static final String BANK_GL = "1010";
    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final LocalDate END = LocalDate.of(2026, 3, 31);

    @Mock private BankReconciliationRepository repository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BankStatementTransactionRepository statementRepository;
    @Mock private BankStatementImportRepository importRepository;
    @Mock private BankLedgerRepository ledgerRepository;
    @Mock private BankReconciliationMatchRepository matchRepository;
    @Mock private BankReconciliationMatchItemRepository matchItemRepository;
    @Mock private BankReconciliationAdjustmentRepository adjustmentRepository;
    @Mock private BankReconciliationAuditLogRepository auditLogRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private AccountingMappingService accountingMappingService;
    @Mock private PermissionService permissionService;
    @Mock private BankMatchingRuleService ruleService;
    @Mock private BankReconciliationJournalService journalService;
    @Mock private BankRecAuditService auditService;

    @InjectMocks
    private BankReconciliationService service;

    private BankAccount bank;
    private BankReconciliation rec;
    private final List<BankStatementTransaction> openStatement = new ArrayList<>();
    private final List<JournalLine> openBook = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bank = new BankAccount();
        bank.setId(1L);
        bank.setAccountName("Operating account");
        bank.setGlAccountCode(BANK_GL);

        rec = new BankReconciliation();
        rec.setId(5L);
        rec.setReconciliationNumber("BREC-0001");
        rec.setBankAccount(bank);
        rec.setStatementDate(END);
        rec.setPeriodStart(START);
        rec.setPeriodEnd(END);
        rec.setOpeningBankBalance(new BigDecimal("1000.00"));
        rec.setClosingBankBalance(new BigDecimal("1000.00"));
        rec.setStatus(ReconciliationStatus.IN_PROGRESS);

        when(repository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(rec));
        when(repository.save(any(BankReconciliation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any(BankReconciliationMatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adjustmentRepository.save(any(BankReconciliationAdjustment.class))).thenAnswer(inv -> {
            BankReconciliationAdjustment a = inv.getArgument(0);
            if (a.getId() == null) a.setId(77L);
            return a;
        });
        when(statementRepository.findOpenAsOf(eq(1L), any(), anyCollection())).thenReturn(openStatement);
        when(ledgerRepository.findOpenLines(eq(BANK_GL), anyCollection(), any(), anyCollection())).thenReturn(openBook);
        when(ledgerRepository.balanceAsOf(eq(BANK_GL), anyCollection(), any())).thenReturn(new BigDecimal("1000.00"));
        when(statementRepository.sumMovement(anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(matchItemRepository.sumMatchedByStatementAsOf(anyCollection(), anyCollection(), any())).thenReturn(List.of());
        when(matchItemRepository.sumMatchedByJournalLineAsOf(anyCollection(), anyCollection(), any())).thenReturn(List.of());
        when(matchItemRepository.findActiveByReconciliation(anyLong(), anyCollection())).thenReturn(List.of());
        when(adjustmentRepository.findByReconciliationIdAndStatus(anyLong(), eq(AdjustmentStatus.POSTED))).thenReturn(List.of());
    }

    private BankStatementTransaction statementLine(long id, String amount, LocalDate date) {
        BankStatementTransaction t = new BankStatementTransaction();
        t.setId(id);
        t.setBankAccount(bank);
        BankStatementImport batch = new BankStatementImport();
        batch.setId(1L);
        t.setStatementImport(batch);
        t.setTransactionDate(date);
        t.setAmount(new BigDecimal(amount));
        t.setMatchedAmount(BigDecimal.ZERO);
        when(statementRepository.findById(id)).thenReturn(Optional.of(t));
        return t;
    }

    private JournalLine bookLine(long id, String debit, String credit, LocalDate date) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(BANK_GL);
        JournalEntry je = new JournalEntry();
        je.setId(100 + id);
        je.setJournalNumber("JRN-" + id);
        je.setJournalDate(date);
        je.setStatus(JournalStatus.POSTED);
        JournalLine line = new JournalLine();
        line.setId(id);
        line.setAccount(account);
        line.setJournalEntry(je);
        line.setDebitAmount(new BigDecimal(debit));
        line.setCreditAmount(new BigDecimal(credit));
        je.getLines().add(line);
        return line;
    }

    // ---- difference calculation -----------------------------------------------------------

    @Test
    void differenceIsStatementMinusBookAdjustedForOutstandingItems() {
        rec.setClosingBankBalance(new BigDecimal("1300.00"));
        openBook.add(bookLine(1, "200.00", "0", END));      // deposit not yet on the statement
        openBook.add(bookLine(2, "0", "500.00", END));      // cheque not yet presented
        when(ledgerRepository.balanceAsOf(eq(BANK_GL), anyCollection(), any())).thenReturn(new BigDecimal("1000.00"));

        service.recalculate(rec);

        assertThat(rec.getOutstandingDeposits()).isEqualByComparingTo("200.00");
        assertThat(rec.getOutstandingWithdrawals()).isEqualByComparingTo("500.00");
        // expected statement = 1000 - 200 + 500 = 1300
        assertThat(rec.getDifference()).isEqualByComparingTo("0.00");
    }

    @Test
    void partlyMatchedBookLinesOnlyCountTheirRemainder() {
        rec.setClosingBankBalance(new BigDecimal("1000.00"));
        openBook.add(bookLine(1, "300.00", "0", END));
        when(matchItemRepository.sumMatchedByJournalLineAsOf(anyCollection(), anyCollection(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, new BigDecimal("100.00")}));

        service.recalculate(rec);

        assertThat(rec.getOutstandingDeposits()).isEqualByComparingTo("200.00");
        assertThat(rec.getDifference()).isEqualByComparingTo("200.00");
    }

    // ---- matching ---------------------------------------------------------------------------

    @Test
    void manualPartialMatchLeavesTheStatementRemainderOpen() {
        BankStatementTransaction deposit = statementLine(11, "1000.00", END.minusDays(2));
        JournalLine receipt = bookLine(21, "600.00", "0", END.minusDays(3));
        when(ledgerRepository.findWithJournal(anyCollection())).thenReturn(List.of(receipt));

        ManualMatchRequest request = new ManualMatchRequest();
        request.setStatementTransactionIds(List.of(11L));
        request.setJournalLineIds(List.of(21L));
        MatchResponse match = service.manualMatch(5L, request);

        assertThat(match.getMatchType()).isEqualTo(MatchType.MANUAL);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(match.getMatchedAmount()).isEqualByComparingTo("600.00");
        assertThat(deposit.getMatchedAmount()).isEqualByComparingTo("600.00");
        assertThat(deposit.getStatus()).isEqualTo(StatementTransactionStatus.PARTIALLY_MATCHED);
        assertThat(deposit.getRemainingAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void unmatchingGivesTheAmountBack() {
        BankStatementTransaction deposit = statementLine(11, "250.00", END);
        deposit.setMatchedAmount(new BigDecimal("250.00"));
        deposit.setStatus(StatementTransactionStatus.MATCHED);
        BankReconciliationMatch m = new BankReconciliationMatch();
        m.setId(9L);
        m.setReconciliation(rec);
        m.setMatchType(MatchType.MANUAL);
        m.setStatus(MatchStatus.CONFIRMED);
        m.setMatchedAmount(new BigDecimal("250.00"));
        BankReconciliationMatchItem item = new BankReconciliationMatchItem();
        item.setSide(MatchItemSide.STATEMENT);
        item.setStatementTransaction(deposit);
        item.setAmount(new BigDecimal("250.00"));
        m.addItem(item);
        when(matchRepository.findAllById(List.of(9L))).thenReturn(List.of(m));

        com.unionsg.xaccounting.dto.bankrec.MatchActionRequest request = new com.unionsg.xaccounting.dto.bankrec.MatchActionRequest();
        request.setMatchIds(List.of(9L));
        request.setReason("Wrong customer");
        service.unmatch(5L, request);

        assertThat(m.getStatus()).isEqualTo(MatchStatus.UNMATCHED);
        assertThat(m.getUnmatchReason()).isEqualTo("Wrong customer");
        assertThat(deposit.getMatchedAmount()).isEqualByComparingTo("0");
        assertThat(deposit.getStatus()).isEqualTo(StatementTransactionStatus.UNMATCHED);
    }

    // ---- adjustments ------------------------------------------------------------------------

    @Test
    void bankChargeAdjustmentPostsAJournalAndClearsTheStatementLine() {
        BankStatementTransaction charge = statementLine(12, "-15.00", END.minusDays(1));
        AccountEntity expense = new AccountEntity();
        expense.setId(50L);
        expense.setAccountId("5100");
        expense.setAccountName("Bank Charges");
        when(accountRepository.findById(50L)).thenReturn(Optional.of(expense));

        JournalLine bankSide = bookLine(31, "0", "15.00", END.minusDays(1));
        JournalEntry journal = bankSide.getJournalEntry();
        when(journalService.postAdjustment(eq(rec), any())).thenReturn(journal);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setAdjustmentType(ReconciliationAdjustmentType.BANK_CHARGE);
        request.setStatementTransactionId(12L);
        request.setOffsetAccountId(50L);
        var response = service.createAdjustment(5L, request);

        assertThat(response.getAmount()).isEqualByComparingTo("15.00");
        assertThat(response.getSignedAmount()).isEqualByComparingTo("-15.00");
        assertThat(response.getTransactionDate()).isEqualTo(END.minusDays(1));
        assertThat(response.getJournalNumber()).isEqualTo("JRN-31");
        assertThat(charge.getStatus()).isEqualTo(StatementTransactionStatus.MATCHED);

        ArgumentCaptor<BankReconciliationMatch> captor = ArgumentCaptor.forClass(BankReconciliationMatch.class);
        verify(matchRepository).save(captor.capture());
        BankReconciliationMatch m = captor.getValue();
        assertThat(m.getMatchType()).isEqualTo(MatchType.ADJUSTMENT);
        assertThat(m.getItems()).extracting(BankReconciliationMatchItem::getSide)
                .containsExactlyInAnyOrder(MatchItemSide.STATEMENT, MatchItemSide.BOOK);
    }

    @Test
    void adjustmentDirectionMustFitTheStatementLine() {
        statementLine(13, "40.00", END);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setAdjustmentType(ReconciliationAdjustmentType.BANK_CHARGE);
        request.setStatementTransactionId(13L);

        assertThatThrownBy(() -> service.createAdjustment(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deposit");
        verify(journalService, never()).postAdjustment(any(), any());
    }

    @Test
    void adjustmentsCannotBeDatedAfterThePeriod() {
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setAdjustmentType(ReconciliationAdjustmentType.BANK_INTEREST);
        request.setAmount(new BigDecimal("3.00"));
        request.setTransactionDate(END.plusDays(1));

        assertThatThrownBy(() -> service.createAdjustment(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("period end");
    }

    // ---- completion -------------------------------------------------------------------------

    @Test
    void completesWhenTheDifferenceIsWithinTolerance() {
        ReconciliationResponse result = service.complete(5L, new CompleteReconciliationRequest());

        assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(rec.getDifferenceOverridden()).isFalse();
        assertThat(rec.getCompletedAt()).isNotNull();
    }

    @Test
    void refusesToCompleteWithAnUnresolvedDifferenceWithoutOverridePermission() {
        rec.setClosingBankBalance(new BigDecimal("990.00"));
        when(permissionService.currentUserHasPermission(BankReconciliationService.OVERRIDE_PERMISSION)).thenReturn(false);

        assertThatThrownBy(() -> service.complete(5L, new CompleteReconciliationRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("out by -10.00");
        assertThat(rec.getStatus()).isEqualTo(ReconciliationStatus.IN_PROGRESS);
    }

    @Test
    void overrideNeedsAReasonAndIsRecorded() {
        rec.setClosingBankBalance(new BigDecimal("990.00"));
        when(permissionService.currentUserHasPermission(BankReconciliationService.OVERRIDE_PERMISSION)).thenReturn(true);

        assertThatThrownBy(() -> service.complete(5L, new CompleteReconciliationRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reason");

        CompleteReconciliationRequest withReason = new CompleteReconciliationRequest();
        withReason.setOverrideReason("Bank error, query raised");
        ReconciliationResponse result = service.complete(5L, withReason);

        assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(result.getDifferenceOverridden()).isTrue();
        assertThat(result.getOverrideReason()).isEqualTo("Bank error, query raised");
    }

    @Test
    void suggestedMatchesMustBeResolvedBeforeCompleting() {
        when(matchRepository.countByReconciliationIdAndStatus(5L, MatchStatus.PROPOSED)).thenReturn(2L);

        assertThatThrownBy(() -> service.complete(5L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 suggested matches");
    }

    @Test
    void aCompletedReconciliationIsLocked() {
        rec.setStatus(ReconciliationStatus.RECONCILED);

        assertThatThrownBy(() -> service.manualMatch(5L, new ManualMatchRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reopen it");
    }

    // ---- reopening --------------------------------------------------------------------------

    @Test
    void reopensTheLatestCompletedReconciliation() {
        rec.setStatus(ReconciliationStatus.RECONCILED);
        when(repository.findByAccountAndStatusLatestFirst(1L, ReconciliationStatus.RECONCILED)).thenReturn(List.of(rec));
        when(repository.findByBankAccountIdAndStatusInAndDeletedFalse(eq(1L), anyCollection())).thenReturn(List.of());

        ReconciliationResponse result = service.reopen(5L, "Late bank correction");

        assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.REOPENED);
        assertThat(result.getReopenReason()).isEqualTo("Late bank correction");
        assertThat(result.getReopenCount()).isEqualTo(1);
    }

    @Test
    void onlyTheLatestCompletedReconciliationCanBeReopened() {
        rec.setStatus(ReconciliationStatus.RECONCILED);
        BankReconciliation later = new BankReconciliation();
        later.setId(6L);
        later.setReconciliationNumber("BREC-0002");
        when(repository.findByAccountAndStatusLatestFirst(1L, ReconciliationStatus.RECONCILED)).thenReturn(List.of(later, rec));

        assertThatThrownBy(() -> service.reopen(5L, "Fix"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BREC-0002");
    }

    @Test
    void reopeningNeedsAReason() {
        rec.setStatus(ReconciliationStatus.RECONCILED);

        assertThatThrownBy(() -> service.reopen(5L, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void cancellingIsRefusedWhilePostedAdjustmentsRemain() {
        when(adjustmentRepository.findByReconciliationIdAndStatus(5L, AdjustmentStatus.POSTED))
                .thenReturn(List.of(new BankReconciliationAdjustment()));

        assertThatThrownBy(() -> service.cancel(5L, "Wrong statement"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reverse");
    }
}
