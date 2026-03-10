package com.unionsg.xaccounting.dto.supplier;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierPaymentTermsDTO {
    private String paymentTermType;
    //private double rate;
    private String paymentMethod;
    private String currency;
}
