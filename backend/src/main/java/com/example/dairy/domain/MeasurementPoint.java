package com.example.dairy.domain;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="measurement_points")
public class MeasurementPoint {
 @Id private UUID id;
 @Column(nullable=false,unique=true) private String code;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String unit="bar";
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public String getCode(){return code;} public void setCode(String v){code=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getUnit(){return unit;} public void setUnit(String v){unit=v;}
}
