package com.saurabh.taskcollab.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.saurabh.taskcollab.dto.CreateTaskRequest;
import com.saurabh.taskcollab.dto.PageResponse;
import com.saurabh.taskcollab.dto.TaskResponse;
import com.saurabh.taskcollab.dto.UserSummaryResponse;
import com.saurabh.taskcollab.entity.Task;
import com.saurabh.taskcollab.entity.TaskAssignment;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.exception.ResourceNotFoundException;
import com.saurabh.taskcollab.repository.TaskAssignmentRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.TaskRepository;
import com.saurabh.taskcollab.repository.UserRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccessService;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(TaskRepository taskRepository,
                       TaskListRepository taskListRepository,
                       TaskAssignmentRepository taskAssignmentRepository,
                       UserRepository userRepository,
                       BoardAccessService boardAccessService,
                       ActivityLogService activityLogService,
                       SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.taskListRepository = taskListRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.userRepository = userRepository;
        this.boardAccessService = boardAccessService;
        this.activityLogService = activityLogService;
        this.messagingTemplate = messagingTemplate;
    }

    public List<TaskResponse> getTasks(Long listId) {
        return taskRepository.findByListIdOrderByPositionAsc(listId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public PageResponse<TaskResponse> searchBoardTasks(Long boardId,
                                                       String query,
                                                       Long listId,
                                                       Long assigneeId,
                                                       int page,
                                                       int size) {
        String normalizedQuery = query == null || query.trim().isEmpty()
                ? null
                : query.trim();

        return PageResponse.from(
                taskRepository.searchBoardTasks(
                        boardId,
                        normalizedQuery,
                        listId,
                        assigneeId,
                        PageRequest.of(page, size)
                ).map(this::mapToResponse)
        );
    }

    @Transactional
    public TaskResponse createTask(Long listId, CreateTaskRequest request, User user) {

        TaskList list = taskListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        int nextPosition = taskRepository.findTopByListIdOrderByPositionDesc(listId)
                .map(existingTask -> existingTask.getPosition() + 1)
                .orElse(1);

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setList(list);
        task.setCreatedBy(user);
        task.setPosition(nextPosition);

        Task savedTask = taskRepository.save(task);
        broadcastListSync(listId);

        activityLogService.log(
                list.getBoard(),
                user,
                "TASK_CREATED",
                "TASK",
                savedTask.getId(),
                user.getName() + " created task \"" + savedTask.getTitle() + "\" in " + list.getName() + "."
        );

        return mapToResponse(savedTask);
    }

    @Transactional
    public void deleteTask(Long taskId, User user) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        Long listId = task.getList().getId();
        String taskTitle = task.getTitle();
        String listName = task.getList().getName();

        taskAssignmentRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
        normalizeTaskPositions(listId);
        broadcastListSync(listId);

        activityLogService.log(
                task.getList().getBoard(),
                user,
                "TASK_DELETED",
                "TASK",
                taskId,
                user.getName() + " deleted task \"" + taskTitle + "\" from " + listName + "."
        );
    }

    @Transactional
    public TaskResponse moveTask(Long taskId, Long newListId, Integer targetPosition, User user) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        TaskList sourceList = task.getList();
        TaskList targetList = taskListRepository.findById(newListId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        Long sourceListId = sourceList.getId();

        if (sourceListId.equals(newListId)) {
            List<Task> reorderedTasks = new ArrayList<>(taskRepository.findByListIdOrderByPositionAsc(sourceListId));
            reorderedTasks.removeIf(existingTask -> existingTask.getId().equals(taskId));
            insertTaskAtPosition(reorderedTasks, task, targetPosition);
            normalizeTaskPositions(reorderedTasks, sourceList);
            broadcastListSync(sourceListId);

            activityLogService.log(
                    sourceList.getBoard(),
                    user,
                    "TASK_REORDERED",
                    "TASK",
                    task.getId(),
                    user.getName() + " reordered task \"" + task.getTitle() + "\" in " + sourceList.getName() + "."
            );

            return mapToResponse(task);
        }

        List<Task> sourceTasks = new ArrayList<>(taskRepository.findByListIdOrderByPositionAsc(sourceListId));
        sourceTasks.removeIf(existingTask -> existingTask.getId().equals(taskId));

        List<Task> targetTasks = new ArrayList<>(taskRepository.findByListIdOrderByPositionAsc(newListId));
        task.setList(targetList);
        insertTaskAtPosition(targetTasks, task, targetPosition);

        normalizeTaskPositions(sourceTasks, sourceList);
        normalizeTaskPositions(targetTasks, targetList);

        broadcastListSync(sourceListId);
        broadcastListSync(newListId);

        activityLogService.log(
                sourceList.getBoard(),
                user,
                "TASK_MOVED",
                "TASK",
                task.getId(),
                user.getName() + " moved task \"" + task.getTitle() + "\" from "
                        + sourceList.getName() + " to " + targetList.getName() + "."
        );

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse assignUser(Long taskId, Long userId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
            return mapToResponse(task);
        }

        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!boardAccessService.isBoardUser(task.getList().getBoard(), assignee)) {
            throw new ResponseStatusException(BAD_REQUEST, "User is not a member of this board");
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUser(assignee);
        taskAssignmentRepository.save(assignment);

        broadcastListSync(task.getList().getId());

        activityLogService.log(
                task.getList().getBoard(),
                actor,
                "TASK_ASSIGNED",
                "TASK",
                task.getId(),
                actor.getName() + " assigned " + assignee.getName() + " to task \"" + task.getTitle() + "\"."
        );

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse unassignUser(Long taskId, Long userId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        TaskAssignment assignment = taskAssignmentRepository.findByTaskIdAndUserId(taskId, userId)
                .orElse(null);

        if (assignment == null) {
            return mapToResponse(task);
        }

        User assignee = assignment.getUser();
        taskAssignmentRepository.delete(assignment);

        broadcastListSync(task.getList().getId());

        activityLogService.log(
                task.getList().getBoard(),
                actor,
                "TASK_UNASSIGNED",
                "TASK",
                task.getId(),
                actor.getName() + " removed " + assignee.getName() + " from task \"" + task.getTitle() + "\"."
        );

        return mapToResponse(task);
    }

    private void insertTaskAtPosition(List<Task> tasks, Task task, Integer targetPosition) {
        int insertionIndex = resolveInsertionIndex(tasks.size(), targetPosition);
        tasks.add(insertionIndex, task);
    }

    private int resolveInsertionIndex(int currentSize, Integer targetPosition) {
        if (targetPosition == null) {
            return currentSize;
        }

        int requestedIndex = targetPosition - 1;
        return Math.max(0, Math.min(requestedIndex, currentSize));
    }

    private void normalizeTaskPositions(Long listId) {
        TaskList list = taskListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        List<Task> tasks = new ArrayList<>(taskRepository.findByListIdOrderByPositionAsc(listId));
        normalizeTaskPositions(tasks, list);
    }

    private void normalizeTaskPositions(List<Task> tasks, TaskList list) {
        for (int index = 0; index < tasks.size(); index++) {
            Task existingTask = tasks.get(index);
            existingTask.setList(list);
            existingTask.setPosition(index + 1);
        }

        taskRepository.saveAll(tasks);
    }

    private void broadcastListSync(Long listId) {
        List<TaskResponse> tasks = taskRepository.findByListIdOrderByPositionAsc(listId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        Map<String, Object> event = new HashMap<>();
        event.put("type", "SYNC");
        event.put("tasks", tasks);

        messagingTemplate.convertAndSend("/topic/list/" + listId, event);
    }

    private TaskResponse mapToResponse(Task task) {
        User createdBy = task.getCreatedBy();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPosition(),
                task.getList().getId(),
                task.getList().getName(),
                createdBy == null ? null : new UserSummaryResponse(
                        createdBy.getId(),
                        createdBy.getName(),
                        createdBy.getEmail()
                ),
                taskAssignmentRepository.findByTaskIdOrderByUserNameAsc(task.getId())
                        .stream()
                        .map(TaskAssignment::getUser)
                        .map(user -> new UserSummaryResponse(user.getId(), user.getName(), user.getEmail()))
                        .toList(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
