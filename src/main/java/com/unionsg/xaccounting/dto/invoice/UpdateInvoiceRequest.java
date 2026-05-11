package com.unionsg.xaccounting.dto.invoice;

import com.unionsg.xaccounting.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateInvoiceRequest {

    private String reference;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private Long customerId;

    private Long paymentTermsId;

    private String notes;

    private String terms;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private InvoiceBillingInfoRequest billingInfo;

    private List<InvoiceItemRequest> items;

}
