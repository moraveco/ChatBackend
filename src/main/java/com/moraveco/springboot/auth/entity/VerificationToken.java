package com.moraveco.springboot.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String token;

    @OneToOne
    private Login user;

    private LocalDateTime expiryDate;

    public VerificationToken() {
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    // Getters and setters
}
