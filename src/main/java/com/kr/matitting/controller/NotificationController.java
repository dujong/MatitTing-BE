package com.kr.matitting.controller;

import com.kr.matitting.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "알림 구독", description = "알림을 받기 위해 SSE 통신을 연결하는 API \n\n" +
            "Request시 Header에 'Last-Event-ID'를 넣어주어 마지막으로 받은 알림이 어디까지인지 BackEnd에게 요청")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
            HttpServletResponse response
    ) {
        response.setHeader("X-Accel-Buffering", "no");
        return ResponseEntity.ok(notificationService.subscribe(userId, lastEventId));
    }

    @Operation(summary = "알림 내역 조회", description = "알림 subscribe시 그동안의 알림 내역을 가져오는 API")
    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }
}