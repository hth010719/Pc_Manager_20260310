package com.pcmanager.presentation.customer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;

public class CustomerLoginPanel extends JPanel {
    private final JTextField loginIdField = new JTextField(16);

    public CustomerLoginPanel(Consumer<String> onSubmit, Runnable onSignup, Runnable onCharge) {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("고객 ID 입력");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(new JLabel("ID"));
        Dimension loginFieldSize = loginIdField.getPreferredSize();
        int compactHeight = Math.max(24, loginFieldSize.height);
        loginIdField.setPreferredSize(new Dimension(loginFieldSize.width, compactHeight));
        loginIdField.setMaximumSize(new Dimension(Integer.MAX_VALUE, compactHeight));
        loginIdField.setMinimumSize(new Dimension(80, compactHeight));
        formPanel.add(loginIdField);
        formPanel.add(Box.createVerticalStrut(12));

        JButton submitButton = new JButton("로그인");
        submitButton.addActionListener(event -> submit(onSubmit));
        loginIdField.addActionListener(event -> submit(onSubmit));
        formPanel.add(submitButton);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton chargeButton = new JButton("시간충전");
        JButton signupButton = new JButton("회원가입");
        chargeButton.addActionListener(event -> onCharge.run());
        signupButton.addActionListener(event -> onSignup.run());
        footerPanel.add(chargeButton);
        footerPanel.add(signupButton);

        add(formPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    public void setLoginId(String loginId) {
        loginIdField.setText(loginId);
    }

    private void submit(Consumer<String> onSubmit) {
        String loginId = loginIdField.getText().trim();
        if (loginId.isEmpty()) {
            return;
        }
        onSubmit.accept(loginId);
    }
}
