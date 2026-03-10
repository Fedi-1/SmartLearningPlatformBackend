// src/main/java/.../dto/admin/RecentActivityEntry.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecentActivityEntry {
    private String studentName;
    private String action;
    private String timestamp;
}
