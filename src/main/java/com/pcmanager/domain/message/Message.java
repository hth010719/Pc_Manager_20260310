package com.pcmanager.domain.message;

import java.time.LocalDateTime;

public class Message {
    private final Long messageId;
    private final Long seatId;
    private final SenderType senderType;
    private final MessageType messageType;
    private final String content;
    private final LocalDateTime sentAt;

    public Message(Long messageId, Long seatId, SenderType senderType, MessageType messageType, String content, LocalDateTime sentAt) {
        this.messageId = messageId;
        this.seatId = seatId;
        this.senderType = senderType;
        this.messageType = messageType;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
