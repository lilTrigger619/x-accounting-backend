package com.unionsg.xaccounting.dto.branch;

import com.unionsg.xaccounting.enums.JournalApprovalAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class JournalApprovalEventResponse {
    private Long id;
    private JournalApprovalAction action;
    private String comment;
    private String userName;
    private LocalDateTime at;
}
