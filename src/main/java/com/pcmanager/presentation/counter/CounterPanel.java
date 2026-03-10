package com.pcmanager.presentation.counter;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.common.util.DisplayText;
import com.pcmanager.infrastructure.network.MessageSnapshot;
import com.pcmanager.infrastructure.network.OrderSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.infrastructure.network.SeatSnapshot;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

public class CounterPanel extends JPanel {
    private static final String NO_USER = "-";
    private static final String REQUIRE_SEAT_MESSAGE = "먼저 좌석을 클릭해 주세요.";

    private final PcSocketClient socketClient;
    private final DefaultListModel<String> seatModel = new DefaultListModel<>();
    private final JList<String> seatList = new JList<>(seatModel);
    private final DefaultListModel<String> orderModel = new DefaultListModel<>();
    private final DefaultListModel<String> messageModel = new DefaultListModel<>();
    private final JComboBox<Long> orderSelector = new JComboBox<>();
    private final JComboBox<Long> seatManageSelector = new JComboBox<>();
    private final JComboBox<Long> replySeatSelector = new JComboBox<>();
    private final JTextArea replyArea = new JTextArea(4, 20);
    private final JList<String> messageList = new JList<>(messageModel);
    private final Map<Long, SeatSnapshot> seatSnapshotMap = new LinkedHashMap<>();
    private final Map<Integer, Long> seatIndexMap = new LinkedHashMap<>();
    private final Map<Integer, Long> messageSeatIndexMap = new LinkedHashMap<>();

    public CounterPanel(PcSocketClient socketClient) {
        this.socketClient = socketClient;
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        orderSelector.setRenderer(new SeatAwareRenderer("주문 "));
        seatManageSelector.setRenderer(new SeatAwareRenderer("좌석 "));
        replySeatSelector.setRenderer(new SeatAwareRenderer("좌석 "));
        configureReplyArea();
        configureMessageList();
        configureSeatList();

        JPanel content = new JPanel(new GridLayout(1, 3, 12, 12));
        content.add(createSeatPanel());
        content.add(createOrderPanel());
        content.add(createMessagePanel());
        add(content, BorderLayout.CENTER);

        refreshAll();
        Timer timer = new Timer(1000, event -> refreshAll());
        timer.start();
    }

    private JPanel createSeatPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("전체 좌석 현황"));
        panel.add(new JScrollPane(seatList), BorderLayout.CENTER);

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

        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("주문 관리"));
        panel.add(new JScrollPane(new JList<>(orderModel)), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.add(new JLabel("상태를 바꿀 주문"));
        actionPanel.add(orderSelector);
        JButton changeButton = new JButton("주문 상태 바꾸기");
        changeButton.addActionListener(event -> chooseOrderStatus());
        actionPanel.add(changeButton);
        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("고객 메시지"));
        panel.add(new JScrollPane(messageList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(new JLabel("답변할 좌석"));
        bottom.add(replySeatSelector);
        bottom.add(new JScrollPane(replyArea));
        JButton replyButton = new JButton("답변 보내기");
        replyButton.addActionListener(event -> sendReply());
        bottom.add(replyButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void addTime(int minutes) {
        Long seatId = requireSelectedSeatId();
        if (seatId == null) {
            return;
        }
        executeWithRefresh("시간 더하기 실패", () -> socketClient.extendTime(seatId, minutes));
    }

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

    private void changeSeatStatus(String status) {
        Long seatId = requireSelectedSeatId();
        if (seatId == null) {
            return;
        }
        executeWithRefresh("좌석 상태 변경 실패", () -> socketClient.changeSeatStatus(seatId, status));
    }

    private void chooseOrderStatus() {
        Long orderId = (Long) orderSelector.getSelectedItem();
        if (orderId == null) {
            return;
        }

        Object[] choices = {"주문 확인", "준비 중", "전달 중", "완료", "취소"};
        Object selected = JOptionPane.showInputDialog(
                this,
                "바꿀 상태를 골라 주세요.",
                "주문 상태 바꾸기",
                JOptionPane.PLAIN_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if (selected == null) {
            return;
        }

        String status = switch (selected.toString()) {
            case "주문 확인" -> "ACCEPTED";
            case "준비 중" -> "PREPARING";
            case "전달 중" -> "DELIVERING";
            case "완료" -> "COMPLETED";
            case "취소" -> "CANCELED";
            default -> "REQUESTED";
        };

        executeWithRefresh("주문 처리 실패", () -> socketClient.changeOrderStatus(orderId, status));
    }

    private void sendReply() {
        Long seatId = resolveReplySeatId();
        String content = replyArea.getText().trim();
        if (seatId == null) {
            JOptionPane.showMessageDialog(this, "메시지를 보낼 사용 중 좌석을 먼저 선택해 주세요.");
            return;
        }
        if (content.isEmpty()) {
            return;
        }
        executeWithRefresh("답변 보내기 실패", () -> {
            socketClient.sendReply(seatId, content);
            replyArea.setText("");
        });
    }

    private void configureReplyArea() {
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-reply");
        replyArea.getActionMap().put("send-reply", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendReply();
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

    private Long requireSelectedSeatId() {
        Long seatId = getSelectedSeatId();
        if (seatId == null) {
            JOptionPane.showMessageDialog(this, REQUIRE_SEAT_MESSAGE);
        }
        return seatId;
    }

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

    private void reloadOrders() {
        orderModel.clear();
        orderSelector.removeAllItems();
        for (OrderSnapshot order : socketClient.getAllOrders()) {
            SeatSnapshot seat = seatSnapshotMap.get(order.seatId());
            String seatText = seat == null ? "좌석 정보 없음" : "좌석 " + seat.seatNumber();
            orderModel.addElement("주문 #" + order.orderId() + " / " + seatText + " / " + order.itemSummary() + " / " + DisplayText.orderStatus(order.status()));
            orderSelector.addItem(order.orderId());
        }
    }

    private void reloadMessages() {
        messageModel.clear();
        int messageIndex = 0;
        for (MessageSnapshot message : socketClient.getAllMessages()) {
            SeatSnapshot seat = seatSnapshotMap.get(message.seatId());
            String seatText = seat == null ? "좌석 ?" : "좌석 " + seat.seatNumber();
            String nickname = seat == null ? NO_USER : seat.userName();
            messageModel.addElement(seatText + " / " + nickname + " / " + DisplayText.senderType(message.senderType()) + " / " + DisplayText.messageType(message.messageType()) + " / " + message.content());
            messageSeatIndexMap.put(messageIndex, message.seatId());
            messageIndex++;
        }
    }

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
