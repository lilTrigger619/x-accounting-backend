package com.unionsg.xaccounting.entity.prepayment;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentLineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One period of a {@link Prepayment}'s recognition schedule (spec §7's amortization schedule). */
@Entity
@Table(name = "prepayment_amortization_lines")
@Getter
@Setter
public class PrepaymentAmortizationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepayment_id")
    private Prepayment prepayment;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrepaymentLineStatus status = PrepaymentLineStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    private LocalDateTime recognizedAt;
}
