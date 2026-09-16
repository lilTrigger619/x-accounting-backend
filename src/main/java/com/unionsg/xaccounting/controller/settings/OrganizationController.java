package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.OrganizationResponse;
import com.unionsg.xaccounting.dto.settings.UpdateOrganizationRequest;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.settings.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<OrganizationResponse> get() {
        return ResponseEntity.ok(organizationService.get());
    }

    @PutMapping
    @RequirePermission(value = "manage_organization", group = "Settings")
    public ResponseEntity<OrganizationResponse> update(@RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(request));
    }
}
