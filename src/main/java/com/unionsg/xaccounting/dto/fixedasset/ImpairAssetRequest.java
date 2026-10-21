package com.unionsg.xaccounting.dto.fixedasset;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImpairAssetRequest {
    private LocalDate impairmentDate;
    private BigDecimal amount;
    private String reason;
}
