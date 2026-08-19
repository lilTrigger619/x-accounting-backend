package com.unionsg.xaccounting.documenttemplate.dto.response;

import lombok.Data;

@Data
public class DocumentTemplateDesignResponse {

    private Long id;
    private Long logoFileId;
    private String logoPosition;
    private Integer logoWidth;
    private Integer logoHeight;
    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;
    private Integer fontSize;
    private String fontColor;
}

