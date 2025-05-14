package com.cinemamanagement.ui;

import com.cinemamanagement.dao.UserDAO;
import com.cinemamanagement.model.User;
import com.cinemamanagement.model.User.Role;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<Role> roleComboBox; // Cho phép chọn vai trò (mặc định USER)
    private JButton registerButton;
    private JButton cancelButton;
    private UserDAO userDAO;

    public RegisterDialog(Frame owner) { // owner là frame cha (LoginFrame)
        super(owner, "Đăng Ký Tài Khoản", true); // true để dialog này là modal
        userDAO = new UserDAO();

        setSize(450, 300);
        setLocationRelativeTo(owner); // Hiển thị dialog ở giữa frame cha
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Đóng dialog khi nhấn nút X
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Tên đăng nhập
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Mật khẩu
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Xác nhận mật khẩu
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        confirmPasswordField = new JPasswordField(20);
        formPanel.add(confirmPasswordField, gbc);

        // Vai trò (Role)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Vai trò:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        roleComboBox = new JComboBox<>(Role.values()); // Lấy tất cả giá trị từ enum Role
        roleComboBox.setSelectedItem(Role.USER); // Mặc định là USER
        // roleComboBox.setEnabled(false); // Nếu bạn không muốn người dùng tự chọn role khi đăng ký
        formPanel.add(roleComboBox, gbc);

        // Panel cho các nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); // Căn phải
        registerButton = new JButton("Đăng ký");
        cancelButton = new JButton("Hủy");
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        // Thêm buttonPanel vào formPanel
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST; // Căn phải
        gbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(buttonPanel, gbc);

        add(formPanel);

        // Action listener cho nút Đăng ký
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegister();
            }
        });
        // Cho phép nhấn Enter để đăng ký từ confirmPasswordField
        confirmPasswordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegister();
            }
        });


        // Action listener cho nút Hủy
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Đóng dialog
            }
        });
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        Role role = (Role) roleComboBox.getSelectedItem();

        // Kiểm tra dữ liệu đầu vào
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin.", "Lỗi Đăng Ký", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự.", "Lỗi Đăng Ký", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu và xác nhận mật khẩu không khớp.", "Lỗi Đăng Ký", JOptionPane.ERROR_MESSAGE);
            confirmPasswordField.setText("");
            confirmPasswordField.requestFocusInWindow();
            return;
        }

        // Tạo đối tượng User mới
        // Trong thực tế, password ở đây nên được hash trước khi truyền vào UserDAO
        User newUser = new User(username, password, role);

        // Gọi phương thức register từ UserDAO
        if (userDAO.registerUser(newUser)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công! Vui lòng đăng nhập.", "Đăng Ký Thành Công", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // Đóng dialog sau khi đăng ký thành công
        } else {
            // UserDAO sẽ in lỗi cụ thể (ví dụ: username đã tồn tại) ra console
            // Bạn có thể lấy thông điệp lỗi từ UserDAO để hiển thị ở đây nếu muốn
            JOptionPane.showMessageDialog(this, "Đăng ký thất bại. Tên đăng nhập có thể đã tồn tại hoặc có lỗi xảy ra.", "Lỗi Đăng Ký", JOptionPane.ERROR_MESSAGE);
        }
    }
}