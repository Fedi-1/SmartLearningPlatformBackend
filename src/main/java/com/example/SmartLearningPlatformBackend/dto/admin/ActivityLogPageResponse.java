package com.example.SmartLearningPlatformBackend.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActivityLogPageResponse {
    private List<ActivityLogItem> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
