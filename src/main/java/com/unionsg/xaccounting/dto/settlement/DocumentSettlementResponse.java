package com.unionsg.xaccounting.dto.settlement;

import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentSettlementResponse {
    private Long id;
    private SettlementDocumentType documentType;
    private Long documentId;
    private String documentNumber;
    private SettlementSourceType sourceType;
    private String sourceTypeLabel;
    private Long sourceId;
    private Long sourceAllocationId;
    private String sourceNumber;
    private BigDecimal amount;
    private LocalDate settlementDate;
    private Boolean reversed;
    private LocalDateTime reversedAt;
}
