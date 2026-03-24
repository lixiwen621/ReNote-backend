package com.renote.backend.controller;

import com.renote.backend.common.ApiResponse;
import com.renote.backend.service.ReminderDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderDispatchService reminderDispatchService;

    @PostMapping("/dispatch")
    public ApiResponse<Void> dispatchNow() {
        reminderDispatchService.dispatchDueReminders();
        return ApiResponse.success(null);
    }
}
