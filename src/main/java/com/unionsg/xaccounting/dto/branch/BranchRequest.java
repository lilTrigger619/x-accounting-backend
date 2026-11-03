package com.unionsg.xaccounting.dto.branch;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchRequest {
    private String code;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    /** Making a branch the Head Office moves the flag off the previous one. */
    private Boolean headOffice;
}
