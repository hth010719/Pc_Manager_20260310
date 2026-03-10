package com.pcmanager.domain.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private final Long orderId;
    private final Long seatId;
    private final Long memberId;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus orderStatus;
    private final LocalDateTime requestedAt;

    public Order(Long orderId, Long seatId, Long memberId, List<OrderItem> items, OrderStatus orderStatus, LocalDateTime requestedAt) {
        this.orderId = orderId;
        this.seatId = seatId;
        this.memberId = memberId;
        this.items.addAll(items);
        this.orderStatus = orderStatus;
        this.requestedAt = requestedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getTotalPrice() {
        return items.stream().mapToInt(OrderItem::getLinePrice).sum();
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void moveNextStatus() {
        orderStatus = orderStatus.next();
    }

    public void changeStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}
