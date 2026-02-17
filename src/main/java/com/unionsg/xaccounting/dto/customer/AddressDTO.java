package com.unionsg.xaccounting.dto.customer;

import lombok.Data;

@Data
public class AddressDTO {
    private String addressLine;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
