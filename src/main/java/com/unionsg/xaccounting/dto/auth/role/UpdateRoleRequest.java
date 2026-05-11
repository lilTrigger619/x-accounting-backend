package com.unionsg.xaccounting.dto.auth.role;
 
import lombok.Getter;
import lombok.Setter;
 
import java.util.Set;

import java.util.UUID;

@Getter
@Setter
public class UpdateRoleRequest {
    private String name;
    private Set<UUID> permissionIds;
 
    public void setPermissionIds(Set<Object> ids) {
        if (ids == null) { this.permissionIds = null; return; }
        this.permissionIds = ids.stream()
                .map(id -> UUID.fromString(id.toString()))
                .collect(java.util.stream.Collectors.toSet());
    }
}