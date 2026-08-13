package com.enterprise.lab.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Kiểm tra API độc lập với MariaDB để CI chạy nhanh trong mỗi Pull Request.
@WebMvcTest(TodoController.class)
class TodoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoRepository repository;

    @Test
    void createsTodoWithValidTitle() throws Exception {
        given(repository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"CI test\"}"))
            .andExpect(status().isCreated());
    }
}
