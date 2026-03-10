package com.pcmanager.presentation.customer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public class CustomerChargePanel extends JPanel {
    @FunctionalInterface
    public interface ChargeHandler {
        void charge(String loginId, int minutes, int price);
    }

    private final JTextField loginIdField = new JTextField(16);

    public CustomerChargePanel(ChargeHandler onCharge, Runnable onBack) {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("시간충전");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(new JLabel("ID"));
        centerPanel.add(loginIdField);
        centerPanel.add(Box.createVerticalStrut(12));

        JButton thirtyMinutesButton = new JButton("30분 (500원)");
        JButton oneHourButton = new JButton("1시간 (1000원)");
        JButton twoHoursButton = new JButton("2시간 (1800원)");
        thirtyMinutesButton.addActionListener(event -> charge(onCharge, 30, 500));
        oneHourButton.addActionListener(event -> charge(onCharge, 60, 1000));
        twoHoursButton.addActionListener(event -> charge(onCharge, 120, 1800));
        centerPanel.add(thirtyMinutesButton);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(oneHourButton);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(twoHoursButton);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton backButton = new JButton("이전");
        backButton.addActionListener(event -> onBack.run());
        footerPanel.add(backButton);

        add(centerPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void charge(ChargeHandler onCharge, int minutes, int price) {
        String loginId = loginIdField.getText().trim();
        if (loginId.isEmpty()) {
            return;
        }
        onCharge.charge(loginId, minutes, price);
    }
}
