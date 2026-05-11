package com.unionsg.xaccounting.dto.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private Set<UUID> roleIds;
    private Set<UUID> permissionIds;
}