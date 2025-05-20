// com.cinemamanagement.ui.UserManagementPanel.java
package com.cinemamanagement.ui;

import com.cinemamanagement.dao.UserDAO;
import com.cinemamanagement.model.User;
import com.cinemamanagement.model.User.Role; // Quan trọng

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton, changeRoleButton, deleteUserButton;
    // private JButton addUserButton; // Tùy chọn, cần dialog riêng

    // Components cho form chi tiết
    private JTextField userIdField;
    private JTextField usernameField;
    private JComboBox<Role> roleComboBox;
    private JButton clearDetailsButton;

    private UserDAO userDAO;
    private User selectedUserInForm = null; // Người dùng đang được hiển thị trong form chi tiết

    public UserManagementPanel() {
        userDAO = new UserDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        refreshUserList(); // Tải danh sách người dùng khi panel được tạo
    }

    private void initComponents() {
        // Panel nút chính ở trên cùng
        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,5));
        refreshButton = new JButton("Làm Mới Danh Sách");
        topButtonPanel.add(refreshButton);

        // addUserButton = new JButton("Thêm Người Dùng Mới");
        // topButtonPanel.add(addUserButton); // Sẽ cần một dialog riêng để nhập thông tin

        add(topButtonPanel, BorderLayout.NORTH);

        // Bảng hiển thị danh sách người dùng
        String[] columnNames = {"ID", "Tên Đăng Nhập", "Vai Trò"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa trực tiếp trên bảng
            }
        };
        userTable = new JTable(tableModel);
        userTable.setRowSorter(new TableRowSorter<>(tableModel));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.getTableHeader().setPreferredSize(new Dimension(0, 30)); // Chiều cao header
        userTable.setRowHeight(25); // Chiều cao dòng

        // Sự kiện khi click vào một dòng trên bảng
        userTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { // Một click
                    int selectedRowOnTable = userTable.getSelectedRow();
                    if (selectedRowOnTable != -1) {
                        // Chuyển đổi index của view sang index của model (quan trọng khi có sort)
                        int modelRow = userTable.convertRowIndexToModel(selectedRowOnTable);
                        populateDetailsFromTable(modelRow);
                    }
                }
            }
        });
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Panel chi tiết người dùng và các nút thao tác (ở dưới cùng)
        JPanel detailsAndActionsPanel = new JPanel(new GridBagLayout());
        detailsAndActionsPanel.setBorder(BorderFactory.createTitledBorder("Chi tiết và Thao tác với Người Dùng"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // ID người dùng
        gbc.gridx = 0; gbc.gridy = 0; detailsAndActionsPanel.add(new JLabel("ID Người Dùng:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; // Cho phép field mở rộng
        userIdField = new JTextField(15);
        userIdField.setEditable(false); // ID không được sửa
        detailsAndActionsPanel.add(userIdField, gbc);

        // Tên đăng nhập
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; // Reset weight
        detailsAndActionsPanel.add(new JLabel("Tên Đăng Nhập:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setEditable(false); // Không cho sửa username từ đây (nếu muốn sửa, cần logic phức tạp hơn)
        detailsAndActionsPanel.add(usernameField, gbc);

        // Vai trò
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        detailsAndActionsPanel.add(new JLabel("Vai Trò:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        roleComboBox = new JComboBox<>(Role.values()); // Lấy tất cả giá trị từ Enum Role
        roleComboBox.setEnabled(false); // Chỉ enable khi user được chọn và không phải admin hiện tại
        detailsAndActionsPanel.add(roleComboBox, gbc);

        // Panel chứa các nút thao tác
        JPanel actionButtonsSubPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        changeRoleButton = new JButton("Lưu Thay Đổi Vai Trò");
        changeRoleButton.setEnabled(false);
        actionButtonsSubPanel.add(changeRoleButton);

        deleteUserButton = new JButton("Xóa Người Dùng");
        deleteUserButton.setEnabled(false);
        deleteUserButton.setBackground(new Color(255, 182, 193)); // Màu hồng nhạt cho nút xóa
        actionButtonsSubPanel.add(deleteUserButton);
        
        clearDetailsButton = new JButton("Xóa Form Chi Tiết");
        actionButtonsSubPanel.add(clearDetailsButton);


        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; // Cho panel nút chiếm 2 cột
        gbc.fill = GridBagConstraints.NONE; // Không cho panel nút mở rộng
        gbc.anchor = GridBagConstraints.SOUTHEAST; // Căn phải dưới
        gbc.weightx = 0; // Reset
        detailsAndActionsPanel.add(actionButtonsSubPanel, gbc);

        add(detailsAndActionsPanel, BorderLayout.SOUTH);

        // Gán sự kiện cho các nút
        refreshButton.addActionListener(e -> refreshUserList());
        changeRoleButton.addActionListener(e -> handleChangeRole());
        deleteUserButton.addActionListener(e -> handleDeleteUser());
        clearDetailsButton.addActionListener(e -> clearDetailsForm());
        // addUserButton.addActionListener(e -> handleAddNewUser());
    }

    public void refreshUserList() {
        System.out.println("UserManagementPanel: Refreshing user list...");
        tableModel.setRowCount(0); // Xóa dữ liệu cũ trên bảng
        List<User> users = userDAO.getAllUsers();
        if (users != null) {
            for (User user : users) {
                tableModel.addRow(new Object[]{
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name() // Hiển thị tên của enum (ADMIN, USER)
                });
            }
        }
        clearDetailsForm(); // Xóa form chi tiết sau khi tải lại bảng
        System.out.println("UserManagementPanel: User list refreshed.");
    }

    private void populateDetailsFromTable(int modelRowIndex) {
        // Lấy ID từ cột đầu tiên của model bảng
        int userId = (int) tableModel.getValueAt(modelRowIndex, 0);
        // Lấy đầy đủ thông tin User từ DAO (vì bảng có thể không chứa tất cả)
        selectedUserInForm = userDAO.getUserById(userId);

        if (selectedUserInForm != null) {
            userIdField.setText(String.valueOf(selectedUserInForm.getId()));
            usernameField.setText(selectedUserInForm.getUsername());
            roleComboBox.setSelectedItem(selectedUserInForm.getRole());

            // Logic không cho Admin tự sửa/xóa chính mình
            boolean isSelf = (LoginFrame.currentUser != null && selectedUserInForm.getId() == LoginFrame.currentUser.getId());
            
            roleComboBox.setEnabled(!isSelf); // Cho phép sửa role nếu không phải là chính mình
            changeRoleButton.setEnabled(!isSelf);
            deleteUserButton.setEnabled(!isSelf);

            if (isSelf) {
                System.out.println("UserManagementPanel: Admin selected self. Disabling modification buttons.");
            }

        } else {
            System.err.println("UserManagementPanel: Could not retrieve user details for ID: " + userId);
            clearDetailsForm(); // Nếu không lấy được user, xóa form
        }
    }

    private void clearDetailsForm() {
        selectedUserInForm = null; // Không còn user nào được chọn trong form
        userIdField.setText("");
        usernameField.setText("");
        roleComboBox.setSelectedIndex(-1); // Bỏ chọn item trong combobox
        roleComboBox.setEnabled(false);    // Vô hiệu hóa combobox
        changeRoleButton.setEnabled(false); // Vô hiệu hóa nút
        deleteUserButton.setEnabled(false); // Vô hiệu hóa nút
        userTable.clearSelection();        // Bỏ chọn dòng trên bảng
        System.out.println("UserManagementPanel: Details form cleared.");
    }

    private void handleChangeRole() {
        if (selectedUserInForm == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng từ bảng để thay đổi vai trò.", "Chưa chọn người dùng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Kiểm tra lại lần nữa (dù đã có logic disable nút)
        if (LoginFrame.currentUser != null && selectedUserInForm.getId() == LoginFrame.currentUser.getId()) {
            JOptionPane.showMessageDialog(this, "Bạn không thể thay đổi vai trò của chính mình.", "Hành động bị chặn", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Role newRole = (Role) roleComboBox.getSelectedItem();
        if (newRole == null) { // Nên có placeholder hoặc luôn có item được chọn
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một vai trò hợp lệ.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newRole == selectedUserInForm.getRole()) {
            JOptionPane.showMessageDialog(this, "Vai trò người dùng không có gì thay đổi.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn thay đổi vai trò của người dùng '" + selectedUserInForm.getUsername() + "' từ " +
                selectedUserInForm.getRole().name() + " thành " + newRole.name() + "?",
                "Xác nhận thay đổi vai trò",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.updateUserRole(selectedUserInForm.getId(), newRole)) {
                JOptionPane.showMessageDialog(this, "Thay đổi vai trò thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                refreshUserList(); // Tải lại danh sách để cập nhật bảng và xóa form
            } else {
                JOptionPane.showMessageDialog(this, "Thay đổi vai trò thất bại. Vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteUser() {
        if (selectedUserInForm == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng từ bảng để xóa.", "Chưa chọn người dùng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Kiểm tra lại
        if (LoginFrame.currentUser != null && selectedUserInForm.getId() == LoginFrame.currentUser.getId()) {
            JOptionPane.showMessageDialog(this, "Bạn không thể xóa tài khoản của chính mình.", "Hành động bị chặn", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn XÓA VĨNH VIỄN người dùng '" + selectedUserInForm.getUsername() + "' (ID: " + selectedUserInForm.getId() + ") không?\n" +
                "HÀNH ĐỘNG NÀY KHÔNG THỂ HOÀN TÁC.\n" +
                (userDAO.getTicketsByUserId(selectedUserInForm.getId()).size() > 0 ? // Kiểm tra nếu có vé
                 "NGƯỜI DÙNG NÀY ĐÃ CÓ VÉ, VIỆC XÓA SẼ XÓA CẢ VÉ CỦA HỌ (DO ON DELETE CASCADE)." :
                 "Người dùng này chưa có vé."),
                "XÁC NHẬN XÓA NGƯỜI DÙNG",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        // Bạn cần thêm phương thức getTicketsByUserId(int userId) vào UserDAO nếu muốn kiểm tra như trên
        // Hoặc dựa vào kết quả của userDAO.deleteUser() để thông báo
        // Ví dụ đơn giản hơn:
        // int confirm = JOptionPane.showConfirmDialog(this,
        //         "Bạn có chắc chắn muốn XÓA VĨNH VIỄN người dùng '" + selectedUserInForm.getUsername() + "' (ID: " + selectedUserInForm.getId() + ") không?\n" +
        //         "HÀNH ĐỘNG NÀY KHÔNG THỂ HOÀN TÁC VÀ CÓ THỂ XÓA CÁC VÉ LIÊN QUAN.",
        //         "XÁC NHẬN XÓA NGƯỜI DÙNG",
        //         JOptionPane.YES_NO_OPTION,
        //         JOptionPane.WARNING_MESSAGE);


        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(selectedUserInForm.getId())) {
                JOptionPane.showMessageDialog(this, "Xóa người dùng thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                refreshUserList(); // Tải lại danh sách và xóa form
            } else {
                // UserDAO.deleteUser có thể trả về false nếu có ràng buộc hoặc lỗi
                JOptionPane.showMessageDialog(this, "Xóa người dùng thất bại. Người dùng có thể đang có dữ liệu liên quan không thể xóa (ví dụ: vé) hoặc có lỗi CSDL.", "Lỗi Xóa", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Để UserDAO.getTicketsByUserId(int userId) hoạt động, bạn cần thêm phương thức này vào UserDAO
    // Hoặc vào TicketDAO (và gọi từ đây TicketDAO.getTicketsByUser(userId).size())
    // Ví dụ (thêm vào UserDAO hoặc gọi từ TicketDAO):
    /*
    // Trong UserDAO (hoặc tốt hơn là TicketDAO và UserDAO gọi nó)
    public List<Ticket> getTicketsByUserId(int userId) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT id, showtime_id, user_id, seat_id, booking_time FROM Ticket WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Tạo đối tượng Ticket và thêm vào list
                    // tickets.add(new Ticket(rs.getInt("id"), rs.getInt("showtime_id"), ...));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy vé của người dùng ID " + userId + ": " + e.getMessage());
        }
        return tickets;
    }
    */
    
    // private void handleAddNewUser() {
    //     // Mở một JDialog tùy chỉnh (ví dụ: AddUserDialog) để Admin nhập thông tin
    //     // AddUserDialog sẽ có các trường: username, password, confirmPassword, roleComboBox
    //     // Khi Admin nhấn "Lưu", nó sẽ gọi userDAO.addUserByAdmin(newUser);
    //     // Sau đó, gọi refreshUserList();
    //     JOptionPane.showMessageDialog(this, "Chức năng Thêm Người Dùng Mới cần được triển khai (mở Dialog).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    // }
}