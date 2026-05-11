package com.unionsg.xaccounting.entity.User;

import jakarta.persistence.*;
import lombok.*;
import com.unionsg.xaccounting.enums.RoleStatus;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private String guardName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus status = RoleStatus.ACTIVE;

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
}