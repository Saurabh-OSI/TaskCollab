package com.saurabh.taskcollab.dto;

import java.util.List;

public record BoardMetaResponse(
        Long id,
        String name,
        List<BoardMemberResponse> members
) {
}
