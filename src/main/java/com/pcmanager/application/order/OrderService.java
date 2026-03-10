package com.pcmanager.application.order;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.order.Order;
import com.pcmanager.domain.order.OrderItem;
import com.pcmanager.domain.order.OrderStatus;
import com.pcmanager.domain.product.Product;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.domain.seat.SeatStatus;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.time.LocalDateTime;
import java.util.List;

public class OrderService {
    private final MemoryStore store;
    private final ProductService productService;

    public OrderService(MemoryStore store, ProductService productService) {
        this.store = store;
        this.productService = productService;
    }

    public List<Order> getAllOrders() {
        return store.getOrders();
    }

    public Order placeOrder(Long seatId, Long memberId, Long productId, int quantity) {
        Seat seat = store.getSeats().stream()
                .filter(item -> item.getSeatId().equals(seatId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 좌석입니다."));

        if (seat.getStatus() != SeatStatus.IN_USE) {
            throw new BusinessException("사용 중인 좌석만 주문할 수 있습니다.");
        }

        Product product = productService.getById(productId);
        if (product.getStock() < quantity) {
            throw new BusinessException("재고가 부족합니다.");
        }

        product.deductStock(quantity);
        OrderItem item = new OrderItem(product.getProductId(), product.getName(), quantity, product.getPrice());
        Order order = new Order(
                store.nextOrderId(),
                seatId,
                memberId,
                List.of(item),
                OrderStatus.REQUESTED,
                LocalDateTime.now()
        );
        store.getOrders().add(order);
        return order;
    }

    public void advanceOrderStatus(Long orderId) {
        Order order = store.getOrders().stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 주문입니다. orderId=" + orderId));
        order.moveNextStatus();
    }

    public void changeOrderStatus(Long orderId, OrderStatus orderStatus) {
        Order order = store.getOrders().stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 주문입니다. orderId=" + orderId));
        order.changeStatus(orderStatus);
    }
}
