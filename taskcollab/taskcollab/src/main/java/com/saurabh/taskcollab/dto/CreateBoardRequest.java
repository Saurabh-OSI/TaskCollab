package com.saurabh.taskcollab.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardRequest(
        @NotBlank(message = "Board name is required")
        String name
) {
}
