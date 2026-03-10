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
        String response = send("ENTER|" + ProtocolCodec.encode(loginId));
        String[] tokens = parseOk(response, 5);
        return new EnterSeatSnapshot(Long.parseLong(tokens[2]), Integer.parseInt(tokens[3]), ProtocolCodec.decode(tokens[4]));
    }

    public SocketResponse registerMember(String loginId) {
        parseOk(send("REGISTER|" + ProtocolCodec.encode(loginId)), 2);
        return new SocketResponse(true, "회원가입이 완료되었습니다.");
    }

    public SocketResponse chargeTime(String loginId, int minutes) {
        parseOk(send("TOP_UP|" + ProtocolCodec.encode(loginId) + "|" + minutes), 2);
        return new SocketResponse(true, "시간이 충전되었습니다.");
    }

    public List<ProductSnapshot> getProducts() {
        return parseRecords(send("PRODUCTS"), 4).stream()
                .map(tokens -> new ProductSnapshot(
                        Long.parseLong(tokens[0]),
                        ProtocolCodec.decode(tokens[1]),
                        Integer.parseInt(tokens[2]),
                        Integer.parseInt(tokens[3])
                ))
                .toList();
    }

    public SeatSnapshot getSeat(Long seatId) {
        String[] tokens = parseOk(send("SEAT|" + seatId), 6);
        return new SeatSnapshot(
                Long.parseLong(tokens[1]),
                Integer.parseInt(tokens[2]),
                tokens[3],
                ProtocolCodec.decode(tokens[4]),
                tokens[5]
        );
    }

    public List<SeatSnapshot> getAllSeats() {
        return parseRecords(send("ALL_SEATS"), 5).stream()
                .map(tokens -> new SeatSnapshot(
                        Long.parseLong(tokens[0]),
                        Integer.parseInt(tokens[1]),
                        tokens[2],
                        ProtocolCodec.decode(tokens[3]),
                        tokens[4]
                ))
                .toList();
    }

    public SocketResponse placeOrder(Long seatId, Long productId, int quantity) {
        String[] tokens = parseOk(send("ORDER|" + seatId + "|" + productId + "|" + quantity), 3);
        return new SocketResponse(true, "주문이 접수되었습니다. 주문번호=" + tokens[2]);
    }

    public List<OrderSnapshot> getOrders(Long seatId) {
        return parseRecords(send("ORDERS|" + seatId), 5).stream()
                .map(tokens -> new OrderSnapshot(
                        Long.parseLong(tokens[0]),
                        Long.parseLong(tokens[1]),
                        ProtocolCodec.decode(tokens[2]),
                        tokens[3],
                        Integer.parseInt(tokens[4])
                ))
                .toList();
    }

    public List<OrderSnapshot> getAllOrders() {
        return parseRecords(send("ALL_ORDERS"), 5).stream()
                .map(tokens -> new OrderSnapshot(
                        Long.parseLong(tokens[0]),
                        Long.parseLong(tokens[1]),
                        ProtocolCodec.decode(tokens[2]),
                        tokens[3],
                        Integer.parseInt(tokens[4])
                ))
                .toList();
    }

    public SocketResponse advanceOrder(Long orderId) {
        parseOk(send("ADVANCE_ORDER|" + orderId), 2);
        return new SocketResponse(true, "주문 상태가 변경되었습니다.");
    }

    public SocketResponse changeOrderStatus(Long orderId, String status) {
        parseOk(send("CHANGE_ORDER|" + orderId + "|" + status), 2);
        return new SocketResponse(true, "주문 상태를 바꿨습니다.");
    }

    public SocketResponse sendMessage(Long seatId, String messageType, String content) {
        parseOk(send("MESSAGE|" + seatId + "|" + messageType + "|" + ProtocolCodec.encode(content)), 2);
        return new SocketResponse(true, "메시지를 전송했습니다.");
    }

    public SocketResponse sendReply(Long seatId, String content) {
        parseOk(send("REPLY|" + seatId + "|" + ProtocolCodec.encode(content)), 2);
        return new SocketResponse(true, "답변을 전송했습니다.");
    }

    public List<MessageSnapshot> getMessages(Long seatId) {
        return parseRecords(send("MESSAGES|" + seatId), 4).stream()
                .map(tokens -> new MessageSnapshot(
                        Long.parseLong(tokens[0]),
                        tokens[1],
                        tokens[2],
                        ProtocolCodec.decode(tokens[3])
                ))
                .toList();
    }

    public List<MessageSnapshot> getAllMessages() {
        return parseRecords(send("ALL_MESSAGES"), 4).stream()
                .map(tokens -> new MessageSnapshot(
                        Long.parseLong(tokens[0]),
                        tokens[1],
                        tokens[2],
                        ProtocolCodec.decode(tokens[3])
                ))
                .toList();
    }

    public SocketResponse extendTime(Long seatId, int minutes) {
        parseOk(send("EXTEND|" + seatId + "|" + minutes), 2);
        return new SocketResponse(true, "시간이 연장되었습니다.");
    }

    public SocketResponse changeSeatStatus(Long seatId, String status) {
        parseOk(send("CHANGE_SEAT_STATUS|" + seatId + "|" + status), 2);
        return new SocketResponse(true, "좌석 상태가 변경되었습니다.");
    }

    public SocketResponse exitSeat(Long seatId) {
        parseOk(send("EXIT|" + seatId), 2);
        return new SocketResponse(true, "남은 시간이 저장되고 종료되었습니다.");
    }

    public SocketResponse forceExit(Long seatId) {
        parseOk(send("FORCE_EXIT|" + seatId), 2);
        return new SocketResponse(true, "해당 좌석을 종료했습니다.");
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
        List<String[]> result = new ArrayList<>();
        if (tokens.length == 2 || tokens[2].isBlank()) {
            return result;
        }
        String[] rows = tokens[2].split(";");
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
