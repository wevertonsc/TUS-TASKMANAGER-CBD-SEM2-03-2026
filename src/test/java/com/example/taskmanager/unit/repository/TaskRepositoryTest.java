package com.example.taskmanager.unit.repository;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        task1 = new Task();
        task1.setTitle("Repository Test 1");
        task1.setDescription("Description 1");
        task1.setStatus(TaskStatus.PENDING);
        task1.setCreatedAt(LocalDateTime.now());
        task1.setUpdatedAt(LocalDateTime.now());

        task2 = new Task();
        task2.setTitle("Repository Test 2");
        task2.setDescription("Description 2");
        task2.setStatus(TaskStatus.COMPLETED);
        task2.setCreatedAt(LocalDateTime.now());
        task2.setUpdatedAt(LocalDateTime.now());

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();
    }

    @Test
    void findByStatus_ShouldReturnTasksWithSpecificStatus() {
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);

        assertThat(pendingTasks).hasSize(1);
        assertThat(pendingTasks.get(0).getTitle()).isEqualTo("Repository Test 1");
    }

    @Test
    void findByTitleContainingIgnoreCase_ShouldReturnMatchingTasks() {
        List<Task> tasks = taskRepository.findByTitleContainingIgnoreCase("test");

        assertThat(tasks).hasSize(2);
    }

    @Test
    void findOverdueTasks_ShouldReturnTasksWithDueDatePassed() {
        Task overdueTask = new Task();
        overdueTask.setTitle("Overdue Task");
        overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
        overdueTask.setStatus(TaskStatus.PENDING);
        overdueTask.setCreatedAt(LocalDateTime.now());
        overdueTask.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(overdueTask);
        entityManager.flush();

        List<Task> overdueTasks = taskRepository.findOverdueTasks(LocalDateTime.now());

        assertThat(overdueTasks).hasSize(1);
        assertThat(overdueTasks.get(0).getTitle()).isEqualTo("Overdue Task");
    }

    @Test
    void shouldSaveAndRetrieveTask() {
        Task newTask = new Task();
        newTask.setTitle("New Task");
        newTask.setDescription("New Description");
        newTask.setStatus(TaskStatus.IN_PROGRESS);
        newTask.setCreatedAt(LocalDateTime.now());
        newTask.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(newTask);

        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getTitle()).isEqualTo("New Task");

        Task foundTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getTitle()).isEqualTo("New Task");
    }
}