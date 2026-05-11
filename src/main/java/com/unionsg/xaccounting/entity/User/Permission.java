package com.unionsg.xaccounting.entity.User;

import com.unionsg.xaccounting.enums.PermissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue
    private UUID id;

    private String name;

    private String guardName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionStatus status = PermissionStatus.ACTIVE;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;

    @ManyToMany(mappedBy = "permissions")
    private Set<User> users;
}