package com.pcmanager.infrastructure.network;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.common.util.ProtocolCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PcSocketClient {
    private final String host;
    private final int port;

    public PcSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public EnterSeatSnapshot enterByLoginId(String loginId) {
        String[] tokens = sendAndParseOk("ENTER|" + ProtocolCodec.encode(loginId), 5);
        return new EnterSeatSnapshot(Long.parseLong(tokens[2]), Integer.parseInt(tokens[3]), ProtocolCodec.decode(tokens[4]));
    }

    public SocketResponse registerMember(String loginId) {
        sendAndParseOk("REGISTER|" + ProtocolCodec.encode(loginId), 2);
        return new SocketResponse(true, "회원가입이 완료되었습니다.");
    }

    public SocketResponse chargeTime(String loginId, int minutes) {
        sendAndParseOk("TOP_UP|" + ProtocolCodec.encode(loginId) + "|" + minutes, 2);
        return new SocketResponse(true, "시간이 충전되었습니다.");
    }

    public List<ProductSnapshot> getProducts() {
        return parseProducts(send("PRODUCTS"));
    }

    public SeatSnapshot getSeat(Long seatId) {
        String[] tokens = sendAndParseOk("SEAT|" + seatId, 6);
        return new SeatSnapshot(
                Long.parseLong(tokens[1]),
                Integer.parseInt(tokens[2]),
                tokens[3],
                ProtocolCodec.decode(tokens[4]),
                tokens[5]
        );
    }

    public List<SeatSnapshot> getAllSeats() {
        return parseSeats(send("ALL_SEATS"));
    }

    public SocketResponse placeOrder(Long seatId, Long productId, int quantity) {
        String[] tokens = sendAndParseOk("ORDER|" + seatId + "|" + productId + "|" + quantity, 3);
        return new SocketResponse(true, "주문이 접수되었습니다. 주문번호=" + tokens[2]);
    }

    public List<OrderSnapshot> getOrders(Long seatId) {
        return parseOrders(send("ORDERS|" + seatId));
    }

    public List<OrderSnapshot> getAllOrders() {
        return parseOrders(send("ALL_ORDERS"));
    }

    public SocketResponse advanceOrder(Long orderId) {
        sendAndParseOk("ADVANCE_ORDER|" + orderId, 2);
        return new SocketResponse(true, "주문 상태가 변경되었습니다.");
    }

    public SocketResponse changeOrderStatus(Long orderId, String status) {
        sendAndParseOk("CHANGE_ORDER|" + orderId + "|" + status, 2);
        return new SocketResponse(true, "주문 상태를 바꿨습니다.");
    }

    public SocketResponse sendMessage(Long seatId, String messageType, String content) {
        sendAndParseOk("MESSAGE|" + seatId + "|" + messageType + "|" + ProtocolCodec.encode(content), 2);
        return new SocketResponse(true, "메시지를 전송했습니다.");
    }

    public SocketResponse sendReply(Long seatId, String content) {
        sendAndParseOk("REPLY|" + seatId + "|" + ProtocolCodec.encode(content), 2);
        return new SocketResponse(true, "답변을 전송했습니다.");
    }

    public List<MessageSnapshot> getMessages(Long seatId) {
        return parseMessages(send("MESSAGES|" + seatId));
    }

    public List<MessageSnapshot> getAllMessages() {
        return parseMessages(send("ALL_MESSAGES"));
    }

    public SocketResponse extendTime(Long seatId, int minutes) {
        sendAndParseOk("EXTEND|" + seatId + "|" + minutes, 2);
        return new SocketResponse(true, "시간이 연장되었습니다.");
    }

    public SocketResponse changeSeatStatus(Long seatId, String status) {
        sendAndParseOk("CHANGE_SEAT_STATUS|" + seatId + "|" + status, 2);
        return new SocketResponse(true, "좌석 상태가 변경되었습니다.");
    }

    public SocketResponse exitSeat(Long seatId) {
        sendAndParseOk("EXIT|" + seatId, 2);
        return new SocketResponse(true, "남은 시간이 저장되고 종료되었습니다.");
    }

    public SocketResponse forceExit(Long seatId) {
        sendAndParseOk("FORCE_EXIT|" + seatId, 2);
        return new SocketResponse(true, "해당 좌석을 종료했습니다.");
    }

    private List<ProductSnapshot> parseProducts(String response) {
        return parseRecords(response, 4).stream()
                .map(tokens -> new ProductSnapshot(
                        Long.parseLong(tokens[0]),
                        ProtocolCodec.decode(tokens[1]),
                        Integer.parseInt(tokens[2]),
                        Integer.parseInt(tokens[3])
                ))
                .toList();
    }

    private List<SeatSnapshot> parseSeats(String response) {
        return parseRecords(response, 5).stream()
                .map(tokens -> new SeatSnapshot(
                        Long.parseLong(tokens[0]),
                        Integer.parseInt(tokens[1]),
                        tokens[2],
                        ProtocolCodec.decode(tokens[3]),
                        tokens[4]
                ))
                .toList();
    }

    private List<OrderSnapshot> parseOrders(String response) {
        return parseRecords(response, 5).stream()
                .map(tokens -> new OrderSnapshot(
                        Long.parseLong(tokens[0]),
                        Long.parseLong(tokens[1]),
                        ProtocolCodec.decode(tokens[2]),
                        tokens[3],
                        Integer.parseInt(tokens[4])
                ))
                .toList();
    }

    private List<MessageSnapshot> parseMessages(String response) {
        return parseRecords(response, 4).stream()
                .map(tokens -> new MessageSnapshot(
                        Long.parseLong(tokens[0]),
                        tokens[1],
                        tokens[2],
                        ProtocolCodec.decode(tokens[3])
                ))
                .toList();
    }

    private String send(String request) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write(request);
            writer.newLine();
            writer.flush();
            return reader.readLine();
        } catch (IOException exception) {
            throw new BusinessException("소켓 통신에 실패했습니다. " + exception.getMessage());
        }
    }

    private String[] sendAndParseOk(String request, int minLength) {
        return parseOk(send(request), minLength);
    }

    private String[] parseOk(String response, int minLength) {
        if (response == null || response.isBlank()) {
            throw new BusinessException("서버 응답이 없습니다.");
        }
        String[] tokens = response.split("\\|");
        if ("ERROR".equals(tokens[0])) {
            throw new BusinessException(tokens.length > 1 ? ProtocolCodec.decode(tokens[1]) : "요청 처리 실패");
        }
        if (!"OK".equals(tokens[0]) || tokens.length < minLength) {
            throw new BusinessException("잘못된 서버 응답입니다.");
        }
        return tokens;
    }

    private List<String[]> parseRecords(String response, int tokenSize) {
        String[] tokens = parseOk(response, 2);
        int expectedCount;
        try {
            expectedCount = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException exception) {
            throw new BusinessException("잘못된 목록 응답입니다.");
        }

        List<String[]> result = new ArrayList<>();
        if (tokens.length == 2 || tokens[2].isBlank()) {
            if (expectedCount != 0) {
                throw new BusinessException("잘못된 목록 응답입니다.");
            }
            return result;
        }

        String[] rows = tokens[2].split(";");
        if (rows.length != expectedCount) {
            throw new BusinessException("잘못된 목록 응답입니다.");
        }

        for (String row : rows) {
            String[] record = row.split(",");
            if (record.length != tokenSize) {
                throw new BusinessException("잘못된 목록 응답입니다.");
            }
            result.add(record);
        }
        return result;
    }
}
