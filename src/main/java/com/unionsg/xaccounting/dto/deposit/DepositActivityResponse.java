package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositActivityResponse {
    private LocalDate date;
    private LocalDateTime timestamp;
    /** CREATED, ACTIVATED, CANCELLED, REVERSED, or an allocation type, or ALLOCATION_REVERSED. */
    private String event;
    private String eventLabel;
    private Long depositId;
    private String depositNumber;
    private DepositDirection direction;
    private String counterpartyName;
    private String currency;
    private BigDecimal amount;
    private Long allocationId;
    private String description;
    private String journalNumber;
}
