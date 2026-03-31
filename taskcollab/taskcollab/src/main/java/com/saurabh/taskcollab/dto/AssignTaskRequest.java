package com.saurabh.taskcollab.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(
        @NotNull(message = "User is required")
        Long userId
) {
}
