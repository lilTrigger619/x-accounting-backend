package com.unionsg.xaccounting.entity.invoice;

import com.unionsg.xaccounting.embeddables.InvoiceBillingInfo;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.enums.DiscountType;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import jakarta.persistence.*;
        import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    private String reference;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    /*
     * Relationships
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_terms_id")
    private PaymentTerms paymentTerms;

    @Embedded
    private InvoiceBillingInfo billingInfo;

    /*
     * Financial Fields
     */

    private BigDecimal subtotal = BigDecimal.ZERO;

    private BigDecimal totalTax = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType = DiscountType.NONE;

    private BigDecimal discountValue = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal totalDue = BigDecimal.ZERO;

    /*
     * Line Items
     */

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InvoiceItem> items = new ArrayList<>();

    /*
     * No file Attachments
     * the files are posted to the fileupload entity with this entity id (invoice id)
     * so we retrieve this entity's files by api call with the entity's id.
     *
     */
//
//    @OneToMany(
//            mappedBy = "invoice",
//            cascade = CascadeType.ALL,
//            orphanRemoval = true
//    )
//    private List<InvoiceAttachment> attachments = new ArrayList<>();
//

    /*
     * Audit Fields
     */

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;


}