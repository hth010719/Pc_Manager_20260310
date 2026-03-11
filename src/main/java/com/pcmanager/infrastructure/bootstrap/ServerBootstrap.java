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

        store.getProducts().add(new Product(1L, 1L, "스팸 마요 덮밥", 6900, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(2L, 1L, "치킨 마요 덮밥", 7200, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(3L, 1L, "김치볶음밥", 6800, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(4L, 1L, "삼겹살 정식", 8900, 25, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(5L, 2L, "짜계치", 5200, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(6L, 2L, "라볶이", 5900, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(7L, 2L, "신라면", 3900, 40, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(8L, 2L, "진라면", 3900, 40, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(9L, 2L, "참깨라면", 4200, 35, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(10L, 3L, "소떡소떡", 3800, 40, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(11L, 3L, "치킨 가라아게", 5500, 30, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(12L, 3L, "감자튀김", 3500, 35, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(13L, 3L, "만두", 4000, 35, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(14L, 4L, "콜라", 2000, 50, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(15L, 4L, "사이다", 2000, 50, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(16L, 4L, "아메리카노", 2800, 40, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(17L, 4L, "아이스티", 2500, 40, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(18L, 5L, "단무지 추가", 0, 999, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(19L, 5L, "김치 추가", 0, 999, SaleStatus.ON_SALE));
        store.getProducts().add(new Product(20L, 5L, "공깃밥 추가", 1000, 50, SaleStatus.ON_SALE));

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
