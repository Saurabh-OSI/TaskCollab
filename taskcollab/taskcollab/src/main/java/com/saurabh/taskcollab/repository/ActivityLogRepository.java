package com.saurabh.taskcollab.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.saurabh.taskcollab.entity.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByBoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    void deleteByBoardId(Long boardId);
}
