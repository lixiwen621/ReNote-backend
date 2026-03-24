package com.renote.backend.scheduler;

import com.renote.backend.service.ReminderDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderDispatchService reminderDispatchService;

    @Scheduled(fixedDelayString = "${review.scheduler.delay-ms:10000}")
    public void scanAndDispatch() {
        reminderDispatchService.dispatchDueReminders();
        log.debug("reminder scheduler executed");
    }
}
