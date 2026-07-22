package com.unionsg.xaccounting.communication.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EmailMessage {

    private String to;
    private String subject;
    private String body;
    private boolean html;
    private List<Long> attachmentFileIds;
}
