package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceResponseDTO(

        LocalDate asOfDate,
        List<TrialBalanceRowDto> rows,
        BigDecimal totalDebit,
        BigDecimal totalCredit,

        /** totalDebit - totalCredit. Guaranteed to be zero as long as every posted journal was
         * itself balanced at posting time - this is the ledger-wide proof of double-entry
         * integrity, not merely a per-report calculation. */
        BigDecimal difference

) {
}
