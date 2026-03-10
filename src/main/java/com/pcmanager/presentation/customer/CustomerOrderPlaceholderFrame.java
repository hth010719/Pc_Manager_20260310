package com.pcmanager.presentation.customer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

public class CustomerOrderPlaceholderFrame extends JFrame {
    public CustomerOrderPlaceholderFrame() {
        setTitle("먹거리 주문");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 220);
        setLocationRelativeTo(null);
        setContentPane(createContent());
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel label = new JLabel("먹거리 주문 다음 창은 나중에 구현 예정입니다.", JLabel.CENTER);
        label.setFont(new Font("Dialog", Font.BOLD, 18));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
