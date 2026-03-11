package com.pcmanager;

import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.presentation.customer.CustomerFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 고객 프로그램 시작점이다.
 */
public class CustomerMain {
    public static void main(String[] args) {
        installLookAndFeel();
        PcSocketClient client = new PcSocketClient("127.0.0.1", 5050);
        SwingUtilities.invokeLater(() -> new CustomerFrame(client).setVisible(true));
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 시스템 LookAndFeel 적용 실패는 치명적이지 않으므로 기본 테마로 진행한다.
        }
    }
}
