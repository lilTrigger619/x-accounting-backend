package com.unionsg.xaccounting.entity.Journals;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
//@Table(name="journal_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_entry_number", columnList = "journal_number"),
                @Index(name = "idx_journal_entry_date", columnList = "journal_date"),
                @Index(name = "idx_journal_entry_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_journal_number", columnNames = "journal_number")
        }
)
public class JournalEntry extends BaseEntity {

    @Column(name = "journal_number", nullable = false, length = 50)
    private String journalNumber;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 30)
    private JournalType journalType = JournalType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JournalStatus status = JournalStatus.DRAFT;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "is_system_generated", nullable = false)
    private Boolean systemGenerated = false;

    @Column(name = "is_adjustment", nullable = false)
    private Boolean adjustmentEntry = false;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "GHS";

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "total_debit", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "source_module", length = 50)
    private String sourceModule;

    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    @Column(name = "reversal_of_journal_id")
    private Long reversalOfJournalId;

    @OneToMany(
            mappedBy = "journalEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("lineNumber ASC")
    private List<JournalLine> lines = new ArrayList<>();

    // ===== Convenience Methods =====

    public void addLine(JournalLine line) {
        line.setJournalEntry(this);
        this.lines.add(line);
    }

    public void removeLine(JournalLine line) {
        line.setJournalEntry(null);
        this.lines.remove(line);
    }


}
