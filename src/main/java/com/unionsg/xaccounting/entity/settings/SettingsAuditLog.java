package com.unionsg.xaccounting.entity.settings;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.settings.SettingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for every material change to a financial setting - an
 * Accounting Mapping or the Organization profile (Settings & Setup §42). Never updated
 * once written. Deliberately narrower than "every settings change": cosmetic settings
 * (theme, page size, etc.) don't carry financial risk and aren't logged here.
 */
@Entity
@Table(name = "settings_audit_logs", indexes = {
        @Index(name = "idx_settings_audit_type_key", columnList = "setting_type, setting_key")
})
@Getter
@Setter
public class SettingsAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false, length = 30)
    private SettingType settingType;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "previous_value", length = 500)
    private String previousValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
