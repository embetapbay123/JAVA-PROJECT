package com.cinemamanagement.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.cinemamanagement.model.*;
import com.cinemamanagement.utils.RounedButton;

import javax.swing.plaf.basic.BasicTabbedPaneUI;

public class AdminDashboardFrame extends JFrame {
    private JLabel welcomeLabel;
    private JButton logoutButton;
    private JTabbedPane tabbedPane;

    // Các panel quản lý (sẽ được tạo sau)
    private MovieManagementPanel movieManagementPanel;
    private RoomManagementPanel roomManagementPanel;
    private ShowtimeManagementPanel showtimeManagementPanel;
    // private UserManagementPanel userManagementPanel; // Tùy chọn nếu admin quản lý user

    public AdminDashboardFrame() {
        if (LoginFrame.currentUser == null || LoginFrame.currentUser.getRole() != User.Role.ADMIN) {
            // Nếu không phải admin hoặc chưa đăng nhập, không cho vào, quay lại Login
            JOptionPane.showMessageDialog(null, "Truy cập bị từ chối. Vui lòng đăng nhập với quyền Admin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            dispose(); // Đóng dashboard này
            // Mở lại LoginFrame (nếu nó đã bị đóng)
            // Cần cơ chế tốt hơn để quản lý việc mở lại LoginFrame nếu cần
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            return; // Không khởi tạo phần còn lại của UI
        }

        setTitle("Admin Dashboard - Quản Lý Rạp Chiếu Phim");
        setSize(900, 700); // Kích thước lớn hơn cho dashboard
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Thoát ứng dụng khi đóng
        setLocationRelativeTo(null); // Căn giữa màn hình
        setResizable(false); // Không cho phép thay đổi kích thước
   
        // Panel trên cùng chứa lời chào và nút đăng xuất
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Padding

        welcomeLabel = new JLabel("Chào mừng, " + LoginFrame.currentUser.getUsername());
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        logoutButton =  new RounedButton("Đăng xuất", new Color(244, 67, 54));
      
        logoutButton.setFocusPainted(false); // Bỏ viền focus khi click
        // Thêm icon cho nút đăng xuất (tùy chọn)
        // try {
        //     ImageIcon logoutIcon = new ImageIcon(new ImageIcon(getClass().getResource("/icons/logout.png")).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        //     logoutButton.setIcon(logoutIcon);
        // } catch (Exception e) {
        //     System.err.println("Không tìm thấy icon logout.png");
        // }
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // JTabbedPane để chứa các panel quản lý
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Khởi tạo các panel quản lý
        movieManagementPanel = new MovieManagementPanel();
        roomManagementPanel = new RoomManagementPanel();
        showtimeManagementPanel = new ShowtimeManagementPanel();
        // userManagementPanel = new UserManagementPanel(); // Nếu có

        // Thêm các tab vào JTabbedPane
        tabbedPane.addTab("Quản lý Phim", createScrollablePanel(movieManagementPanel));
        tabbedPane.addTab("Quản lý Phòng Chiếu", createScrollablePanel(roomManagementPanel));
        tabbedPane.addTab("Quản lý Suất Chiếu", createScrollablePanel(showtimeManagementPanel));
        
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
        
        // tabbedPane.addTab("Quản lý Người Dùng", userManagementPanel); // Nếu có
        tabbedPane.addChangeListener(e -> {
            // Lấy component (panel) của tab vừa được chọn
            Component selectedComponent = null;
            if (tabbedPane.getSelectedComponent() instanceof JScrollPane) {
                // Nếu panel được bọc trong JScrollPane, lấy view bên trong viewport
                JScrollPane selectedScrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
                selectedComponent = selectedScrollPane.getViewport().getView();
            } else {
                // Nếu panel không được bọc trong JScrollPane (ít khả năng với thiết kế hiện tại)
                selectedComponent = tabbedPane.getSelectedComponent();
            }

            // Kiểm tra xem panel được chọn là gì và gọi phương thức làm mới tương ứng
            if (selectedComponent instanceof ShowtimeManagementPanel) {
                System.out.println("AdminDashboard: Tab 'Quản lý Suất Chiếu' được chọn. Đang làm mới danh sách phim, phòng...");
                // Gọi phương thức làm mới danh sách phim trong ComboBox của ShowtimeManagementPanel
                ((ShowtimeManagementPanel) selectedComponent).refreshMovieSelectionInForm();
                ((ShowtimeManagementPanel) selectedComponent).refreshRoomSelectionInForm();
            }
            // Bạn có thể thêm các điều kiện else if cho các panel khác nếu chúng cũng cần
            // được làm mới khi người dùng chuyển tab đến chúng.
            // Ví dụ, nếu RoomManagementPanel cũng cần làm mới:
            // else if (selectedComponent instanceof RoomManagementPanel) {
            //     System.out.println("AdminDashboard: Tab 'Quản lý Phòng Chiếu' được chọn. Đang làm mới...");
            //     ((RoomManagementPanel) selectedComponent).loadRooms(); // Giả sử có phương thức này
            // }
            // MovieManagementPanel thường tự load khi khởi tạo, và các thay đổi trên đó
            // thường được phản ánh ngay lập tức trên bảng của nó.
        });
        add(tabbedPane, BorderLayout.CENTER);

        // Xử lý sự kiện cho nút Đăng xuất
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        AdminDashboardFrame.this,
                        "Bạn có chắc chắn muốn đăng xuất?",
                        "Xác nhận đăng xuất",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    LoginFrame.currentUser = null; // Xóa thông tin người dùng hiện tại
                    dispose(); // Đóng AdminDashboardFrame
                    // Mở lại LoginFrame
                    SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
                }
            }
        });
        
        // Đóng kết nối CSDL khi cửa sổ AdminDashboardFrame đóng lại
        // (Chỉ cần thiết nếu LoginFrame không còn tồn tại hoặc nếu có khả năng
        // AdminDashboardFrame là cửa sổ cuối cùng đóng)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Nếu LoginFrame.currentUser vẫn còn (ví dụ, người dùng chỉ đóng cửa sổ thay vì đăng xuất)
                // thì không nên đóng kết nối ở đây mà nên để LoginFrame xử lý khi nó đóng.
                // Tuy nhiên, nếu đây là cửa sổ chính và không có LoginFrame nào khác,
                // thì việc đóng kết nối ở đây là hợp lý.
                // Hiện tại, LoginFrame đã có logic đóng kết nối.
                // com.cinemamanagement.dao.DatabaseConnection.closeConnection();
                // System.out.println("Admin Dashboard đóng, kết nối CSDL có thể đã được giải phóng bởi LoginFrame.");
            }
        });
        
        pack(); // Adjusts frame size to fit contents
        setLocationRelativeTo(null); // Center on screen
        setVisible(true); // Show the frame
    }

    /**
     * Tạo một JScrollPane bao quanh một JPanel.
     * Điều này hữu ích nếu nội dung của panel có thể vượt quá kích thước hiển thị.
     * @param panel Panel cần được cuộn.
     * @return JScrollPane chứa panel.
     */
    
    private JScrollPane createScrollablePanel(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Tăng tốc độ cuộn chuột (tùy chọn)
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

//   //  (Optional) main method để chạy thử riêng AdminDashboardFrame
//     public static void main(String[] args) {
//         SwingUtilities.invokeLater(() -> {
//             try {
//                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//                 // Giả lập người dùng admin đã đăng nhập để test
//                 LoginFrame.currentUser = new User(1, "testadmin", "password", User.Role.ADMIN);
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//             
//             new AdminDashboardFrame().setVisible(true);
//         });
//     }
}