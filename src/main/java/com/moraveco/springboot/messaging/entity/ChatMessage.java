package com.moraveco.springboot.messaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "messages")
public class ChatMessage {
    @Id
    @Column(length = 100, nullable = false, unique = true)
    private String id;

    private String senderUid;
    private String receiverUid;
    private String content;
    private String timestamp;
    @Column(name = "`read`")
    private Boolean isRead = false;
    @Column(name = "respond_message_id")
    private String respondMessageId;
    @Column(name = "image_url")
    private String imageUrl;
    // Constructors, Getters, Setters
}
