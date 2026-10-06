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
public class DepositTypeResponse {
    private Long id;
    private String name;
    private String description;
    private DepositDirection direction;
    private String directionLabel;
    private Boolean refundableByDefault;
    private Boolean interestBearingByDefault;
    private Long depositAccountId;
    private String depositAccountCode;
    private String depositAccountName;
    private Long forfeitureAccountId;
    private String forfeitureAccountCode;
    private String forfeitureAccountName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
