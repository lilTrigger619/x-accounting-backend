package com.unionsg.xaccounting.dto.settings;

import com.unionsg.xaccounting.enums.settings.SettingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAuditLogResponse {
    private Long id;
    private SettingType settingType;
    private String settingKey;
    private String previousValue;
    private String newValue;
    private UUID changedById;
    private String changedByName;
    private LocalDateTime changedAt;
    private String reason;
}
