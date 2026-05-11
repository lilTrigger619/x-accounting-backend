package com.unionsg.xaccounting.entity.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal taxRate;

    /*
     * Calculated Fields
     */

    private BigDecimal lineSubtotal;

    private BigDecimal lineTax;

    private BigDecimal lineTotal;


    /*
     * Relationship
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

}