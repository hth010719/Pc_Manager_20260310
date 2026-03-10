package com.pcmanager;

import com.pcmanager.infrastructure.network.PcSocketClient;
import com.pcmanager.presentation.counter.CounterFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
        }
    }
}
