package com.moraveco.springboot.messaging.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadReceiptMessage {
    private String messageId;
    private String senderUid;
    private String receiverUid;
    private String timestamp;
}