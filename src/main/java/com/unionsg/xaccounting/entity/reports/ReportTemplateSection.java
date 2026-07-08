package com.unionsg.xaccounting.entity.reports;

import com.unionsg.xaccounting.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "report_template_sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_template_section_code", columnNames = {"report_template_id", "section_code"})
        },
        indexes = {
                @Index(name = "idx_rt_sections_template", columnList = "report_template_id"),
                @Index(name = "idx_rt_sections_parent", columnList = "parent_section_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_template_id", nullable = false)
    private ReportTemplate reportTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_section_id")
    private ReportTemplateSection parentSection;

    @Column(name = "section_code", nullable = false, length = 200)
    private String sectionCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private SectionType sectionType;

    @Column(length = 2000)
    private String formula;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "expanded_by_default", nullable = false)
    private boolean expandedByDefault;
}

