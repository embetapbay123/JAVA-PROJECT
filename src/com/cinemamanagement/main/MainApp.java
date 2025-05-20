package com.cinemamanagement.main;

import com.cinemamanagement.ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.cinemamanagement.utils.*;

public class MainApp {
    public static void main(String[] args) {
        // Thiết lập Look and Feel của hệ thống để giao diện đẹp hơn
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Lỗi khi thiết lập Look and Feel: " + e.getMessage());
            // e.printStackTrace(); // Bỏ comment nếu muốn xem chi tiết lỗi
        }

        // Chạy giao diện trên Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
}