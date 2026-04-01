package com.saurabh.taskcollab.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderListRequest {

    @NotEmpty(message = "List order is required")
    private List<Long> listIds;
}
