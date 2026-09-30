package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "valve_groups")
public class ValveGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    private String description;
    private boolean active = true;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
