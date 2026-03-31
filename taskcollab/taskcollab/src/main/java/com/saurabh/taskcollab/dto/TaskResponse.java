package com.saurabh.taskcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Integer position;
    private Long listId;
    private String listName;
    private UserSummaryResponse createdBy;
    private List<UserSummaryResponse> assignees;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
