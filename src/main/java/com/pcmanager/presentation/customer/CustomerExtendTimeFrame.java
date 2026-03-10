package com.pcmanager.presentation.customer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

public class CustomerExtendTimeFrame extends JFrame {
    public CustomerExtendTimeFrame(CustomerPanel customerPanel) {
        setTitle("시간 추가");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(360, 260);
        setLocationRelativeTo(null);
        setContentPane(createContent(customerPanel));
    }

    private JPanel createContent(CustomerPanel customerPanel) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("추가할 시간을 선택하세요");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton thirtyMinutesButton = new JButton("30분 (500원)");
        JButton oneHourButton = new JButton("1시간 (1000원)");
        JButton twoHoursButton = new JButton("2시간 (1800원)");
        JButton backButton = new JButton("이전");

        thirtyMinutesButton.addActionListener(event -> apply(customerPanel, 30, 500));
        oneHourButton.addActionListener(event -> apply(customerPanel, 60, 1000));
        twoHoursButton.addActionListener(event -> apply(customerPanel, 120, 1800));
        backButton.addActionListener(event -> dispose());

        buttonPanel.add(thirtyMinutesButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(oneHourButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(twoHoursButton);
        buttonPanel.add(Box.createVerticalStrut(16));
        buttonPanel.add(backButton);

        panel.add(buttonPanel, BorderLayout.CENTER);
        return panel;
    }

    private void apply(CustomerPanel customerPanel, int minutes, int price) {
        customerPanel.extendTime(minutes, price);
        dispose();
    }
}
