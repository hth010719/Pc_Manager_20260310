package com.pcmanager.presentation.counter;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.common.util.DisplayText;
import com.pcmanager.infrastructure.network.MemberSnapshot;
import com.pcmanager.infrastructure.network.MessageSnapshot;
import com.pcmanager.infrastructure.network.OrderSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.infrastructure.network.SeatSnapshot;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 카운터 업무를 한 화면에서 처리하는 메인 패널이다.
 *
 * 좌석 관리, 주문 상태 변경, 고객 메시지 응답, 회원 탈퇴를 모두 여기서 다룬다.
 * 서버 상태와 최대한 맞춰 보여주기 위해 1초 주기로 전체 데이터를 다시 불러온다.
 */
public class CounterPanel extends JPanel {
    private static final String NO_USER = "-";
    private static final String REQUIRE_SEAT_MESSAGE = "먼저 좌석을 클릭해 주세요.";

    private final PcSocketClient socketClient;
    private final DefaultListModel<String> seatModel = new DefaultListModel<>();
    private final JList<String> seatList = new JList<>(seatModel);
    private final DefaultListModel<String> orderModel = new DefaultListModel<>();
    private final JList<String> orderList = new JList<>(orderModel);
    private final DefaultListModel<String> messageModel = new DefaultListModel<>();
    private final javax.swing.JComboBox<Long> seatManageSelector = new javax.swing.JComboBox<>();
    private final javax.swing.JComboBox<Long> replySeatSelector = new javax.swing.JComboBox<>();
    private final JTextArea replyArea = new JTextArea(4, 20);
    private final JList<String> messageList = new JList<>(messageModel);
    private final Map<Long, SeatSnapshot> seatSnapshotMap = new LinkedHashMap<>();
    private final Map<Integer, Long> seatIndexMap = new LinkedHashMap<>();
    private final Map<Integer, Long> orderIndexMap = new LinkedHashMap<>();
    private final Map<Integer, Long> messageSeatIndexMap = new LinkedHashMap<>();

    public CounterPanel(PcSocketClient socketClient) {
        this.socketClient = socketClient;
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        seatManageSelector.setRenderer(new SeatAwareRenderer("좌석 "));
        replySeatSelector.setRenderer(new SeatAwareRenderer("좌석 "));
        configureReplyArea();
        configureMessageList();
        configureSeatList();
        configureOrderList();

        JPanel content = new JPanel(new GridLayout(1, 3, 12, 12));
        content.add(createSeatPanel());
        content.add(createOrderPanel());
        content.add(createMessagePanel());
        add(content, BorderLayout.CENTER);

        refreshAll();
        // 카운터는 실시간성이 중요하므로 1초 주기로 좌석/주문/메시지를 다시 동기화한다.
        Timer timer = new Timer(1000, event -> refreshAll());
        timer.start();
    }

    /**
     * 좌석 현황 목록과 좌석 제어 버튼 영역을 만든다.
     */
    private JPanel createSeatPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("전체 좌석 현황"));
        panel.add(new JScrollPane(seatList), BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(0, 8));

        JButton memberListButton = new JButton("회원목록");
        memberListButton.addActionListener(event -> openMemberDialog());
        southPanel.add(memberListButton, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.add(new JLabel("선택한 좌석에 적용"));

        JButton add30Button = new JButton("30분 더하기");
        JButton add60Button = new JButton("1시간 더하기");
        JButton availableButton = new JButton("빈좌석");
        JButton cleaningButton = new JButton("청소 중");
        JButton maintenanceButton = new JButton("점검 중");
        JButton forceExitButton = new JButton("강제 종료");
        add30Button.addActionListener(event -> addTime(30));
        add60Button.addActionListener(event -> addTime(60));
        availableButton.addActionListener(event -> changeSeatStatus("AVAILABLE"));
        cleaningButton.addActionListener(event -> changeSeatStatus("CLEANING"));
        maintenanceButton.addActionListener(event -> changeSeatStatus("MAINTENANCE"));
        forceExitButton.addActionListener(event -> forceExit());
        actionPanel.add(add30Button);
        actionPanel.add(add60Button);
        actionPanel.add(availableButton);
        actionPanel.add(cleaningButton);
        actionPanel.add(maintenanceButton);
        actionPanel.add(forceExitButton);

        southPanel.add(actionPanel, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 주문 목록과 상태 변경 버튼을 묶는 패널이다.
     * 상태 변경은 목록에서 주문을 선택한 뒤 하단 버튼으로만 처리한다.
     */
    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("주문 관리"));
        panel.add(new JScrollPane(orderList), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.add(new JLabel("목록에서 주문을 클릭한 뒤 상태 변경"));
        JButton acceptButton = new JButton("주문 확인");
        JButton completeButton = new JButton("전달 완료");
        JButton clearButton = new JButton("초기화");
        acceptButton.addActionListener(event -> changeSelectedOrderStatus("ACCEPTED"));
        completeButton.addActionListener(event -> changeSelectedOrderStatus("COMPLETED"));
        clearButton.addActionListener(event -> clearOrders());
        actionPanel.add(acceptButton);
        actionPanel.add(completeButton);
        actionPanel.add(clearButton);
        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 고객 메시지 목록과 카운터 발신 입력 영역을 구성한다.
     */
    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("고객 메시지"));
        panel.add(new JScrollPane(messageList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(new JLabel("메시지 보낼 좌석"));
        bottom.add(replySeatSelector);
        bottom.add(new JScrollPane(replyArea));
        JButton replyButton = new JButton("메시지 보내기");
        replyButton.addActionListener(event -> sendCounterMessage());
        bottom.add(replyButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 현재 선택한 좌석에 시간을 추가한다.
     */
    private void addTime(int minutes) {
        Long seatId = requireSelectedSeatId();
        if (seatId == null) {
            return;
        }
        executeWithRefresh("시간 더하기 실패", () -> socketClient.extendTime(seatId, minutes));
    }

    /**
     * 선택 좌석을 즉시 종료시키는 관리자 동작이다.
     */
    private void forceExit() {
        Long seatId = requireSelectedSeatId();
        if (seatId == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "정말 이 좌석을 강제로 종료할까요?", "강제 종료", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        executeWithRefresh("강제 종료 실패", () -> socketClient.forceExit(seatId));
    }

    /**
     * 선택 좌석 상태를 빈좌석/청소/점검으로 강제 변경한다.
     */
    private void changeSeatStatus(String status) {
        Long seatId = requireSelectedSeatId();
        if (seatId == null) {
            return;
        }
        executeWithRefresh("좌석 상태 변경 실패", () -> socketClient.changeSeatStatus(seatId, status));
    }

    /**
     * 목록에서 고른 주문의 상태를 하단 버튼으로 바꾼다.
     */
    private void changeSelectedOrderStatus(String status) {
        Long orderId = getSelectedOrderId();
        if (orderId == null) {
            JOptionPane.showMessageDialog(this, "상태를 바꿀 주문을 먼저 클릭해 주세요.");
            return;
        }
        executeWithRefresh("주문 처리 실패", () -> socketClient.changeOrderStatus(orderId, status));
    }

    /**
     * 카운터 주문내역 전체를 초기화한다.
     * 고객 쪽 과거 주문 숨김과는 별도로, 관리 화면에서 전체 기록을 비우는 용도다.
     */
    private void clearOrders() {
        int result = JOptionPane.showConfirmDialog(this, "카운터 주문내역을 모두 초기화할까요?", "주문내역 초기화", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        executeWithRefresh("주문내역 초기화 실패", socketClient::clearOrders);
    }

    /**
     * 카운터에서 먼저 고객에게 메시지를 보낸다.
     * 현재 선택 좌석이 사용 중이면 그 좌석을 우선 사용하고, 아니면 콤보박스 선택값을 사용한다.
     */
    private void sendCounterMessage() {
        Long seatId = resolveReplySeatId();
        String content = replyArea.getText().trim();
        if (seatId == null) {
            JOptionPane.showMessageDialog(this, "메시지를 보낼 사용 중 좌석을 먼저 선택해 주세요.");
            return;
        }
        if (content.isEmpty()) {
            return;
        }
        executeWithRefresh("메시지 보내기 실패", () -> {
            socketClient.sendReply(seatId, content);
            replyArea.setText("");
        });
    }

    /**
     * Enter는 전송, Shift+Enter는 줄바꿈으로 동작하도록 입력 규칙을 바꾼다.
     */
    private void configureReplyArea() {
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-reply");
        replyArea.getActionMap().put("send-reply", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendCounterMessage();
            }
        });
        replyArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        replyArea.getActionMap().put("insert-break", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                replyArea.append(System.lineSeparator());
            }
        });
    }

    /**
     * 메시지 목록을 더블클릭하면 해당 좌석을 답장 대상 콤보박스에 자동 반영한다.
     */
    private void configureMessageList() {
        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    int index = messageList.locationToIndex(event.getPoint());
                    Long seatId = messageSeatIndexMap.get(index);
                    if (seatId != null) {
                        replySeatSelector.setSelectedItem(seatId);
                    }
                }
            }
        });
    }

    /**
     * 좌석 목록 선택이 바뀌면 관리 콤보박스와 메시지 대상 콤보박스도 같은 좌석으로 동기화한다.
     */
    private void configureSeatList() {
        seatList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            Long seatId = getSelectedSeatId();
            if (seatId != null) {
                seatManageSelector.setSelectedItem(seatId);
                replySeatSelector.setSelectedItem(seatId);
            }
        });
    }

    /**
     * 현재 구현에서는 주문 선택 자체만 추적하면 충분하므로, 선택 이벤트에서 orderId 캐시를 확인만 한다.
     */
    private void configureOrderList() {
        orderList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            getSelectedOrderId();
        });
    }

    /**
     * 좌석 선택이 필수인 동작에서 공통으로 사용하는 검증 메서드다.
     */
    private Long requireSelectedSeatId() {
        Long seatId = getSelectedSeatId();
        if (seatId == null) {
            JOptionPane.showMessageDialog(this, REQUIRE_SEAT_MESSAGE);
        }
        return seatId;
    }

    /**
     * 서버 작업 성공 후 전체 화면을 다시 동기화하고,
     * 업무 오류는 공통 다이얼로그로 보여준다.
     */
    private void executeWithRefresh(String title, Runnable action) {
        try {
            action.run();
            refreshAll();
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        }
    }

    private Long getSelectedSeatId() {
        int selectedIndex = seatList.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        return seatIndexMap.get(selectedIndex);
    }

    private Long getSelectedOrderId() {
        int selectedIndex = orderList.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        return orderIndexMap.get(selectedIndex);
    }

    /**
     * 답장 대상 좌석 결정 우선순위:
     * 1. 현재 선택한 좌석이 사용 중이면 그 좌석
     * 2. 아니면 콤보박스에서 직접 고른 좌석
     */
    private Long resolveReplySeatId() {
        Long selectedSeatId = getSelectedSeatId();
        if (selectedSeatId != null) {
            SeatSnapshot seat = seatSnapshotMap.get(selectedSeatId);
            if (seat != null && !NO_USER.equals(seat.userName())) {
                return selectedSeatId;
            }
        }
        return (Long) replySeatSelector.getSelectedItem();
    }

    /**
     * 화면 전체 데이터를 다시 불러와 좌석/주문/메시지 영역을 갱신한다.
     */
    private void refreshAll() {
        try {
            Long previousSelectedSeatId = getSelectedSeatId();
            reloadSeats(previousSelectedSeatId);
            reloadOrders();
            reloadMessages();
        } catch (BusinessException exception) {
            seatModel.clear();
            seatModel.addElement("서버와 연결되지 않았습니다: " + exception.getMessage());
        }
    }

    /**
     * 회원 목록 다이얼로그를 열고 조회/탈퇴 동작을 연결한다.
     */
    private void openMemberDialog() {
        DefaultListModel<MemberSnapshot> memberModel = new DefaultListModel<>();
        JList<MemberSnapshot> memberList = new JList<>(memberModel);
        memberList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MemberSnapshot member) {
                    String name = member.name() == null || member.name().isBlank() ? "-" : member.name();
                    setText("ID " + member.loginId() + " / 이름 " + name + " / 남은 시간 " + member.remainingMinutes() + "분");
                }
                return this;
            }
        });

        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "회원 목록", true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(memberList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("새로고침");
        JButton deleteButton = new JButton("회원탈퇴");
        JButton closeButton = new JButton("닫기");
        refreshButton.addActionListener(event -> loadMembers(memberModel));
        deleteButton.addActionListener(event -> deleteSelectedMember(memberList, memberModel));
        closeButton.addActionListener(event -> dialog.dispose());
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        loadMembers(memberModel);
        dialog.setSize(720, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 서버의 전체 회원 목록을 다이얼로그 리스트 모델에 채운다.
     */
    private void loadMembers(DefaultListModel<MemberSnapshot> memberModel) {
        memberModel.clear();
        for (MemberSnapshot member : socketClient.getAllMembers()) {
            memberModel.addElement(member);
        }
    }

    /**
     * 다이얼로그에서 선택한 회원을 탈퇴 처리한다.
     */
    private void deleteSelectedMember(JList<MemberSnapshot> memberList, DefaultListModel<MemberSnapshot> memberModel) {
        MemberSnapshot member = memberList.getSelectedValue();
        if (member == null) {
            JOptionPane.showMessageDialog(this, "탈퇴시킬 회원을 먼저 선택해 주세요.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "'" + member.loginId() + "' 회원을 탈퇴시키겠습니까?",
                "회원 탈퇴",
                JOptionPane.YES_NO_OPTION
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            socketClient.deleteMember(member.memberId());
            loadMembers(memberModel);
            refreshAll();
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "회원 탈퇴 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 좌석 목록을 다시 만들고, 가능한 경우 기존 선택 좌석도 복원한다.
     */
    private void reloadSeats(Long previousSelectedSeatId) {
        seatSnapshotMap.clear();
        seatIndexMap.clear();
        seatModel.clear();
        seatManageSelector.removeAllItems();
        replySeatSelector.removeAllItems();
        messageSeatIndexMap.clear();

        int seatIndex = 0;
        for (SeatSnapshot seat : socketClient.getAllSeats()) {
            seatSnapshotMap.put(seat.seatId(), seat);
            seatIndexMap.put(seatIndex, seat.seatId());
            seatModel.addElement("좌석 " + seat.seatNumber() + " / " + DisplayText.seatStatus(seat.status()) + " / 손님 " + seat.userName() + " / 남은 시간 " + seat.remainingTime());
            seatManageSelector.addItem(seat.seatId());
            if (!NO_USER.equals(seat.userName())) {
                replySeatSelector.addItem(seat.seatId());
            }
            seatIndex++;
        }
        restoreSelectedSeat(previousSelectedSeatId);
    }

    /**
     * 전체 주문을 다시 읽어 화면 문자열과 실제 orderId 매핑을 동시에 갱신한다.
     */
    private void reloadOrders() {
        Long previousSelectedOrderId = getSelectedOrderId();
        orderModel.clear();
        orderIndexMap.clear();
        int orderIndex = 0;

        // 주문 리스트 인덱스와 실제 orderId를 따로 매핑해두면
        // 화면에 보여주는 문자열 형식이 바뀌어도 클릭한 주문을 안정적으로 찾을 수 있다.
        for (OrderSnapshot order : socketClient.getAllOrders()) {
            SeatSnapshot seat = seatSnapshotMap.get(order.seatId());
            String seatText = seat == null ? "좌석 정보 없음" : "좌석 " + seat.seatNumber();
            orderModel.addElement("주문 #" + order.orderId() + " / " + seatText + " / " + order.itemSummary() + " / " + order.totalPrice() + "원 / " + DisplayText.orderStatus(order.status()));
            orderIndexMap.put(orderIndex, order.orderId());
            orderIndex++;
        }
        restoreSelectedOrder(previousSelectedOrderId);
    }

    /**
     * 전체 메시지를 좌석 정보와 합쳐 사람이 읽기 쉬운 문자열로 만든다.
     */
    private void reloadMessages() {
        messageModel.clear();
        int messageIndex = 0;
        for (MessageSnapshot message : socketClient.getAllMessages()) {
            SeatSnapshot seat = seatSnapshotMap.get(message.seatId());
            String seatText = seat == null ? "좌석 ?" : "좌석 " + seat.seatNumber();
            String nickname = seat == null ? NO_USER : seat.userName();
            messageModel.addElement(seatText + " / " + nickname + " / " + DisplayText.senderType(message.senderType()) + " / " + message.content());
            messageSeatIndexMap.put(messageIndex, message.seatId());
            messageIndex++;
        }
    }

    /**
     * 새로고침 후에도 가능하면 이전에 보던 좌석을 다시 선택한다.
     */
    private void restoreSelectedSeat(Long selectedSeatId) {
        if (selectedSeatId == null) {
            if (!seatIndexMap.isEmpty()) {
                seatList.setSelectedIndex(0);
            }
            return;
        }
        for (Map.Entry<Integer, Long> entry : seatIndexMap.entrySet()) {
            if (selectedSeatId.equals(entry.getValue())) {
                seatList.setSelectedIndex(entry.getKey());
                return;
            }
        }
        if (!seatIndexMap.isEmpty()) {
            seatList.setSelectedIndex(0);
        }
    }

    /**
     * 주문 목록 갱신 후에도 이전에 선택한 주문을 최대한 유지한다.
     */
    private void restoreSelectedOrder(Long selectedOrderId) {
        if (selectedOrderId == null) {
            if (!orderIndexMap.isEmpty()) {
                orderList.setSelectedIndex(0);
            }
            return;
        }
        for (Map.Entry<Integer, Long> entry : orderIndexMap.entrySet()) {
            if (selectedOrderId.equals(entry.getValue())) {
                orderList.setSelectedIndex(entry.getKey());
                return;
            }
        }
        if (!orderIndexMap.isEmpty()) {
            orderList.setSelectedIndex(0);
        }
    }

    /**
     * 좌석 ID를 좌석 번호 기반 표시 문자열로 바꿔주는 콤보박스 렌더러다.
     */
    private final class SeatAwareRenderer extends DefaultListCellRenderer {
        private final String prefix;

        private SeatAwareRenderer(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Long id) {
                if ("주문 ".equals(prefix)) {
                    setText("주문 " + id);
                } else {
                    SeatSnapshot seat = seatSnapshotMap.get(id);
                    setText(seat == null ? "좌석 " + id : "좌석 " + seat.seatNumber());
                }
            }
            return this;
        }
    }
}
