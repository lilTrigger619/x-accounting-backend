package com.unionsg.xaccounting.entity.supplier;

import com.unionsg.xaccounting.enums.PaymentTermType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "supplier_payment_terms"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentTerms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term_type", nullable = false)
    private PaymentTermType paymentTermType;

    /** Code of a "currencies" config item. */
    @Column(nullable = false)
    private String currency;

    @Column(nullable = true)
    private String paymentMethod;

    @Column(name = "date_created", nullable =false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated", nullable = true, updatable = true)
    private LocalDateTime dateUpdated;

    @PrePersist
    protected void onCreate(){
        this.dateCreated = LocalDateTime.now();
    }

    protected void onUpdate(){
        this.dateUpdated = LocalDateTime.now();
    };
}
