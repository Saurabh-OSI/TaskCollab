package com.saurabh.taskcollab.dto;

public record UserSummaryResponse(
        Long id,
        String name,
        String email
) {
}
