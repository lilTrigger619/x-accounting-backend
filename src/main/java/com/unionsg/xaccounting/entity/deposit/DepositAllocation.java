package com.unionsg.xaccounting.entity.deposit;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.deposit.DepositAllocationType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One movement out of a deposit's balance: applied to an invoice or bill, refunded, forfeited,
 * or transferred to another deposit. Rows are never edited or deleted; a reversal flags the row
 * and posts an offsetting journal, so the history stays complete.
 */
@Entity
@Table(name = "deposit_allocations")
@Getter
@Setter
public class DepositAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_id")
    private Deposit deposit;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 30)
    private DepositAllocationType allocationType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    /** The new deposit a TRANSFER created. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_deposit_id")
    private Deposit targetDeposit;

    /** Bank/cash account a REFUND moved money through. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(length = 100)
    private String reference;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @Column(nullable = false)
    private Boolean reversed = false;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;
}
