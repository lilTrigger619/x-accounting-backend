package com.unionsg.xaccounting.entity.product;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.enums.ProductItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Basic Info
     */

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductItemType itemType;

    private String category;

    private String costGroup;

    private String imageFileId;

    /*
     * Sales
     */

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_account_id")
    private AccountEntity incomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_id")
    private TaxCategory taxCategory;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
