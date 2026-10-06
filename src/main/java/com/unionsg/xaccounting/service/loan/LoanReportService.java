package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.MapperLayer.LoanMapper;
import com.unionsg.xaccounting.dto.loan.LoanDashboardResponse;
import com.unionsg.xaccounting.dto.loan.LoanStatementResponse;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanInterestAccrual;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.repository.loan.LoanInterestAccrualRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepaymentRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Loan dashboard and loan statement. Read-only. */
@Service
@RequiredArgsConstructor
public class LoanReportService {

    /** Loans that have been disbursed and not undone. */
    private static final Set<LoanStatus> DISBURSED = EnumSet.of(
            LoanStatus.ACTIVE, LoanStatus.PARTIALLY_PAID, LoanStatus.FULLY_PAID, LoanStatus.DEFAULTED, LoanStatus.CLOSED);

    private static final int UPCOMING_DAYS = 30;

    private final LoanRepository repository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanInterestAccrualRepository accrualRepository;
    private final LoanService loanService;

    /**
     * Totals per currency. Interest income/expense is what was recognised in the period: interest
     * accrued plus interest paid that had not been accrued first. Defaults to the year to date.
     */
    @Transactional(readOnly = true)
    public LoanDashboardResponse dashboard(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = from != null ? from : today.withDayOfYear(1);
        LocalDate toDate = to != null ? to : today;

        List<Loan> loans = repository.findByDeletedFalseAndStatusIn(EnumSet.allOf(LoanStatus.class));
        LoanDashboardResponse r = new LoanDashboardResponse();
        r.setAsOf(today);
        r.setFromDate(fromDate);
        r.setToDate(toDate);
        r.setActiveLoans(loans.stream().filter(l -> LoanMapper.isLive(l)).count());
        r.setDraftLoans(loans.stream().filter(l -> l.getStatus() == LoanStatus.DRAFT || l.getStatus() == LoanStatus.APPROVED).count());
        r.setDefaultedLoans(loans.stream().filter(l -> l.getStatus() == LoanStatus.DEFAULTED).count());

        List<Loan> disbursed = loans.stream().filter(l -> DISBURSED.contains(l.getStatus())).toList();
        List<Long> ids = disbursed.stream().map(Loan::getId).toList();
        Map<Long, List<LoanRepayment>> payments = ids.isEmpty() ? Map.of()
                : repaymentRepository.findByLoanIdIn(ids).stream().filter(LoanRepayment::isPosted)
                .collect(Collectors.groupingBy(p -> p.getLoan().getId()));
        Map<Long, List<LoanInterestAccrual>> accruals = ids.isEmpty() ? Map.of()
                : accrualRepository.findByLoanIdIn(ids).stream().filter(a -> !Boolean.TRUE.equals(a.getReversed()))
                .collect(Collectors.groupingBy(a -> a.getLoan().getId()));

        Map<String, LoanDashboardResponse.CurrencyTotals> byCurrency = new LinkedHashMap<>();
        for (Loan loan : disbursed) {
            var t = byCurrency.computeIfAbsent(loan.getCurrency() != null ? loan.getCurrency() : "", c -> {
                var n = new LoanDashboardResponse.CurrencyTotals();
                n.setCurrency(c);
                return n;
            });
            boolean borrowed = loan.getDirection() == LoanDirection.BORROWED_LOAN;
            boolean live = LoanMapper.isLive(loan);
            BigDecimal financed = LoanService.financedAmount(loan);

            BigDecimal interestDue = live ? loan.getSchedule().stream()
                    .filter(l -> !l.getDueDate().isAfter(today))
                    .map(LoanAmortizationLine::interestOwed).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

            BigDecimal recognised = accruals.getOrDefault(loan.getId(), List.of()).stream()
                    .filter(a -> inRange(a.getAccrualDate(), fromDate, toDate))
                    .map(LoanInterestAccrual::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(payments.getOrDefault(loan.getId(), List.of()).stream()
                            .filter(p -> inRange(p.getRepaymentDate(), fromDate, toDate))
                            .map(p -> p.getInterestAmount().subtract(p.getAccruedInterestApplied()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

            if (borrowed) {
                t.setTotalBorrowed(t.getTotalBorrowed().add(financed));
                if (live) t.setOutstandingBorrowed(t.getOutstandingBorrowed().add(loan.getOutstandingPrincipal()));
                t.setInterestDuePayable(t.getInterestDuePayable().add(interestDue));
                t.setInterestExpense(t.getInterestExpense().add(recognised));
            } else {
                t.setTotalLent(t.getTotalLent().add(financed));
                if (live) t.setOutstandingLent(t.getOutstandingLent().add(loan.getOutstandingPrincipal()));
                t.setInterestDueReceivable(t.getInterestDueReceivable().add(interestDue));
                t.setInterestIncome(t.getInterestIncome().add(recognised));
            }

            if (!live) continue;
            for (LoanAmortizationLine line : loan.getSchedule()) {
                BigDecimal owed = line.totalOwed();
                if (owed.signum() == 0) continue;
                if (line.getDueDate().isBefore(today)) {
                    t.setOverdueAmount(t.getOverdueAmount().add(owed));
                    r.getOverduePayments().add(due(loan, line, today));
                } else if (!line.getDueDate().isAfter(today.plusDays(UPCOMING_DAYS))) {
                    t.setUpcomingAmount(t.getUpcomingAmount().add(owed));
                    r.getUpcomingPayments().add(due(loan, line, today));
                }
            }
        }
        r.getTotals().addAll(byCurrency.values());
        r.getUpcomingPayments().sort(Comparator.comparing(LoanDashboardResponse.DuePayment::getDueDate));
        r.getOverduePayments().sort(Comparator.comparing(LoanDashboardResponse.DuePayment::getDueDate));
        return r;
    }

    private static LoanDashboardResponse.DuePayment due(Loan loan, LoanAmortizationLine line, LocalDate today) {
        var d = new LoanDashboardResponse.DuePayment();
        d.setLoanId(loan.getId());
        d.setLoanNumber(loan.getLoanNumber());
        d.setCounterpartyName(loan.getCounterpartyName());
        d.setDirection(loan.getDirection());
        d.setCurrency(loan.getCurrency());
        d.setInstallmentNumber(line.getInstallmentNumber());
        d.setDueDate(line.getDueDate());
        d.setAmountDue(line.totalOwed());
        d.setMissed(line.isMissed());
        d.setDaysOverdue(line.getDueDate().isBefore(today) ? ChronoUnit.DAYS.between(line.getDueDate(), today) : 0);
        return d;
    }

    /**
     * The loan account as the counterparty sees it: the amount advanced, interest and fees as
     * each installment falls due, payments, reversals and write-offs, with the running balance.
     */
    @Transactional(readOnly = true)
    public LoanStatementResponse statement(Long loanId, LocalDate from, LocalDate to) {
        Loan loan = loanService.loadLoan(loanId);
        LocalDate today = LocalDate.now();
        LocalDate toDate = to != null ? to : today;

        List<LoanStatementResponse.Entry> all = new ArrayList<>();
        boolean disbursed = loan.getJournal() != null;
        if (disbursed) {
            LoanStatementResponse.Entry e = entry(loan.getJournal().getJournalDate(), "DISBURSEMENT",
                    loan.getDirection() == LoanDirection.BORROWED_LOAN ? "Loan received" : "Loan disbursed",
                    loan.getLoanNumber(), loan.getJournal().getJournalNumber());
            BigDecimal financed = LoanService.financedAmount(loan);
            e.setPrincipal(financed);
            e.setCharge(financed);
            if (loan.getFeeTreatment() == LoanFeeTreatment.CAPITALIZED && loan.getTotalFees().signum() > 0) {
                e.setFees(loan.getTotalFees());
                e.setDescription(e.getDescription() + " (includes " + loan.getTotalFees() + " fees added to the balance)");
            }
            all.add(e);

            if (loan.getStatus() != LoanStatus.REVERSED) {
                for (LoanAmortizationLine line : loan.getSchedule()) {
                    BigDecimal charges = line.getInterestDue().add(line.getFeesDue());
                    if (line.getDueDate().isAfter(toDate) || charges.signum() == 0) continue;
                    LoanStatementResponse.Entry c = entry(line.getDueDate(), "INSTALLMENT_CHARGES",
                            "Interest and fees for installment " + line.getInstallmentNumber(), null, null);
                    c.setInterest(line.getInterestDue());
                    c.setFees(line.getFeesDue());
                    c.setCharge(charges);
                    all.add(c);
                }
            }
        }

        for (LoanRepayment p : repaymentRepository.findByLoanIdOrderByRepaymentDateAscIdAsc(loanId)) {
            LoanStatementResponse.Entry e = entry(p.getRepaymentDate(), "PAYMENT",
                    (p.getPaymentType() != null ? LoanService.label(p.getPaymentType()) + " payment" : "Payment"),
                    p.getReferenceNumber(), p.getJournal() != null ? p.getJournal().getJournalNumber() : null);
            e.setPrincipal(p.getPrincipalAmount().add(p.getOverpaymentAmount()));
            e.setInterest(p.getInterestAmount());
            e.setFees(p.getFeesAmount());
            e.setPayment(p.getTotalAmount());
            all.add(e);
            if (!p.isPosted() && p.getReversedAt() != null) {
                LoanStatementResponse.Entry rev = entry(p.getReversedAt().toLocalDate(), "PAYMENT_REVERSAL",
                        "Reversal of payment dated " + p.getRepaymentDate()
                                + (p.getReversalReason() != null ? ": " + p.getReversalReason() : ""),
                        p.getReferenceNumber(), p.getReversalJournal() != null ? p.getReversalJournal().getJournalNumber() : null);
                rev.setCharge(p.getTotalAmount());
                all.add(rev);
            }
        }

        if (loan.getWrittenOffAmount().signum() > 0 && loan.getClosedAt() != null) {
            LoanStatementResponse.Entry e = entry(loan.getClosedAt().toLocalDate(), "WRITE_OFF", "Balance written off",
                    null, loan.getWriteOffJournal() != null ? loan.getWriteOffJournal().getJournalNumber() : null);
            e.setPayment(loan.getWrittenOffAmount());
            all.add(e);
        }
        if (loan.getStatus() == LoanStatus.REVERSED && disbursed && loan.getReversedAt() != null) {
            LoanStatementResponse.Entry e = entry(loan.getReversedAt().toLocalDate(), "LOAN_REVERSAL",
                    "Loan reversed" + (loan.getStatusReason() != null ? ": " + loan.getStatusReason() : ""), null, null);
            e.setPayment(LoanService.financedAmount(loan));
            all.add(e);
        }

        all.sort(Comparator.comparing(LoanStatementResponse.Entry::getDate)
                .thenComparing(e -> order(e.getType())));

        LoanStatementResponse s = new LoanStatementResponse();
        s.setLoanId(loan.getId());
        s.setLoanNumber(loan.getLoanNumber());
        s.setCounterpartyName(loan.getCounterpartyName());
        s.setCurrency(loan.getCurrency());
        s.setDirection(loan.getDirection().name());
        s.setFromDate(from);
        s.setToDate(toDate);

        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal opening = BigDecimal.ZERO;
        BigDecimal charged = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (LoanStatementResponse.Entry e : all) {
            if (e.getDate().isAfter(toDate)) continue;
            balance = balance.add(e.getCharge()).subtract(e.getPayment());
            e.setBalance(balance);
            if (from != null && e.getDate().isBefore(from)) {
                opening = balance;
                continue;
            }
            charged = charged.add(e.getCharge());
            paid = paid.add(e.getPayment());
            s.getEntries().add(e);
        }
        s.setOpeningBalance(opening);
        s.setClosingBalance(balance);
        s.setTotalCharged(charged);
        s.setTotalPaid(paid);
        return s;
    }

    private static int order(String type) {
        return switch (type) {
            case "DISBURSEMENT" -> 0;
            case "INSTALLMENT_CHARGES" -> 1;
            case "PAYMENT" -> 2;
            case "PAYMENT_REVERSAL" -> 3;
            default -> 4;
        };
    }

    private static LoanStatementResponse.Entry entry(LocalDate date, String type, String description, String reference, String journal) {
        LoanStatementResponse.Entry e = new LoanStatementResponse.Entry();
        e.setDate(date);
        e.setType(type);
        e.setDescription(description);
        e.setReference(reference);
        e.setJournalNumber(journal);
        return e;
    }

    private static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        return d != null && !d.isBefore(from) && !d.isAfter(to);
    }
}
