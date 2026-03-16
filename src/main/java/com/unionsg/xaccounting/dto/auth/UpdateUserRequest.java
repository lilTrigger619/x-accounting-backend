package com.unionsg.xaccounting.dto.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private Set<Long> roleIds;
    private Set<Long> permissionIds;
}