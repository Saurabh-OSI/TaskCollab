package com.saurabh.taskcollab.dto;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        Long id,
        Long boardId,
        String action,
        String entityType,
        Long entityId,
        String description,
        UserSummaryResponse user,
        LocalDateTime createdAt
) {
}
