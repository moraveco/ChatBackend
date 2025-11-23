package com.moraveco.springboot.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "login")
@Getter
@Setter
public class Login {
    @Id
    @Column(length = 100, nullable = false, unique = true)
    private String id;

    private String email; // acts as username

    private String password; // hashed password
    private Boolean emailVerified;

}
