package com.moraveco.springboot.auth.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")  // Add this annotation!
public class User {

    @Id
    @Column(length = 100, nullable = false, unique = true)
    private String id;

    @Column(nullable = false, length = 45)
    private String name;

    @Column(nullable = false, length = 45)
    private String lastname;

    @Column(name = "profile_image", length = 100)
    private String profileImage;

    // Getters and Setters
}
