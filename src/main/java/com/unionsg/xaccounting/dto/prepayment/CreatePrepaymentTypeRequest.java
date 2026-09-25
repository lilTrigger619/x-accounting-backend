package com.unionsg.xaccounting.dto.prepayment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePrepaymentTypeRequest {
    private String name;
    private String description;
}
