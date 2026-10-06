package com.unionsg.xaccounting.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A management alert's on/off switch and threshold, editable from the BI settings screen. */
@Entity
@Table(name = "analytics_alert_settings")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsAlertSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_key", nullable = false, unique = true, length = 60)
    private String alertKey;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(precision = 19, scale = 4)
    private BigDecimal threshold;

    @Column(name = "window_days")
    private Integer windowDays;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
