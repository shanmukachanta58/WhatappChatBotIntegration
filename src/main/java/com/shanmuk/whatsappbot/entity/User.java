package com.shanmuk.whatsappbot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, unique = true)
    private String phoneNumber;

    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName() {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt( LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
