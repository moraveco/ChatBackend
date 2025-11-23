package com.moraveco.springboot.messaging.controller;

import com.moraveco.springboot.storage.controller.FileController;
import com.moraveco.springboot.messaging.entity.ChatMessage;
import com.moraveco.springboot.messaging.entity.ReadReceiptMessage;
import com.moraveco.springboot.messaging.entity.TypingIndicator;
import com.moraveco.springboot.messaging.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository chatMessageRepository;
    private final FileController fileController;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Long> typingUsers = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    public MessageController(MessageRepository chatMessageRepository, FileController fileController, SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.fileController = fileController;
        this.messagingTemplate = messagingTemplate;

        // Create upload directory if it doesn't exist
        try {
            // Configure upload directory
            String UPLOAD_DIR = "uploads/images/";
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    // WebSocket message endpoint
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessage message) {
        String id = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().toString();

        long count = chatMessageRepository.isBlocked(message.getSenderUid(), message.getReceiverUid());
        log.info("Block check result: {}", count);
        if (count > 0) {
            log.info("Blocked - message will not be sent");
            return;
        }

        chatMessageRepository.insertMessage(
                id,
                message.getSenderUid(),
                message.getReceiverUid(),
                message.getContent(),
                timestamp,
                false,
                message.getRespondMessageId(),
                message.getImageUrl()
        );

        message.setId(id);
        message.setTimestamp(timestamp);
        message.setIsRead(false);

        // Send to specific user
        messagingTemplate.convertAndSendToUser(
                message.getReceiverUid(),
                "/queue/messages",
                message
        );

        // Send confirmation back to sender
        messagingTemplate.convertAndSendToUser(
                message.getSenderUid(),
                "/queue/messages",
                message
        );

        // Clear typing indicator for sender
        typingUsers.remove(message.getSenderUid());
        sendTypingStatus(message.getSenderUid(), message.getReceiverUid(), false);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable String messageId) {
        try {
            chatMessageRepository.deleteMessage(messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to delete message");
        }
    }

    // NEW: Respond to a message (REST endpoint)

    // NEW: Send image message
    @PostMapping("/sendImage")
    public void sendImageMessage(
            @RequestParam String senderUid,
            @RequestParam String receiverUid,
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(required = false) String caption) {

        // Validate file
        if (imageFile.isEmpty()) {
            return;
        }

        // Check if file is an image
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return;
        }

        // Generate unique filename
        String originalFilename = imageFile.getOriginalFilename();
        assert originalFilename != null;
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID() + fileExtension;

        // Save file
        fileController.serveImage(filename);

        // Create message content (image URL + caption)
        String imageUrl = "/uploads/images/" + filename;
        String messageContent = caption != null ? caption : "";

        // Save message to database
        String id = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().toString();

        chatMessageRepository.insertMessage(
                id,
                senderUid,
                receiverUid,
                messageContent,
                timestamp,
                false,
                null,
                imageUrl
        );

        // Create message object
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSenderUid(senderUid);
        message.setReceiverUid(receiverUid);
        message.setContent(messageContent);
        message.setTimestamp(timestamp);
        message.setIsRead(false);

        // Send via WebSocket
        messagingTemplate.convertAndSendToUser(
                receiverUid,
                "/queue/messages",
                message
        );

        // Send confirmation back to sender
        messagingTemplate.convertAndSendToUser(
                senderUid,
                "/queue/messages",
                message
        );


    }

    // REST endpoint to delete a message


    @MessageMapping("/chat/read")
    public void markAsRead(ReadReceiptMessage readReceipt) {
        // Update database
        chatMessageRepository.markMessageAsRead(readReceipt.getMessageId());

        // Send read receipt to sender
        messagingTemplate.convertAndSendToUser(
                readReceipt.getSenderUid(),
                "/queue/read-receipts",
                readReceipt
        );
    }

    @MessageMapping("/chat/typing")
    public void handleTyping(TypingIndicator typingIndicator) {
        String userKey = typingIndicator.getSenderUid();

        if (typingIndicator.isTyping()) {
            typingUsers.put(userKey, System.currentTimeMillis());
        } else {
            typingUsers.remove(userKey);
        }

        sendTypingStatus(
                typingIndicator.getSenderUid(),
                typingIndicator.getReceiverUid(),
                typingIndicator.isTyping()
        );
    }

    private void sendTypingStatus(String senderUid, String receiverUid, boolean isTyping) {
        TypingIndicator indicator = new TypingIndicator();
        indicator.setSenderUid(senderUid);
        indicator.setReceiverUid(receiverUid);
        indicator.setTyping(isTyping);

        messagingTemplate.convertAndSendToUser(
                receiverUid,
                "/queue/typing",
                indicator
        );
    }

    // Scheduled task to clear stale typing indicators
    @Scheduled(fixedRate = 3000)
    public void clearStaleTypingIndicators() {
        long currentTime = System.currentTimeMillis();
        long timeout = 5000;

        typingUsers.entrySet().removeIf(entry -> {
            boolean isStale = currentTime - entry.getValue() > timeout;
            if (isStale) {
                // Send typing stopped notification
                // Note: You'd need to store receiver info to send proper notification
                // This is a simplified version
            }
            return isStale;
        });
    }

    // REST endpoint to mark multiple messages as read
    @PostMapping("/markAsRead")
    public ResponseEntity<?> markMessagesAsRead(@RequestBody List<String> messageIds) {
        for (String messageId : messageIds) {
            chatMessageRepository.markMessageAsRead(messageId);
        }
        return ResponseEntity.ok().build();
    }

    // REST endpoint to get unread message count
    @GetMapping("/unreadCount")
    public ResponseEntity<Integer> getUnreadCount(@RequestParam String userUid) {
        int count = chatMessageRepository.getUnreadMessageCount(userUid);
        return ResponseEntity.ok(count);
    }

    // REST endpoint to get all messages
    @GetMapping("/getAllMessages")
    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.getAllMessages();
    }

    @GetMapping("/messages")
    public ResponseEntity<Page<ChatMessage>> getMessages(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<ChatMessage> messages = chatMessageRepository.findConversation(senderId, receiverId, pageable);

        return ResponseEntity.ok(messages);
    }
}