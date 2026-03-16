package com.unionsg.xaccounting.controller.auth;

import com.unionsg.xaccounting.dto.auth.role.CreateRoleRequest;
import com.unionsg.xaccounting.dto.auth.role.RoleDetailResponse;
import com.unionsg.xaccounting.dto.auth.role.UpdateRoleRequest;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.auth.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // GET /roles
    @GetMapping
    @RequirePermission(value = "view_role", group = "Role Management")
    public ResponseEntity<ApiResponse<List<RoleDetailResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.<List<RoleDetailResponse>>builder()
                .success(true)
                .message("Roles retrieved successfully")
                .content(roleService.getAllRoles())
                .build());
    }

    // GET /roles/{id}
    @GetMapping("/{id}")
    @RequirePermission(value = "view_role", group = "Role Management")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.<RoleDetailResponse>builder()
                .success(true)
                .message("Role retrieved successfully")
                .content(roleService.getRoleById(id))
                .build());
    }

    // POST /roles
    @PostMapping
    @RequirePermission(value = "create_role", group = "Role Management")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> createRole(@RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<RoleDetailResponse>builder()
                .success(true)
                .message("Role created successfully")
                .content(roleService.createRole(request))
                .build());
    }

    // PUT /roles/{id}
    @PutMapping("/{id}")
    @RequirePermission(value = "edit_role", group = "Role Management")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> updateRole(
            @PathVariable("id") Long id,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleDetailResponse>builder()
                .success(true)
                .message("Role updated successfully")
                .content(roleService.updateRole(id, request))
                .build());
    }

    // DELETE /roles/{id} — soft delete (disables role)
    @DeleteMapping("/{id}")
    @RequirePermission(value = "delete_role", group = "Role Management")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> toggleRoleStatus(@PathVariable("id") Long id) {
        RoleDetailResponse role = roleService.toggleRoleStatus(id);
        String message = role.getStatus().equals("ACTIVE") ? "Role activated successfully" : "Role disabled successfully";
        return ResponseEntity.ok(ApiResponse.<RoleDetailResponse>builder()
                .success(true)
                .message(message)
                .content(role)
                .build());
    }
}