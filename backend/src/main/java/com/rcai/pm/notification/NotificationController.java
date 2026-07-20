package com.rcai.pm.notification;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public NotificationService.NotificationInbox inbox(Authentication authentication) {
        return service.inbox(authentication);
    }
    @PostMapping("/{id}/read") public void markRead(@PathVariable Long id, Authentication authentication) {
        service.markRead(id, authentication);
    }
}
