package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.AccountingMappingResponse;
import com.unionsg.xaccounting.dto.settings.UpdateAccountingMappingRequest;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Central "Automatic Accounting Configuration" screen (Settings & Setup §8): every GL account
 * an automated posting engine resolves at transaction time, in one editable table.
 */
@RestController
@RequestMapping("/api/settings/accounting-mappings")
@RequiredArgsConstructor
public class AccountingMappingController {

    private final AccountingMappingService accountingMappingService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<List<AccountingMappingResponse>> list() {
        return ResponseEntity.ok(accountingMappingService.list());
    }

    @PutMapping("/{mappingKey}")
    @RequirePermission(value = "manage_accounting_mappings", group = "Settings")
    public ResponseEntity<AccountingMappingResponse> update(
            @PathVariable MappingKey mappingKey,
            @RequestBody UpdateAccountingMappingRequest request) {
        return ResponseEntity.ok(
                accountingMappingService.update(mappingKey, request.getAccountCode(), request.getReason()));
    }
}
