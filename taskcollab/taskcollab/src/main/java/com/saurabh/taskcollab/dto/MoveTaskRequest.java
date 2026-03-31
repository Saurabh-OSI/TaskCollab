package com.saurabh.taskcollab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveTaskRequest {

    @NotNull(message = "Target list is required")
    private Long targetListId;

    private Integer targetPosition;
}
