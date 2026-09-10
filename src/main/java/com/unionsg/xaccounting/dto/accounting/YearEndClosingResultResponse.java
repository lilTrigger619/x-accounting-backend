package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.dto.journal.JournalResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YearEndClosingResultResponse {
    private FinancialYearResponse financialYear;
    private JournalResponse closingJournal;
}
