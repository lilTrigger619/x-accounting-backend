package com.unionsg.xaccounting.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private List<RoleResponse> roles;
    private Set<String> directPermissions;
}