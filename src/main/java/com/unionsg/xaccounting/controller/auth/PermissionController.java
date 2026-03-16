package com.unionsg.xaccounting.controller.auth;

import com.unionsg.xaccounting.dto.auth.permission.PermissionResponse;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.auth.PermissionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionQueryService permissionQueryService;

    public PermissionController(PermissionQueryService permissionQueryService) {
        this.permissionQueryService = permissionQueryService;
    }

    // GET /permissions
    @GetMapping
    @RequirePermission(value = "view_role", group = "Role Management")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> permissions = permissionQueryService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
        .success(true)
        .message("Permissions retrieved successfully")
        .content(permissions)
        .build());
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "view_role", group = "Role Management")
    public ResponseEntity<ApiResponse<PermissionResponse>> togglePermissionStatus(@PathVariable("id") Long id) {
        PermissionResponse permission = permissionQueryService.togglePermissionStatus(id);
        String message = permission.getStatus().equals("ACTIVE") 
                ? "Permission activated successfully" 
                : "Permission disabled successfully";
        return ResponseEntity.ok(ApiResponse.<PermissionResponse>builder()
                .success(true)
                .message(message)
                .content(permission)
                .build());
    }


}