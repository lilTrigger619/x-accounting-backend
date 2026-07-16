package com.unionsg.xaccounting.entity.reports;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "report_template_draft_locks",
        indexes = {
                @Index(name = "idx_rt_dl_template_id", columnList = "template_id"),
                @Index(name = "idx_rt_dl_edit_session_id", columnList = "edit_session_id"),
                @Index(name = "uidx_rt_dl_template_id_unique_active", columnList = "template_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateDraftLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "edit_session_id", nullable = false, length = 100)
    private String editSessionId;

    @Column(name = "locked_by", nullable = false, length = 200)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "last_heartbeat", nullable = false)
    private LocalDateTime lastHeartbeat;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}

