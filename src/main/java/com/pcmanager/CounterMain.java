package com.pcmanager;

import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.presentation.counter.CounterFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 카운터 프로그램 시작점이다.
 */
public class CounterMain {
    public static void main(String[] args) {
        installLookAndFeel();
        PcSocketClient client = new PcSocketClient("127.0.0.1", 5050);
        SwingUtilities.invokeLater(() -> new CounterFrame(client).setVisible(true));
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 시스템 LookAndFeel 적용에 실패해도 기본 Swing 테마로 계속 실행한다.
        }
    }
}
