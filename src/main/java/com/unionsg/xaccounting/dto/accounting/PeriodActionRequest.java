package com.unionsg.xaccounting.dto.accounting;

import lombok.Getter;
import lombok.Setter;

/** Shared body for lock/unlock/close/reopen actions on a period or financial year. */
@Getter
@Setter
public class PeriodActionRequest {
    private String reason;
}
