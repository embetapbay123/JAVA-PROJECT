package com.cinemamanagement.dao;

import com.cinemamanagement.model.User;
import com.cinemamanagement.model.User.Role; // Import enum Role

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Kiểm tra thông tin đăng nhập của người dùng.
     * @param username Tên đăng nhập
     * @param password Mật khẩu (chưa hash)
     * @return Đối tượng User nếu đăng nhập thành công, null nếu thất bại.
     */
    public User login(String username, String password) {
        String sql = "SELECT id, username, password, role FROM User WHERE username = ? AND password = ?";
        // LƯU Ý QUAN TRỌNG: Trong thực tế, không bao giờ so sánh password trực tiếp như thế này.
        // Bạn cần hash password khi đăng ký và hash password người dùng nhập vào rồi so sánh hash.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // So sánh password plain text (KHÔNG AN TOÀN)

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"), // Không nên trả về password ra ngoài
                            Role.valueOf(rs.getString("role").toUpperCase())
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đăng nhập user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Đăng ký một người dùng mới.
     * @param user Đối tượng User chứa thông tin đăng ký.
     * @return true nếu đăng ký thành công, false nếu thất bại (ví dụ: username đã tồn tại).
     */
    public boolean registerUser(User user) {
        String sql = "INSERT INTO User (username, password, role) VALUES (?, ?, ?)";
        // LƯU Ý: Password nên được hash trước khi gọi phương thức này và lưu hash vào CSDL.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Lưu password plain text (KHÔNG AN TOÀN)
            pstmt.setString(3, user.getRole().name()); // Chuyển enum Role thành String

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID tự tăng được tạo ra
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Mã lỗi SQL cho vi phạm ràng buộc unique (username đã tồn tại)
                System.err.println("Lỗi đăng ký: Username '" + user.getUsername() + "' đã tồn tại.");
            } else {
                System.err.println("Lỗi khi đăng ký user: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Lấy thông tin người dùng bằng ID.
     * @param userId ID của người dùng.
     * @return Đối tượng User nếu tìm thấy, null nếu không.
     */
    public User getUserById(int userId) {
        String sql = "SELECT id, username, role FROM User WHERE id = ?"; // Không lấy password
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
            System.err.println("Lỗi khi lấy user bằng ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả người dùng (chủ yếu cho admin).
     * @return Danh sách các User.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role FROM User"; // Không lấy password
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        null, // Không trả về password
                        Role.valueOf(rs.getString("role").toUpperCase())
                ));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả user: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // TODO: Các phương thức khác nếu cần (updateUser, deleteUser, etc.)
    // Việc xóa User cần cẩn thận vì có thể liên quan đến các Ticket đã đặt.
    // Cập nhật User có thể bao gồm thay đổi mật khẩu (cần cơ chế reset/thay đổi an toàn).
}