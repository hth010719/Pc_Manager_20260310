package com.pcmanager.presentation.counter;

import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JFrame;

/**
 * 카운터 전용 메인 프레임이다.
 *
 * 실제 업무 UI는 CounterPanel이 담당하고,
 * 이 클래스는 창 크기와 기본 종료 정책만 설정한다.
 */
public class CounterFrame extends JFrame {
    public CounterFrame(PcSocketClient client) {
        setTitle("PC Manager Counter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setContentPane(new CounterPanel(client));
    }
}
