package com.taskmanager.service;

import com.taskmanager.model.Project;
import com.taskmanager.model.Role;
import com.taskmanager.model.Status;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> getSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        List<Task> allTasks;
        long total;
        long completed;
        long inProgress;
        long todo;

        if (user.getRole() == Role.DEVELOPER) {
            List<Task> devTasks = taskRepository.findByAssignedUserId(user.getId());
            List<Long> projectIds = devTasks.stream()
                    .map(Task::getProject)
                    .filter(java.util.Objects::nonNull)
                    .map(Project::getId)
                    .distinct()
                    .toList();
            if (projectIds.isEmpty()) {
                allTasks = List.of();
            } else {
                allTasks = taskRepository.findByProjectIdIn(projectIds);
            }
            total = allTasks.size();
            completed = allTasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
            inProgress = allTasks.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count();
            todo = allTasks.stream().filter(t -> t.getStatus() == Status.TODO).count();
        } else {
            allTasks = taskRepository.findAll();
            total = allTasks.size();
            completed = taskRepository.countByStatus(Status.COMPLETED);
            inProgress = taskRepository.countByStatus(Status.IN_PROGRESS);
            todo = taskRepository.countByStatus(Status.TODO);
        }

        Map<String, Long> tasksPerDeveloper = allTasks.stream()
                .filter(t -> t.getAssignedUser() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getAssignedUser().getName(),
                        Collectors.counting()
                ));

        // same grouping as above, but with per-task detail (title, project, status,
        // priority) instead of just a count - lets the manager see exactly what
        // each developer is working on, not just how many tasks they have
        Map<String, List<Map<String, Object>>> tasksByDeveloper = allTasks.stream()
                .filter(t -> t.getAssignedUser() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getAssignedUser().getName(),
                        Collectors.mapping(t -> Map.<String, Object>of(
                                "id", t.getId(),
                                "title", t.getTitle(),
                                "project", t.getProject() != null ? t.getProject().getName() : "",
                                "status", t.getStatus().name(),
                                "priority", t.getPriority().name()
                        ), Collectors.toList())
                ));

        return Map.of(
                "totalTasks", total,
                "completedTasks", completed,
                "inProgressTasks", inProgress,
                "pendingTasks", todo,
                "tasksPerDeveloper", tasksPerDeveloper,
                "tasksByDeveloper", tasksByDeveloper
        );
    }
}
