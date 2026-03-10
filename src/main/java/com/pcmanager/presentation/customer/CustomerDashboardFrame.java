package com.pcmanager.presentation.customer;

import com.pcmanager.infrastructure.network.EnterSeatSnapshot;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.swing.JFrame;

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

    private void openExtendTimeFrame() {
        CustomerExtendTimeFrame frame = new CustomerExtendTimeFrame(customerPanel);
        frame.setVisible(true);
    }
}
