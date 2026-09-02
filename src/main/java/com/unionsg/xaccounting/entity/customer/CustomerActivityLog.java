package com.unionsg.xaccounting.entity.customer;

import com.unionsg.xaccounting.enums.CustomerActivityReferenceType;
import com.unionsg.xaccounting.enums.CustomerActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single entry in a customer's activity feed — status changes, invoices,
 * payments and emails sent, all in one timeline so the customer view screen
 * can show "everything that happened" and deep-link into the source record.
 */
@Entity
@Table(
        name = "customer_activity_logs",
        indexes = {
                @Index(name = "idx_customer_activity_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_activity_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerActivityType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "reference_type", nullable = false, length = 20)
    private CustomerActivityReferenceType referenceType = CustomerActivityReferenceType.NONE;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 150)
    private String actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
