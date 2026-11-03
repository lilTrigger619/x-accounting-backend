package com.unionsg.xaccounting.entity.Journals;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.JournalApprovalAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** One step of a journal's approval history; the acting user is {@code createdBy}. */
@Entity
@Table(name = "journal_approval_events", indexes = {
        @Index(name = "idx_journal_approval_journal", columnList = "journal_entry_id")
})
@Getter
@Setter
public class JournalApprovalEvent extends BaseEntity {

    @Column(name = "journal_entry_id", nullable = false)
    private Long journalEntryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalApprovalAction action;

    @Column(length = 500)
    private String comment;
}
