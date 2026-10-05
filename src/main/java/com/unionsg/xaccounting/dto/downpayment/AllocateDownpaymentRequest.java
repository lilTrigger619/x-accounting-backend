package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Apply a downpayment to one or more invoices (customer) or bills (supplier) in one go. */
@Data
public class AllocateDownpaymentRequest {
    private LocalDate allocationDate;
    private String notes;
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {
        /** Invoice id for a customer downpayment, bill id for a supplier downpayment. */
        private Long documentId;
        private BigDecimal amount;
    }
}
