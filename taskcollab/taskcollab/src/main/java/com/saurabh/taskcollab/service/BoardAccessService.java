package com.saurabh.taskcollab.service;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.Task;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.exception.ResourceNotFoundException;
import com.saurabh.taskcollab.repository.BoardMemberRepository;
import com.saurabh.taskcollab.repository.BoardRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.TaskRepository;

@Service
public class BoardAccessService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;

    public BoardAccessService(BoardRepository boardRepository,
                              BoardMemberRepository boardMemberRepository,
                              TaskListRepository taskListRepository,
                              TaskRepository taskRepository) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
    }

    public Board getAccessibleBoard(Long boardId, User user) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        assertCanAccessBoard(board, user);
        return board;
    }

    public TaskList getAccessibleList(Long listId, User user) {
        TaskList list = taskListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));

        assertCanAccessBoard(list.getBoard(), user);
        return list;
    }

    public Task getAccessibleTask(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        assertCanAccessBoard(task.getList().getBoard(), user);
        return task;
    }

    public void assertCanAccessBoard(Board board, User user) {
        if (!isBoardUser(board, user)) {
            throw new ResponseStatusException(FORBIDDEN, "Not authorized");
        }
    }

    public void assertBoardOwner(Board board, User user) {
        if (!board.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only the board owner can perform this action");
        }
    }

    public boolean isBoardUser(Board board, User user) {
        if (board.getOwner().getId().equals(user.getId())) {
            return true;
        }

        return boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId());
    }
}
