package com.saurabh.taskcollab.dto;

public record BoardSummaryResponse(
        Long id,
        String name,
        String role,
        long memberCount
) {
}
