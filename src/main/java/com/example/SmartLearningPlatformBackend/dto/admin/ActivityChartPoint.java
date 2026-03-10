// src/main/java/.../dto/admin/ActivityChartPoint.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivityChartPoint {
    private String date;
    private long count;
}
