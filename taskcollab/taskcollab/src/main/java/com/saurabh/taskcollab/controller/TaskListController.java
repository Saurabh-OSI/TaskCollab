package com.saurabh.taskcollab.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.saurabh.taskcollab.dto.CreateListRequest;
import com.saurabh.taskcollab.dto.ReorderListRequest;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.Task;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.repository.TaskAssignmentRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.TaskRepository;
import com.saurabh.taskcollab.service.ActivityLogService;
import com.saurabh.taskcollab.service.BoardAccessService;
import com.saurabh.taskcollab.service.BoardRealtimeService;
import com.saurabh.taskcollab.service.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lists")
public class TaskListController {

    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final CurrentUserService currentUserService;
    private final BoardAccessService boardAccessService;
    private final BoardRealtimeService boardRealtimeService;
    private final ActivityLogService activityLogService;

    public TaskListController(TaskListRepository taskListRepository,
                              TaskRepository taskRepository,
                              TaskAssignmentRepository taskAssignmentRepository,
                              CurrentUserService currentUserService,
                              BoardAccessService boardAccessService,
                              BoardRealtimeService boardRealtimeService,
                              ActivityLogService activityLogService) {
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.currentUserService = currentUserService;
        this.boardAccessService = boardAccessService;
        this.boardRealtimeService = boardRealtimeService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/{boardId}")
    @Transactional
    public TaskList createList(@PathVariable Long boardId,
                               @Valid @RequestBody CreateListRequest request) {

        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);

        int nextPosition = taskListRepository.findByBoardIdOrderByPositionAsc(boardId).size() + 1;

        TaskList list = new TaskList();
        list.setName(request.name().trim());
        list.setBoard(board);
        list.setPosition(nextPosition);

        TaskList savedList = taskListRepository.save(list);
        boardRealtimeService.broadcastBoardLists(board);

        activityLogService.log(
                board,
                user,
                "LIST_CREATED",
                "LIST",
                savedList.getId(),
                user.getName() + " created list \"" + savedList.getName() + "\"."
        );

        return savedList;
    }

    @GetMapping("/{boardId}")
    public List<TaskList> getLists(@PathVariable Long boardId) {
        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);
        return taskListRepository.findByBoardIdOrderByPositionAsc(board.getId());
    }

    @PutMapping("/reorder/{boardId}")
    @Transactional
    public List<TaskList> reorderLists(@PathVariable Long boardId,
                                       @Valid @RequestBody ReorderListRequest request) {

        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);

        List<TaskList> currentLists = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);
        List<Long> requestedOrder = request.getListIds();

        if (currentLists.size() != requestedOrder.size()
                || requestedOrder.size() != new LinkedHashSet<>(requestedOrder).size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid list order");
        }

        Map<Long, TaskList> listsById = currentLists.stream()
                .collect(Collectors.toMap(TaskList::getId, list -> list, (left, right) -> left, LinkedHashMap::new));

        if (!listsById.keySet().containsAll(requestedOrder)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid list order");
        }

        List<TaskList> reorderedLists = requestedOrder.stream()
                .map(listsById::get)
                .toList();

        for (int index = 0; index < reorderedLists.size(); index++) {
            reorderedLists.get(index).setPosition(index + 1);
        }

        taskListRepository.saveAll(reorderedLists);
        boardRealtimeService.broadcastBoardLists(board);

        activityLogService.log(
                board,
                user,
                "LIST_REORDERED",
                "BOARD",
                board.getId(),
                user.getName() + " reordered lists on board \"" + board.getName() + "\"."
        );

        return reorderedLists;
    }

    @DeleteMapping("/{listId}")
    @Transactional
    public String deleteList(@PathVariable Long listId) {

        User user = currentUserService.getCurrentUser();
        TaskList list = boardAccessService.getAccessibleList(listId, user);
        Board board = list.getBoard();
        String listName = list.getName();

        List<Task> tasks = taskRepository.findByListIdOrderByPositionAsc(listId);
        tasks.forEach(task -> taskAssignmentRepository.deleteByTaskId(task.getId()));
        taskRepository.deleteAll(tasks);
        taskListRepository.delete(list);

        normalizeListPositions(board.getId());
        boardRealtimeService.broadcastBoardLists(board);

        activityLogService.log(
                board,
                user,
                "LIST_DELETED",
                "LIST",
                listId,
                user.getName() + " deleted list \"" + listName + "\"."
        );

        return "List deleted successfully";
    }

    private void normalizeListPositions(Long boardId) {
        List<TaskList> remainingLists = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);

        for (int index = 0; index < remainingLists.size(); index++) {
            remainingLists.get(index).setPosition(index + 1);
        }

        taskListRepository.saveAll(remainingLists);
    }
}
