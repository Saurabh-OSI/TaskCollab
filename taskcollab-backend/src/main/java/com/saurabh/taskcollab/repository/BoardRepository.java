package com.saurabh.taskcollab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.saurabh.taskcollab.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
            SELECT DISTINCT b
            FROM Board b
            LEFT JOIN BoardMember bm ON bm.board = b
            WHERE b.owner.id = :userId OR bm.user.id = :userId
            """)
    List<Board> findAccessibleBoards(@Param("userId") Long userId);
}
