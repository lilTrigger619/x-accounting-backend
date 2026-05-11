package com.unionsg.xaccounting.dto.invoice;

import com.unionsg.xaccounting.enums.DiscountType;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class InvoiceResponse {

    private Long id;

    private String invoiceNumber;

    private String reference;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private InvoiceStatus status;

    private Long customerId;

    private String customerName;

    private Long paymentTermsId;

    private String paymentTermsName;

    private String notes;

    private String terms;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal discountAmount;

    private BigDecimal subtotal;

    private BigDecimal totalTax;

    private BigDecimal totalAmount;

    private BigDecimal totalDue;

    private InvoiceBillingInfoResponse billingInfo;

    private List<InvoiceItemResponse> items;

}
