package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "model_has_roles")
public class ModelHasRole {

    @EmbeddedId
    private ModelRoleId id;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

}