// src/main/java/.../dto/admin/CategoryDistributionPoint.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryDistributionPoint {
    private String category;
    private long count;
}
