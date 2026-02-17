package com.unionsg.xaccounting.entity.customer;
import com.unionsg.xaccounting.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "customers",
        uniqueConstraints =  {
                @UniqueConstraint(name = "uk_customer_code", columnNames = "customer_code"),
                @UniqueConstraint(name = "uk_display_name", columnNames = "display_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // -----------------
    // Identity
    // ------------------

    @Column(name = "customer_code", nullable = false, updatable = false)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;


    @Enumerated(EnumType.STRING)
    private Title title;


   @Column(name = "first_name")
   private String firstName;

   @Column(name = "last_name")
    private String lastName;

   @Column(name = "company_name")
    private String companyName;

   @Column(name = "display_name", nullable = false)
    private String displayName;

   @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;


   // ---------------------
    // Contact info
    //...................

    private String email;
    private String phone;
    private String mobile;
    private String website;

    // ----------------
    // Relationships
    // ............

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tax_info_id")
    private TaxInfo taxInfo;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "payment_terms_id")
    private PaymentTerms paymentTerms;

    // ---------------------------
    // Audit
    // --------------------------

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    // -----------------------
    // Lifecycle Hooks
    // ------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
