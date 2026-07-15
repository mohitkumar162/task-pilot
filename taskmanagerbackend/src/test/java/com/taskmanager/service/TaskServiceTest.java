package com.taskmanager.service;

import com.taskmanager.model.*;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private User manager;
    private User developer;

    @BeforeEach
    void setUp() {
        project = new Project("Website Revamp", "Redesign the marketing site");
        project.setId(1L);

        manager = new User("Alice Manager", "alice@company.com", "hashed-pw", Role.MANAGER);
        manager.setId(10L);

        developer = new User("Bob Developer", "bob@company.com", "hashed-pw", Role.DEVELOPER);
        developer.setId(20L);
    }

    // ---------- createTask ----------

    @Test
    void createTask_appliesDefaultStatusAndPriority_whenNotProvided() {
        Task task = new Task();
        task.setTitle("Set up CI pipeline");
        task.setStatus(null);
        task.setPriority(null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.createTask(task, 1L);

        assertEquals(Status.TODO, saved.getStatus());
        assertEquals(Priority.MEDIUM, saved.getPriority());
        assertEquals(project, saved.getProject());
        verify(taskRepository).save(task);
    }

    @Test
    void createTask_keepsProvidedStatusAndPriority_whenAlreadySet() {
        Task task = new Task();
        task.setTitle("Fix production bug");
        task.setStatus(Status.IN_PROGRESS);
        task.setPriority(Priority.HIGH);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.createTask(task, 1L);

        assertEquals(Status.IN_PROGRESS, saved.getStatus());
        assertEquals(Priority.HIGH, saved.getPriority());
    }

    @Test
    void createTask_throws_whenProjectDoesNotExist() {
        Task task = new Task();
        task.setTitle("Orphan task");

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(task, 99L));
        verify(taskRepository, never()).save(any());
    }

    // ---------- assignTask ----------

    @Test
    void assignTask_setsAssignedUserAndSaves_onHappyPath() {
        Task task = new Task();
        task.setId(5L);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(developer));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.assignTask(5L, 20L);

        assertEquals(developer, result.getAssignedUser());
        verify(taskRepository).save(task);
    }

    @Test
    void assignTask_throws_whenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskService.assignTask(5L, null));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void assignTask_throws_whenTaskNotFound() {
        when(taskRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.assignTask(5L, 20L));
    }

    @Test
    void assignTask_throws_whenUserNotFound() {
        Task task = new Task();
        task.setId(5L);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.assignTask(5L, 999L));
    }

    // ---------- getAllTasks (role-based visibility) ----------

    @Test
    void getAllTasks_returnsEveryTask_forManager() {
        Task t1 = new Task();
        Task t2 = new Task();

        when(userRepository.findByEmail("alice@company.com")).thenReturn(Optional.of(manager));
        when(taskRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Task> result = taskService.getAllTasks("alice@company.com");

        assertEquals(2, result.size());
        verify(taskRepository, never()).findByAssignedUserId(any());
    }

    @Test
    void getAllTasks_returnsOnlyOwnProjectTasks_forDeveloper() {
        Task assignedToBob = new Task();
        assignedToBob.setProject(project);

        when(userRepository.findByEmail("bob@company.com")).thenReturn(Optional.of(developer));
        when(taskRepository.findByAssignedUserId(20L)).thenReturn(List.of(assignedToBob));
        when(taskRepository.findByProjectIdIn(List.of(1L))).thenReturn(List.of(assignedToBob));

        List<Task> result = taskService.getAllTasks("bob@company.com");

        assertEquals(1, result.size());
        verify(taskRepository, never()).findAll();
    }

    @Test
    void getAllTasks_returnsEmptyList_forDeveloperWithNoAssignedTasks() {
        when(userRepository.findByEmail("bob@company.com")).thenReturn(Optional.of(developer));
        when(taskRepository.findByAssignedUserId(20L)).thenReturn(List.of());

        List<Task> result = taskService.getAllTasks("bob@company.com");

        assertTrue(result.isEmpty());
        verify(taskRepository, never()).findByProjectIdIn(any());
    }

    @Test
    void getAllTasks_throws_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@company.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.getAllTasks("ghost@company.com"));
    }
}
