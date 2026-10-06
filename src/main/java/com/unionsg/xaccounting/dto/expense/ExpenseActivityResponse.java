package com.unionsg.xaccounting.dto.expense;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.expense.ExpenseAction;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseActivityResponse {
    private Long id;
    private ExpenseAction action;
    private ExpenseStatus fromStatus;
    private ExpenseStatus toStatus;
    private String details;
    private CreatedByDTO performedBy;
    private LocalDateTime performedAt;
}
