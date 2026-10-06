package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.loan.CustomInstallmentRequest;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.exception.BusinessException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure loan arithmetic with no persistence: builds amortization schedules, splits a payment
 * into fees, interest and principal, applies a split to the schedule and re-amortizes what is
 * left after an early principal payment.
 *
 * <p>Invariant kept by every method: the loan's outstanding principal equals the sum of
 * {@code principalDue - principalPaid} over its installments.</p>
 */
public final class LoanScheduleEngine {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private LoanScheduleEngine() {
    }

    /** Everything the schedule depends on. {@code customLines} is only read for CUSTOM_SCHEDULE. */
    public record Terms(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            LoanFrequency frequency,
            int installments,
            int graceInstallments,
            LoanInterestMethod method,
            LocalDate startDate,
            BigDecimal installmentFee,
            List<CustomInstallmentRequest> customLines
    ) {
    }

    /** How one payment divides across the loan. {@code overpayment} is beyond the whole balance. */
    public record Split(BigDecimal fees, BigDecimal interest, BigDecimal principal, BigDecimal overpayment) {
        public BigDecimal total() {
            return fees.add(interest).add(principal).add(overpayment);
        }
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    public static List<LoanAmortizationLine> generate(Terms t) {
        validate(t);
        List<LoanAmortizationLine> lines = t.method() == LoanInterestMethod.CUSTOM_SCHEDULE
                ? generateCustom(t)
                : generateStandard(t);
        lines.forEach(LoanAmortizationLine::snapshotOriginals);
        return lines;
    }

    private static void validate(Terms t) {
        if (t.principal() == null || t.principal().signum() <= 0) {
            throw new BusinessException("Principal amount must be greater than zero");
        }
        if (t.annualRatePercent() == null || t.annualRatePercent().signum() < 0) {
            throw new BusinessException("Interest rate cannot be negative");
        }
        if (t.startDate() == null) {
            throw new BusinessException("Start date is required");
        }
        if (t.method() == null) {
            throw new BusinessException("Interest calculation method is required");
        }
        if (t.method() == LoanInterestMethod.CUSTOM_SCHEDULE) {
            if (t.customLines() == null || t.customLines().isEmpty()) {
                throw new BusinessException("A custom schedule needs at least one installment");
            }
            return;
        }
        if (t.frequency() == null) {
            throw new BusinessException("Payment frequency is required");
        }
        if (t.installments() <= 0) {
            throw new BusinessException("Number of installments must be at least 1");
        }
        if (t.graceInstallments() < 0 || t.graceInstallments() >= t.installments()) {
            throw new BusinessException("Grace period must be shorter than the number of installments");
        }
    }

    private static List<LoanAmortizationLine> generateStandard(Terms t) {
        int n = t.installments();
        int g = t.graceInstallments();
        int amortizing = n - g;
        BigDecimal principal = t.principal();
        BigDecimal r = periodRate(t.annualRatePercent(), t.frequency());
        BigDecimal fee = nz(t.installmentFee());

        BigDecimal annuity = t.method() == LoanInterestMethod.FIXED_INSTALLMENT ? annuity(principal, r, amortizing) : null;
        BigDecimal equalPrincipal = principal.divide(BigDecimal.valueOf(amortizing), 2, RoundingMode.HALF_UP);
        BigDecimal flatInterest = money(principal.multiply(r));

        List<LoanAmortizationLine> lines = new ArrayList<>();
        BigDecimal balance = principal;
        LocalDate due = t.startDate();
        for (int i = 1; i <= n; i++) {
            due = advance(due, t.frequency());
            BigDecimal opening = balance;
            boolean grace = i <= g;
            boolean last = i == n;

            BigDecimal interest = t.method() == LoanInterestMethod.SIMPLE ? flatInterest : money(opening.multiply(r));
            BigDecimal principalDue;
            if (grace) {
                principalDue = BigDecimal.ZERO;
            } else if (last) {
                principalDue = opening;
            } else if (t.method() == LoanInterestMethod.FIXED_INSTALLMENT) {
                principalDue = annuity.subtract(interest).max(BigDecimal.ZERO).min(opening);
            } else {
                principalDue = equalPrincipal.min(opening);
            }
            lines.add(line(i, due, opening, principalDue, interest, fee));
            balance = opening.subtract(principalDue);
        }
        return lines;
    }

    private static List<LoanAmortizationLine> generateCustom(Terms t) {
        List<CustomInstallmentRequest> input = new ArrayList<>(t.customLines());
        input.sort(Comparator.comparing(CustomInstallmentRequest::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        BigDecimal sum = BigDecimal.ZERO;
        LocalDate previous = t.startDate();
        for (CustomInstallmentRequest c : input) {
            if (c.getDueDate() == null) {
                throw new BusinessException("Every custom installment needs a due date");
            }
            if (!c.getDueDate().isAfter(t.startDate())) {
                throw new BusinessException("Custom installment due dates must fall after the start date");
            }
            if (c.getDueDate().equals(previous)) {
                throw new BusinessException("Two custom installments share the due date " + c.getDueDate());
            }
            if (c.getPrincipal() == null || c.getPrincipal().signum() < 0) {
                throw new BusinessException("Custom installment principal cannot be negative");
            }
            sum = sum.add(c.getPrincipal());
            previous = c.getDueDate();
        }
        if (money(sum).compareTo(money(t.principal())) != 0) {
            throw new BusinessException("Custom installments repay " + money(sum) + " of principal but the loan is "
                    + money(t.principal()) + ". The principal column must add up to the loan amount.");
        }

        // Without a frequency, interest for a blank row accrues by days on the reducing balance.
        BigDecimal annual = t.annualRatePercent().divide(HUNDRED, MC);
        List<LoanAmortizationLine> lines = new ArrayList<>();
        BigDecimal balance = t.principal();
        LocalDate from = t.startDate();
        int i = 1;
        for (CustomInstallmentRequest c : input) {
            BigDecimal interest = c.getInterest() != null
                    ? money(c.getInterest())
                    : money(dayInterest(balance, annual, from, c.getDueDate()));
            BigDecimal fee = c.getFees() != null ? money(c.getFees()) : nz(t.installmentFee());
            BigDecimal principalDue = money(c.getPrincipal()).min(balance);
            lines.add(line(i++, c.getDueDate(), balance, principalDue, interest, fee));
            balance = balance.subtract(principalDue);
            from = c.getDueDate();
        }
        return lines;
    }

    private static BigDecimal dayInterest(BigDecimal balance, BigDecimal annualRate, LocalDate from, LocalDate to) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        return balance.multiply(annualRate).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(365), MC);
    }

    private static LoanAmortizationLine line(int number, LocalDate due, BigDecimal opening, BigDecimal principal,
                                             BigDecimal interest, BigDecimal fee) {
        LoanAmortizationLine line = new LoanAmortizationLine();
        line.setInstallmentNumber(number);
        line.setDueDate(due);
        line.setOpeningPrincipal(money(opening));
        line.setPrincipalDue(money(principal));
        line.setInterestDue(money(interest));
        line.setFeesDue(money(fee));
        line.setClosingPrincipal(money(opening.subtract(principal)));
        line.setTotalInstallment(money(principal.add(interest).add(fee)));
        return line;
    }

    // ------------------------------------------------------------------
    // Payments
    // ------------------------------------------------------------------

    /** Fees, interest and principal of installments due on or before {@code date}, still unpaid. */
    public static BigDecimal dueAsOf(List<LoanAmortizationLine> lines, LocalDate date) {
        return lines.stream()
                .filter(l -> !l.getDueDate().isAfter(date))
                .map(LoanAmortizationLine::totalOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Everything still owed on the schedule, whether due yet or not. */
    public static BigDecimal totalOwed(List<LoanAmortizationLine> lines) {
        return lines.stream().map(LoanAmortizationLine::totalOwed).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Splits an amount the way a payment settles a loan: each installment already due takes
     * its fees, then interest, then principal, oldest first. Anything left reduces principal
     * early. Anything beyond the whole outstanding principal is an overpayment.
     */
    public static Split autoSplit(List<LoanAmortizationLine> lines, LocalDate date, BigDecimal amount) {
        BigDecimal remaining = amount;
        BigDecimal fees = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal principal = BigDecimal.ZERO;

        for (LoanAmortizationLine l : lines) {
            if (l.getDueDate().isAfter(date) || remaining.signum() <= 0) {
                continue;
            }
            BigDecimal f = remaining.min(l.feesOwed());
            fees = fees.add(f);
            remaining = remaining.subtract(f);
            BigDecimal i = remaining.min(l.interestOwed());
            interest = interest.add(i);
            remaining = remaining.subtract(i);
            BigDecimal p = remaining.min(l.principalOwed());
            principal = principal.add(p);
            remaining = remaining.subtract(p);
        }

        BigDecimal futurePrincipal = lines.stream()
                .filter(l -> l.getDueDate().isAfter(date))
                .map(LoanAmortizationLine::principalOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal early = remaining.min(futurePrincipal);
        principal = principal.add(early);
        remaining = remaining.subtract(early);

        return new Split(money(fees), money(interest), money(principal), money(remaining.max(BigDecimal.ZERO)));
    }

    /**
     * Applies a fixed split to the schedule: each bucket settles installments due by
     * {@code date} oldest first. Leftover fees and interest prepay later installments in order;
     * leftover principal reduces the balance and the remaining installments are re-amortized.
     */
    public static void apply(List<LoanAmortizationLine> lines, Split split, LocalDate date, Terms terms) {
        BigDecimal fees = split.fees();
        BigDecimal interest = split.interest();
        BigDecimal principal = split.principal();

        for (LoanAmortizationLine l : lines) {
            if (l.getDueDate().isAfter(date)) {
                continue;
            }
            BigDecimal f = fees.min(l.feesOwed());
            l.setFeesPaid(l.getFeesPaid().add(f));
            fees = fees.subtract(f);
            BigDecimal i = interest.min(l.interestOwed());
            l.setInterestPaid(l.getInterestPaid().add(i));
            interest = interest.subtract(i);
            BigDecimal p = principal.min(l.principalOwed());
            l.setPrincipalPaid(l.getPrincipalPaid().add(p));
            principal = principal.subtract(p);
        }
        for (LoanAmortizationLine l : lines) {
            if (!l.getDueDate().isAfter(date)) {
                continue;
            }
            BigDecimal f = fees.min(l.feesOwed());
            l.setFeesPaid(l.getFeesPaid().add(f));
            fees = fees.subtract(f);
            BigDecimal i = interest.min(l.interestOwed());
            l.setInterestPaid(l.getInterestPaid().add(i));
            interest = interest.subtract(i);
        }
        if (principal.signum() > 0) {
            reamortize(lines, principal, date, terms);
        }
        lines.forEach(LoanAmortizationLine::refreshStatus);
    }

    /**
     * Takes {@code extraPrincipal} off the installments due after {@code date} and works out
     * their principal and interest again on the lower balance, keeping their due dates. For a
     * custom schedule the extra comes off the last installments first.
     */
    static void reamortize(List<LoanAmortizationLine> lines, BigDecimal extraPrincipal, LocalDate date, Terms t) {
        List<LoanAmortizationLine> future = lines.stream().filter(l -> l.getDueDate().isAfter(date)).toList();
        BigDecimal futurePrincipal = future.stream().map(LoanAmortizationLine::principalOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (future.isEmpty() || extraPrincipal.compareTo(futurePrincipal) > 0) {
            throw new BusinessException("Principal paid exceeds the outstanding principal");
        }

        // The prepaid principal is shown on the next installment, as principal already paid,
        // so opening and closing balances down the schedule reflect it.
        LoanAmortizationLine next = future.get(0);

        if (t.method() == LoanInterestMethod.CUSTOM_SCHEDULE) {
            BigDecimal left = extraPrincipal;
            for (int k = future.size() - 1; k >= 0 && left.signum() > 0; k--) {
                LoanAmortizationLine l = future.get(k);
                BigDecimal cut = left.min(l.principalOwed());
                l.setPrincipalDue(l.getPrincipalDue().subtract(cut));
                left = left.subtract(cut);
            }
            next.setPrincipalDue(next.getPrincipalDue().add(extraPrincipal));
            next.setPrincipalPaid(next.getPrincipalPaid().add(extraPrincipal));
            BigDecimal remaining = future.stream().map(LoanAmortizationLine::principalOwed)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (remaining.signum() == 0) {
                for (LoanAmortizationLine l : future) {
                    l.setInterestDue(l.getInterestPaid());
                    l.setFeesDue(l.getFeesPaid());
                }
            }
            restateBalances(lines);
            return;
        }

        BigDecimal balance = futurePrincipal.subtract(extraPrincipal);
        next.setPrincipalPaid(next.getPrincipalPaid().add(extraPrincipal));
        BigDecimal r = periodRate(t.annualRatePercent(), t.frequency());
        List<LoanAmortizationLine> amortizing = future.stream()
                .filter(l -> l.getInstallmentNumber() > t.graceInstallments()).toList();
        int m = Math.max(amortizing.size(), 1);
        BigDecimal annuity = t.method() == LoanInterestMethod.FIXED_INSTALLMENT ? annuity(balance, r, m) : null;
        BigDecimal equalPrincipal = balance.divide(BigDecimal.valueOf(m), 2, RoundingMode.HALF_UP);
        BigDecimal flatInterest = money(balance.multiply(r));

        BigDecimal running = balance;
        for (int k = 0; k < future.size(); k++) {
            LoanAmortizationLine l = future.get(k);
            boolean grace = l.getInstallmentNumber() <= t.graceInstallments();
            boolean last = k == future.size() - 1;
            BigDecimal interest = t.method() == LoanInterestMethod.SIMPLE ? flatInterest : money(running.multiply(r));
            BigDecimal p;
            if (grace || running.signum() == 0) {
                p = BigDecimal.ZERO;
            } else if (last) {
                p = running;
            } else if (t.method() == LoanInterestMethod.FIXED_INSTALLMENT) {
                p = annuity.subtract(interest).max(BigDecimal.ZERO).min(running);
            } else {
                p = equalPrincipal.min(running);
            }
            if (running.signum() == 0) {
                // Nothing left to repay: later installments carry no interest or fees.
                interest = BigDecimal.ZERO;
                l.setFeesDue(l.getFeesPaid());
            }
            l.setPrincipalDue(money(p).add(l.getPrincipalPaid()));
            l.setInterestDue(money(interest).max(l.getInterestPaid()));
            running = running.subtract(money(p));
        }
        restateBalances(lines);
    }

    /** Recomputes opening/closing principal and totals down the schedule after principal changed. */
    private static void restateBalances(List<LoanAmortizationLine> lines) {
        BigDecimal balance = lines.isEmpty() ? BigDecimal.ZERO : lines.get(0).getOpeningPrincipal();
        for (LoanAmortizationLine l : lines) {
            l.setOpeningPrincipal(money(balance));
            l.setClosingPrincipal(money(balance.subtract(l.getPrincipalDue())));
            l.setTotalInstallment(money(l.getPrincipalDue().add(l.getInterestDue()).add(l.getFeesDue())));
            balance = balance.subtract(l.getPrincipalDue());
        }
    }

    // ------------------------------------------------------------------
    // Arithmetic helpers
    // ------------------------------------------------------------------

    public static BigDecimal periodRate(BigDecimal annualRatePercent, LoanFrequency frequency) {
        if (frequency == null) {
            return BigDecimal.ZERO;
        }
        return annualRatePercent.divide(HUNDRED, MC).divide(BigDecimal.valueOf(periodsPerYear(frequency)), MC);
    }

    public static int periodsPerYear(LoanFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> 52;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
        };
    }

    public static LocalDate advance(LocalDate date, LoanFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case SEMI_ANNUALLY -> date.plusMonths(6);
            case ANNUALLY -> date.plusYears(1);
        };
    }

    /** Equal payment that repays {@code principal} over {@code n} periods: P r / (1 - (1+r)^-n). */
    static BigDecimal annuity(BigDecimal principal, BigDecimal r, int n) {
        if (r.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        BigDecimal growth = BigDecimal.ONE.add(r).pow(n, MC);
        BigDecimal discount = BigDecimal.ONE.divide(growth, MC);
        return principal.multiply(r).divide(BigDecimal.ONE.subtract(discount), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
