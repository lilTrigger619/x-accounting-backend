package com.unionsg.xaccounting.dto.journal;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class JournalLineResponse {

    private Long id;

    private Integer lineNumber;

    private Long accountId;

    private String accountCode;

    private String accountName;

    private String description;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;

}
