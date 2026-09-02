package com.unionsg.xaccounting.dto.customer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCustomerStatusRequest {

    @NotNull(message = "Status is required")
    private String status;
}
