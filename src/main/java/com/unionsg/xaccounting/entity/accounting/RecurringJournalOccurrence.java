package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.enums.RecurringOccurrenceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records one attempted/generated occurrence of a RecurringJournalTemplate,
 * so history, upcoming, pending and failed occurrences are all individually visible
 * and the same scheduled date can never be generated twice.
 */
@Entity
@Table(name = "recurring_journal_occurrences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recurring_occurrence", columnNames = {"template_id", "scheduled_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringJournalOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private RecurringJournalTemplate template;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringOccurrenceStatus status;

    @Column(name = "generated_journal_id")
    private Long generatedJournalId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
}
