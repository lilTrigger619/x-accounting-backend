package com.unionsg.xaccounting.dto.customer;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CustomerResponseDTO {
    private Long id;
    private String customerCode;
    private String customerType;
    private String displayName;
    private String status;
    private String email;
}
