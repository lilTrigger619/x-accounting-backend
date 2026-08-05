package com.unionsg.xaccounting.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ActivityResponse {

    private Long id;

    private String action;

    private String description;

    private String performedBy;

    private LocalDateTime timestamp;

    private String details;
}
