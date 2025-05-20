package com.cinemamanagement.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // TODO: CẬP NHẬT CÁC THÔNG TIN NÀY CHO PHÙ HỢP VỚI MYSQL CỦA BẠN
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cinema_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root"; // Ví dụ: root
    private static final String DB_PASSWORD = "root"; // Ví dụ: 123456

    private static Connection connection = null;

    // Phương thức private constructor để ngăn việc tạo instance từ bên ngoài
    private DatabaseConnection() {}

    /**
     * Lấy một đối tượng Connection để tương tác với CSDL.
     * Nếu kết nối chưa được tạo hoặc đã đóng, một kết nối mới sẽ được thiết lập.
     *
     * @return Đối tượng Connection, hoặc null nếu có lỗi.
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // 1. Load MySQL JDBC Driver (không bắt buộc từ JDBC 4.0, nhưng để cho chắc)
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("MySQL JDBC Driver không tìm thấy!");
                    e.printStackTrace();
                    return null; // Trả về null nếu không tìm thấy driver
                }

                // 2. Thiết lập kết nối
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                // System.out.println("Kết nối CSDL thành công!"); // Bỏ comment nếu muốn log
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối CSDL: " + e.getMessage());
            e.printStackTrace();
            // Trong trường hợp lỗi, đảm bảo connection là null để lần gọi sau thử kết nối lại
            connection = null;
        }
        return connection;
    }

    /**
     * Đóng kết nối CSDL hiện tại nếu nó đang mở.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    // System.out.println("Đã đóng kết nối CSDL."); // Bỏ comment nếu muốn log
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi đóng kết nối CSDL: " + e.getMessage());
                e.printStackTrace();
            } finally {
                connection = null; // Đặt lại connection để lần sau getConnection() sẽ tạo mới
            }
        }
    }
}