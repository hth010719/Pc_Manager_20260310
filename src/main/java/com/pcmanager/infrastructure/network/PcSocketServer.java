package com.pcmanager.infrastructure.network;

import com.pcmanager.application.member.MemberService;
import com.pcmanager.application.message.MessageService;
import com.pcmanager.application.order.OrderService;
import com.pcmanager.application.order.ProductService;
import com.pcmanager.application.seat.SeatService;
import com.pcmanager.common.util.ProtocolCodec;
import com.pcmanager.common.util.TimeFormatter;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.message.Message;
import com.pcmanager.domain.message.MessageType;
import com.pcmanager.domain.order.Order;
import com.pcmanager.domain.order.OrderStatus;
import com.pcmanager.domain.product.Product;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.domain.seat.SeatStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PcSocketServer {
    private final int port;
    private final SeatService seatService;
    private final MemberService memberService;
    private final ProductService productService;
    private final OrderService orderService;
    private final MessageService messageService;
    private volatile boolean running;

    public PcSocketServer(int port, SeatService seatService, MemberService memberService, ProductService productService, OrderService orderService, MessageService messageService) {
        this.port = port;
        this.seatService = seatService;
        this.memberService = memberService;
        this.productService = productService;
        this.orderService = orderService;
        this.messageService = messageService;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread serverThread = new Thread(this::runServer, "pc-socket-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                handleClient(serverSocket.accept());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("소켓 서버 실행 실패", exception);
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String request = reader.readLine();
            String response = process(request);
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private String process(String request) {
        if (request == null || request.isBlank()) {
            return error("빈 요청입니다.");
        }
        String[] tokens = request.split("\\|");
        try {
            return switch (tokens[0]) {
                case "ENTER" -> handleEnter(tokens);
                case "REGISTER" -> handleRegister(tokens);
                case "TOP_UP" -> handleTopUp(tokens);
                case "ALL_MEMBERS" -> enqueueAllMembers();
                case "DELETE_MEMBER" -> handleDeleteMember(tokens);
                case "PRODUCTS" -> enqueueProducts();
                case "SEAT" -> handleSeat(tokens);
                case "ALL_SEATS" -> enqueueAllSeats();
                case "ORDER" -> handleOrder(tokens);
                case "ORDERS" -> enqueueOrders(Long.parseLong(tokens[1]));
                case "ALL_ORDERS" -> enqueueAllOrders();
                case "ADVANCE_ORDER" -> handleAdvanceOrder(tokens);
                case "CHANGE_ORDER" -> handleChangeOrder(tokens);
                case "MESSAGE" -> handleMessage(tokens);
                case "REPLY" -> handleReply(tokens);
                case "MESSAGES" -> enqueueMessages(Long.parseLong(tokens[1]));
                case "ALL_MESSAGES" -> enqueueAllMessages();
                case "EXTEND" -> handleExtend(tokens);
                case "CHANGE_SEAT_STATUS" -> handleChangeSeatStatus(tokens);
                case "EXIT" -> handleExit(tokens);
                case "FORCE_EXIT" -> handleForceExit(tokens);
                default -> error("지원하지 않는 요청입니다.");
            };
        } catch (RuntimeException exception) {
            return error(exception.getMessage());
        }
    }

    private String handleEnter(String[] tokens) {
        Member member = memberService.getByLoginId(ProtocolCodec.decode(tokens[1]));
        Seat seat = seatService.enterMember(member);
        return "OK|" + ProtocolCodec.encode("입장 완료") + "|" + seat.getSeatId() + "|" + seat.getSeatNumber() + "|" + ProtocolCodec.encode(seat.getCurrentNickname());
    }

    private String handleRegister(String[] tokens) {
        Member member = memberService.register(ProtocolCodec.decode(tokens[1]));
        return "OK|" + ProtocolCodec.encode("회원가입 완료") + "|" + member.getMemberId();
    }

    private String handleTopUp(String[] tokens) {
        memberService.addRemainingMinutes(ProtocolCodec.decode(tokens[1]), Integer.parseInt(tokens[2]));
        return "OK|" + ProtocolCodec.encode("시간 충전 완료");
    }

    private String handleDeleteMember(String[] tokens) {
        memberService.deleteMember(Long.parseLong(tokens[1]));
        return "OK|" + ProtocolCodec.encode("회원 탈퇴 완료");
    }

    private String handleSeat(String[] tokens) {
        Seat seat = seatService.getById(Long.parseLong(tokens[1]));
        return "OK|" + seat.getSeatId() + "|" + seat.getSeatNumber() + "|" + seat.getStatus() + "|" +
                ProtocolCodec.encode(resolveUserName(seat)) + "|" + TimeFormatter.formatRemaining(seatService.getRemainingDuration(seat.getSeatId()));
    }

    private String handleOrder(String[] tokens) {
        Order order = orderService.placeOrder(Long.parseLong(tokens[1]), null, Long.parseLong(tokens[2]), Integer.parseInt(tokens[3]));
        return "OK|" + ProtocolCodec.encode("주문 완료") + "|" + order.getOrderId();
    }

    private String handleAdvanceOrder(String[] tokens) {
        orderService.advanceOrderStatus(Long.parseLong(tokens[1]));
        return "OK|" + ProtocolCodec.encode("상태 변경 완료");
    }

    private String handleChangeOrder(String[] tokens) {
        orderService.changeOrderStatus(Long.parseLong(tokens[1]), OrderStatus.valueOf(tokens[2]));
        return "OK|" + ProtocolCodec.encode("주문 상태를 바꿨습니다.");
    }

    private String handleMessage(String[] tokens) {
        messageService.sendCustomerMessage(Long.parseLong(tokens[1]), MessageType.valueOf(tokens[2]), ProtocolCodec.decode(tokens[3]));
        return "OK|" + ProtocolCodec.encode("메시지 전송 완료");
    }

    private String handleReply(String[] tokens) {
        messageService.sendCounterReply(Long.parseLong(tokens[1]), ProtocolCodec.decode(tokens[2]));
        return "OK|" + ProtocolCodec.encode("답변 전송 완료");
    }

    private String handleExtend(String[] tokens) {
        seatService.extendTime(Long.parseLong(tokens[1]), Integer.parseInt(tokens[2]));
        return "OK|" + ProtocolCodec.encode("시간 연장 완료");
    }

    private String handleChangeSeatStatus(String[] tokens) {
        seatService.changeSeatStatus(Long.parseLong(tokens[1]), SeatStatus.valueOf(tokens[2]));
        return "OK|" + ProtocolCodec.encode("좌석 상태 변경 완료");
    }

    private String handleExit(String[] tokens) {
        Long seatId = Long.parseLong(tokens[1]);
        Seat seat = seatService.getById(seatId);
        if (seat.getCurrentUserId() == null) {
            throw new IllegalStateException("회원 좌석 정보가 없습니다.");
        }
        long remainingSeconds = Math.max(seatService.getRemainingDuration(seatId).getSeconds(), 0L);
        int remainingMinutes = (int) ((remainingSeconds + 59) / 60);
        memberService.updateRemainingMinutes(seat.getCurrentUserId(), remainingMinutes);
        messageService.clearMessagesBySeat(seatId);
        seatService.forceExit(seatId);
        return "OK|" + ProtocolCodec.encode("종료 완료");
    }

    private String handleForceExit(String[] tokens) {
        Long seatId = Long.parseLong(tokens[1]);
        Seat seat = seatService.getById(seatId);
        if (seat.getCurrentUserId() != null) {
            long remainingSeconds = Math.max(seatService.getRemainingDuration(seatId).getSeconds(), 0L);
            int remainingMinutes = (int) ((remainingSeconds + 59) / 60);
            memberService.updateRemainingMinutes(seat.getCurrentUserId(), remainingMinutes);
        }
        messageService.clearMessagesBySeat(seatId);
        seatService.forceExit(seatId);
        return "OK|" + ProtocolCodec.encode("해당 좌석을 종료했습니다.");
    }

    private String enqueueProducts() {
        List<Product> products = productService.getOnSaleProducts();
        String payload = products.stream()
                .map(product -> product.getProductId() + "," + ProtocolCodec.encode(product.getName()) + "," + product.getPrice() + "," + product.getStock())
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
        return "OK|" + products.size() + "|" + payload;
    }

    private String enqueueAllMembers() {
        List<Member> members = memberService.getAllMembers();
        String payload = members.stream()
                .map(member -> member.getMemberId() + "," +
                        ProtocolCodec.encode(member.getLoginId()) + "," +
                        ProtocolCodec.encode(member.getName()) + "," +
                        ProtocolCodec.encode(member.getPhone()) + "," +
                        member.getRemainingMinutes() + "," +
                        member.getPoint() + "," +
                        member.getGrade())
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
        return "OK|" + members.size() + "|" + payload;
    }

    private String enqueueAllSeats() {
        List<Seat> seats = seatService.getAllSeats();
        String payload = seats.stream()
                .map(seat -> {
                    String remaining = seat.getExpectedEndTime() == null ? "-" : TimeFormatter.formatRemaining(seatService.getRemainingDuration(seat.getSeatId()));
                    return seat.getSeatId() + "," + seat.getSeatNumber() + "," + seat.getStatus() + "," + ProtocolCodec.encode(resolveUserName(seat)) + "," + remaining;
                })
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
        return "OK|" + seats.size() + "|" + payload;
    }

    private String enqueueOrders(Long seatId) {
        List<Order> orders = orderService.getAllOrders().stream().filter(order -> order.getSeatId().equals(seatId)).toList();
        return "OK|" + orders.size() + "|" + buildOrderPayload(orders);
    }

    private String enqueueAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return "OK|" + orders.size() + "|" + buildOrderPayload(orders);
    }

    private String buildOrderPayload(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    String itemSummary = order.getItems().stream()
                            .map(item -> item.getProductName() + " x" + item.getQuantity())
                            .findFirst()
                            .orElse("-");
                    return order.getOrderId() + "," + order.getSeatId() + "," + ProtocolCodec.encode(itemSummary) + "," + order.getOrderStatus() + "," + order.getTotalPrice();
                })
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
    }

    private String enqueueMessages(Long seatId) {
        List<Message> messages = messageService.getMessagesBySeat(seatId);
        return "OK|" + messages.size() + "|" + buildMessagePayload(messages);
    }

    private String enqueueAllMessages() {
        List<Message> messages = seatService.getAllSeats().stream()
                .flatMap(seat -> messageService.getMessagesBySeat(seat.getSeatId()).stream())
                .toList();
        return "OK|" + messages.size() + "|" + buildMessagePayload(messages);
    }

    private String buildMessagePayload(List<Message> messages) {
        return messages.stream()
                .map(message -> message.getSeatId() + "," + message.getSenderType() + "," + message.getMessageType() + "," + ProtocolCodec.encode(message.getContent()))
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
    }

    private String resolveUserName(Seat seat) {
        return seat.getCurrentNickname() == null ? "-" : seat.getCurrentNickname();
    }

    private String error(String message) {
        return "ERROR|" + ProtocolCodec.encode(message == null ? "요청 처리 실패" : message);
    }
}
