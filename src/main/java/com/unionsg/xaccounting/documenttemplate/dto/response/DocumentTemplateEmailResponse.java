package com.unionsg.xaccounting.documenttemplate.dto.response;

import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import lombok.Data;

@Data
public class DocumentTemplateEmailResponse {

    private Long id;
    private EmailType emailType;
    private String subject;
    private boolean useGreeting;
    private String salutation;
    private String nameFormat;
    private String body;
}

