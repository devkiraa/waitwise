package com.example.waitwise.models;

import java.io.Serializable;

public class User implements Serializable {
    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_ADMIN = "admin";

    private String id;
    private String name;
    private String email;
    private String role = ROLE_CUSTOMER;
    private long createdAt;

    public User() {
    }

    public User(String id, String name, String email, long createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = ROLE_CUSTOMER;
        this.createdAt = createdAt;
    }

    public User(String id, String name, String email, String role, long createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role != null ? role : ROLE_CUSTOMER;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role != null ? role : ROLE_CUSTOMER;
    }

    public void setRole(String role) {
        this.role = role != null ? role : ROLE_CUSTOMER;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(this.role);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
