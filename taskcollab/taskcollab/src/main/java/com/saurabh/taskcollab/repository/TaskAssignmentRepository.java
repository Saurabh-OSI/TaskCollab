package com.saurabh.taskcollab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saurabh.taskcollab.entity.TaskAssignment;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskIdOrderByUserNameAsc(Long taskId);

    Optional<TaskAssignment> findByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);
}
