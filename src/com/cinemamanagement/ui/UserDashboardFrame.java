package com.cinemamanagement.ui;

import com.cinemamanagement.model.*;
import com.cinemamanagement.utils.RounedButton;
import com.cinemamanagement.utils.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UserDashboardFrame extends JFrame {
    private JLabel welcomeLabel;
    private JButton logoutButton;
    private JTabbedPane tabbedPane;

    // Các panel của User
    private BookTicketPanel bookTicketPanel;
    private MyTicketsPanel myTicketsPanel;
    // private ViewMoviesPanel viewMoviesPanel; // Tùy chọn, có thể tích hợp vào BookTicketPanel

    public UserDashboardFrame() {
        if (LoginFrame.currentUser == null || LoginFrame.currentUser.getRole() != User.Role.USER) {
            JOptionPane.showMessageDialog(null, "Truy cập bị từ chối. Vui lòng đăng nhập với quyền User.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            return;
        }

        setTitle("User Dashboard - Rạp Chiếu Phim");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        welcomeLabel = new JLabel("Chào mừng, " + LoginFrame.currentUser.getUsername());
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        logoutButton =  new RounedButton("Đăng xuất", new Color(244, 67, 54));
        logoutButton.setFocusPainted(false);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Khởi tạo các panel
        bookTicketPanel = new BookTicketPanel();
        myTicketsPanel = new MyTicketsPanel(); // Sẽ được tạo sau

        tabbedPane.addTab("Đặt Vé Xem Phim", createScrollablePanel(bookTicketPanel));
        tabbedPane.addTab("Vé Của Tôi", createScrollablePanel(myTicketsPanel));
        // tabbedPane.addTab("Xem Phim Đang Chiếu", viewMoviesPanel); // Nếu có
        
        //set màu nền cho tabbedPane
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                if (isSelected) {
                    g.setColor(new Color(255, 182, 193)); // Light pink for selected tab
                } else {
                    g.setColor(tabbedPane.getBackground()); // Default background for others
                }
                g.fillRect(x, y, w, h);
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Action Listener cho nút Đăng xuất
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        UserDashboardFrame.this,
                        "Bạn có chắc chắn muốn đăng xuất?",
                        "Xác nhận đăng xuất",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    LoginFrame.currentUser = null;
                    dispose();
                    SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
                }
            }
        });
        
        // Đảm bảo MyTicketsPanel được làm mới khi tab được chọn
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() instanceof JScrollPane) {
                JScrollPane selectedScrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
                Component view = selectedScrollPane.getViewport().getView();
                if (view instanceof MyTicketsPanel) {
                    ((MyTicketsPanel) view).loadUserTickets(); // Gọi phương thức load vé
                }
                // Tương tự, nếu BookTicketPanel cần refresh khi được chọn
                // else if (view instanceof BookTicketPanel) {
                //    ((BookTicketPanel) view).refreshData(); // Ví dụ
                // }
            }
        });

        pack(); // Adjusts frame size to fit contents
        setLocationRelativeTo(null); // Center on screen
        setVisible(true); // Show the frame
        // Xử lý đóng cửa sổ
        // addWindowListener(new WindowAdapter() {
        //     @Override
        //     public void windowClosing(WindowEvent e) {
        //         // com.cinemamanagement.dao.DatabaseConnection.closeConnection();
        //     }
        // });
    }

    private JScrollPane createScrollablePanel(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    // (Optional) main method để chạy thử riêng UserDashboardFrame
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
             try {
                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                 // Giả lập người dùng user đã đăng nhập để test
                 LoginFrame.currentUser = new User(2, "testuser", "password", User.Role.USER);
             } catch (Exception e) {
                 e.printStackTrace();
             }
             new UserDashboardFrame().setVisible(true);
         });
     }
}