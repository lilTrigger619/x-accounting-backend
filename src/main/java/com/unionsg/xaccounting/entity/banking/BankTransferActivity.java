package com.unionsg.xaccounting.entity.banking;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.banking.BankTransferAction;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Append-only history of a bank transfer. Who and when come from {@link BaseEntity}'s
 * createdBy/createdAt.
 */
@Entity
@Getter
@Setter
@Table(
        name = "bank_transfer_activities",
        indexes = @Index(name = "idx_bank_transfer_activity_transfer", columnList = "bank_transfer_id")
)
public class BankTransferActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_transfer_id", nullable = false, updatable = false)
    private BankTransfer bankTransfer;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30, updatable = false)
    private BankTransferAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private BankTransferStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, updatable = false)
    private BankTransferStatus toStatus;

    @Column(name = "details", length = 1000, updatable = false)
    private String details;
}
