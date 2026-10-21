package com.unionsg.xaccounting.dto.fixedasset;

import lombok.Getter;
import lombok.Setter;

/** An account as screens show it: database id, code and name. */
@Getter
@Setter
public class AccountRef {
    private Long id;
    private String code;
    private String name;
}
