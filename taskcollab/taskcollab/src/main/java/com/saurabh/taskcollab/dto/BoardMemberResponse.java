package com.saurabh.taskcollab.dto;

public record BoardMemberResponse(
        Long userId,
        String name,
        String email,
        String role
) {
}
