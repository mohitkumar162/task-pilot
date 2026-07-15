package com.taskmanager.repository;

import com.taskmanager.model.Status;
import com.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedUserId(Long userId);
    long countByStatus(Status status);
    void deleteByProjectId(Long projectId);
    List<Task> findByProjectIdIn(List<Long> projectIds);
}
