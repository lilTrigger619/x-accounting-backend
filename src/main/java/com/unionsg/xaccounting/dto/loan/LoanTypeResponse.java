package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoanTypeResponse {
    private Long id;
    private String name;
    private String description;
    private LoanDirection defaultDirection;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
