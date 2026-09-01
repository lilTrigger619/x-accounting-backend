package com.unionsg.xaccounting.entity.bill;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.DiscountType;
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
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String billNumber;

    /*
     * The supplier's own reference/number for this bill (their invoice number to us)
     */
    private String supplierReference;

    @Column(nullable = false)
    private LocalDate billDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private BillStatus status = BillStatus.DRAFT;

    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    /*
     * Relationships
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

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

    private BigDecimal amountPaid = BigDecimal.ZERO;

    private BigDecimal balance = BigDecimal.ZERO;

    /*
     * Line Items
     */

    @OneToMany(
            mappedBy = "bill",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BillItem> items = new ArrayList<>();

    /*
     * Audit Fields
     */

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

}
