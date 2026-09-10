package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.RecurringJournalFrequency;
import com.unionsg.xaccounting.enums.RecurringJournalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A template describing a recurring accounting transaction (e.g. monthly rent,
 * depreciation, accruals). The template itself is never a posted transaction —
 * each due occurrence generates its own real JournalEntry.
 */
@Entity
@Table(name = "recurring_journal_templates")
@Getter
@Setter
public class RecurringJournalTemplate extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalType journalType = JournalType.ADJUSTMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringJournalFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "occurrences_generated", nullable = false)
    private int occurrencesGenerated = 0;

    /** Null once the schedule has run its course (end date / occurrence cap reached). */
    @Column(name = "next_run_date")
    private LocalDate nextRunDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringJournalStatus status = RecurringJournalStatus.ACTIVE;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecurringJournalTemplateLine> lines = new ArrayList<>();

    @PrePersist
    protected void initNextRunDate() {
        if (nextRunDate == null) {
            nextRunDate = startDate;
        }
    }

    public void addLine(RecurringJournalTemplateLine line) {
        line.setTemplate(this);
        this.lines.add(line);
    }
}
