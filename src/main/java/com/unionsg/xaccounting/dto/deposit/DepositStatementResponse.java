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
public class DepositStatementResponse {
    private String title;
    private String counterpartyName;
    private DepositDirection direction;
    private String currency;
    private List<String> availableCurrencies = new ArrayList<>();
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal totalIncrease = BigDecimal.ZERO;
    private BigDecimal totalDecrease = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;
    private List<DepositStatementLineResponse> lines = new ArrayList<>();
}
