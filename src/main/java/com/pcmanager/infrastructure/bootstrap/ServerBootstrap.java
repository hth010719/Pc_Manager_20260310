package com.pcmanager.infrastructure.bootstrap;

import com.pcmanager.application.member.MemberService;
import com.pcmanager.application.message.MessageService;
import com.pcmanager.application.order.OrderService;
import com.pcmanager.application.order.ProductService;
import com.pcmanager.application.seat.SeatService;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.member.UserRole;
import com.pcmanager.domain.product.Product;
import com.pcmanager.domain.product.SaleStatus;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.domain.seat.SeatStatus;
import com.pcmanager.infrastructure.network.PcSocketServer;
import com.pcmanager.infrastructure.persistence.file.MemberFileStore;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.nio.file.Path;
import java.util.List;

public record ServerBootstrap(
        SeatService seatService,
        MemberService memberService,
        ProductService productService,
        OrderService orderService,
        MessageService messageService,
        PcSocketServer socketServer
) {
    public static ServerBootstrap create(int port) {
        MemoryStore store = new MemoryStore();
        SeatService seatService = new SeatService(store);
        MemberFileStore memberFileStore = new MemberFileStore(Path.of("data", "members.txt"));
        List<Member> defaultMembers = List.of(
                new Member(1L, "hong", "hashed_pw_1", "홍길동", "010-1111-2222", 120, 300, "SILVER", UserRole.CUSTOMER),
                new Member(2L, "lee", "hashed_pw_2", "이영희", "010-2222-3333", 80, 150, "BRONZE", UserRole.CUSTOMER)
        );
        store.getMembers().addAll(memberFileStore.loadMembers(defaultMembers));
        long maxMemberId = store.getMembers().stream().mapToLong(Member::getMemberId).max().orElse(0L);
        store.initializeMemberSequence(maxMemberId);

        store.getSeats().add(new Seat(1L, 1, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(2L, 2, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(3L, 3, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(4L, 4, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(5L, 5, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(6L, 6, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(7L, 7, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(8L, 8, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(9L, 9, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(10L, 10, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(11L, 11, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(12L, 12, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(13L, 13, SeatStatus.AVAILABLE, null, null, null, null));
        store.getSeats().add(new Seat(14L, 14, SeatStatus.AVAILABLE, null, null, null, null));

        store.getProducts().add(new Product(1L, 1L, "콜라", 2000, 20, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(2L, 1L, "사이다", 2000, 14, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(3L, 2L, "컵라면", 3500, 7, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(4L, 2L, "핫도그", 3000, 5, SaleStatus.ON_SALE));

        ProductService productService = new ProductService(store);
        MemberService memberService = new MemberService(store, memberFileStore);
        MessageService messageService = new MessageService(store);
        OrderService orderService = new OrderService(store, productService);
        PcSocketServer socketServer = new PcSocketServer(port, seatService, memberService, productService, orderService, messageService);

        return new ServerBootstrap(
                seatService,
                memberService,
                productService,
                orderService,
                messageService,
                socketServer
        );
    }
}
