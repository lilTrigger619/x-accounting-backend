package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "model_has_permissions")
public class ModelHasPermission {

    @EmbeddedId
    private ModelPermissionId id;

    @ManyToOne
    @JoinColumn(name = "permission_id")
    private Permission permission;

}