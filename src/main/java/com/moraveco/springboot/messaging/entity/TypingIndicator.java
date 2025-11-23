package com.moraveco.springboot.messaging.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TypingIndicator {
    private String senderUid;
    private String receiverUid;
    private boolean typing;

}