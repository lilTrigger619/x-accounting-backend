package com.unionsg.xaccounting.entity;

import com.unionsg.xaccounting.enums.TaxCategoryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A tax rate (VAT, sales tax, withholding tax) that products and documents can carry.
 * Authority, filing frequency, applies-to, transaction type and tax group hold configuration
 * values (see {@code ConfigSeedData}). The newer columns are nullable so existing rows load.
 */
@Entity
@Table(name = "tax_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxCategoryType type;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(length = 500)
    private String description;

    private String authority;

    private String registrationNumber;

    private String filingFrequency;

    private String reportingCode;

    private String appliesTo;

    private String transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    private AccountEntity linkedAccount;

    private String taxGroup;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    /** Null on rows created before the flag existed; treated as active. */
    private Boolean active;

    private Boolean compound;

    private Boolean recoverable;

    /** When true, new products start with this tax rate. At most one rate has it. */
    private Boolean defaultForNewItems;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isActiveRate() {
        return !Boolean.FALSE.equals(active);
    }
}
