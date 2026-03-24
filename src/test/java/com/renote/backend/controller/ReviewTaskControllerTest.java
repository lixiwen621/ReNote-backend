package com.renote.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.service.ReviewTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewTaskController.class)
class ReviewTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewTaskService reviewTaskService;

    @Test
    void shouldCreateTaskSuccessfully() throws Exception {
        CreateReviewTaskRequest request = new CreateReviewTaskRequest();
        request.setUserId(1L);
        request.setTitle("Java并发笔记");
        request.setSourceType(1);
        request.setNoteUrl("https://example.com/note/1");

        ReviewTaskResponse response = ReviewTaskResponse.builder()
                .id(1001L)
                .userId(1L)
                .title("Java并发笔记")
                .sourceType(1)
                .noteUrl("https://example.com/note/1")
                .timezone("Asia/Shanghai")
                .scheduleMode(2)
                .status(1)
                .build();

        Mockito.when(reviewTaskService.createTask(Mockito.any(CreateReviewTaskRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/review-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.title").value("Java并发笔记"));
    }

    @Test
    void shouldReturnValidationErrorWhenTitleMissing() throws Exception {
        CreateReviewTaskRequest request = new CreateReviewTaskRequest();
        request.setUserId(1L);
        request.setSourceType(1);

        mockMvc.perform(post("/api/review-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldGetTaskSuccessfully() throws Exception {
        ReviewTaskResponse response = ReviewTaskResponse.builder()
                .id(1001L)
                .userId(1L)
                .title("Java并发笔记")
                .sourceType(1)
                .noteUrl("https://example.com/note/1")
                .timezone("Asia/Shanghai")
                .scheduleMode(2)
                .status(1)
                .build();

        Mockito.when(reviewTaskService.getTask(1001L)).thenReturn(response);

        mockMvc.perform(get("/api/review-tasks/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void shouldCompleteReviewSuccessfully() throws Exception {
        ReviewCompleteRequest request = new ReviewCompleteRequest();
        request.setUserId(1L);
        request.setReviewResult(1);
        request.setConfidenceScore(4);
        request.setNote("已完成一次复习");

        mockMvc.perform(post("/api/review-tasks/1001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
