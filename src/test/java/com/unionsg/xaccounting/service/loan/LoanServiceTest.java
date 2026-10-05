package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.loan.LoanActionRequest;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanPaymentStatus;
import com.unionsg.xaccounting.enums.loan.LoanPaymentType;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.loan.LoanInterestAccrualRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepaymentRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanServiceTest {

    @Mock private LoanRepository repository;
    @Mock private LoanRepaymentRepository repaymentRepository;
    @Mock private LoanInterestAccrualRepository accrualRepository;
    @Mock private LoanJournalService journalService;
    @Mock private LoanAuditService auditService;

    @InjectMocks
    private LoanService service;

    private Loan loan;
    private final List<LoanRepayment> saved = new ArrayList<>();

    /** A 12,000 reducing-balance loan at 12% a year, monthly, started a year ago so every installment is due. */
    @BeforeEach
    void setUp() {
        loan = new Loan();
        loan.setId(1L);
        loan.setLoanNumber("LN-1");
        loan.setLoanType(new LoanType());
        loan.setDirection(LoanDirection.LENT_LOAN);
        loan.setCounterpartyName("Acme");
        loan.setCurrency("USD");
        loan.setPrincipalAmount(new BigDecimal("12000.00"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setInterestMethod(LoanInterestMethod.REDUCING_BALANCE);
        loan.setPaymentFrequency(LoanFrequency.MONTHLY);
        loan.setNumberOfInstallments(12);
        loan.setStartDate(LocalDate.now().minusMonths(13));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingPrincipal(new BigDecimal("12000.00"));
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(new LoanScheduleEngine.Terms(
                loan.getPrincipalAmount(), loan.getInterestRate(), LoanFrequency.MONTHLY, 12, 0,
                LoanInterestMethod.REDUCING_BALANCE, loan.getStartDate(), BigDecimal.ZERO, null));
        lines.forEach(l -> l.setLoan(loan));
        loan.getSchedule().addAll(lines);

        when(repository.findById(1L)).thenReturn(Optional.of(loan));
        when(repository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
            LoanRepayment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId((long) saved.size() + 10);
                saved.add(p);
            }
            return p;
        });
        when(repaymentRepository.findByLoanIdOrderByRepaymentDateAscIdAsc(1L)).thenAnswer(inv -> List.copyOf(saved));
        when(repaymentRepository.findById(anyLong())).thenAnswer(inv ->
                saved.stream().filter(p -> p.getId().equals(inv.getArgument(0))).findFirst());
        when(accrualRepository.findByLoanIdOrderByAccrualDateAscIdAsc(1L)).thenReturn(List.of());
        when(journalService.postRepaymentJournal(any(), any())).thenReturn(journal("JV-P"));
        when(journalService.reverse(any(), any())).thenReturn(journal("JV-R"));
    }

    private static JournalEntry journal(String number) {
        JournalEntry j = new JournalEntry();
        j.setId(99L);
        j.setJournalNumber(number);
        return j;
    }

    private RecordLoanRepaymentRequest pay(String amount) {
        RecordLoanRepaymentRequest r = new RecordLoanRepaymentRequest();
        r.setAmount(new BigDecimal(amount));
        r.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        r.setRepaymentDate(LocalDate.now());
        return r;
    }

    @Test
    void aPartPaymentIsAllocatedToTheOldestInstallmentAndPostsAJournal() {
        LoanResponse r = service.recordRepayment(1L, pay("500"));

        LoanRepayment p = saved.get(0);
        assertThat(p.getInterestAmount()).isEqualByComparingTo("120.00");
        assertThat(p.getPrincipalAmount()).isEqualByComparingTo("380.00");
        assertThat(p.getPaymentType()).isEqualTo(LoanPaymentType.PARTIAL);
        assertThat(r.getOutstandingPrincipal()).isEqualByComparingTo("11620.00");
        assertThat(r.getStatus()).isEqualTo(LoanStatus.PARTIALLY_PAID);
        verify(journalService).postRepaymentJournal(loan, p);
    }

    @Test
    void payingEverythingMakesTheLoanFullyPaid() {
        BigDecimal all = LoanScheduleEngine.totalOwed(loan.getSchedule());

        LoanResponse r = service.recordRepayment(1L, pay(all.toPlainString()));

        assertThat(r.getStatus()).isEqualTo(LoanStatus.FULLY_PAID);
        assertThat(r.getOutstandingPrincipal()).isEqualByComparingTo("0");
        assertThat(saved.get(0).getPaymentType()).isEqualTo(LoanPaymentType.SCHEDULED);
    }

    @Test
    void anOverpaymentIsRefusedUnlessTheLoanAllowsIt() {
        BigDecimal tooMuch = LoanScheduleEngine.totalOwed(loan.getSchedule()).add(BigDecimal.TEN);

        assertThatThrownBy(() -> service.recordRepayment(1L, pay(tooMuch.toPlainString())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Allow overpayment");
        verify(journalService, never()).postRepaymentJournal(any(), any());

        loan.setAllowOverpayment(true);
        LoanResponse r = service.recordRepayment(1L, pay(tooMuch.toPlainString()));
        assertThat(saved.get(0).getOverpaymentAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.get(0).getPaymentType()).isEqualTo(LoanPaymentType.OVERPAYMENT);
        assertThat(r.getOverpaymentBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void aManualSplitCannotPayMoreInterestThanTheScheduleHolds() {
        RecordLoanRepaymentRequest r = pay("0");
        r.setAmount(null);
        r.setInterestAmount(new BigDecimal("999999"));

        assertThatThrownBy(() -> service.recordRepayment(1L, r))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Interest paid");
    }

    @Test
    void reversingAPaymentRestoresTheBalanceAndKeepsLaterOnes() {
        service.recordRepayment(1L, pay("1120"));
        service.recordRepayment(1L, pay("1110"));
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("10000.00");

        LoanActionRequest reason = new LoanActionRequest();
        reason.setReason("Bounced cheque");
        LoanResponse r = service.reversePayment(1L, saved.get(0).getId(), reason);

        assertThat(saved.get(0).getStatus()).isEqualTo(LoanPaymentStatus.REVERSED);
        assertThat(saved.get(0).getReversalJournal().getJournalNumber()).isEqualTo("JV-R");
        // The second payment keeps its split (interest 110, principal 1000) and now lands on installment 1.
        assertThat(r.getOutstandingPrincipal()).isEqualByComparingTo("11000.00");
        assertThat(r.getSchedule().get(0).getAmountOutstanding()).isEqualByComparingTo("10.00");
        assertThat(r.getStatus()).isEqualTo(LoanStatus.PARTIALLY_PAID);
    }

    @Test
    void aReasonIsRequiredToReverseAPayment() {
        service.recordRepayment(1L, pay("500"));
        assertThatThrownBy(() -> service.reversePayment(1L, saved.get(0).getId(), new LoanActionRequest()))
                .hasMessageContaining("reason");
    }

    @Test
    void aBorrowedLoanWithABalanceCannotBeClosed() {
        loan.setDirection(LoanDirection.BORROWED_LOAN);
        LoanActionRequest req = new LoanActionRequest();
        req.setWriteOff(true);
        req.setReason("x");

        assertThatThrownBy(() -> service.close(1L, req)).hasMessageContaining("fully paid");
    }

    @Test
    void aLentLoanClosesWithAWriteOffOfWhatIsStillOwed() {
        when(journalService.postWriteOffJournal(any(), any(), any(), any())).thenReturn(journal("JV-WO"));
        LoanActionRequest req = new LoanActionRequest();
        req.setWriteOff(true);
        req.setReason("Borrower insolvent");

        LoanResponse r = service.close(1L, req);

        assertThat(r.getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(r.getWrittenOffAmount()).isEqualByComparingTo("12000.00");
        assertThat(r.getWriteOffJournalNumber()).isEqualTo("JV-WO");
    }

    @Test
    void paymentsAreRefusedOnAClosedOrDraftLoan() {
        loan.setStatus(LoanStatus.DRAFT);
        assertThatThrownBy(() -> service.recordRepayment(1L, pay("100"))).isInstanceOf(BusinessException.class);
        loan.setStatus(LoanStatus.CLOSED);
        assertThatThrownBy(() -> service.recordRepayment(1L, pay("100"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void aFuturePaymentDateIsRefused() {
        RecordLoanRepaymentRequest r = pay("100");
        r.setRepaymentDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.recordRepayment(1L, r)).hasMessageContaining("future");
    }

    @Test
    void reversingTheLoanReversesPaymentsAndTheDisbursement() {
        loan.setJournal(journal("JV-D"));
        service.recordRepayment(1L, pay("500"));
        when(repaymentRepository.findByLoanIdOrderByRepaymentDateDescIdDesc(1L)).thenReturn(List.copyOf(saved));
        LoanActionRequest reason = new LoanActionRequest();
        reason.setReason("Entered against the wrong counterparty");

        LoanResponse r = service.reverseLoan(1L, reason);

        assertThat(r.getStatus()).isEqualTo(LoanStatus.REVERSED);
        assertThat(saved.get(0).getStatus()).isEqualTo(LoanPaymentStatus.REVERSED);
        verify(journalService).reverse(loan.getJournal(), "Loan LN-1 reversed: Entered against the wrong counterparty");
        assertThat(r.getOutstandingPrincipal()).isEqualByComparingTo("0");
    }

    @Test
    void classifyComparesThePaymentWithWhatWasDue() {
        var split = new LoanScheduleEngine.Split(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO);
        assertThat(LoanService.classify(split, BigDecimal.ZERO, BigDecimal.TEN)).isEqualTo(LoanPaymentType.EARLY);
        assertThat(LoanService.classify(split, new BigDecimal("100"), BigDecimal.TEN)).isEqualTo(LoanPaymentType.SCHEDULED);
        assertThat(LoanService.classify(split, new BigDecimal("150"), BigDecimal.TEN)).isEqualTo(LoanPaymentType.PARTIAL);
        assertThat(LoanService.classify(split, new BigDecimal("50"), BigDecimal.TEN)).isEqualTo(LoanPaymentType.EARLY);
    }
}
