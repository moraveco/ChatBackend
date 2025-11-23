package com.moraveco.springboot.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String token;
    
    @OneToOne
    private Login user;
    
    private LocalDateTime expiryDate;
    
    public PasswordResetToken() {
        this.expiryDate = LocalDateTime.now().plusHours(1); // Shorter expiry
    }
    
    // Getters and setters
}