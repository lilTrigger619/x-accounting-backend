package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.SettingsAuditLogResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.settings.SettingsAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read surface for {@link SettingsAuditLogService} (Settings & Setup §42) - who changed an
 * Accounting Mapping or the Organization profile, what it was before/after, and why.
 */
@RestController
@RequestMapping("/api/settings/audit-log")
@RequiredArgsConstructor
public class SettingsAuditLogController {

    private final SettingsAuditLogService settingsAuditLogService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<List<SettingsAuditLogResponse>> getAll() {
        return ResponseEntity.ok(settingsAuditLogService.getAllHistory());
    }
}
