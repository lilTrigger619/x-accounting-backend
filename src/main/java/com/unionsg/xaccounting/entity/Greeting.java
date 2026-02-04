package com.unionsg.xaccounting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="Greeting")
public class Greeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique=true)
    private String email;

    /*
    protected User(){}

    public User(String name, String email){
        this.name = name;
        this.email = email;
    }
    * */
}
