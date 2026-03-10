// src/main/java/.../dto/admin/StudentCourseItem.java
package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseItem {
    private Long id;
    private String title;
    private String category;
    private int lessonsCompleted;
    private int totalLessons;
    private Boolean examPassed;
    private Integer examScore;
}
