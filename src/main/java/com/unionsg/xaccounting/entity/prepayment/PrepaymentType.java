package com.unionsg.xaccounting.entity.prepayment;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Configurable category of prepayment (e.g. Prepaid Insurance, Prepaid Rent, Software
 * Subscription) - purely descriptive master data, does not itself resolve a GL account. A
 * {@link Prepayment} still resolves its prepaid-asset/expense accounts through
 * {@code MappingKey.PREPAYMENT_DEFAULT_ASSET}/{@code PREPAYMENT_DEFAULT_EXPENSE} or its own
 * per-record override, the same override pattern already used by {@code Product.incomeAccount}.
 */
@Entity
@Table(name = "prepayment_types")
@Getter
@Setter
public class PrepaymentType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;
}
