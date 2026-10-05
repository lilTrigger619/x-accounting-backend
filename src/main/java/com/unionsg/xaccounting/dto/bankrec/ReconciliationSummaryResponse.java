package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.unionsg.xaccounting.service.bankrec.engine.ReconciliationFigures;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummaryResponse {
    private ReconciliationResponse reconciliation;
    private ReconciliationFigures figures;
    private Integer unmatchedStatementCount;
    private Integer unmatchedBookCount;
    private Integer proposedMatchCount;
    private Integer confirmedMatchCount;
    private Integer adjustmentCount;
    /** Why completion is blocked right now, empty when it can be completed. */
    private List<String> completionBlockers;
    private Boolean canOverrideDifference;
}
