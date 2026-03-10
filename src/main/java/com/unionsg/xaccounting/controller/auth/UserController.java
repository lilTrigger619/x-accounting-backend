package com.unionsg.xaccounting.controller.auth;

//package com.yourapp.user.controller;

import com.unionsg.xaccounting.dto.auth.CreateUserRequest;
import com.unionsg.xaccounting.dto.auth.UserResponse;
import com.unionsg.xaccounting.service.auth.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}