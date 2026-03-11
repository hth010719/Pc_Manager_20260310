package com.pcmanager.application.message;

import com.pcmanager.domain.message.Message;
import com.pcmanager.domain.message.MessageType;
import com.pcmanager.domain.message.SenderType;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 고객/카운터 메시지 도메인을 관리하는 서비스다.
 *
 * 현재 구현은 MemoryStore 기반이라 별도 DB 없이 메모리 컬렉션을 바로 조작한다.
 * 좌석별 대화 조회, 고객 메시지 생성, 카운터 답변 생성, 좌석 종료 시 대화 정리를 담당한다.
 */
public class MessageService {
    private final MemoryStore store;

    public MessageService(MemoryStore store) {
        this.store = store;
    }

    /**
     * 특정 좌석에 속한 대화만 시간순 원본 그대로 반환한다.
     */
    public List<Message> getMessagesBySeat(Long seatId) {
        return store.getMessages().stream()
                .filter(message -> message.getSeatId().equals(seatId))
                .toList();
    }

    /**
     * 고객이 보낸 메시지를 새 메시지 ID와 현재 시각으로 저장한다.
     */
    public Message sendCustomerMessage(Long seatId, MessageType messageType, String content) {
        Message message = new Message(store.nextMessageId(), seatId, SenderType.CUSTOMER, messageType, content, LocalDateTime.now());
        store.getMessages().add(message);
        return message;
    }

    /**
     * 카운터 답변은 일반 채팅 타입으로 통일해 저장한다.
     * 카운터 화면에서는 메시지 타입보다 답변 내용 전달이 더 중요하기 때문이다.
     */
    public Message sendCounterReply(Long seatId, String content) {
        Message message = new Message(store.nextMessageId(), seatId, SenderType.COUNTER, MessageType.GENERAL_CHAT, content, LocalDateTime.now());
        store.getMessages().add(message);
        return message;
    }

    /**
     * 좌석 종료나 강제 종료 시 해당 좌석의 이전 메시지를 한 번에 정리한다.
     */
    public void clearMessagesBySeat(Long seatId) {
        store.getMessages().removeIf(message -> message.getSeatId().equals(seatId));
    }
}
