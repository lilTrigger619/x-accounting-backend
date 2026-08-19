package com.unionsg.xaccounting.documenttemplate.dto.request;

import lombok.Data;

@Data
public class UpdateDesignRequest {

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

