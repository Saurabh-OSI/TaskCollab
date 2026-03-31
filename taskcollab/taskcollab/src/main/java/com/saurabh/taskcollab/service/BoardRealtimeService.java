package com.saurabh.taskcollab.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.saurabh.taskcollab.dto.BoardMemberResponse;
import com.saurabh.taskcollab.dto.BoardMetaResponse;
import com.saurabh.taskcollab.dto.BoardSummaryResponse;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.BoardMember;
import com.saurabh.taskcollab.entity.BoardRole;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.repository.BoardMemberRepository;
import com.saurabh.taskcollab.repository.BoardRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;

@Service
public class BoardRealtimeService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final TaskListRepository taskListRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardRealtimeService(BoardRepository boardRepository,
                                BoardMemberRepository boardMemberRepository,
                                TaskListRepository taskListRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.taskListRepository = taskListRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastBoardsSync(User user) {
        List<BoardSummaryResponse> boards = getBoardsForUser(user);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "SYNC");
        event.put("boards", boards);

        messagingTemplate.convertAndSend(getBoardsTopic(user), event);
    }

    public void broadcastBoardsSync(Board board) {
        broadcastBoardsSync(getBoardUsers(board));
    }

    public List<BoardSummaryResponse> getBoardsForUser(User user) {
        return boardRepository.findAccessibleBoards(user.getId())
                .stream()
                .map(board -> mapBoardSummary(board, user))
                .sorted(Comparator
                        .comparing((BoardSummaryResponse board) -> board.name().toLowerCase())
                        .thenComparing(BoardSummaryResponse::id))
                .toList();
    }

    public void broadcastBoardsSync(Collection<User> users) {
        Map<Long, User> uniqueUsers = users.stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));

        uniqueUsers.values().forEach(this::broadcastBoardsSync);
    }

    public void broadcastBoardLists(Board board) {
        List<TaskList> orderedLists = taskListRepository.findByBoardIdOrderByPositionAsc(board.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("type", "SYNC");
        event.put("lists", orderedLists);

        messagingTemplate.convertAndSend("/topic/board/" + board.getId(), event);
    }

    public void broadcastBoardMeta(Board board) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "SYNC");
        event.put("board", buildBoardMeta(board));

        messagingTemplate.convertAndSend("/topic/board/" + board.getId() + "/meta", event);
    }

    public BoardMetaResponse buildBoardMeta(Board board) {
        List<BoardMemberResponse> members = new ArrayList<>();
        members.add(new BoardMemberResponse(
                board.getOwner().getId(),
                board.getOwner().getName(),
                board.getOwner().getEmail(),
                BoardRole.OWNER.name()
        ));

        boardMemberRepository.findByBoardIdOrderByUserNameAsc(board.getId())
                .stream()
                .filter(member -> !member.getUser().getId().equals(board.getOwner().getId()))
                .map(this::mapBoardMember)
                .forEach(members::add);

        return new BoardMetaResponse(board.getId(), board.getName(), members);
    }

    public List<User> getBoardUsers(Board board) {
        List<User> users = new ArrayList<>();
        users.add(board.getOwner());
        boardMemberRepository.findByBoardIdOrderByUserNameAsc(board.getId())
                .stream()
                .map(BoardMember::getUser)
                .filter(user -> !user.getId().equals(board.getOwner().getId()))
                .forEach(users::add);
        return users;
    }

    private BoardSummaryResponse mapBoardSummary(Board board, User user) {
        String role = board.getOwner().getId().equals(user.getId())
                ? BoardRole.OWNER.name()
                : boardMemberRepository.findByBoardIdAndUserId(board.getId(), user.getId())
                        .map(member -> member.getRole().name())
                        .orElse(BoardRole.MEMBER.name());

        long memberCount = boardMemberRepository.countByBoardId(board.getId()) + 1;
        return new BoardSummaryResponse(board.getId(), board.getName(), role, memberCount);
    }

    private BoardMemberResponse mapBoardMember(BoardMember member) {
        return new BoardMemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole().name()
        );
    }

    private String getBoardsTopic(User user) {
        return "/topic/boards/" + sanitizeEmailForTopic(user.getEmail());
    }

    private String sanitizeEmailForTopic(String email) {
        return email.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
