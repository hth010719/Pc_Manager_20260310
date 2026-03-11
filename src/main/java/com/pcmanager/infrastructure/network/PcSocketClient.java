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

/**
 * 서버 소켓 프로토콜을 호출하는 클라이언트 게이트웨이다.
 *
 * UI 계층은 이 클래스만 통해 문자열 프로토콜을 사용하고,
 * 각 메서드는 요청 문자열 생성, 응답 검증, 스냅샷 변환을 한 번에 처리한다.
 */
public class PcSocketClient {
    private final String host;
    private final int port;

    public PcSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 로그인 ID로 좌석 입장을 요청하고, 배정된 좌석 정보를 반환한다.
     */
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

    /**
     * 카운터 회원관리에서 사용할 전체 회원 목록을 받아온다.
     */
    public List<MemberSnapshot> getAllMembers() {
        return parseMembers(send("ALL_MEMBERS"));
    }

    public SocketResponse deleteMember(Long memberId) {
        sendAndParseOk("DELETE_MEMBER|" + memberId, 2);
        return new SocketResponse(true, "회원 탈퇴가 처리되었습니다.");
    }

    public List<ProductSnapshot> getProducts() {
        return parseProducts(send("PRODUCTS"));
    }

    /**
     * 특정 좌석의 최신 상태를 조회한다.
     */
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

    /**
     * 서버 프로토콜은 상품 1건씩 주문을 받으므로,
     * 장바구니도 항목별로 반복 호출해 처리한다.
     */
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

    public SocketResponse clearOrders() {
        sendAndParseOk("CLEAR_ORDERS", 2);
        return new SocketResponse(true, "주문내역을 초기화했습니다.");
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

    /**
     * `OK|count|row1;row2...` 형식 상품 목록을 ProductSnapshot 리스트로 바꾼다.
     */
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

    /**
     * 회원 목록 응답을 MemberSnapshot 리스트로 변환한다.
     */
    private List<MemberSnapshot> parseMembers(String response) {
        return parseRecords(response, 7).stream()
                .map(tokens -> new MemberSnapshot(
                        Long.parseLong(tokens[0]),
                        ProtocolCodec.decode(tokens[1]),
                        ProtocolCodec.decode(tokens[2]),
                        ProtocolCodec.decode(tokens[3]),
                        Integer.parseInt(tokens[4]),
                        Integer.parseInt(tokens[5]),
                        tokens[6]
                ))
                .toList();
    }

    /**
     * 좌석 목록 응답을 SeatSnapshot 리스트로 변환한다.
     */
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

    /**
     * 주문 목록 응답을 OrderSnapshot 리스트로 변환한다.
     */
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

    /**
     * 메시지 목록 응답을 MessageSnapshot 리스트로 변환한다.
     */
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

    /**
     * 소켓을 1회 연결해 요청 1건을 보내고 응답 1줄을 받는 가장 저수준 메서드다.
     *
     * 현재 프로토콜은 request/response 단건 왕복 구조라
     * 요청마다 새 소켓을 열고 바로 닫는 방식으로 단순하게 유지한다.
     */
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

    /**
     * 공통 `OK` 응답 검증 후 토큰 배열을 반환한다.
     */
    private String[] sendAndParseOk(String request, int minLength) {
        return parseOk(send(request), minLength);
    }

    /**
     * 단건 응답이 `OK|...` 형식인지 확인한다.
     * 서버가 업무 오류를 `ERROR|메시지`로 보내면 BusinessException으로 승격한다.
     */
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

    /**
     * 목록 응답 공통 파서다.
     *
     * 서버 목록 형식:
     * `OK|개수|record1;record2;...`
     * 각 record는 다시 `,` 기준으로 나뉘며 tokenSize 개수와 일치해야 한다.
     */
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
