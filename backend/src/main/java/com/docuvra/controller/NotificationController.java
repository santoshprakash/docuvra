package com.docuvra.controller;

import com.docuvra.dto.NotificationResponse;
import com.docuvra.dto.NotificationSummaryResponse;
import com.docuvra.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public List<NotificationResponse> notifications() {
        return notificationService.listMine();
    }

    @GetMapping("/api/notifications/summary")
    public NotificationSummaryResponse summary() {
        return notificationService.summary();
    }

    @PostMapping("/api/notifications/{notificationId}/read")
    public void markRead(@PathVariable UUID notificationId) {
        notificationService.markRead(notificationId);
    }

    @PostMapping("/api/notifications/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }
}
