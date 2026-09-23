package com.unionsg.xaccounting.communication.controller;

import com.unionsg.xaccounting.communication.dto.MailConfigurationRequest;
import com.unionsg.xaccounting.communication.dto.MailConfigurationResponse;
import com.unionsg.xaccounting.communication.service.MailConfigurationService;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Settings & Setup §26 (Email Settings) - these endpoints carry SMTP host/credentials, so every
 * one of them requires an explicit Settings permission (previously wide open to any authenticated
 * user, unlike every other Settings controller in this package).
 */
@RestController
@RequestMapping("/api/mail-configurations")
@RequiredArgsConstructor
public class MailConfigurationController {

    private final MailConfigurationService mailConfigurationService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<List<MailConfigurationResponse>> list() {
        return ResponseEntity.ok(mailConfigurationService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<MailConfigurationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(mailConfigurationService.getById(id));
    }

    @PostMapping
    @RequirePermission(value = "manage_mail_configuration", group = "Settings")
    public ResponseEntity<MailConfigurationResponse> create(@Valid @RequestBody MailConfigurationRequest request) {
        return ResponseEntity.ok(mailConfigurationService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_mail_configuration", group = "Settings")
    public ResponseEntity<MailConfigurationResponse> update(@PathVariable Long id, @Valid @RequestBody MailConfigurationRequest request) {
        return ResponseEntity.ok(mailConfigurationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_mail_configuration", group = "Settings")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mailConfigurationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
