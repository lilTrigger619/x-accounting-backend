package com.unionsg.xaccounting.entity.User;


import com.unionsg.xaccounting.entity.AuditableBase;
import com.unionsg.xaccounting.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
//import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET status = 'DISABLED' WHERE id = ?")
//@Where(clause = "status <> 'DISABLED'")
public class User extends AuditableBase {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private Instant lastLoginAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}