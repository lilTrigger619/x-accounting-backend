package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Resolves "the start of the current accounting year" for reports that need a year-to-date
 * window (Balance Sheet's current-year-earnings line, the Dashboard's YTD net profit). Prefers
 * the Financial Year marked current; falls back to calendar year-to-date so these reports still
 * produce a sensible number before Financial Years have been set up at all.
 */
@Component
@RequiredArgsConstructor
public class CurrentFiscalPeriodResolver {

    private final FinancialYearRepository financialYearRepository;

    public LocalDate resolveYearStart(LocalDate asOfDate) {
        return financialYearRepository.findByIsCurrentTrue()
                .map(FinancialYear::getStartDate)
                .orElse(asOfDate.withDayOfYear(1));
    }
}
