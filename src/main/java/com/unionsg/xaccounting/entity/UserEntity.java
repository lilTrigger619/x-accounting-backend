package com.unionsg.xaccounting.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(nullable = true, unique = false)
//    private String first_name;

//    @Column(nullable = true, unique = false)
//    private String last_name;

//    @Column(nullable = false, unique =true)
    //private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
}
