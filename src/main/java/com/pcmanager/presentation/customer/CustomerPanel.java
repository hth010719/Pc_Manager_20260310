package com.pcmanager.presentation.customer;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.common.util.DisplayText;
import com.pcmanager.infrastructure.network.EnterSeatSnapshot;
import com.pcmanager.infrastructure.network.MessageSnapshot;
import com.pcmanager.infrastructure.network.OrderSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.infrastructure.network.SeatSnapshot;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class CustomerPanel extends JPanel {
    private final PcSocketClient socketClient;
    private final JLabel memberInfoLabel = new JLabel();
    private final JLabel seatNumberLabel = new JLabel();
    private final JLabel seatStatusLabel = new JLabel();
    private final JLabel remainInfoLabel = new JLabel();
    private final DefaultListModel<String> messageModel = new DefaultListModel<>();
    private final JList<String> messageList = new JList<>(messageModel);
    private final JTextArea messageInputArea = new JTextArea(4, 20);
    private final Long currentSeatId;
    private final String currentLoginId;
    private final Runnable onExitComplete;
    private final Runnable onOpenExtendTime;
    private boolean redirectedToLogin;

    public CustomerPanel(PcSocketClient socketClient, EnterSeatSnapshot seatSnapshot, String loginId, Runnable onExitComplete, Runnable onOpenExtendTime) {
        this.socketClient = socketClient;
        this.currentSeatId = seatSnapshot.seatId();
        this.currentLoginId = loginId;
        this.onExitComplete = onExitComplete;
        this.onOpenExtendTime = onOpenExtendTime;
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        configureMessageInput();
        refreshAll();
        Timer timer = new Timer(1000, event -> refreshAll());
        timer.start();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        memberInfoLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        panel.add(memberInfoLabel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 12));
        panel.add(createEventPanel());
        panel.add(createFoodOrderPanel());
        panel.add(createSeatInfoPanel());
        panel.add(createMessagePanel());
        return panel;
    }

    private JPanel createEventPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("이벤트"));

        JTextArea eventArea = new JTextArea();
        eventArea.setEditable(false);
        eventArea.setLineWrap(true);
        eventArea.setWrapStyleWord(true);
        eventArea.setText("""
                - 평일 오전 타임 1시간 추가 충전 시 10분 서비스
                - 컵라면 + 음료 세트 주문 시 500원 할인
                - 카운터 문의로 좌석 이동 요청 가능
                """);
        panel.add(new JScrollPane(eventArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFoodOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("먹거리 주문"));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        JButton orderButton = new JButton("먹거리 주문");
        orderButton.setFont(new Font("Dialog", Font.BOLD, 20));
        orderButton.addActionListener(event -> openOrderWindow());
        JButton orderHistoryButton = new JButton("주문내역");
        orderHistoryButton.setFont(new Font("Dialog", Font.BOLD, 18));
        orderHistoryButton.addActionListener(event -> openOrderHistoryDialog());

        buttonPanel.add(orderButton);
        buttonPanel.add(orderHistoryButton);
        panel.add(buttonPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSeatInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createTitledBorder("자리정보"));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        seatNumberLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        seatStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
        remainInfoLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        infoPanel.add(seatNumberLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(seatStatusLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(remainInfoLabel);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        JButton extendButton = new JButton("시간추가");
        JButton exitButton = new JButton("종료하기");
        extendButton.addActionListener(event -> onOpenExtendTime.run());
        exitButton.addActionListener(event -> exitSeat());
        buttonPanel.add(extendButton);
        buttonPanel.add(exitButton);

        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("카운터 문의하기"));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(new JLabel("보낼 내용"));
        bottom.add(new JScrollPane(messageInputArea));
        JButton sendButton = new JButton("문의 보내기");
        sendButton.addActionListener(event -> sendMessage());
        bottom.add(sendButton);

        JScrollPane scrollPane = new JScrollPane(messageList);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void configureMessageInput() {
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);
        messageInputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        messageInputArea.getActionMap().put("send-message", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendMessage();
            }
        });
        messageInputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        messageInputArea.getActionMap().put("insert-break", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                messageInputArea.append(System.lineSeparator());
            }
        });
    }

    private void openOrderWindow() {
        CustomerOrderPlaceholderFrame frame = new CustomerOrderPlaceholderFrame(socketClient, currentSeatId);
        frame.setVisible(true);
    }

    private void openOrderHistoryDialog() {
        DefaultListModel<String> orderHistoryModel = new DefaultListModel<>();
        JList<String> orderHistoryList = new JList<>(orderHistoryModel);

        for (OrderSnapshot order : socketClient.getOrders(currentSeatId)) {
            orderHistoryModel.addElement(
                    "주문 #" + order.orderId()
                            + " / " + order.itemSummary()
                            + " / " + order.totalPrice() + "원 / "
                            + customerOrderStatusText(order.status())
            );
        }

        if (orderHistoryModel.isEmpty()) {
            orderHistoryModel.addElement("주문한 내역이 없습니다.");
        }

        JDialog dialog = new JDialog((Window) SwingUtilities.getWindowAncestor(this), "주문내역", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(orderHistoryList), BorderLayout.CENTER);

        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(event -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(closeButton, BorderLayout.EAST);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setSize(520, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String customerOrderStatusText(String status) {
        if ("REQUESTED".equals(status)) {
            return "주문완료";
        }
        return DisplayText.orderStatus(status);
    }

    public void extendTime(int minutes, int price) {
        try {
            socketClient.extendTime(currentSeatId, minutes);
            JOptionPane.showMessageDialog(this, minutes + "분이 추가되었습니다. 금액은 " + price + "원입니다.");
            refreshAll();
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "시간 더하기 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String content = messageInputArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        try {
            socketClient.sendMessage(currentSeatId, "GENERAL_CHAT", content);
            messageInputArea.setText("");
            refreshAll();
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "메시지 보내기 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exitSeat() {
        int answer = JOptionPane.showConfirmDialog(this, "종료하면 남은 시간이 저장되고 로그인 화면으로 돌아갑니다.", "종료 확인", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String message = socketClient.exitSeat(currentSeatId).message();
            redirectToLogin(message);
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "종료 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAll() {
        try {
            SeatSnapshot seat = socketClient.getSeat(currentSeatId);
            if (!currentLoginId.equals(seat.userName()) || !"IN_USE".equals(seat.status())) {
                handleForcedLogout();
                return;
            }
            if ("00:00:00".equals(seat.remainingTime())) {
                handleTimeExpired();
                return;
            }
            updateSeatInfo(seat);
            reloadMessages();
        } catch (BusinessException exception) {
            if (!redirectedToLogin) {
                memberInfoLabel.setText("서버와 연결되지 않았습니다.");
            }
        }
    }

    private void updateSeatInfo(SeatSnapshot seat) {
        memberInfoLabel.setText("ID: " + currentLoginId);
        seatNumberLabel.setText("좌석 번호: " + seat.seatNumber());
        seatStatusLabel.setText("상태: " + DisplayText.seatStatus(seat.status()));
        remainInfoLabel.setText("남은 시간: " + seat.remainingTime());
    }

    private void reloadMessages() {
        messageModel.clear();
        for (MessageSnapshot message : socketClient.getMessages(currentSeatId)) {
            if (!message.content().isBlank()) {
                messageModel.addElement(formatMessage(message));
            }
        }
        scrollMessageListToBottom();
    }

    private String formatMessage(MessageSnapshot message) {
        if ("COUNTER".equals(message.senderType())) {
            return "카운터 : " + message.content();
        }
        return "고객 " + currentLoginId + " : " + message.content();
    }

    private void scrollMessageListToBottom() {
        if (messageModel.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            int lastIndex = messageModel.getSize() - 1;
            messageList.ensureIndexIsVisible(lastIndex);
        });
    }

    private void handleForcedLogout() {
        redirectToLogin("카운터에서 좌석이 종료되었습니다. 남은 시간은 저장되었고 로그인 화면으로 돌아갑니다.");
    }

    private void handleTimeExpired() {
        if (redirectedToLogin) {
            return;
        }
        try {
            socketClient.exitSeat(currentSeatId);
        } catch (BusinessException ignored) {
        }
        redirectToLogin("남은 시간이 0이 되어 종료되었습니다. 로그인 화면으로 돌아갑니다.");
    }

    private void redirectToLogin(String notice) {
        if (redirectedToLogin) {
            return;
        }
        redirectedToLogin = true;
        JOptionPane.showMessageDialog(this, notice);
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        onExitComplete.run();
    }
}
