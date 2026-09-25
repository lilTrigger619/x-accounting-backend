package com.unionsg.xaccounting.dto.prepayment;

import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PrepaymentResponse {

    private Long id;
    private String prepaymentNumber;
    private Long prepaymentTypeId;
    private String prepaymentTypeName;
    private PrepaymentCounterpartyType counterpartyType;
    private String counterpartyName;
    private Long supplierId;
    private Long employeeId;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate paymentDate;
    private LocalDate recognitionStartDate;
    private Integer numberOfPeriods;
    private PrepaymentFrequency recognitionFrequency;
    private BigDecimal amountRecognized;
    private BigDecimal amountRemaining;
    private String notes;
    private PrepaymentStatus status;
    private List<PrepaymentLineResponse> schedule;
}
