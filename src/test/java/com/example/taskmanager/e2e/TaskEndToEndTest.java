package com.example.taskmanager.e2e;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end workflow tests at the top of the Testing Pyramid.
 *
 * These tests load the complete Spring Boot application context and exercise
 * real HTTP request/response cycles through MockMvc against an in-memory H2
 * database. They verify that all application layers (controller, service,
 * repository, exception handling) work correctly as a system.
 *
 * No mocks are used here. Dependencies are satisfied through the real
 * application beans wired by the Spring IoC container, demonstrating that
 * constructor injection enables testability across the full stack.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskEndToEndTest {

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

    /**
     * Scenario: Complete task lifecycle - create, read, update, delete.
     * Verifies the CRUD workflow succeeds end-to-end without any mocking.
     */
    @Test
    void fullTaskLifecycle_CreateReadUpdateDelete_ShouldSucceed() throws Exception {
        // Step 1: Create a task
        TaskRequest createRequest = new TaskRequest();
        createRequest.setTitle("E2E Lifecycle Task");
        createRequest.setDescription("Testing the complete lifecycle");
        createRequest.setDueDate(LocalDateTime.now().plusDays(5));

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("E2E Lifecycle Task")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        TaskResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TaskResponse.class);
        Long taskId = created.getId();
        assertThat(taskId).isNotNull();

        // Step 2: Read the task back
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("E2E Lifecycle Task")));

        // Step 3: Update task fields
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated E2E Task");
        updateRequest.setDescription("Updated description");

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated E2E Task")));

        // Step 4: Update task status through the dedicated endpoint
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Step 5: Verify the status change persisted
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Step 6: Delete the task
        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        // Step 7: Verify deletion
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }

    /**
     * Scenario: Status filter workflow.
     * Creates tasks with different statuses and verifies the filter endpoint
     * returns only the correct subset.
     */
    @Test
    void statusFilterWorkflow_ShouldReturnCorrectSubsets() throws Exception {
        createTaskDirectly("Task A", TaskStatus.PENDING);
        createTaskDirectly("Task B", TaskStatus.PENDING);
        createTaskDirectly("Task C", TaskStatus.IN_PROGRESS);
        createTaskDirectly("Task D", TaskStatus.COMPLETED);

        mockMvc.perform(get("/api/tasks/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/tasks/status/IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task C")));

        mockMvc.perform(get("/api/tasks/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/tasks/status/CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Scenario: Validation enforcement end-to-end.
     * Verifies that bean validation prevents invalid data from reaching
     * the service or database.
     */
    @Test
    void validation_ShouldRejectInvalidRequests_EndToEnd() throws Exception {
        // Missing title
        TaskRequest noTitle = new TaskRequest();
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());

        // Empty title
        TaskRequest emptyTitle = new TaskRequest();
        emptyTitle.setTitle("");
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyTitle)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Scenario: Not-found error propagation.
     * Verifies the error response structure returned for missing resources.
     */
    @Test
    void notFound_ShouldReturnStructuredErrorResponse() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("999")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ---- Helper ----

    private void createTaskDirectly(String title, TaskStatus status) throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle(title);

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), TaskResponse.class);

        // Update status if not PENDING
        if (status != TaskStatus.PENDING) {
            mockMvc.perform(patch("/api/tasks/{id}/status", created.getId())
                    .param("status", status.name()))
                    .andExpect(status().isOk());
        }
    }
}
