package com.unionsg.xaccounting.entity.reports;

import com.unionsg.xaccounting.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "report_sections",
        indexes = {
                @Index(name = "idx_report_sections_definition", columnList = "report_definition_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_definition_id", nullable = false)
    private ReportDefinition reportDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_section_id")
    private ReportSection parentSection;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private SectionType sectionType;

    @Column(length = 2000)
    private String formula;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    @PreUpdate
    protected void validate() {
        Objects.requireNonNull(reportDefinition, "reportDefinition must not be null");
        if (displayOrder == null) {
            throw new IllegalArgumentException("displayOrder must not be null");
        }
    }
}

