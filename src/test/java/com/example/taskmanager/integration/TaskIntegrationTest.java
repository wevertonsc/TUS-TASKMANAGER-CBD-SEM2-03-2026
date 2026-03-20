package com.example.taskmanager.integration;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // Usa perfil de teste com H2
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void createAndGetTask_ShouldWorkCorrectly() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Integration Test Task");
        request.setDescription("Testing full flow");
        request.setDueDate(LocalDateTime.now().plusDays(7));

        // Create task
        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Integration Test Task")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        TaskResponse createdTask = objectMapper.readValue(response, TaskResponse.class);
        Long taskId = createdTask.getId();

        // Get task by ID
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Integration Test Task")));

        // Get all tasks
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(taskId.intValue())));
    }

    @Test
    void updateTaskStatus_ShouldUpdateCorrectly() throws Exception {
        // Create task
        Task task = new Task();
        task.setTitle("Status Update Test");
        task.setDescription("Testing status update");
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        // Update status
        mockMvc.perform(patch("/api/tasks/{id}/status", savedTask.getId())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // Verify update
        mockMvc.perform(get("/api/tasks/{id}", savedTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void deleteTask_ShouldRemoveTask() throws Exception {
        // Create task
        Task task = new Task();
        task.setTitle("Delete Test");
        task.setDescription("Testing delete");
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        // Delete task
        mockMvc.perform(delete("/api/tasks/{id}", savedTask.getId()))
                .andExpect(status().isNoContent());

        // Verify task is deleted
        mockMvc.perform(get("/api/tasks/{id}", savedTask.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTasksByStatus_ShouldReturnFilteredResults() throws Exception {
        // Create multiple tasks with different statuses
        Task pendingTask = new Task();
        pendingTask.setTitle("Pending Task");
        pendingTask.setStatus(TaskStatus.PENDING);
        pendingTask.setCreatedAt(LocalDateTime.now());
        pendingTask.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(pendingTask);

        Task completedTask = new Task();
        completedTask.setTitle("Completed Task");
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask.setCreatedAt(LocalDateTime.now());
        completedTask.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(completedTask);

        // Get pending tasks
        mockMvc.perform(get("/api/tasks/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Pending Task")));

        // Get completed tasks
        mockMvc.perform(get("/api/tasks/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Completed Task")));
    }
}