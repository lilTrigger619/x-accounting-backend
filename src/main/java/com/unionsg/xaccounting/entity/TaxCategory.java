package com.unionsg.xaccounting.entity;

import com.unionsg.xaccounting.enums.TaxCategoryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxCategoryType type;

    @Column(nullable = false)
    private BigDecimal rate;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;
}
