package com.saurabh.taskcollab.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateListRequest(
        @NotBlank(message = "List name is required")
        String name
) {
}
