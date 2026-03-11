package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import java.io.Serializable;

@Embeddable
public class RolePermissionId implements Serializable {

    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "role_id")
    private Long roleId;

}