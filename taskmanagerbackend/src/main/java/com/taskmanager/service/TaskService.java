package com.taskmanager.service;

import com.taskmanager.model.*;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public Task createTask(Task task, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        task.setProject(project);
        if (task.getStatus() == null) {
            task.setStatus(Status.TODO);
        }
        if (task.getPriority() == null) {
            task.setPriority(Priority.MEDIUM);
        }
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        if (user.getRole() == Role.DEVELOPER) {
            List<Task> devTasks = taskRepository.findByAssignedUserId(user.getId());
            List<Long> projectIds = devTasks.stream()
                    .map(Task::getProject)
                    .filter(java.util.Objects::nonNull)
                    .map(Project::getId)
                    .distinct()
                    .toList();
            if (projectIds.isEmpty()) {
                return List.of();
            }
            return taskRepository.findByProjectIdIn(projectIds);
        }
        return taskRepository.findAll();
    }

    public Task assignTask(Long taskId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        Task task = getTaskOrThrow(taskId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        task.setAssignedUser(user);
        return taskRepository.save(task);
    }

    public Task updateStatus(Long taskId, Status status) {
        Task task = getTaskOrThrow(taskId);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public Task updatePriority(Long taskId, Priority priority) {
        Task task = getTaskOrThrow(taskId);
        task.setPriority(priority);
        return taskRepository.save(task);
    }

    public Task updateDeadline(Long taskId, LocalDate deadline) {
        Task task = getTaskOrThrow(taskId);
        task.setDeadline(deadline);
        return taskRepository.save(task);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }
}
