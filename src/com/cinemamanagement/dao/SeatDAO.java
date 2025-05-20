package com.cinemamanagement.dao;

import com.cinemamanagement.model.Seat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeatDAO {

    /**
     * Thêm một ghế cụ thể (ít dùng, thường ghế được tạo tự động).
     * @param seat Đối tượng Seat.
     * @return true nếu thêm thành công.
     */
    public boolean addSeat(Seat seat) {
        String sql = "INSERT INTO Seat (room_id, seat_number) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, seat.getRoomId());
            pstmt.setString(2, seat.getSeatNumber());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        seat.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm ghế: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy thông tin ghế bằng ID.
     * @param seatId ID của ghế.
     * @return Đối tượng Seat nếu tìm thấy, null nếu không.
     */
    public Seat getSeatById(int seatId) {
        String sql = "SELECT * FROM Seat WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, seatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSeat(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy ghế bằng ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các ghế của một phòng cụ thể.
     * @param roomId ID của phòng.
     * @return List các đối tượng Seat.
     */
    public List<Seat> getSeatsByRoom(int roomId) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT * FROM Seat WHERE room_id = ? ORDER BY seat_number ASC"; // Sắp xếp theo số ghế
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy ghế theo phòng: " + e.getMessage());
            e.printStackTrace();
        }
        return seats;
    }

    /**
     * Tự động tạo các bản ghi ghế cho một phòng dựa trên tổng số ghế.
     * Quy ước đặt tên ghế: A1, A2, ..., B1, B2, ... (10 ghế mỗi hàng)
     * @param roomId ID của phòng.
     * @param totalSeats Tổng số ghế của phòng.
     * @return true nếu tạo ghế thành công, false nếu có lỗi.
     */
    public boolean generateSeatsForRoom(int roomId, int totalSeats) {
        return generateSeatsForRoom(roomId, totalSeats, null); // Gọi phiên bản có connection
    }
    
    /**
     * Tự động tạo các bản ghi ghế cho một phòng dựa trên tổng số ghế.
     * Phiên bản này cho phép truyền vào một Connection để có thể sử dụng trong một transaction.
     * @param roomId ID của phòng.
     * @param totalSeats Tổng số ghế của phòng.
     * @param existingConnection Connection hiện có (nếu null, sẽ tạo mới).
     * @return true nếu tạo ghế thành công, false nếu có lỗi.
     */
    public boolean generateSeatsForRoom(int roomId, int totalSeats, Connection existingConnection) {
        System.out.println("SeatDAO: generateSeatsForRoom called for roomID: " + roomId + ", totalSeats: " + totalSeats);
        if (roomId <= 0 || totalSeats <= 0) {
            System.err.println("SeatDAO: Invalid roomId (" + roomId + ") or totalSeats (" + totalSeats + "). Cannot generate seats.");
            return false;
        }

        String sql = "INSERT INTO Seat (room_id, seat_number) VALUES (?, ?)";
        Connection conn = null;
        boolean isNewConnection = false; // Cờ để biết connection này có phải do phương thức này tạo ra không
        boolean success = false;
        boolean originalAutoCommitStateForNewConnection = true; // Chỉ dùng nếu isNewConnection

        try {
            if (existingConnection != null && !existingConnection.isClosed()) {
                conn = existingConnection;
                System.out.println("SeatDAO: Using existing database connection. Current autoCommit: " + conn.getAutoCommit());
                // Không thay đổi autoCommit của existingConnection
            } else {
                conn = DatabaseConnection.getConnection();
                if (conn == null) {
                    System.err.println("SeatDAO: Failed to get a new database connection.");
                    return false;
                }
                isNewConnection = true;
                originalAutoCommitStateForNewConnection = conn.getAutoCommit(); // Lưu trạng thái gốc
                if (originalAutoCommitStateForNewConnection) { // Chỉ setAutoCommit(false) nếu nó đang là true
                    conn.setAutoCommit(false);
                }
                System.out.println("SeatDAO: Created new database connection. Original autoCommit: " + originalAutoCommitStateForNewConnection + ", Current autoCommit: " + conn.getAutoCommit());
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                char rowChar = 'A';
                int seatNumInRow = 1;
                int seatsPerRow = 10; // Có thể làm tham số hoặc đọc từ cấu hình
                int batchedCount = 0;

                for (int i = 0; i < totalSeats; i++) {
                    String seatNumber = String.valueOf(rowChar) + seatNumInRow;
                    pstmt.setInt(1, roomId);
                    pstmt.setString(2, seatNumber);
                    pstmt.addBatch();
                    batchedCount++;

                    seatNumInRow++;
                    if (seatNumInRow > seatsPerRow) {
                        seatNumInRow = 1;
                        rowChar++;
                    }

                    if (batchedCount % 100 == 0 || batchedCount == totalSeats) { // Thực thi batch
                        System.out.println("SeatDAO: Executing batch of " + batchedCount + " seats for room " + roomId);
                        int[] batchResults = pstmt.executeBatch();
                        System.out.println("SeatDAO: Batch execution finished. Results length: " + batchResults.length);
                        for (int j = 0; j < batchResults.length; j++) {
                            if (batchResults[j] == Statement.EXECUTE_FAILED) {
                                System.err.println("SeatDAO: Batch insert failed for seat at index " + j + " in the current batch.");
                                throw new SQLException("Batch update failed for one or more seat insertions. Room ID: " + roomId);
                            }
                            // Statement.SUCCESS_NO_INFO (-2) cũng là thành công
                            // Các giá trị >= 0 là số dòng bị ảnh hưởng (thường là 1 cho INSERT)
                            if (batchResults[j] < 0 && batchResults[j] != Statement.SUCCESS_NO_INFO) {
                                 System.err.println("SeatDAO: Unexpected result from batch execution: " + batchResults[j]);
                                 // Có thể coi đây là lỗi tùy theo yêu cầu
                            }
                        }
                        // Nếu không có lỗi, reset batchedCount cho lần batch tiếp theo (nếu có)
                        // (Không cần thiết vì vòng lặp for sẽ kết thúc hoặc tiếp tục)
                    }
                }
                // Sau khi vòng lặp kết thúc, tất cả các batch đã được thực thi và kiểm tra
                success = true; // Nếu không có SQLException nào được ném từ kiểm tra batch
                System.out.println("SeatDAO: All " + totalSeats + " seats processed successfully for room ID: " + roomId);
            }

            if (isNewConnection) {
                if (success) {
                    conn.commit();
                    System.out.println("SeatDAO: New connection transaction committed for seat generation of room ID: " + roomId);
                } else {
                    System.err.println("SeatDAO: Seat generation failed with new connection, rolling back transaction for room ID: " + roomId);
                    conn.rollback(); // Rollback nếu success là false (do lỗi trước đó hoặc kiểm tra batch)
                }
            } else if (!success) {
                // Nếu dùng existingConnection và success = false (do lỗi từ executeBatch),
                // bên gọi phải xử lý rollback. Phương thức này không nên tự rollback existingConnection.
                System.err.println("SeatDAO: Seat generation failed using existing connection for room ID: " + roomId + ". Caller should handle rollback.");
            }

        } catch (SQLException e) {
            System.err.println("SeatDAO: SQLException during seat generation for roomID " + roomId + ": " + e.getMessage());
            e.printStackTrace();
            success = false; // Đảm bảo success là false khi có lỗi
            if (isNewConnection && conn != null) {
                try {
                    System.err.println("SeatDAO: Rolling back new connection transaction due to SQLException for room ID: " + roomId);
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("SeatDAO: Error during rollback after SQLException for room ID: " + roomId + ": " + ex.getMessage());
                }
            }
        } finally {
            if (isNewConnection && conn != null) {
                try {
                    if (originalAutoCommitStateForNewConnection) { // Chỉ khôi phục nếu ban đầu nó là true
                        conn.setAutoCommit(true);
                    }
                    System.out.println("SeatDAO: Restored autoCommit to " + originalAutoCommitStateForNewConnection + " for new connection for room ID: " + roomId);
                    // Không đóng connection ở đây, để DatabaseConnection.closeConnection() xử lý khi ứng dụng thoát
                    // hoặc nếu bạn dùng connection pool thì trả connection về pool.
                } catch (SQLException ex) {
                    System.err.println("SeatDAO: Error restoring autoCommit for new connection for room ID: " + roomId + ": " + ex.getMessage());
                }
            }
        }
        System.out.println("SeatDAO: generateSeatsForRoom finished for roomID: " + roomId + ". Overall success: " + success);
        return success;
    }


    /**
     * Xóa tất cả ghế của một phòng (thường dùng khi phòng bị xóa hoặc số ghế thay đổi).
     * @param roomId ID của phòng.
     * @return true nếu xóa thành công.
     */
    public boolean deleteSeatsByRoom(int roomId) {
        String sql = "DELETE FROM Seat WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate(); // Không cần kiểm tra số dòng ảnh hưởng
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa ghế theo phòng: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    private Seat mapResultSetToSeat(ResultSet rs) throws SQLException {
        return new Seat(
                rs.getInt("id"),
                rs.getInt("room_id"),
                rs.getString("seat_number")
        );
    }
}