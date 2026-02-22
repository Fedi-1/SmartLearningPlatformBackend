package com.example.SmartLearningPlatformBackend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "administrators")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Administrator extends User {
    // No additional fields as per UML diagram
}
