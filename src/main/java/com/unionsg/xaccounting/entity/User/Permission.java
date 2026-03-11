package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(
    name = "permissions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
    }
)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "permission")
    private Set<RoleHasPermission> rolePermissions;

}