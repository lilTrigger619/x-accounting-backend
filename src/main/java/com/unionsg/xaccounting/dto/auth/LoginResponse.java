package com.unionsg.xaccounting.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private List<RoleResponse> roles;
    private Set<String> directPermissions;
}