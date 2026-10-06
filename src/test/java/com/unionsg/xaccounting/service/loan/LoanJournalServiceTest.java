package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.journal.JournalService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanJournalServiceTest {

    @Mock private JournalService journalService;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountingMappingService accountingMappingService;
    @Mock private PeriodLockGuard periodLockGuard;

    @InjectMocks
    private LoanJournalService service;

    private final ArgumentCaptor<CreateJournalRequest> captor = ArgumentCaptor.forClass(CreateJournalRequest.class);

    @BeforeEach
    void setUp() {
        for (MappingKey key : MappingKey.values()) {
            when(accountingMappingService.resolve(key)).thenReturn(key.getDefaultAccountCode());
        }
        when(accountRepository.findByAccountId(anyString())).thenAnswer(inv -> {
            AccountEntity a = new AccountEntity();
            a.setAccountId(inv.getArgument(0));
            return Optional.of(a);
        });
        when(journalService.create(captor.capture())).thenReturn(JournalResponse.builder().id(1L).build());
        when(journalService.post(1L)).thenReturn(JournalResponse.builder().id(1L).build());
        when(journalEntryRepository.findById(anyLong())).thenReturn(Optional.of(new JournalEntry()));
    }

    private static Loan loan(LoanDirection direction) {
        Loan loan = new Loan();
        loan.setId(7L);
        loan.setLoanNumber("LN-0007");
        loan.setDirection(direction);
        loan.setCounterpartyName("First Bank");
        loan.setCurrency("USD");
        loan.setPrincipalAmount(new BigDecimal("10000.00"));
        loan.setTotalFees(BigDecimal.ZERO);
        return loan;
    }

    /** account code -> {debit, credit} */
    private Map<String, BigDecimal[]> lines() {
        return captor.getValue().getLines().stream().collect(Collectors.toMap(
                l -> String.valueOf(l.getAccountId()),
                l -> new BigDecimal[]{l.getDebitAmount(), l.getCreditAmount()},
                (a, b) -> new BigDecimal[]{a[0].add(b[0]), a[1].add(b[1])}));
    }

    private void assertBalanced() {
        List<CreateJournalLineRequest> l = captor.getValue().getLines();
        BigDecimal dr = l.stream().map(CreateJournalLineRequest::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cr = l.stream().map(CreateJournalLineRequest::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(dr).isEqualByComparingTo(cr);
    }

    @Test
    void borrowedDisbursementDebitsBankAndCreditsLoanLiability() {
        service.postDisbursementJournal(loan(LoanDirection.BORROWED_LOAN), LocalDate.of(2026, 1, 1));

        var l = lines();
        assertThat(l.get("1010")[0]).isEqualByComparingTo("10000.00");
        assertThat(l.get("2160")[1]).isEqualByComparingTo("10000.00");
        assertBalanced();
        verify(periodLockGuard).assertPostable(LocalDate.of(2026, 1, 1));
    }

    @Test
    void lentDisbursementDebitsReceivableAndCreditsBank() {
        service.postDisbursementJournal(loan(LoanDirection.LENT_LOAN), LocalDate.of(2026, 1, 1));

        var l = lines();
        assertThat(l.get("1780")[0]).isEqualByComparingTo("10000.00");
        assertThat(l.get("1010")[1]).isEqualByComparingTo("10000.00");
        assertBalanced();
    }

    @Test
    void capitalizedFeesAddToTheBalanceButNotTheCash() {
        Loan loan = loan(LoanDirection.BORROWED_LOAN);
        loan.setTotalFees(new BigDecimal("200.00"));
        loan.setFeeTreatment(LoanFeeTreatment.CAPITALIZED);

        service.postDisbursementJournal(loan, LocalDate.of(2026, 1, 1));

        var l = lines();
        assertThat(l.get("1010")[0]).isEqualByComparingTo("10000.00");
        assertThat(l.get("5090")[0]).isEqualByComparingTo("200.00");
        assertThat(l.get("2160")[1]).isEqualByComparingTo("10200.00");
        assertBalanced();
    }

    @Test
    void borrowedRepaymentSeparatesPrincipalInterestAndFees() {
        LoanRepayment p = payment("800.00", "100.00", "20.00", "0");

        service.postRepaymentJournal(loan(LoanDirection.BORROWED_LOAN), p);

        var l = lines();
        assertThat(l.get("2160")[0]).isEqualByComparingTo("800.00");
        assertThat(l.get("5080")[0]).isEqualByComparingTo("100.00");
        assertThat(l.get("5090")[0]).isEqualByComparingTo("20.00");
        assertThat(l.get("1010")[1]).isEqualByComparingTo("920.00");
        assertBalanced();
    }

    @Test
    void lentRepaymentCreditsReceivableAndInterestIncome() {
        LoanRepayment p = payment("800.00", "100.00", "0", "0");

        service.postRepaymentJournal(loan(LoanDirection.LENT_LOAN), p);

        var l = lines();
        assertThat(l.get("1010")[0]).isEqualByComparingTo("900.00");
        assertThat(l.get("1780")[1]).isEqualByComparingTo("800.00");
        assertThat(l.get("4040")[1]).isEqualByComparingTo("100.00");
        assertBalanced();
    }

    @Test
    void accruedInterestIsSettledAgainstInterestPayable() {
        LoanRepayment p = payment("0", "100.00", "0", "0");
        p.setAccruedInterestApplied(new BigDecimal("60.00"));

        service.postRepaymentJournal(loan(LoanDirection.BORROWED_LOAN), p);

        var l = lines();
        assertThat(l.get("2170")[0]).isEqualByComparingTo("60.00");
        assertThat(l.get("5080")[0]).isEqualByComparingTo("40.00");
        assertBalanced();
    }

    @Test
    void overpaymentGoesToThePrincipalAccount() {
        LoanRepayment p = payment("500.00", "0", "0", "50.00");

        service.postRepaymentJournal(loan(LoanDirection.LENT_LOAN), p);

        assertThat(lines().get("1780")[1]).isEqualByComparingTo("550.00");
        assertBalanced();
    }

    @Test
    void writeOffIsOnlyForLentLoans() {
        assertThatThrownBy(() -> service.postWriteOffJournal(loan(LoanDirection.BORROWED_LOAN),
                BigDecimal.TEN, BigDecimal.ZERO, LocalDate.now()))
                .isInstanceOf(BusinessException.class);

        service.postWriteOffJournal(loan(LoanDirection.LENT_LOAN), new BigDecimal("300.00"), new BigDecimal("20.00"), LocalDate.now());
        var l = lines();
        assertThat(l.get("5085")[0]).isEqualByComparingTo("320.00");
        assertThat(l.get("1780")[1]).isEqualByComparingTo("300.00");
        assertThat(l.get("1790")[1]).isEqualByComparingTo("20.00");
    }

    @Test
    void aLockedPeriodStopsThePostingBeforeAnyJournalIsCreated() {
        doThrow(new BusinessException("locked")).when(periodLockGuard).assertPostable(any());

        assertThatThrownBy(() -> service.postDisbursementJournal(loan(LoanDirection.BORROWED_LOAN), LocalDate.of(2025, 1, 1)))
                .hasMessage("locked");
        verify(journalService, never()).create(any());
    }

    private static LoanRepayment payment(String principal, String interest, String fees, String over) {
        LoanRepayment p = new LoanRepayment();
        p.setId(3L);
        p.setRepaymentDate(LocalDate.of(2026, 2, 1));
        p.setPrincipalAmount(new BigDecimal(principal));
        p.setInterestAmount(new BigDecimal(interest));
        p.setFeesAmount(new BigDecimal(fees));
        p.setOverpaymentAmount(new BigDecimal(over));
        p.setTotalAmount(p.getPrincipalAmount().add(p.getInterestAmount()).add(p.getFeesAmount()).add(p.getOverpaymentAmount()));
        return p;
    }
}
