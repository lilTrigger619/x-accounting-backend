package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import java.io.Serializable;


@Embeddable
public class ModelPermissionId implements Serializable {

    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_type")
    private String modelType;

}