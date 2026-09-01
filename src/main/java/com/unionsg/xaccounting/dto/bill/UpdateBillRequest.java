package com.unionsg.xaccounting.dto.bill;

import com.unionsg.xaccounting.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateBillRequest {

    private String supplierReference;

    private LocalDate billDate;

    private LocalDate dueDate;

    private Long supplierId;

    private String notes;

    private String terms;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private List<BillItemRequest> items;

}
