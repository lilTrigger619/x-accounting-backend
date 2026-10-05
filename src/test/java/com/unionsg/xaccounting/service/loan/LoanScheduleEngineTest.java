package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.loan.CustomInstallmentRequest;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInstallmentStatus;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanScheduleEngineTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private static LoanScheduleEngine.Terms terms(LoanInterestMethod method, int n, int grace, String fee) {
        return new LoanScheduleEngine.Terms(new BigDecimal("12000.00"), new BigDecimal("12"), LoanFrequency.MONTHLY,
                n, grace, method, START, new BigDecimal(fee), null);
    }

    private static BigDecimal total(List<LoanAmortizationLine> lines, java.util.function.Function<LoanAmortizationLine, BigDecimal> f) {
        return lines.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void fixedInstallmentPaysEqualAmountsAndRepaysThePrincipal() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.FIXED_INSTALLMENT, 12, 0, "0"));

        assertThat(lines).hasSize(12);
        assertThat(lines.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(lines.get(0).getInterestDue()).isEqualByComparingTo("120.00");
        assertThat(lines.get(0).getTotalInstallment()).isEqualByComparingTo("1066.19");
        assertThat(lines.get(5).getTotalInstallment()).isEqualByComparingTo("1066.19");
        assertThat(total(lines, LoanAmortizationLine::getPrincipalDue)).isEqualByComparingTo("12000.00");
        assertThat(lines.get(11).getClosingPrincipal()).isEqualByComparingTo("0.00");
        assertThat(lines.get(0).getOriginalPrincipalDue()).isEqualByComparingTo(lines.get(0).getPrincipalDue());
    }

    @Test
    void reducingBalanceRepaysEqualPrincipalWithFallingInterest() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.REDUCING_BALANCE, 12, 0, "0"));

        assertThat(lines.get(0).getPrincipalDue()).isEqualByComparingTo("1000.00");
        assertThat(lines.get(0).getInterestDue()).isEqualByComparingTo("120.00");
        assertThat(lines.get(1).getInterestDue()).isEqualByComparingTo("110.00");
        assertThat(lines.get(11).getInterestDue()).isEqualByComparingTo("10.00");
        assertThat(total(lines, LoanAmortizationLine::getPrincipalDue)).isEqualByComparingTo("12000.00");
    }

    @Test
    void simpleInterestIsFlatOnTheOriginalPrincipal() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.SIMPLE, 12, 0, "0"));

        assertThat(lines).allSatisfy(l -> assertThat(l.getInterestDue()).isEqualByComparingTo("120.00"));
        assertThat(total(lines, LoanAmortizationLine::getInterestDue)).isEqualByComparingTo("1440.00");
        assertThat(total(lines, LoanAmortizationLine::getPrincipalDue)).isEqualByComparingTo("12000.00");
    }

    @Test
    void gracePeriodInstallmentsCarryInterestOnly() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.REDUCING_BALANCE, 12, 2, "0"));

        assertThat(lines.get(0).getPrincipalDue()).isEqualByComparingTo("0.00");
        assertThat(lines.get(1).getPrincipalDue()).isEqualByComparingTo("0.00");
        assertThat(lines.get(1).getInterestDue()).isEqualByComparingTo("120.00");
        assertThat(lines.get(2).getPrincipalDue()).isEqualByComparingTo("1200.00");
        assertThat(total(lines, LoanAmortizationLine::getPrincipalDue)).isEqualByComparingTo("12000.00");
    }

    @Test
    void gracePeriodMustLeaveAnInstallmentToRepayPrincipal() {
        assertThatThrownBy(() -> LoanScheduleEngine.generate(terms(LoanInterestMethod.REDUCING_BALANCE, 3, 3, "0")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void installmentFeeIsAddedToEveryInstallment() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.REDUCING_BALANCE, 12, 0, "5"));

        assertThat(lines.get(0).getFeesDue()).isEqualByComparingTo("5.00");
        assertThat(lines.get(0).getTotalInstallment()).isEqualByComparingTo("1125.00");
    }

    @Test
    void customScheduleMustRepayThePrincipalAndWorksOutBlankInterest() {
        var bad = new LoanScheduleEngine.Terms(bd("1000"), bd("10"), LoanFrequency.MONTHLY, 0, 0,
                LoanInterestMethod.CUSTOM_SCHEDULE, START, BigDecimal.ZERO,
                List.of(new CustomInstallmentRequest(LocalDate.of(2026, 7, 1), bd("400"), null, null)));
        assertThatThrownBy(() -> LoanScheduleEngine.generate(bad))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("add up");

        var ok = new LoanScheduleEngine.Terms(bd("1000"), bd("10"), LoanFrequency.MONTHLY, 0, 0,
                LoanInterestMethod.CUSTOM_SCHEDULE, START, BigDecimal.ZERO,
                List.of(new CustomInstallmentRequest(LocalDate.of(2027, 1, 1), bd("600"), null, null),
                        new CustomInstallmentRequest(LocalDate.of(2026, 7, 1), bd("400"), bd("25"), bd("2"))));
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(ok);

        assertThat(lines.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(lines.get(0).getInterestDue()).isEqualByComparingTo("25.00");
        assertThat(lines.get(0).getFeesDue()).isEqualByComparingTo("2.00");
        // 600 for 184 days at 10%
        assertThat(lines.get(1).getInterestDue()).isEqualByComparingTo("30.25");
        assertThat(lines.get(1).getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @Test
    void autoSplitPaysFeesThenInterestThenPrincipalOfDueInstallments() {
        var t = terms(LoanInterestMethod.REDUCING_BALANCE, 12, 0, "5");
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(t);

        var split = LoanScheduleEngine.autoSplit(lines, LocalDate.of(2026, 2, 1), bd("500"));
        assertThat(split.fees()).isEqualByComparingTo("5.00");
        assertThat(split.interest()).isEqualByComparingTo("120.00");
        assertThat(split.principal()).isEqualByComparingTo("375.00");
        assertThat(split.overpayment()).isEqualByComparingTo("0");

        LoanScheduleEngine.apply(lines, split, LocalDate.of(2026, 2, 1), t);
        assertThat(lines.get(0).getStatus()).isEqualTo(LoanInstallmentStatus.PARTIALLY_PAID);
        assertThat(lines.get(0).totalOwed()).isEqualByComparingTo("625.00");
    }

    @Test
    void earlyPrincipalReamortizesTheRemainingInstallments() {
        var t = terms(LoanInterestMethod.REDUCING_BALANCE, 12, 0, "0");
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(t);

        // First installment (1120) plus 5500 extra principal, on its due date.
        var split = LoanScheduleEngine.autoSplit(lines, LocalDate.of(2026, 2, 1), bd("6620"));
        assertThat(split.principal()).isEqualByComparingTo("6500.00");
        LoanScheduleEngine.apply(lines, split, LocalDate.of(2026, 2, 1), t);

        BigDecimal owed = total(lines, LoanAmortizationLine::principalOwed);
        assertThat(owed).isEqualByComparingTo("5500.00");
        assertThat(lines.get(0).getStatus()).isEqualTo(LoanInstallmentStatus.PAID);
        // The 5500 prepaid shows on installment 2 as principal already paid.
        assertThat(lines.get(1).getPrincipalPaid()).isEqualByComparingTo("5500.00");
        assertThat(lines.get(1).principalOwed()).isEqualByComparingTo("500.00");
        assertThat(lines.get(1).getInterestDue()).isEqualByComparingTo("55.00");
        assertThat(lines.get(1).getClosingPrincipal()).isEqualByComparingTo("5000.00");
        assertThat(lines.get(2).getOpeningPrincipal()).isEqualByComparingTo("5000.00");
        assertThat(lines.get(11).getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @Test
    void payingEverythingEarlyClearsLaterInterestAndFees() {
        var t = terms(LoanInterestMethod.FIXED_INSTALLMENT, 12, 0, "5");
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(t);

        var split = LoanScheduleEngine.autoSplit(lines, LocalDate.of(2026, 1, 15), bd("12000"));
        LoanScheduleEngine.apply(lines, split, LocalDate.of(2026, 1, 15), t);

        assertThat(LoanScheduleEngine.totalOwed(lines)).isEqualByComparingTo("0");
        assertThat(lines).allSatisfy(l -> assertThat(l.getStatus()).isEqualTo(LoanInstallmentStatus.PAID));
    }

    @Test
    void anythingBeyondTheOutstandingPrincipalIsAnOverpayment() {
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(LoanInterestMethod.SIMPLE, 12, 0, "0"));

        var split = LoanScheduleEngine.autoSplit(lines, LocalDate.of(2026, 1, 10), bd("12500"));
        assertThat(split.principal()).isEqualByComparingTo("12000.00");
        assertThat(split.overpayment()).isEqualByComparingTo("500.00");
    }

    @Test
    void resetPutsTheLineBackAsGenerated() {
        var t = terms(LoanInterestMethod.REDUCING_BALANCE, 12, 0, "0");
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(t);
        LoanScheduleEngine.apply(lines, LoanScheduleEngine.autoSplit(lines, LocalDate.of(2026, 2, 1), bd("8000")),
                LocalDate.of(2026, 2, 1), t);

        lines.forEach(LoanAmortizationLine::resetToOriginal);

        assertThat(lines.get(1).getPrincipalDue()).isEqualByComparingTo("1000.00");
        assertThat(lines.get(1).getInterestDue()).isEqualByComparingTo("110.00");
        assertThat(LoanScheduleEngine.totalOwed(lines)).isEqualByComparingTo(
                total(lines, LoanAmortizationLine::getTotalInstallment));
    }
}
