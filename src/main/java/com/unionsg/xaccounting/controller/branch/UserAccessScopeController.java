package com.unionsg.xaccounting.controller.branch;

import com.unionsg.xaccounting.dto.branch.UserAccessScopeRequest;
import com.unionsg.xaccounting.dto.branch.UserAccessScopeResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.branch.UserAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/access-scope")
@RequiredArgsConstructor
public class UserAccessScopeController {

    private final UserAccessScopeService service;

    @GetMapping
    @RequirePermission(value = "view_user", group = "User Management")
    public ResponseEntity<UserAccessScopeResponse> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.get(userId));
    }

    /** Replaces the user's whole scope: Global, or branches with all or some cost centers. */
    @PutMapping
    @RequirePermission(value = "manage_user_scopes", group = "User Management")
    public ResponseEntity<UserAccessScopeResponse> replace(@PathVariable UUID userId,
                                                           @RequestBody UserAccessScopeRequest request) {
        return ResponseEntity.ok(service.replace(userId, request));
    }
}
