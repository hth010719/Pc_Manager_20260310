package com.pcmanager.presentation.counter;

import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JFrame;

public class CounterFrame extends JFrame {
    public CounterFrame(PcSocketClient client) {
        setTitle("PC Manager Counter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setContentPane(new CounterPanel(client));
    }
}
