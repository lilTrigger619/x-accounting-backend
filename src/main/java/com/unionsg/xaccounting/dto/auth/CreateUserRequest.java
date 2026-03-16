package com.unionsg.xaccounting.dto.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class CreateUserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    private Set<Long> roleIds = new HashSet<>();
    private Set<Long> permissionIds = new HashSet<>();

    // Force Jackson to deserialize as Long not Integer
    public void setRoleIds(Set<Object> ids) {
        this.roleIds = ids == null ? new HashSet<>() :
            ids.stream().map(id -> Long.parseLong(id.toString())).collect(Collectors.toSet());
    }

    public void setPermissionIds(Set<Object> ids) {
        this.permissionIds = ids == null ? new HashSet<>() :
            ids.stream().map(id -> Long.parseLong(id.toString())).collect(Collectors.toSet());
    }
}