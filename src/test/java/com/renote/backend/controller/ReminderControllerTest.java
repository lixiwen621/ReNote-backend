package com.renote.backend.controller;

import com.renote.backend.service.ReminderDispatchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReminderController.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReminderDispatchService reminderDispatchService;

    @Test
    void shouldDispatchSuccessfully() throws Exception {
        Mockito.doNothing().when(reminderDispatchService).dispatchDueReminders();

        mockMvc.perform(post("/api/reminders/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
