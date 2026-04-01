package com.saurabh.taskcollab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.saurabh.taskcollab.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.list.id = :listId ORDER BY t.position ASC")
    List<Task> findByListIdOrderByPositionAsc(@Param("listId") Long listId);

    Optional<Task> findTopByListIdOrderByPositionDesc(Long listId);

    @Query(
            value = """
                    SELECT DISTINCT t
                    FROM Task t
                    LEFT JOIN TaskAssignment ta ON ta.task = t
                    WHERE t.list.board.id = :boardId
                      AND (:query IS NULL
                           OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
                      AND (:listId IS NULL OR t.list.id = :listId)
                      AND (:assigneeId IS NULL OR ta.user.id = :assigneeId)
                    ORDER BY t.updatedAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT t.id)
                    FROM Task t
                    LEFT JOIN TaskAssignment ta ON ta.task = t
                    WHERE t.list.board.id = :boardId
                      AND (:query IS NULL
                           OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
                      AND (:listId IS NULL OR t.list.id = :listId)
                      AND (:assigneeId IS NULL OR ta.user.id = :assigneeId)
                    """
    )
    Page<Task> searchBoardTasks(@Param("boardId") Long boardId,
                                @Param("query") String query,
                                @Param("listId") Long listId,
                                @Param("assigneeId") Long assigneeId,
                                Pageable pageable);
}
