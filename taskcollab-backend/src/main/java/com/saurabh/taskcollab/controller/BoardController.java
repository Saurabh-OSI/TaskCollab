package com.saurabh.taskcollab.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
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

import com.saurabh.taskcollab.dto.ActivityLogResponse;
import com.saurabh.taskcollab.dto.AddBoardMemberRequest;
import com.saurabh.taskcollab.dto.BoardMetaResponse;
import com.saurabh.taskcollab.dto.BoardSummaryResponse;
import com.saurabh.taskcollab.dto.CreateBoardRequest;
import com.saurabh.taskcollab.dto.PageResponse;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.BoardMember;
import com.saurabh.taskcollab.entity.BoardRole;
import com.saurabh.taskcollab.entity.Task;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.exception.ResourceNotFoundException;
import com.saurabh.taskcollab.repository.ActivityLogRepository;
import com.saurabh.taskcollab.repository.BoardMemberRepository;
import com.saurabh.taskcollab.repository.BoardRepository;
import com.saurabh.taskcollab.repository.TaskAssignmentRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.TaskRepository;
import com.saurabh.taskcollab.repository.UserRepository;
import com.saurabh.taskcollab.service.ActivityLogService;
import com.saurabh.taskcollab.service.BoardAccessService;
import com.saurabh.taskcollab.service.BoardRealtimeService;
import com.saurabh.taskcollab.service.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserService currentUserService;
    private final BoardAccessService boardAccessService;
    private final BoardRealtimeService boardRealtimeService;
    private final ActivityLogService activityLogService;

    public BoardController(BoardRepository boardRepository,
                           BoardMemberRepository boardMemberRepository,
                           UserRepository userRepository,
                           TaskListRepository taskListRepository,
                           TaskRepository taskRepository,
                           TaskAssignmentRepository taskAssignmentRepository,
                           ActivityLogRepository activityLogRepository,
                           CurrentUserService currentUserService,
                           BoardAccessService boardAccessService,
                           BoardRealtimeService boardRealtimeService,
                           ActivityLogService activityLogService) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.userRepository = userRepository;
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.activityLogRepository = activityLogRepository;
        this.currentUserService = currentUserService;
        this.boardAccessService = boardAccessService;
        this.boardRealtimeService = boardRealtimeService;
        this.activityLogService = activityLogService;
    }

    @PostMapping
    public BoardSummaryResponse createBoard(@Valid @RequestBody CreateBoardRequest request) {
        User user = currentUserService.getCurrentUser();

        Board board = new Board();
        board.setName(request.name().trim());
        board.setOwner(user);

        Board savedBoard = boardRepository.save(board);
        boardRealtimeService.broadcastBoardsSync(user);

        activityLogService.log(
                savedBoard,
                user,
                "BOARD_CREATED",
                "BOARD",
                savedBoard.getId(),
                user.getName() + " created board \"" + savedBoard.getName() + "\"."
        );

        return boardRealtimeService.getBoardsForUser(user)
                .stream()
                .filter(existingBoard -> existingBoard.id().equals(savedBoard.getId()))
                .findFirst()
                .orElse(new BoardSummaryResponse(savedBoard.getId(), savedBoard.getName(), BoardRole.OWNER.name(), 1));
    }

    @GetMapping
    public List<BoardSummaryResponse> getBoards() {
        return boardRealtimeService.getBoardsForUser(currentUserService.getCurrentUser());
    }

    @PutMapping("/{boardId}")
    public BoardSummaryResponse renameBoard(@PathVariable Long boardId,
                                            @Valid @RequestBody CreateBoardRequest request) {
        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);
        boardAccessService.assertBoardOwner(board, user);

        board.setName(request.name().trim());
        Board savedBoard = boardRepository.save(board);

        boardRealtimeService.broadcastBoardsSync(savedBoard);
        boardRealtimeService.broadcastBoardMeta(savedBoard);

        activityLogService.log(
                savedBoard,
                user,
                "BOARD_RENAMED",
                "BOARD",
                savedBoard.getId(),
                user.getName() + " renamed the board to \"" + savedBoard.getName() + "\"."
        );

        return boardRealtimeService.getBoardsForUser(user)
                .stream()
                .filter(existingBoard -> existingBoard.id().equals(savedBoard.getId()))
                .findFirst()
                .orElse(new BoardSummaryResponse(savedBoard.getId(), savedBoard.getName(), BoardRole.OWNER.name(), 1));
    }

    @GetMapping("/{boardId}/meta")
    public BoardMetaResponse getBoardMeta(@PathVariable Long boardId) {
        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);
        return boardRealtimeService.buildBoardMeta(board);
    }

    @GetMapping("/{boardId}/activity")
    public PageResponse<ActivityLogResponse> getBoardActivity(@PathVariable Long boardId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        User user = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, user);

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        return activityLogService.getBoardActivity(board, safePage, safeSize);
    }

    @PostMapping("/{boardId}/members")
    public BoardMetaResponse addBoardMember(@PathVariable Long boardId,
                                            @Valid @RequestBody AddBoardMemberRequest request) {
        User actor = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, actor);
        boardAccessService.assertBoardOwner(board, actor);

        String email = request.email().toLowerCase().trim();
        User memberUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (board.getOwner().getId().equals(memberUser.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Board owner already has access");
        }

        if (boardMemberRepository.existsByBoardIdAndUserId(boardId, memberUser.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "User is already a board member");
        }

        BoardMember boardMember = new BoardMember();
        boardMember.setBoard(board);
        boardMember.setUser(memberUser);
        boardMember.setRole(BoardRole.MEMBER);
        boardMemberRepository.save(boardMember);

        boardRealtimeService.broadcastBoardsSync(board);
        boardRealtimeService.broadcastBoardMeta(board);

        activityLogService.log(
                board,
                actor,
                "MEMBER_ADDED",
                "BOARD_MEMBER",
                memberUser.getId(),
                actor.getName() + " added " + memberUser.getName() + " to board \"" + board.getName() + "\"."
        );

        return boardRealtimeService.buildBoardMeta(board);
    }

    @DeleteMapping("/{boardId}/members/{userId}")
    public BoardMetaResponse removeBoardMember(@PathVariable Long boardId,
                                               @PathVariable Long userId) {
        User actor = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, actor);
        boardAccessService.assertBoardOwner(board, actor);

        if (board.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Board owner cannot be removed");
        }

        BoardMember membership = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board member not found"));

        User removedUser = membership.getUser();
        boardMemberRepository.delete(membership);

        List<User> recipients = new ArrayList<>(boardRealtimeService.getBoardUsers(board));
        recipients.add(removedUser);

        boardRealtimeService.broadcastBoardsSync(recipients);
        boardRealtimeService.broadcastBoardMeta(board);

        activityLogService.log(
                board,
                actor,
                "MEMBER_REMOVED",
                "BOARD_MEMBER",
                removedUser.getId(),
                actor.getName() + " removed " + removedUser.getName() + " from board \"" + board.getName() + "\"."
        );

        return boardRealtimeService.buildBoardMeta(board);
    }

    @DeleteMapping("/{boardId}")
    @Transactional
    public String deleteBoard(@PathVariable Long boardId) {
        User actor = currentUserService.getCurrentUser();
        Board board = boardAccessService.getAccessibleBoard(boardId, actor);
        boardAccessService.assertBoardOwner(board, actor);

        List<User> recipients = new ArrayList<>(boardRealtimeService.getBoardUsers(board));
        List<TaskList> lists = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);

        for (TaskList list : lists) {
            List<Task> tasks = taskRepository.findByListIdOrderByPositionAsc(list.getId());
            tasks.forEach(task -> taskAssignmentRepository.deleteByTaskId(task.getId()));
            taskRepository.deleteAll(tasks);
        }

        taskListRepository.deleteAll(lists);
        boardMemberRepository.deleteByBoardId(boardId);
        activityLogRepository.deleteByBoardId(boardId);
        boardRepository.delete(board);

        boardRealtimeService.broadcastBoardsSync(recipients);

        return "Board deleted successfully";
    }
}
