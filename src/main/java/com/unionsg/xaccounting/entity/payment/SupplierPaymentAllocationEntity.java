package com.unionsg.xaccounting.entity.payment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.bill.Bill;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "supplier_payment_allocations",
        indexes = {
                @Index(name = "idx_sp_allocation_payment", columnList = "supplier_payment_id"),
                @Index(name = "idx_sp_allocation_bill", columnList = "bill_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentAllocationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_payment_id", nullable = false)
    private SupplierPaymentEntity supplierPayment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;
}
