package com.unionsg.xaccounting.dto.prepayment;

import com.unionsg.xaccounting.enums.prepayment.PrepaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PrepaymentListItemResponse {
    private Long id;
    private String prepaymentNumber;
    private String prepaymentTypeName;
    private String counterpartyName;
    private BigDecimal totalAmount;
    private BigDecimal amountRecognized;
    private BigDecimal amountRemaining;
    private String currency;
    private LocalDate paymentDate;
    private PrepaymentStatus status;
}
