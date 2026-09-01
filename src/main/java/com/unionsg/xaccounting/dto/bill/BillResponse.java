package com.unionsg.xaccounting.dto.bill;

import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BillResponse {

    private Long id;

    private String billNumber;

    private String supplierReference;

    private LocalDate billDate;

    private LocalDate dueDate;

    private BillStatus status;

    private Long supplierId;

    private String supplierName;

    private String notes;

    private String terms;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal discountAmount;

    private BigDecimal subtotal;

    private BigDecimal totalTax;

    private BigDecimal totalAmount;

    private BigDecimal totalDue;

    private BigDecimal amountPaid;

    private BigDecimal balance;

    private List<BillItemResponse> items;

}
