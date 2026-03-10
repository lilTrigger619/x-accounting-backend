package com.unionsg.xaccounting.entity.supplier;
import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.customer.TaxInfo;
import com.unionsg.xaccounting.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table (
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supplier_code", columnNames = "supplier_code"),
                @UniqueConstraint(name = "uk_display_name", columnNames="display_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------
    // Identity
    // ------------

    @Column(name = "supplier_code", nullable = false, updatable = false)
    private String supplierCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false)
    private CustomerType supplierType;

//    @Enumerated(EnumType.STRING)
//    private Title title;

//    @Column(name = "first_name")
//    private String firstName;
//
//    @Column(name = "last_name")
//    private String lastName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "display_name", nullable =false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierCategory category;

    //--------------------
    // contact info
    // ------------------------

    private String email;
    private String phone;
    private String mobile;
    private String website;

    private String additionalInformation;


    // --
    // Relationships
    // -----------
//    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "billing_address_id")
//    private Address billingAddress;
//
//    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "shipping_address_id")
//    private Address shippingAddress;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Address address;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tax_info_id")
    private WithholdingTax taxInfo;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "payment_terms_id")
    private SupplierPaymentTerms paymentTerms;

    // --------------------
    // Audit
    // ----------------

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    // -------------
    // Lifecycle hooks
    // -----------------

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

}
