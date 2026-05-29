package com.homeservices.controller;

import com.homeservices.dto.request.AdminSupportReplyRequest;
import com.homeservices.dto.request.SendSupportMessageRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.SupportMessageResponse;
import com.homeservices.dto.response.UserThreadResponse;
import com.homeservices.service.SupportChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SupportChatController {

    private final SupportChatService supportChatService;

    // ── USER endpoints (secured via /user/** → hasRole USER) ─────────────────

    /**
     * GET /user/support/messages
     * User fetches their own support thread.
     */
    @GetMapping("/user/support/messages")
    public ResponseEntity<ApiResponse<List<SupportMessageResponse>>> getUserMessages() {
        return ResponseEntity.ok(ApiResponse.success(supportChatService.getUserMessages()));
    }

    /**
     * POST /user/support/send
     * User sends a support message.
     */
    @PostMapping("/user/support/send")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> userSend(
            @RequestBody SendSupportMessageRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.userSend(req.getContent())));
    }

    /**
     * POST /user/support/mark-read
     * User marks all admin messages as read (called when chat opens).
     */
    @PostMapping("/user/support/mark-read")
    public ResponseEntity<ApiResponse<String>> userMarkRead() {
        supportChatService.userMarkAdminMessagesRead();
        return ResponseEntity.ok(ApiResponse.success("Marked as read"));
    }

    // ── ADMIN endpoints (secured via /admin/** → hasRole ADMIN) ─────────────

    /**
     * GET /admin/support/threads
     * Admin gets list of all user support threads.
     */
    @GetMapping("/admin/support/threads")
    public ResponseEntity<ApiResponse<List<UserThreadResponse>>> getAllThreads() {
        return ResponseEntity.ok(ApiResponse.success(supportChatService.getAllThreads()));
    }

    /**
     * GET /admin/support/threads/{userId}
     * Admin views a specific user's thread.
     */
    @GetMapping("/admin/support/threads/{userId}")
    public ResponseEntity<ApiResponse<List<SupportMessageResponse>>> getThread(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.getThreadMessages(userId)));
    }

    /**
     * POST /admin/support/reply
     * Admin replies to a user's support thread.
     */
    @PostMapping("/admin/support/reply")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> adminReply(
            @RequestBody AdminSupportReplyRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.adminReply(req.getUserId(), req.getContent())));
    }

    /**
     * POST /admin/support/mark-read/{userId}
     * Admin marks user messages in a thread as read.
     */
    @PostMapping("/admin/support/mark-read/{userId}")
    public ResponseEntity<ApiResponse<String>> adminMarkRead(
            @PathVariable String userId) {
        supportChatService.adminMarkUserMessagesRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read"));
    }
}
