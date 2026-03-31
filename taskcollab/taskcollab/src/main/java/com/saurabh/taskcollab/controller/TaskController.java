package com.saurabh.taskcollab.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.saurabh.taskcollab.dto.AssignTaskRequest;
import com.saurabh.taskcollab.dto.CreateTaskRequest;
import com.saurabh.taskcollab.dto.MoveTaskRequest;
import com.saurabh.taskcollab.dto.PageResponse;
import com.saurabh.taskcollab.dto.TaskResponse;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.Task;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.service.BoardAccessService;
import com.saurabh.taskcollab.service.CurrentUserService;
import com.saurabh.taskcollab.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;
    private final BoardAccessService boardAccessService;

    public TaskController(TaskService taskService,
                          CurrentUserService currentUserService,
                          BoardAccessService boardAccessService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
        this.boardAccessService = boardAccessService;
    }

    @PostMapping("/{listId}")
    public TaskResponse createTask(@PathVariable Long listId,
                                   @Valid @RequestBody CreateTaskRequest request) {
        User user = currentUserService.getCurrentUser();
        boardAccessService.getAccessibleList(listId, user);
        return taskService.createTask(listId, request, user);
    }

    @GetMapping("/{listId}")
    public List<TaskResponse> getTasks(@PathVariable Long listId) {
        User user = currentUserService.getCurrentUser();
        boardAccessService.getAccessibleList(listId, user);
        return taskService.getTasks(listId);
    }

    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        User user = currentUserService.getCurrentUser();
        boardAccessService.getAccessibleTask(taskId, user);
        taskService.deleteTask(taskId, user);
    }

    @PutMapping("/{taskId}/move")
    public TaskResponse moveTask(@PathVariable Long taskId,
                                 @Valid @RequestBody MoveTaskRequest request) {

        User user = currentUserService.getCurrentUser();
        Task task = boardAccessService.getAccessibleTask(taskId, user);
        TaskList targetList = boardAccessService.getAccessibleList(request.getTargetListId(), user);

        if (!task.getList().getBoard().getId().equals(targetList.getBoard().getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Tasks can only move within the same board");
        }

        return taskService.moveTask(taskId, request.getTargetListId(), request.getTargetPosition(), user);
    }

    @PostMapping("/{taskId}/assignees")
    public TaskResponse assignUser(@PathVariable Long taskId,
                                   @Valid @RequestBody AssignTaskRequest request) {
        User user = currentUserService.getCurrentUser();
        boardAccessService.getAccessibleTask(taskId, user);
        return taskService.assignUser(taskId, request.userId(), user);
    }

    @DeleteMapping("/{taskId}/assignees/{userId}")
    public TaskResponse unassignUser(@PathVariable Long taskId,
                                     @PathVariable Long userId) {
        User user = currentUserService.getCurrentUser();
        boardAccessService.getAccessibleTask(taskId, user);
        return taskService.unassignUser(taskId, userId, user);
    }

    @GetMapping("/board/{boardId}/search")
    public PageResponse<TaskResponse> searchBoardTasks(@PathVariable Long boardId,
                                                       @RequestParam(required = false) String query,
                                                       @RequestParam(required = false) Long listId,
                                                       @RequestParam(required = false) Long assigneeId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);

        if (listId != null) {
            TaskList list = boardAccessService.getAccessibleList(listId, user);
            if (!list.getBoard().getId().equals(board.getId())) {
                throw new ResponseStatusException(BAD_REQUEST, "List does not belong to this board");
            }
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        return taskService.searchBoardTasks(board.getId(), query, listId, assigneeId, safePage, safeSize);
    }
}
