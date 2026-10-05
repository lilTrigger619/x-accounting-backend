package com.unionsg.xaccounting.dto.deposit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyDepositRequest {
    private LocalDate allocationDate;
    private String reference;
    private String notes;
    private List<ApplyDepositLineRequest> lines = new ArrayList<>();
}
