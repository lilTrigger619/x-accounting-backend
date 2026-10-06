package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Figure → category → account level of a drill-down. */
public record DrillAccountsResponse(
        String label,
        LocalDate from,
        LocalDate to,
        String basis,
        String baseCurrency,
        BigDecimal total,
        List<Category> categories
) {
    public record Category(String name, BigDecimal amount, List<Account> accounts) {
    }

    public record Account(Long id, String code, String name, String accountClass, String classLabel, BigDecimal amount) {
    }
}
