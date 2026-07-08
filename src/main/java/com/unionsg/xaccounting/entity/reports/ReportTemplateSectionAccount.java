package com.unionsg.xaccounting.entity.reports;

import com.unionsg.xaccounting.entity.AccountEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "report_template_section_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rt_section_account", columnNames = {"report_template_section_id", "account_id"})
        },
        indexes = {
                @Index(name = "idx_rt_section_accounts_section", columnList = "report_template_section_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateSectionAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_template_section_id", nullable = false)
    private ReportTemplateSection reportTemplateSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}

