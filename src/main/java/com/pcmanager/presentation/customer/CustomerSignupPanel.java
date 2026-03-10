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
import java.util.function.Consumer;

public class CustomerSignupPanel extends JPanel {
    private final JTextField loginIdField = new JTextField(16);

    public CustomerSignupPanel(Consumer<String> onSave, Runnable onBack) {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("회원가입");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(new JLabel("새 ID"));
        formPanel.add(loginIdField);
        formPanel.add(Box.createVerticalStrut(12));

        JButton saveButton = new JButton("저장");
        saveButton.addActionListener(event -> save(onSave));
        loginIdField.addActionListener(event -> save(onSave));
        formPanel.add(saveButton);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton backButton = new JButton("이전");
        backButton.addActionListener(event -> onBack.run());
        footerPanel.add(backButton);

        add(formPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void save(Consumer<String> onSave) {
        String loginId = loginIdField.getText().trim();
        if (loginId.isEmpty()) {
            return;
        }
        onSave.accept(loginId);
    }
}
