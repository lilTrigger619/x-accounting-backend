package com.unionsg.xaccounting.repository.analytics;

import com.unionsg.xaccounting.entity.analytics.AnalyticsAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalyticsAlertSettingRepository extends JpaRepository<AnalyticsAlertSetting, Long> {
    Optional<AnalyticsAlertSetting> findByAlertKey(String alertKey);
}
