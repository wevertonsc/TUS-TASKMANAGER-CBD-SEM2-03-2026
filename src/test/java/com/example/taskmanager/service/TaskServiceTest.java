package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
    }

    @Test
    void createTask_ShouldReturnTaskResponse_WhenTaskIsCreated() {
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.createTask(taskRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Task");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING);

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void getAllTasks_ShouldReturnListOfTaskResponses() {
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Second Task");
        task2.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findAll()).thenReturn(Arrays.asList(task, task2));

        List<TaskResponse> responses = taskService.getAllTasks();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Test Task");
        assertThat(responses.get(1).getTitle()).isEqualTo("Second Task");

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getTaskById_ShouldReturnTaskResponse_WhenTaskExists() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Task");

        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void getTaskById_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found with id: 99");

        verify(taskRepository, times(1)).findById(99L);
    }

    @Test
    void updateTask_ShouldReturnUpdatedTaskResponse_WhenTaskExists() {
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");

        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Updated Title");  // Corrigido: deve ser o título atualizado
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.PENDING);
        updatedTask.setCreatedAt(task.getCreatedAt());
        updatedTask.setUpdatedAt(LocalDateTime.now());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskResponse response = taskService.updateTask(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Title");  // Corrigido: espera título atualizado
        assertThat(response.getDescription()).isEqualTo("Updated Description");

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTaskStatus_ShouldUpdateStatus_WhenTaskExists() {
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Test Task");
        updatedTask.setDescription("Test Description");
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);  // Status atualizado
        updatedTask.setCreatedAt(task.getCreatedAt());
        updatedTask.setUpdatedAt(LocalDateTime.now());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskResponse response = taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);  // Corrigido: espera novo status
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void getTasksByStatus_ShouldReturnFilteredTasks() {
        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(Arrays.asList(task));

        List<TaskResponse> responses = taskService.getTasksByStatus(TaskStatus.PENDING);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(TaskStatus.PENDING);

        verify(taskRepository, times(1)).findByStatus(TaskStatus.PENDING);
    }

    @Test
    void deleteTask_ShouldDeleteTask_WhenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);

        taskService.deleteTask(1L);

        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTask_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found with id: 99");

        verify(taskRepository, times(1)).existsById(99L);
        verify(taskRepository, never()).deleteById(anyLong());
    }
}