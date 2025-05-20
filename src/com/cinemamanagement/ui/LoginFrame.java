package com.cinemamanagement.ui;

import com.cinemamanagement.dao.UserDAO;

import com.cinemamanagement.model.User;
import com.cinemamanagement.model.User.Role; // Import enum Role
import com.cinemamanagement.utils.*;

import javax.swing.*;
import javax.swing.JButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private UserDAO userDAO;

    // Biến static để lưu thông tin người dùng đang đăng nhập
    // Sẽ được truy cập từ các frame/panel khác
    public static User currentUser = null;

    public LoginFrame() {
        userDAO = new UserDAO(); // Khởi tạo UserDAO

        setTitle("Đăng nhập - Rạp Chiếu Phim");
        setSize(420, 300); // Kích thước cửa sổ
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Thoát ứng dụng khi đóng cửa sổ
        setLocationRelativeTo(null); // Căn giữa màn hình
        setResizable(false); // Không cho phép thay đổi kích thước

        // Set modern font for all components
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 16); // Thay đổi font
        UIManager.put("Label.font", mainFont);
        UIManager.put("Button.font", mainFont);
        UIManager.put("TextField.font", mainFont);
        UIManager.put("PasswordField.font", mainFont);
        
        // Panel chính với GridBagLayout để sắp xếp linh hoạt
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding
        mainPanel.setBackground( new Color(204, 229, 255)); // blue color
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Khoảng cách giữa các component
        gbc.fill = GridBagConstraints.HORIZONTAL; // Cho phép component mở rộng theo chiều ngang

        // Nhãn và trường nhập Tên đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST; // Căn lề trái
        mainPanel.add(new JLabel("Tên đăng nhập:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Cho phép trường nhập mở rộng
        usernameField = new JTextField(20);
        
        mainPanel.add(usernameField, gbc);

        // Nhãn và trường nhập Mật khẩu
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0; // Reset weightx
        mainPanel.add(new JLabel("Mật khẩu:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        mainPanel.add(passwordField, gbc);

        // Panel cho các nút, sử dụng FlowLayout để căn giữa
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground( new Color(204, 229, 255));
        loginButton = new RounedButton("Đăng nhập", new Color(244, 67, 54));
        registerButton = new RounedButton("Đăng ký", new Color(76, 175, 80));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        // Thêm buttonPanel vào mainPanel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Nút chiếm 2 cột
        gbc.fill = GridBagConstraints.NONE; // Không cho nút mở rộng
        gbc.anchor = GridBagConstraints.CENTER; // Căn giữa
        gbc.insets = new Insets(15, 5, 5, 5); // Thêm khoảng cách trên cho nút
        mainPanel.add(buttonPanel, gbc);

        // Thêm mainPanel vào JFrame
        add(mainPanel);

        // Xử lý sự kiện cho nút Đăng nhập
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
        // Cho phép nhấn Enter để đăng nhập từ passwordField
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
        usernameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.requestFocusInWindow(); // Chuyển focus sang password
            }
        });


        // Xử lý sự kiện cho nút Đăng ký
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tạo và hiển thị RegisterDialog
                // 'LoginFrame.this' là frame cha của dialog
                RegisterDialog registerDialog = new RegisterDialog(LoginFrame.this);
                registerDialog.setVisible(true);
            }
        });
        
        // Đóng kết nối CSDL khi cửa sổ LoginFrame đóng lại
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                com.cinemamanagement.dao.DatabaseConnection.closeConnection();
                System.out.println("Ứng dụng đã đóng và kết nối CSDL đã được giải phóng.");
            }
        });
    }

    private void styleButton(JButton loginButton2, Color color) {
		// TODO Auto-generated method stub
		
	}

	/**
     * Xử lý logic đăng nhập
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tên đăng nhập và mật khẩu.",
                    "Lỗi Đăng Nhập", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Gọi phương thức login từ UserDAO
        currentUser = userDAO.login(username, password);

        if (currentUser != null) {
            JOptionPane.showMessageDialog(this,
                    "Đăng nhập thành công! Chào mừng " + currentUser.getUsername() + ".",
                    "Đăng Nhập Thành Công", JOptionPane.INFORMATION_MESSAGE);

            // Đóng cửa sổ LoginFrame
            this.dispose();

            // Mở Dashboard tương ứng với vai trò người dùng
            if (currentUser.getRole() == Role.ADMIN) {
                AdminDashboardFrame adminDashboard = new AdminDashboardFrame();
                adminDashboard.setVisible(true);
            } else if (currentUser.getRole() == Role.USER) {
                UserDashboardFrame userDashboard = new UserDashboardFrame();
                userDashboard.setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Tên đăng nhập hoặc mật khẩu không đúng.",
                    "Lỗi Đăng Nhập", JOptionPane.ERROR_MESSAGE);
            passwordField.setText(""); // Xóa mật khẩu để người dùng nhập lại
            passwordField.requestFocusInWindow();
        }
    }

    // (Optional) main method để chạy thử riêng LoginFrame
    // public static void main(String[] args) {
    //     SwingUtilities.invokeLater(() -> {
    //         try {
    //             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         }
    //         new LoginFrame().setVisible(true);
    //     });
    // }
}