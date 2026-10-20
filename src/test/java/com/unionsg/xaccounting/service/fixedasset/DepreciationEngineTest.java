package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationEngine;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationInput;
import com.unionsg.xaccounting.service.fixedasset.depreciation.DepreciationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The depreciation engine on its own: no database, no Spring. */
class DepreciationEngineTest {

    private final DepreciationEngine engine = DepreciationEngine.standard();

    private static DepreciationInput.DepreciationInputBuilder straightLine(String cost, String residual, int life) {
        return DepreciationInput.builder()
                .method(DepreciationMethod.STRAIGHT_LINE)
                .convention(DepreciationConvention.DAILY_PRORATA)
                .cost(new BigDecimal(cost))
                .accumulatedDepreciation(BigDecimal.ZERO)
                .accumulatedImpairment(BigDecimal.ZERO)
                .residualValue(new BigDecimal(residual))
                .usefulLifeMonths(life)
                .depreciationStart(LocalDate.of(2026, 1, 1));
    }

    @Test
    void straightLineChargesAnEqualShareEachFullMonth() {
        DepreciationResult r = engine.calculate(straightLine("12000", "0", 12)
                .windowFrom(LocalDate.of(2026, 1, 1)).windowTo(LocalDate.of(2026, 1, 31)).build());

        assertThat(r.charge()).isEqualByComparingTo("1000.00");
        assertThat(r.monthsCharged()).isEqualByComparingTo("1");
        assertThat(r.remainingLifeMonths()).isEqualByComparingTo("12");
        assertThat(r.explanation()).contains("Straight line").contains("1,000.00");
    }

    @Test
    void dailyProRataChargesOnlyTheDaysInServiceInTheFirstMonth() {
        DepreciationResult r = engine.calculate(straightLine("12000", "0", 12)
                .depreciationStart(LocalDate.of(2026, 4, 16))
                .windowFrom(LocalDate.of(2026, 4, 16)).windowTo(LocalDate.of(2026, 4, 30)).build());

        // 15 of April's 30 days: half a month of a 12-month life.
        assertThat(r.monthsCharged()).isEqualByComparingTo("0.5");
        assertThat(r.charge()).isEqualByComparingTo("500.00");
    }

    @Test
    void fullMonthConventionChargesTheWholeFirstMonthAndNothingInTheDisposalMonth() {
        LocalDate start = LocalDate.of(2026, 4, 16);
        DepreciationInput first = straightLine("12000", "0", 12).convention(DepreciationConvention.FULL_MONTH)
                .depreciationStart(start).windowFrom(start).windowTo(LocalDate.of(2026, 4, 30)).build();
        assertThat(engine.calculate(first).charge()).isEqualByComparingTo("1000.00");

        BigDecimal disposalMonth = DepreciationEngine.monthsCharged(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                start, LocalDate.of(2026, 6, 10), DepreciationConvention.FULL_MONTH);
        assertThat(disposalMonth).isEqualByComparingTo("0");
    }

    @Test
    void midMonthConventionChargesHalfTheFirstAndLastMonths() {
        LocalDate start = LocalDate.of(2026, 4, 3);
        assertThat(DepreciationEngine.monthsCharged(start, LocalDate.of(2026, 4, 30), start, null, DepreciationConvention.MID_MONTH))
                .isEqualByComparingTo("0.5");
        assertThat(DepreciationEngine.monthsCharged(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20), start,
                LocalDate.of(2026, 7, 20), DepreciationConvention.MID_MONTH)).isEqualByComparingTo("0.5");
    }

    @Test
    void neverChargesBelowResidualValue() {
        // 100 left above residual, but a year at 50% of the carrying amount would ask for 1,050.
        DepreciationResult r = engine.calculate(straightLine("10000", "2000", 120)
                .method(DepreciationMethod.REDUCING_BALANCE).annualRatePercent(new BigDecimal("50"))
                .accumulatedDepreciation(new BigDecimal("7900"))
                .windowFrom(LocalDate.of(2027, 1, 1)).windowTo(LocalDate.of(2027, 12, 31)).build());

        assertThat(r.charge()).isEqualByComparingTo("100.00");
        assertThat(r.closingCarrying()).isEqualByComparingTo("2000.00");
    }

    @Test
    void chargesNothingOnceAtResidualValue() {
        DepreciationResult r = engine.calculate(straightLine("10000", "2000", 12)
                .accumulatedDepreciation(new BigDecimal("8000"))
                .windowFrom(LocalDate.of(2027, 1, 1)).windowTo(LocalDate.of(2027, 1, 31)).build());

        assertThat(r.charge()).isEqualByComparingTo("0");
        assertThat(r.explanation()).contains("already at or below residual value");
    }

    @Test
    void theLastMonthOfUsefulLifeChargesWhateverIsLeft() {
        // 1,000 of rounding drift left with one month of life to go: all of it is charged.
        DepreciationResult r = engine.calculate(straightLine("12000", "0", 12)
                .accumulatedDepreciation(new BigDecimal("10000"))
                .windowFrom(LocalDate.of(2026, 12, 1)).windowTo(LocalDate.of(2026, 12, 31)).build());

        assertThat(r.endOfLife()).isTrue();
        assertThat(r.charge()).isEqualByComparingTo("2000.00");
    }

    @Test
    void straightLineIsProspectiveAfterAnImpairment() {
        // 12,000 over 12 months, 6 months charged (6,000), then impaired by 3,000: the remaining
        // 3,000 is spread over the 6 months left.
        DepreciationResult r = engine.calculate(straightLine("12000", "0", 12)
                .accumulatedDepreciation(new BigDecimal("6000")).accumulatedImpairment(new BigDecimal("3000"))
                .windowFrom(LocalDate.of(2026, 7, 1)).windowTo(LocalDate.of(2026, 7, 31)).build());

        assertThat(r.charge()).isEqualByComparingTo("500.00");
    }

    @Test
    void reducingBalanceAppliesTheAnnualRateToTheCarryingAmount() {
        DepreciationResult r = engine.calculate(DepreciationInput.builder()
                .method(DepreciationMethod.REDUCING_BALANCE).convention(DepreciationConvention.DAILY_PRORATA)
                .cost(new BigDecimal("10000")).accumulatedDepreciation(new BigDecimal("2000"))
                .accumulatedImpairment(BigDecimal.ZERO).residualValue(BigDecimal.ZERO)
                .annualRatePercent(new BigDecimal("25")).usefulLifeMonths(60)
                .depreciationStart(LocalDate.of(2025, 1, 1))
                .windowFrom(LocalDate.of(2026, 1, 1)).windowTo(LocalDate.of(2026, 1, 31)).build());

        // 8,000 x 25% / 12
        assertThat(r.charge()).isEqualByComparingTo("166.67");
        assertThat(r.annualRatePercent()).isEqualByComparingTo("25");
    }

    @Test
    void notDepreciatedNeverCharges() {
        DepreciationResult r = engine.calculate(DepreciationInput.builder()
                .method(DepreciationMethod.NOT_DEPRECIATED).convention(DepreciationConvention.DAILY_PRORATA)
                .cost(new BigDecimal("50000")).accumulatedDepreciation(BigDecimal.ZERO).accumulatedImpairment(BigDecimal.ZERO)
                .residualValue(BigDecimal.ZERO).depreciationStart(LocalDate.of(2026, 1, 1))
                .windowFrom(LocalDate.of(2026, 1, 1)).windowTo(LocalDate.of(2026, 12, 31)).build());

        assertThat(r.charge()).isEqualByComparingTo("0");
    }

    @Test
    void straightLineWithoutAUsefulLifeIsRefused() {
        assertThatThrownBy(() -> engine.calculate(straightLine("1000", "0", 12).usefulLifeMonths(null)
                .windowFrom(LocalDate.of(2026, 1, 1)).windowTo(LocalDate.of(2026, 1, 31)).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("useful life");
    }

    @Test
    void aProjectedScheduleDepreciatesExactlyCostLessResidual() {
        for (DepreciationMethod method : List.of(DepreciationMethod.STRAIGHT_LINE, DepreciationMethod.REDUCING_BALANCE)) {
            for (DepreciationConvention convention : DepreciationConvention.values()) {
                DepreciationInput base = DepreciationInput.builder()
                        .method(method).convention(convention)
                        .cost(new BigDecimal("10000.00")).accumulatedDepreciation(BigDecimal.ZERO)
                        .accumulatedImpairment(BigDecimal.ZERO).residualValue(new BigDecimal("1000.00"))
                        .usefulLifeMonths(37).depreciationStart(LocalDate.of(2026, 3, 17)).build();
                List<DepreciationEngine.ProjectedCharge> schedule = engine.project(base, base.depreciationStart(), 600);

                BigDecimal total = schedule.stream().map(p -> p.result().charge()).reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(total).as(method + " / " + convention).isEqualByComparingTo("9000.00");
                assertThat(schedule).allSatisfy(p -> assertThat(p.result().charge().signum()).isGreaterThanOrEqualTo(0));
                assertThat(schedule.get(schedule.size() - 1).result().closingCarrying()).isEqualByComparingTo("1000.00");
            }
        }
    }
}
