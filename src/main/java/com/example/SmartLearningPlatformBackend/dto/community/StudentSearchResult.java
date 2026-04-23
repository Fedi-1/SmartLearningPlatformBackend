// src/main/java/com/example/SmartLearningPlatformBackend/dto/community/StudentSearchResult.java
package com.example.SmartLearningPlatformBackend.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSearchResult {

    private Long id;
    private String fullName;
    private String initials;
    private String email;
}
