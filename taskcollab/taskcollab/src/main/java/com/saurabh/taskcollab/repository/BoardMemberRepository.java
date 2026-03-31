package com.saurabh.taskcollab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saurabh.taskcollab.entity.BoardMember;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardMember> findByBoardIdOrderByUserNameAsc(Long boardId);

    long countByBoardId(Long boardId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardId(Long boardId);
}
