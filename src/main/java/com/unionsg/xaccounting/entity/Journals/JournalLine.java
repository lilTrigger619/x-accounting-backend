package com.unionsg.xaccounting.entity.Journals;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(
        name = "journal_lines",
        indexes = {
                @Index(name = "idx_journal_line_journal", columnList = "journal_entry_id"),
                @Index(name = "idx_journal_line_account", columnList = "account_id"),
                @Index(name = "idx_journal_line_branch", columnList = "branch_id"),
                @Index(name = "idx_journal_line_cost_center", columnList = "cost_center_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalLine extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "debit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "credit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;


    /** Always the cost center's branch; stored so per-branch checks and reports skip a join. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    /** On an inter-branch line: the other branch this branch is owed by (debit) or owes (credit). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_branch_id")
    private Branch counterpartyBranch;

    /** System-generated due to / due from line added by the inter-branch balancer. */
    @Builder.Default
    @Column(name = "is_inter_branch", nullable = false, columnDefinition = "boolean default false not null")
    private Boolean interBranch = false;

    /** Document ID of one of the journal's attachments that supports this line, e.g. "DOC-2". */
    @Column(name = "document_ref", length = 20)
    private String documentRef;

    // Optional tags, only accepted while subledger journals are turned on in Branch Accounting
    // settings. They label the line for reporting; they don't change the bill, invoice or
    // payroll records they name.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    /** Vendor (supplier). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** The vendor bill this line settles or adjusts. At most one of bill and invoice is set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_bill_id")
    private Bill allocatedBill;

    /** The customer invoice this line settles or adjusts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_invoice_id")
    private Invoice allocatedInvoice;
}
