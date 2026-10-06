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
import com.unionsg.xaccounting.enums.bankrec.MatchItemSide;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchItemResponse {
    private Long id;
    private MatchItemSide side;
    private Long statementTransactionId;
    private Long journalLineId;
    private String journalNumber;
    private LocalDate date;
    private String description;
    private String reference;
    private BigDecimal itemAmount;
    private BigDecimal allocatedAmount;
}
