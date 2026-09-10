package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.entity.AccountEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "recurring_journal_template_lines")
@Getter
@Setter
public class RecurringJournalTemplateLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private RecurringJournalTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(length = 500)
    private String description;

    @Column(name = "debit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal creditAmount = BigDecimal.ZERO;
}
