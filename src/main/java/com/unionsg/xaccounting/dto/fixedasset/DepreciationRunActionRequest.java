package com.unionsg.xaccounting.dto.fixedasset;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepreciationRunActionRequest {
    private String reason;
    private LocalDate reverseDate;
}
