package com.pcmanager.presentation.customer;

import com.pcmanager.infrastructure.network.EnterSeatSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JFrame;

/**
 * 로그인 이후 고객이 실제로 사용하는 메인 대시보드 프레임이다.
 *
 * 핵심 화면은 CustomerPanel이 담당하고,
 * 이 프레임은 창 껍데기와 보조 팝업 진입점만 제공한다.
 */
public class CustomerDashboardFrame extends JFrame {
    private final CustomerPanel customerPanel;

    public CustomerDashboardFrame(PcSocketClient client, EnterSeatSnapshot seatSnapshot, String loginId, Runnable onExitComplete) {
        setTitle("PC Manager Customer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 310);
        setLocationRelativeTo(null);
        customerPanel = new CustomerPanel(client, seatSnapshot, loginId, onExitComplete, this::openExtendTimeFrame);
        setContentPane(customerPanel);
    }

    /**
     * 시간 연장 전용 서브 프레임을 연다.
     */
    private void openExtendTimeFrame() {
        CustomerExtendTimeFrame frame = new CustomerExtendTimeFrame(customerPanel);
        frame.setVisible(true);
    }
}
