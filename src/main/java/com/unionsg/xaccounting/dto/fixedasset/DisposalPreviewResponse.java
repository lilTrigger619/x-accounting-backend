package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisposalPreviewResponse {
    private LocalDate disposalDate;
    private AssetDisposalType disposalType;
    private BigDecimal cost;
    private BigDecimal accumulatedDepreciationBefore;
    private BigDecimal catchUpDepreciation;
    private String catchUpExplanation;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal accumulatedImpairment;
    private BigDecimal carryingAmount;
    private BigDecimal proceeds;
    private BigDecimal gainLoss;
    private List<JournalPreviewLine> journalLines;
}
