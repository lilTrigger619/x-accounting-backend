package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.SetupCompletenessResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.settings.SetupCompletenessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/setup-status")
@RequiredArgsConstructor
public class SetupCompletenessController {

    private final SetupCompletenessService setupCompletenessService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<SetupCompletenessResponse> check() {
        return ResponseEntity.ok(setupCompletenessService.check());
    }
}
