package com.unionsg.xaccounting.dto.deposit;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyDepositLineRequest {
    private Long invoiceId;
    private Long billId;
    private BigDecimal amount;
}
