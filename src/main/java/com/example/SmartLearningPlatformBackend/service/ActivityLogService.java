// c:\Users\firas\Desktop\PFE Project\SmartLearningPlatformBackend\src\main\java\com\example\SmartLearningPlatformBackend\service\ActivityLogService.java
package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.models.ActivityLog;
import com.example.SmartLearningPlatformBackend.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(Long userId, ActionType action, String entityType, Long entityId) {
        activityLogRepository.save(ActivityLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .build());
    }
}
