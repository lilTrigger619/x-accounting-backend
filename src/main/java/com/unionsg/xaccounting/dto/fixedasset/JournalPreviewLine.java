package com.unionsg.xaccounting.dto.fixedasset;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** A journal line as it would be posted. */
@Getter
@Setter
public class JournalPreviewLine {
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
}
