package com.saurabh.taskcollab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saurabh.taskcollab.entity.TaskList;

public interface TaskListRepository extends JpaRepository<TaskList, Long> {

    List<TaskList> findByBoardIdOrderByPositionAsc(Long boardId);
}
