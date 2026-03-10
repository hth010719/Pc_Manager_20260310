package com.pcmanager.presentation.customer;

import com.pcmanager.infrastructure.network.EnterSeatSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.CardLayout;

public class CustomerFrame extends JFrame {
    private final PcSocketClient client;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final CustomerLoginPanel loginPanel;
    private final CustomerSignupPanel signupPanel;
    private final CustomerChargePanel chargePanel;

    public CustomerFrame(PcSocketClient client) {
        this.client = client;
        this.loginPanel = new CustomerLoginPanel(this::enterWithLoginId, this::showSignupCard, this::showChargeCard);
        this.signupPanel = new CustomerSignupPanel(this::registerMember, this::showLoginCard);
        this.chargePanel = new CustomerChargePanel(this::chargeTime, this::showLoginCard);
        setTitle("PC Manager Customer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 260);
        setLocationRelativeTo(null);
        cardPanel.add(loginPanel, "login");
        cardPanel.add(signupPanel, "signup");
        cardPanel.add(chargePanel, "charge");
        setContentPane(cardPanel);
    }

    private void enterWithLoginId(String loginId) {
        try {
            EnterSeatSnapshot response = client.enterByLoginId(loginId);
            CustomerDashboardFrame dashboardFrame = new CustomerDashboardFrame(client, response, loginId, this::showLoginFrameAgain);
            dashboardFrame.setVisible(true);
            dispose();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "입장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registerMember(String loginId) {
        try {
            client.registerMember(loginId);
            JOptionPane.showMessageDialog(this, "ID가 저장되었습니다. 이제 로그인해 주세요.");
            loginPanel.setLoginId(loginId);
            showLoginCard();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "회원가입 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSignupCard() {
        cardLayout.show(cardPanel, "signup");
    }

    private void showLoginCard() {
        cardLayout.show(cardPanel, "login");
    }

    private void showChargeCard() {
        cardLayout.show(cardPanel, "charge");
    }

    private void showLoginFrameAgain() {
        CustomerFrame frame = new CustomerFrame(client);
        frame.setVisible(true);
    }

    private void chargeTime(String loginId, int minutes, int price) {
        try {
            client.chargeTime(loginId, minutes);
            JOptionPane.showMessageDialog(this, minutes + "분 충전되었습니다. 금액은 " + price + "원입니다.");
            loginPanel.setLoginId(loginId);
            showLoginCard();
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "시간충전 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
