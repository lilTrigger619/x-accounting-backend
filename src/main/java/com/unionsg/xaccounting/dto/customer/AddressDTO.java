package com.unionsg.xaccounting.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

@Builder
@Data
@AllArgsConstructor
public class AddressDTO {
    private String addressLine;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
