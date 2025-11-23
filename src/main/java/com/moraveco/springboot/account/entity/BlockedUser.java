package com.moraveco.springboot.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Setter
@Getter
@Entity
@Table(name = "blocked_users")
public class BlockedUser {
    @Id
    @Column(length = 100, nullable = false, unique = true)
    private String id;
    @Column(nullable = false, length = 100)
    private String blocker_id;
    @Column(nullable = false, length = 100)
    private String blocked_id;
    private Timestamp blocked_at;
}
