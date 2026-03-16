package com.unionsg.xaccounting.controller.auth;

import com.unionsg.xaccounting.dto.auth.CreateUserRequest;
import com.unionsg.xaccounting.dto.auth.UpdateUserRequest;
import com.unionsg.xaccounting.dto.auth.UserResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.auth.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.unionsg.xaccounting.response.ApiResponse;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
        System.out.println("UserController initialized with UserService: " + userService);
    }

    // POST /users
    @PostMapping
    @RequirePermission(value = "create_user", group = "User Management")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // GET /users/{id}
    @GetMapping("/{id}")
    @RequirePermission(value = "view_user", group = "User Management")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // GET /users?page=0&size=20&sort=firstName,asc
    @GetMapping
    @RequirePermission(value = "view_users", group = "User Management")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "firstName") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    // PUT /users/{id}
    @PutMapping("/{id}")
    @RequirePermission(value = "update_user", group = "User Management")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // DELETE /users/{id}  (soft delete — sets status = DISABLED)
    @DeleteMapping("/{id}")
    @RequirePermission(value = "delete_user", group = "User Management")
    public ResponseEntity<ApiResponse<Object>> toggleUserStatus(@PathVariable("id") Long id) {
        UserResponse user = userService.toggleUserStatus(id);
        String message = user.getStatus().equals("ACTIVE") ? "User activated successfully" : "User disabled successfully";
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message(message)
                .content(user)
                .build()
        );
    }
}