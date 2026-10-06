package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.Granularity;
import com.unionsg.xaccounting.dto.analytics.Period;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BucketsAndPeriodsTest {

    @Test
    void wholeMonthsCompareWithTheSameNumberOfWholeMonths() {
        Period p = AnalyticsScopeService.previousPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(p.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(p.to()).isEqualTo(LocalDate.of(2026, 2, 28));

        Period q = AnalyticsScopeService.previousPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
        assertThat(q.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(q.to()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void partialPeriodsCompareWithTheSameNumberOfDays() {
        Period p = AnalyticsScopeService.previousPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));
        assertThat(p.from()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(p.to()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void bucketsAreClippedToThePeriod() {
        List<Buckets.Bucket> months = Buckets.of(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10), Granularity.MONTH);
        assertThat(months).hasSize(3);
        assertThat(months.get(0).start()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(months.get(2).end()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(months).extracting(Buckets.Bucket::label).containsExactly("Jan 2026 (partial)", "Feb 2026", "Mar 2026 (partial)");

        List<Buckets.Bucket> quarters = Buckets.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), Granularity.QUARTER);
        assertThat(quarters).extracting(Buckets.Bucket::label).containsExactly("Q1 2026", "Q2 2026", "Q3 2026", "Q4 2026");

        List<Buckets.Bucket> weeks = Buckets.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), Granularity.WEEK);
        assertThat(weeks.get(0).start()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(weeks.get(1).start().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
    }

    @Test
    void growthIsNullWhenThereIsNothingToCompareWith() {
        assertThat(Metrics.growth(java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO)).isNull();
        assertThat(Metrics.growth(new java.math.BigDecimal("110"), new java.math.BigDecimal("100"))).isEqualByComparingTo("10");
    }
}
