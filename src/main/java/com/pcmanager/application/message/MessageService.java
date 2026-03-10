package com.pcmanager.application.message;

import com.pcmanager.domain.message.Message;
import com.pcmanager.domain.message.MessageType;
import com.pcmanager.domain.message.SenderType;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.time.LocalDateTime;
import java.util.List;

public class MessageService {
    private final MemoryStore store;

    public MessageService(MemoryStore store) {
        this.store = store;
    }

    public List<Message> getMessagesBySeat(Long seatId) {
        return store.getMessages().stream()
                .filter(message -> message.getSeatId().equals(seatId))
                .toList();
    }

    public Message sendCustomerMessage(Long seatId, MessageType messageType, String content) {
        Message message = new Message(store.nextMessageId(), seatId, SenderType.CUSTOMER, messageType, content, LocalDateTime.now());
        store.getMessages().add(message);
        return message;
    }

    public Message sendCounterReply(Long seatId, String content) {
        Message message = new Message(store.nextMessageId(), seatId, SenderType.COUNTER, MessageType.GENERAL_CHAT, content, LocalDateTime.now());
        store.getMessages().add(message);
        return message;
    }

    public void clearMessagesBySeat(Long seatId) {
        store.getMessages().removeIf(message -> message.getSeatId().equals(seatId));
    }
}
