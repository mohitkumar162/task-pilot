package com.taskmanager.controller;

import com.taskmanager.model.Priority;
import com.taskmanager.model.Status;
import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // body must include a "projectId" field alongside the task fields
    @PostMapping
    public Task createTask(@Valid @RequestBody Task task, @RequestParam Long projectId) {
        return taskService.createTask(task, projectId);
    }

    @GetMapping
    public List<Task> getAllTasks(java.security.Principal principal) {
        return taskService.getAllTasks(principal.getName());
    }

    @PutMapping("/{id}/assign")
    public Task assignTask(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        return taskService.assignTask(id, body.get("userId"));
    }

    @PutMapping("/{id}/status")
    public Task updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.updateStatus(id, Status.valueOf(body.get("status")));
    }

    @PutMapping("/{id}/priority")
    public Task updatePriority(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.updatePriority(id, Priority.valueOf(body.get("priority")));
    }

    @PutMapping("/{id}/deadline")
    public Task updateDeadline(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.updateDeadline(id, LocalDate.parse(body.get("deadline")));
    }
}
