package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.SettingsAuditLogResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.settings.SettingsAuditLog;
import com.unionsg.xaccounting.enums.settings.SettingType;
import com.unionsg.xaccounting.repository.settings.SettingsAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Records financial-setting changes in their own transaction, so a failure to write the
 * audit row can never roll back (or be rolled back by) the settings change it documents.
 */
@Service
@RequiredArgsConstructor
public class SettingsAuditLogService {

    private final SettingsAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SettingType settingType, String settingKey, String previousValue, String newValue, String reason) {
        SettingsAuditLog log = new SettingsAuditLog();
        log.setSettingType(settingType);
        log.setSettingKey(settingKey);
        log.setPreviousValue(previousValue);
        log.setNewValue(newValue);
        log.setChangedBy(SecurityUtils.getCurrentUser());
        log.setChangedAt(LocalDateTime.now());
        log.setReason(reason);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<SettingsAuditLogResponse> getHistory(SettingType settingType, String settingKey) {
        return repository.findBySettingTypeAndSettingKeyOrderByChangedAtDesc(settingType, settingKey)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SettingsAuditLogResponse> getAllHistory() {
        return repository.findAllByOrderByChangedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SettingsAuditLogResponse toResponse(SettingsAuditLog log) {
        User user = log.getChangedBy();
        return SettingsAuditLogResponse.builder()
                .id(log.getId())
                .settingType(log.getSettingType())
                .settingKey(log.getSettingKey())
                .previousValue(log.getPreviousValue())
                .newValue(log.getNewValue())
                .changedById(user != null ? user.getId() : null)
                .changedByName(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "System")
                .changedAt(log.getChangedAt())
                .reason(log.getReason())
                .build();
    }
}
