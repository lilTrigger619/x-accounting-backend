package com.unionsg.xaccounting.repository.settings;

import com.unionsg.xaccounting.entity.settings.SettingsAuditLog;
import com.unionsg.xaccounting.enums.settings.SettingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettingsAuditLogRepository extends JpaRepository<SettingsAuditLog, Long> {

    List<SettingsAuditLog> findBySettingTypeAndSettingKeyOrderByChangedAtDesc(SettingType settingType, String settingKey);

    List<SettingsAuditLog> findAllByOrderByChangedAtDesc();
}
