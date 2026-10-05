package com.unionsg.xaccounting.entity.deposit;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Configurable kind of deposit (Security Deposit, Utility Deposit, Tenant Deposit, Customer
 * Advance Deposit...). Fixes which direction it applies to and supplies the defaults a new
 * deposit of this type starts from: whether it is refundable, whether it bears interest, and
 * which balance-sheet account holds it and which P&amp;L account absorbs a forfeiture.
 */
@Entity
@Table(name = "deposit_types")
@Getter
@Setter
public class DepositType extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepositDirection direction;

    @Column(nullable = false)
    private Boolean refundableByDefault = true;

    @Column(nullable = false)
    private Boolean interestBearingByDefault = false;

    /** Asset (paid) or liability (received) account; overrides the direction's default mapping. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_account_id")
    private AccountEntity depositAccount;

    /** Expense (paid) or income (received) account hit when a deposit is forfeited. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forfeiture_account_id")
    private AccountEntity forfeitureAccount;

    @Column(nullable = false)
    private Boolean active = true;
}
