package com.moraveco.springboot.messaging.service;

import com.moraveco.springboot.messaging.entity.ChatMessage;
import com.moraveco.springboot.messaging.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final MessageRepository messageRepository;

    public SearchService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<ChatMessage> searchMessagesInConversation(String senderId, String receiverId, String query) {
        List<ChatMessage> allMessages = messageRepository.getConversationChatMessages(senderId, receiverId);

        // Ideally, this should be a database query (LIKE %query%), but for now:
        return allMessages.stream()
                .filter(msg -> msg.getContent() != null &&
                        msg.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
}