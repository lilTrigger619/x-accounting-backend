package com.unionsg.xaccounting.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponseDto {

    private Long partyId;
    private String partyCode;
    private String partyName;
    private String partyEmail;
    private String partyPhone;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private List<StatementLineDto> lines;
    private BigDecimal closingBalance;
}
