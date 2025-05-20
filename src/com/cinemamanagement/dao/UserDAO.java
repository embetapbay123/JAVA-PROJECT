package com.cinemamanagement.dao;

import com.cinemamanagement.model.Ticket;
import com.cinemamanagement.model.User;
import com.cinemamanagement.model.User.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User login(String username, String password) {
        String sql = "SELECT id, username, password, role FROM User WHERE username = ? AND password = ?";
        // CẢNH BÁO: VẪN LÀ SO SÁNH PLAINTEXT PASSWORD - CẦN HASHING TRONG THỰC TẾ
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"), // Cân nhắc không trả về password
                            Role.valueOf(rs.getString("role").toUpperCase())
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.login: Lỗi khi đăng nhập user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Người dùng tự đăng ký tài khoản (mặc định vai trò USER).
     * @param user Đối tượng User (password nên đã được hash ở tầng UI/Service).
     * @return true nếu đăng ký thành công.
     */
    public boolean registerUserSelf(User user) {
        // Người dùng tự đăng ký sẽ luôn có vai trò là USER
        user.setRole(Role.USER); // Đảm bảo vai trò là USER
        String sql = "INSERT INTO User (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Nên lưu hash password
            pstmt.setString(3, user.getRole().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("UserDAO.registerUserSelf: User " + user.getUsername() + " registered successfully with ID " + user.getId());
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Unique constraint violation
                System.err.println("UserDAO.registerUserSelf: Lỗi đăng ký: Username '" + user.getUsername() + "' đã tồn tại.");
            } else {
                System.err.println("UserDAO.registerUserSelf: Lỗi SQL khi đăng ký user: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Admin thêm một người dùng mới (có thể chọn vai trò).
     * @param user Đối tượng User (password nên đã được hash).
     * @return true nếu thêm thành công.
     */
    public boolean addUserByAdmin(User user) {
        // Admin có thể đặt vai trò khi thêm user
        String sql = "INSERT INTO User (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Nên lưu hash password
            pstmt.setString(3, user.getRole().name()); // Vai trò do Admin cung cấp

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("UserDAO.addUserByAdmin: User " + user.getUsername() + " added successfully by admin with ID " + user.getId() + " and Role " + user.getRole());
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                System.err.println("UserDAO.addUserByAdmin: Lỗi thêm user: Username '" + user.getUsername() + "' đã tồn tại.");
            } else {
                System.err.println("UserDAO.addUserByAdmin: Lỗi SQL khi thêm user: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }


    public User getUserById(int userId) {
        String sql = "SELECT id, username, role FROM User WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            null, // Không trả về password
                            Role.valueOf(rs.getString("role").toUpperCase())
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.getUserById: Lỗi khi lấy user bằng ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role FROM User ORDER BY username ASC"; // Sắp xếp cho dễ nhìn
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        null,
                        Role.valueOf(rs.getString("role").toUpperCase())
                ));
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.getAllUsers: Lỗi khi lấy tất cả user: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Admin cập nhật vai trò của một người dùng.
     * @param userId ID của người dùng cần cập nhật.
     * @param newRole Vai trò mới.
     * @return true nếu cập nhật thành công.
     */
    public boolean updateUserRole(int userId, Role newRole) {
        String sql = "UPDATE User SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRole.name());
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("UserDAO.updateUserRole: Updated role for user ID " + userId + " to " + newRole.name());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.updateUserRole: Lỗi khi cập nhật vai trò user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Admin có thể cập nhật thông tin người dùng (ví dụ: chỉ username, hoặc sau này là status).
     * Việc cập nhật password nên có phương thức riêng và xử lý hash cẩn thận.
     * Hiện tại, ví dụ này chỉ cho phép cập nhật username (mặc dù không khuyến khích thay đổi username thường xuyên)
     * và vai trò. Nếu bạn không muốn cho sửa username, hãy bỏ nó ra.
     * @param user Đối tượng User với thông tin mới. ID phải được giữ nguyên.
     * @return true nếu cập nhật thành công.
     */
    public boolean updateUserByAdmin(User user) {
        // Giả sử Admin có thể sửa username và role. KHÔNG SỬA PASSWORD ở đây.
        String sql = "UPDATE User SET username = ?, role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getRole().name());
            pstmt.setInt(3, user.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("UserDAO.updateUserByAdmin: Updated user ID " + user.getId());
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Username có thể bị trùng nếu sửa
                System.err.println("UserDAO.updateUserByAdmin: Lỗi cập nhật: Username '" + user.getUsername() + "' có thể đã tồn tại.");
            } else {
                System.err.println("UserDAO.updateUserByAdmin: Lỗi SQL khi cập nhật user ID " + user.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * Admin xóa một người dùng.
     * CẢNH BÁO: Xem xét ràng buộc khóa ngoại và hành vi ON DELETE CASCADE.
     * @param userId ID của người dùng cần xóa.
     * @return true nếu xóa thành công.
     */
    public boolean deleteUser(int userId) {
        // Kiểm tra xem người dùng có phải là admin cuối cùng không (tùy chọn)
        // if (isLastAdmin(userId)) {
        //     System.err.println("UserDAO.deleteUser: Không thể xóa admin cuối cùng.");
        //     return false;
        // }

        // Hiện tại, CSDL có ON DELETE CASCADE cho Ticket. Xóa User sẽ xóa Ticket của họ.
        // Nếu bạn không muốn điều này, cần thay đổi schema hoặc logic ở đây.
        // Ví dụ, kiểm tra xem user có ticket không trước khi xóa:
        String sqlCheckTickets = "SELECT COUNT(*) FROM Ticket WHERE user_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // conn.setAutoCommit(false); // Nếu cần transaction cho việc kiểm tra và xóa

            try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckTickets)) {
                pstmtCheck.setInt(1, userId);
                try (ResultSet rs = pstmtCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.err.println("UserDAO.deleteUser: Không thể xóa người dùng ID " + userId + " vì họ đã có vé. Cân nhắc vô hiệu hóa tài khoản thay vì xóa.");
                        // if (conn != null) conn.rollback(); // Nếu dùng transaction
                        return false;
                    }
                }
            }

            String sqlDelete = "DELETE FROM User WHERE id = ?";
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDelete)) {
                pstmtDelete.setInt(1, userId);
                int affectedRows = pstmtDelete.executeUpdate();
                if (affectedRows > 0) {
                    // if (conn != null) conn.commit(); // Nếu dùng transaction
                    System.out.println("UserDAO.deleteUser: Deleted user ID " + userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.deleteUser: Lỗi SQL khi xóa user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            // if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
        } finally {
            // if (conn != null) { try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
        return false;
    }
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
    // (Tùy chọn) private boolean isLastAdmin(int userIdToCheck) {
    //     String sql = "SELECT COUNT(*) FROM User WHERE role = 'ADMIN'";
    //     String sqlCheckUserRole = "SELECT role FROM User WHERE id = ?";
    //     try (Connection conn = DatabaseConnection.getConnection();
    //          Statement stmt = conn.createStatement();
    //          PreparedStatement pstmtCheckRole = conn.prepareStatement(sqlCheckUserRole)) {

    //         pstmtCheckRole.setInt(1, userIdToCheck);
    //         String roleOfUserToCheck = null;
    //         try(ResultSet rsRole = pstmtCheckRole.executeQuery()){
    //             if(rsRole.next()){
    //                 roleOfUserToCheck = rsRole.getString("role");
    //             }
    //         }

    //         if ("ADMIN".equals(roleOfUserToCheck)) {
    //             try (ResultSet rs = stmt.executeQuery(sql)) {
    //                 if (rs.next() && rs.getInt(1) <= 1) {
    //                     return true; // Chỉ còn 1 admin (và đó là user này)
    //                 }
    //             }
    //         }
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }
    //     return false;
    // }
}