package com.unionsg.xaccounting.dto.fixedasset;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** A reason, and a date where the action takes one (a reversal date). */
@Getter
@Setter
public class AssetActionRequest {
    private LocalDate date;
    private String reason;
}
