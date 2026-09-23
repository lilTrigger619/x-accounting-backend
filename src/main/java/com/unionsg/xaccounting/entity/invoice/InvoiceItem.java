package com.unionsg.xaccounting.entity.invoice;

import com.unionsg.xaccounting.entity.product.Product;
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

    /** The catalog item this line was billed for, if any (Settings & Setup §17 - free-text lines are still allowed). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

}