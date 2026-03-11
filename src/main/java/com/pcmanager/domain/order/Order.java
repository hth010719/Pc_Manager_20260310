package com.pcmanager.domain.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 고객이 한 번에 요청한 주문 묶음을 표현하는 도메인 객체다.
 *
 * 한 주문 안에는 여러 OrderItem이 들어갈 수 있고,
 * 좌석/회원/상태/요청 시각을 함께 보관한다.
 */
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

    /**
     * 주문 총액은 각 라인 금액의 합으로 계산한다.
     */
    public int getTotalPrice() {
        return items.stream().mapToInt(OrderItem::getLinePrice).sum();
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    /**
     * 상태 흐름을 다음 단계로 한 칸 전진시킨다.
     * enum 내부 next() 규칙을 그대로 따른다.
     */
    public void moveNextStatus() {
        orderStatus = orderStatus.next();
    }

    /**
     * 카운터에서 지정한 상태로 직접 변경한다.
     */
    public void changeStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}
