package com.pcmanager.presentation.customer;

import com.pcmanager.infrastructure.network.EnterSeatSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.CardLayout;

/**
 * 고객용 첫 진입 프레임이다.
 *
 * 로그인, 회원가입, 시간충전 화면을 CardLayout으로 묶고
 * 성공 시 실제 사용 화면인 CustomerDashboardFrame으로 넘긴다.
 */
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

    /**
     * 로그인 ID로 입장 요청을 보내고, 성공 시 대시보드 프레임으로 화면을 교체한다.
     */
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

    /**
     * 간단 회원가입 후 바로 로그인 화면으로 복귀시켜 같은 ID로 이어서 입장할 수 있게 한다.
     */
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

    /**
     * 고객 좌석 종료 후 다시 로그인 프레임을 새로 띄우는 콜백이다.
     */
    private void showLoginFrameAgain() {
        CustomerFrame frame = new CustomerFrame(client);
        frame.setVisible(true);
    }

    /**
     * 충전 요청은 서버에 남은 시간만 반영하고, 결제 금액 문구는 클라이언트 화면에서 안내한다.
     */
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
