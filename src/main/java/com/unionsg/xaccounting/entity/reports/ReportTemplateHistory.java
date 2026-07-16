package com.unionsg.xaccounting.entity.reports;

import com.unionsg.xaccounting.enums.ReportTemplateHistoryAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "report_template_histories",
        indexes = {
                @Index(name = "idx_rth_template_id_performed_at", columnList = "template_id, performed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ReportTemplateHistoryAction action;

    @Column(name = "performed_by", nullable = false, length = 200)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Lob
    @Column(name = "metadata")
    private String metadata;
}

