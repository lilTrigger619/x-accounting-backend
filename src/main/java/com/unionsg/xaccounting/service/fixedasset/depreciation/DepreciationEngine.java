package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a {@link DepreciationInput} into a charge. It works out how many months of service the
 * window holds under the asset's convention and how much useful life was left before it, asks the
 * method's {@link DepreciationMethodCalculator} for a charge, then applies two rules every method
 * shares:
 * <ul>
 *   <li>the charge never takes the carrying amount below residual value, and</li>
 *   <li>the window that uses up the useful life charges whatever is left above residual.</li>
 * </ul>
 * It reads nothing from the database, so it can be unit tested on its own.
 */
@Component
public class DepreciationEngine {

    static final BigDecimal EPSILON = new BigDecimal("0.000001");
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final Map<DepreciationMethod, DepreciationMethodCalculator> calculators = new EnumMap<>(DepreciationMethod.class);

    public DepreciationEngine(List<DepreciationMethodCalculator> calculators) {
        for (DepreciationMethodCalculator c : calculators) {
            this.calculators.put(c.method(), c);
        }
    }

    /** The built-in methods, for callers outside Spring (tests). */
    public static DepreciationEngine standard() {
        return new DepreciationEngine(List.of(
                new StraightLineCalculator(), new ReducingBalanceCalculator(), new NoDepreciationCalculator()));
    }

    public boolean requiresUsefulLife(DepreciationMethod method) {
        return calculator(method).requiresUsefulLife();
    }

    public DepreciationResult calculate(DepreciationInput in) {
        BigDecimal cost = nz(in.cost());
        BigDecimal opening = cost.subtract(nz(in.accumulatedDepreciation())).subtract(nz(in.accumulatedImpairment()));
        BigDecimal residual = nz(in.residualValue());
        BigDecimal depreciable = opening.subtract(residual).max(BigDecimal.ZERO);
        DepreciationMethodCalculator calculator = calculator(in.method());

        if (in.windowFrom() == null || in.windowTo() == null || in.windowFrom().isAfter(in.windowTo())) {
            return zero(opening, depreciable, null, "Nothing to charge in this period");
        }
        if (calculator.requiresUsefulLife() && (in.usefulLifeMonths() == null || in.usefulLifeMonths() <= 0)) {
            throw new IllegalArgumentException("A useful life is required for " + in.method().getLabel());
        }

        BigDecimal months = monthsCharged(in.windowFrom(), in.windowTo(), in.depreciationStart(), in.cutoffDate(), in.convention());
        BigDecimal remaining = null;
        if (in.usefulLifeMonths() != null && in.usefulLifeMonths() > 0) {
            BigDecimal consumed = in.windowFrom().isAfter(in.depreciationStart())
                    ? monthsCharged(in.depreciationStart(), in.windowFrom().minusDays(1), in.depreciationStart(), null, in.convention())
                    : BigDecimal.ZERO;
            remaining = BigDecimal.valueOf(in.usefulLifeMonths()).subtract(consumed).max(BigDecimal.ZERO);
        }

        if (months.signum() == 0) {
            return zero(opening, depreciable, remaining, "No months of service fall in this period under the "
                    + in.convention().getLabel().toLowerCase() + " convention");
        }
        if (depreciable.signum() == 0) {
            return new DepreciationResult(BigDecimal.ZERO, opening, depreciable, months, remaining, null, false,
                    "Carrying amount " + Money.format(opening) + " is already at or below residual value "
                            + Money.format(residual) + "; nothing to charge");
        }

        DepreciationContext context = new DepreciationContext(in, opening, depreciable, months, remaining);
        MethodResult raw = calculator.calculate(context);
        if (in.method() == DepreciationMethod.NOT_DEPRECIATED) {
            return new DepreciationResult(BigDecimal.ZERO, opening, depreciable, months, remaining, null, false, raw.working());
        }

        boolean endOfLife = remaining != null && remaining.subtract(months).compareTo(EPSILON) <= 0;
        BigDecimal charge;
        StringBuilder explanation = new StringBuilder(raw.working());
        if (endOfLife) {
            charge = depreciable;
            explanation.append("; this uses up the remaining useful life, so the rest above residual (")
                    .append(Money.format(depreciable)).append(") is charged");
        } else {
            charge = raw.charge().setScale(2, RoundingMode.HALF_UP);
            if (charge.compareTo(depreciable) > 0) {
                charge = depreciable;
                explanation.append("; limited to ").append(Money.format(depreciable)).append(" so the carrying amount stays at residual value");
            }
        }
        charge = charge.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        explanation.append(" = ").append(Money.format(charge));
        return new DepreciationResult(charge, opening, depreciable, months, remaining, raw.annualRatePercent(), endOfLife,
                explanation.toString());
    }

    /**
     * Months of service in {@code [from, to]} under the convention. {@code start} is the asset's
     * depreciation start and {@code cutoff} its disposal date (or null); the window must already
     * lie inside both.
     */
    public static BigDecimal monthsCharged(LocalDate from, LocalDate to, LocalDate start, LocalDate cutoff,
                                           DepreciationConvention convention) {
        if (from == null || to == null || from.isAfter(to)) {
            return BigDecimal.ZERO;
        }
        DepreciationConvention c = convention != null ? convention : DepreciationConvention.DAILY_PRORATA;
        YearMonth startMonth = start != null ? YearMonth.from(start) : null;
        YearMonth cutoffMonth = cutoff != null ? YearMonth.from(cutoff) : null;
        BigDecimal total = BigDecimal.ZERO;
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            LocalDate mFrom = max(m.atDay(1), from);
            LocalDate mTo = min(m.atEndOfMonth(), to);
            BigDecimal length = BigDecimal.valueOf(m.lengthOfMonth());
            if (c == DepreciationConvention.DAILY_PRORATA) {
                total = total.add(BigDecimal.valueOf(days(mFrom, mTo)).divide(length, MathContext.DECIMAL64));
                continue;
            }
            // The month-based conventions ignore where in the month the asset started or left;
            // only the window's own edges (a period that does not follow calendar months) count.
            if (m.equals(startMonth) && mFrom.equals(start)) mFrom = m.atDay(1);
            if (m.equals(cutoffMonth) && mTo.equals(cutoff)) mTo = m.atEndOfMonth();
            BigDecimal share = BigDecimal.valueOf(days(mFrom, mTo)).divide(length, MathContext.DECIMAL64);
            boolean first = m.equals(startMonth);
            boolean last = m.equals(cutoffMonth);
            BigDecimal factor;
            if (c == DepreciationConvention.FULL_MONTH) {
                factor = last ? BigDecimal.ZERO : BigDecimal.ONE;
            } else {
                factor = first && last ? BigDecimal.ZERO : (first || last) ? HALF : BigDecimal.ONE;
            }
            total = total.add(share.multiply(factor));
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Projects month-by-month charges from {@code from} until the asset reaches residual value, its
     * useful life ends, or {@code maxMonths} months have been projected.
     */
    public List<ProjectedCharge> project(DepreciationInput base, LocalDate from, int maxMonths) {
        List<ProjectedCharge> out = new ArrayList<>();
        BigDecimal accumulated = nz(base.accumulatedDepreciation());
        LocalDate start = base.depreciationStart();
        if (start == null || base.method() == DepreciationMethod.NOT_DEPRECIATED) {
            return out;
        }
        LocalDate cursor = max(from, start);
        for (int i = 0; i < maxMonths; i++) {
            LocalDate monthEnd = YearMonth.from(cursor).atEndOfMonth();
            DepreciationInput in = DepreciationInput.builder()
                    .method(base.method()).convention(base.convention()).cost(base.cost())
                    .accumulatedDepreciation(accumulated).accumulatedImpairment(base.accumulatedImpairment())
                    .residualValue(base.residualValue()).usefulLifeMonths(base.usefulLifeMonths())
                    .annualRatePercent(base.annualRatePercent()).depreciationStart(start)
                    .windowFrom(cursor).windowTo(monthEnd).build();
            DepreciationResult r = calculate(in);
            accumulated = accumulated.add(r.charge());
            out.add(new ProjectedCharge(cursor, monthEnd, r));
            if (r.endOfLife() || r.depreciableAmount().subtract(r.charge()).signum() <= 0) {
                break;
            }
            cursor = monthEnd.plusDays(1);
        }
        return out;
    }

    public record ProjectedCharge(LocalDate from, LocalDate to, DepreciationResult result) {
    }

    private DepreciationMethodCalculator calculator(DepreciationMethod method) {
        DepreciationMethodCalculator c = calculators.get(method);
        if (c == null) {
            throw new IllegalArgumentException("No calculator is registered for " + method);
        }
        return c;
    }

    private static DepreciationResult zero(BigDecimal opening, BigDecimal depreciable, BigDecimal remaining, String why) {
        return new DepreciationResult(BigDecimal.ZERO, opening, depreciable, BigDecimal.ZERO, remaining, null, false, why);
    }

    private static long days(LocalDate from, LocalDate to) {
        return from.isAfter(to) ? 0 : ChronoUnit.DAYS.between(from, to) + 1;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
