package com.unionsg.xaccounting.dto.prepayment;

import com.unionsg.xaccounting.enums.prepayment.PrepaymentLineStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PrepaymentLineResponse {
    private Long id;
    private Integer periodNumber;
    private LocalDate periodDate;
    private BigDecimal amount;
    private PrepaymentLineStatus status;
}
