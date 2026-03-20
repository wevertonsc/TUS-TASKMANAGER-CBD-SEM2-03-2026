package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TaskService taskService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private TaskResponse taskResponse;
    private TaskRequest taskRequest;
    
    @BeforeEach
    void setUp() {
        taskResponse = new TaskResponse(
            1L,
            "Test Task",
            "Test Description",
            TaskStatus.PENDING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7)
        );
        
        taskRequest = new TaskRequest();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
    }
    
    @Test
    void createTask_ShouldReturnCreatedTask_WhenValidRequest() throws Exception {
        when(taskService.createTask(any(TaskRequest.class))).thenReturn(taskResponse);
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")))
                .andExpect(jsonPath("$.status", is("PENDING")));
        
        verify(taskService, times(1)).createTask(any(TaskRequest.class));
    }
    
    @Test
    void createTask_ShouldReturnBadRequest_WhenInvalidTitle() throws Exception {
        taskRequest.setTitle("");
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Title is required")));
        
        verify(taskService, never()).createTask(any(TaskRequest.class));
    }
    
    @Test
    void getAllTasks_ShouldReturnListOfTasks() throws Exception {
        List<TaskResponse> tasks = Arrays.asList(taskResponse);
        when(taskService.getAllTasks()).thenReturn(tasks);
        
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Task")));
        
        verify(taskService, times(1)).getAllTasks();
    }
    
    @Test
    void getTaskById_ShouldReturnTask_WhenExists() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(taskResponse);
        
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")));
        
        verify(taskService, times(1)).getTaskById(1L);
    }
    
    @Test
    void updateTask_ShouldReturnUpdatedTask_WhenValidRequest() throws Exception {
        when(taskService.updateTask(eq(1L), any(TaskRequest.class))).thenReturn(taskResponse);
        
        mockMvc.perform(put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")));
        
        verify(taskService, times(1)).updateTask(eq(1L), any(TaskRequest.class));
    }
    
    @Test
    void updateTaskStatus_ShouldReturnUpdatedTask() throws Exception {
        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.COMPLETED))).thenReturn(taskResponse);
        
        mockMvc.perform(patch("/api/tasks/1/status")
                .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
        
        verify(taskService, times(1)).updateTaskStatus(eq(1L), eq(TaskStatus.COMPLETED));
    }
    
    @Test
    void getTasksByStatus_ShouldReturnFilteredTasks() throws Exception {
        List<TaskResponse> tasks = Arrays.asList(taskResponse);
        when(taskService.getTasksByStatus(TaskStatus.PENDING)).thenReturn(tasks);
        
        mockMvc.perform(get("/api/tasks/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
        
        verify(taskService, times(1)).getTasksByStatus(TaskStatus.PENDING);
    }
    
    @Test
    void deleteTask_ShouldReturnNoContent_WhenTaskExists() throws Exception {
        doNothing().when(taskService).deleteTask(1L);
        
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
        
        verify(taskService, times(1)).deleteTask(1L);
    }
}