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


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingRuleRequest {
    private String name;
    private String description;
    private Integer priority;
    private Boolean active;
    private Long bankAccountId;
    private Integer dateToleranceDays;
    private Boolean matchReference;
    private Boolean matchTransactionNumber;
    private Boolean matchDescription;
    private Boolean matchChequeNumber;
    private Boolean matchCounterparty;
    private Integer autoConfirmThreshold;
    private Integer suggestThreshold;
}
