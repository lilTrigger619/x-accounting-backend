package com.unionsg.xaccounting.communication.controller;

import com.unionsg.xaccounting.communication.dto.MailConfigurationRequest;
import com.unionsg.xaccounting.communication.dto.MailConfigurationResponse;
import com.unionsg.xaccounting.communication.service.MailConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail-configurations")
@RequiredArgsConstructor
public class MailConfigurationController {

    private final MailConfigurationService mailConfigurationService;

    @GetMapping
    public ResponseEntity<List<MailConfigurationResponse>> list() {
        return ResponseEntity.ok(mailConfigurationService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MailConfigurationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(mailConfigurationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MailConfigurationResponse> create(@Valid @RequestBody MailConfigurationRequest request) {
        return ResponseEntity.ok(mailConfigurationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MailConfigurationResponse> update(@PathVariable Long id, @Valid @RequestBody MailConfigurationRequest request) {
        return ResponseEntity.ok(mailConfigurationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mailConfigurationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
