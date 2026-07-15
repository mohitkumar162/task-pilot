package com.taskmanager.service;

import com.taskmanager.model.Project;
import com.taskmanager.model.Role;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    public List<Project> getAllProjects(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        if (user.getRole() == Role.DEVELOPER) {
            List<Task> devTasks = taskRepository.findByAssignedUserId(user.getId());
            return devTasks.stream()
                    .map(Task::getProject)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return projectRepository.findAll();
    }

    public Project updateProject(Long id, Project updated) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        project.setName(updated.getName());
        project.setDescription(updated.getDescription());
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        taskRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }
}
