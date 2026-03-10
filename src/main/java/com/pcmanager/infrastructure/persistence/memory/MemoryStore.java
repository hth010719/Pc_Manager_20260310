package com.pcmanager.infrastructure.persistence.memory;

import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.message.Message;
import com.pcmanager.domain.order.Order;
import com.pcmanager.domain.product.Product;
import com.pcmanager.domain.seat.Seat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryStore {
    private final List<Seat> seats = Collections.synchronizedList(new ArrayList<>());
    private final List<Member> members = Collections.synchronizedList(new ArrayList<>());
    private final List<Product> products = Collections.synchronizedList(new ArrayList<>());
    private final List<Order> orders = Collections.synchronizedList(new ArrayList<>());
    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    private final AtomicLong orderSequence = new AtomicLong(0L);
    private final AtomicLong messageSequence = new AtomicLong(0L);
    private final AtomicLong memberSequence = new AtomicLong(0L);

    public List<Seat> getSeats() {
        return seats;
    }

    public List<Member> getMembers() {
        return members;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public long nextOrderId() {
        return orderSequence.incrementAndGet();
    }

    public long nextMessageId() {
        return messageSequence.incrementAndGet();
    }

    public long nextMemberId() {
        return memberSequence.incrementAndGet();
    }

    public void initializeMemberSequence(long currentMaxMemberId) {
        memberSequence.set(currentMaxMemberId);
    }
}
