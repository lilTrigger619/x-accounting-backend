package com.unionsg.xaccounting.entity.reports;

import com.unionsg.xaccounting.entity.AccountEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "report_section_accounts",
        indexes = {
                @Index(name = "idx_report_section_accounts_section", columnList = "report_section_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_section_account", columnNames = {"report_section_id", "account_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSectionAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_section_id", nullable = false)
    private ReportSection reportSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

}

