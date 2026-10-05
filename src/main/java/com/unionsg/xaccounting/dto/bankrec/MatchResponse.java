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
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    private Long id;
    private Long reconciliationId;
    private MatchType matchType;
    private MatchStatus status;
    private Integer confidence;
    private String ruleName;
    private String matchReasons;
    private BigDecimal matchedAmount;
    private String notes;
    private String matchedByName;
    private LocalDateTime matchedAt;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String unmatchedByName;
    private LocalDateTime unmatchedAt;
    private String unmatchReason;
    private List<MatchItemResponse> items;
}
