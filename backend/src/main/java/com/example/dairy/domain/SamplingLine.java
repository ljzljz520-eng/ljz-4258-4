package com.example.dairy.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "sampling_lines")
public class SamplingLine {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="valve_group_id") private ValveGroup valveGroup;
    @Column(nullable=false, unique=true) private String code;
    @Column(nullable=false) private String name;
    private boolean active = true;
    @Column(name="created_at") private Instant createdAt = Instant.now();
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public ValveGroup getValveGroup(){return valveGroup;} public void setValveGroup(ValveGroup v){valveGroup=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
