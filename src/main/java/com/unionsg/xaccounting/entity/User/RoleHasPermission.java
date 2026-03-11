package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;

@Entity
@Table(name = "role_has_permissions")
public class RoleHasPermission {

    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

}