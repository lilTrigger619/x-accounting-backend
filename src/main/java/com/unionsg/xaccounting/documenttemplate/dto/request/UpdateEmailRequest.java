package com.unionsg.xaccounting.documenttemplate.dto.request;

import lombok.Data;

@Data
public class UpdateEmailRequest {

    private String subject;

    private Boolean useGreeting;

    private String salutation;

    private String nameFormat;

    private String body;
}

