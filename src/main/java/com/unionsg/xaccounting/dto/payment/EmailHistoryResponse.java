package com.unionsg.xaccounting.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class EmailHistoryResponse {

    private Long id;

    private String recipient;

    private String subject;

    private String status;

    private LocalDateTime sentAt;

    private String sentBy;

    private List<String> cc;

    private List<String> bcc;

    private String errorMessage;
}
