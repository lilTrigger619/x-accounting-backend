package com.unionsg.xaccounting.entity.payment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "payment_allocations",
        indexes = {
                @Index(name = "idx_allocation_payment", columnList = "payment_id"),
                @Index(name = "idx_allocation_invoice", columnList = "invoice_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;
}

