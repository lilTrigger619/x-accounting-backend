package com.unionsg.xaccounting.entity.User;


import jakarta.persistence.*;
import java.io.Serializable;

@Embeddable
public class ModelRoleId implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_type")
    private String modelType;

}