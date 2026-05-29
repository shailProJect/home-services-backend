package com.homeservices.service;

import com.homeservices.dto.response.SupportMessageResponse;
import com.homeservices.dto.response.UserThreadResponse;
import com.homeservices.entity.SupportMessage;
import com.homeservices.entity.User;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.repository.SupportMessageRepository;
import com.homeservices.repository.UserRepository;
import com.homeservices.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportChatService {

    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    
    // ── USER side ──────────────────────────────────────────────────────────────

    /** Get all messages in the calling user's support thread. */
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getUserMessages() {
        User user = getCurrentUser();
        return supportMessageRepository.findByUserOrderByCreatedAtAsc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** User sends a support message. */
    @Transactional
    public SupportMessageResponse userSend(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content cannot be empty");
        }
        User user = getCurrentUser();
        SupportMessage msg = SupportMessage.builder()
                .user(user)
                .content(content.trim())
                .senderRole(SupportMessage.SenderRole.USER)
                .read(false)
                .build();
        return toResponse(supportMessageRepository.save(msg));
    }

    /** Mark all ADMIN messages in the user's thread as read (user opened the chat). */
    @Transactional
    public void userMarkAdminMessagesRead() {
        User user = getCurrentUser();
        supportMessageRepository.markReadByUserAndRole(user, SupportMessage.SenderRole.ADMIN);
    }

    // ── ADMIN side ─────────────────────────────────────────────────────────────

    /** Get all user threads with last-message info (for admin thread list). */
    @Transactional(readOnly = true)
    public List<UserThreadResponse> getAllThreads() {
        return supportMessageRepository.findLatestPerUser()
                .stream()
                .map(m -> {
                    long unread = supportMessageRepository.countUnreadUserMessages(m.getUser());
                    return UserThreadResponse.builder()
                            .userId(m.getUser().getId().toString())
                            .userName(m.getUser().getName())
                            .userEmail(m.getUser().getEmail())
                            .lastMessage(truncate(m.getContent(), 60))
                            .lastMessageAt(m.getCreatedAt())
                            .unreadCount(unread)
                            .build();
                })
                .toList();
    }

    /** Get all messages for a specific user's thread (admin view). */
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getThreadMessages(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return supportMessageRepository.findByUserOrderByCreatedAtAsc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Admin sends a reply to a user's thread. */
    @Transactional
    public SupportMessageResponse adminReply(String userId, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Reply content cannot be empty");
        }
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportMessage msg = SupportMessage.builder()
                .user(user)
                .content(content.trim())
                .senderRole(SupportMessage.SenderRole.ADMIN)
                .read(false)
                .build();
        return toResponse(supportMessageRepository.save(msg));
    }

    /** Admin marks all USER messages in a thread as read. */
    @Transactional
    public void adminMarkUserMessagesRead(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        supportMessageRepository.markReadByUserAndRole(user, SupportMessage.SenderRole.USER);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private SupportMessageResponse toResponse(SupportMessage m) {
        return SupportMessageResponse.builder()
                .id(m.getId().toString())
                .content(m.getContent())
                .senderRole(m.getSenderRole().name())
                .createdAt(m.getCreatedAt())
                .read(m.isRead())
                .build();
    }

    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
