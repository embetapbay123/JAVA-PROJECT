package com.cinemamanagement.dao;

import com.cinemamanagement.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

	/**
	 * Lấy thông tin phòng bằng ID, sử dụng một Connection đã tồn tại.
	 * Phương thức này không mở hoặc đóng Connection.
	 *
	 * @param roomId ID của phòng cần tìm.
	 * @param conn   Đối tượng Connection đã được thiết lập và (có thể) đang trong một transaction.
	 * @return Đối tượng Room nếu tìm thấy, null nếu không hoặc có lỗi.
	 * @throws SQLException Nếu có lỗi khi thực hiện truy vấn SQL.
	 */
	private Room getRoomByIdWithConnection(int roomId, Connection conn) throws SQLException {
	    // 1. Kiểm tra đầu vào (Connection không null và chưa đóng)
	    if (conn == null || conn.isClosed()) {
	        System.err.println("RoomDAO.getRoomByIdWithConnection: Connection is null or closed.");
	        // Ném SQLException để bên gọi biết có vấn đề với connection,
	        // thay vì trả về null có thể gây nhầm lẫn.
	        throw new SQLException("Connection provided to getRoomByIdWithConnection is null or closed.");
	    }

	    String sql = "SELECT id, name, seat_count FROM Room WHERE id = ?";
	    System.out.println("RoomDAO.getRoomByIdWithConnection: Executing query for room ID: " + roomId); // DEBUG

	    // 2. Sử dụng try-with-resources cho PreparedStatement và ResultSet
	    //    KHÔNG dùng try-with-resources cho `conn` ở đây vì nó được truyền từ bên ngoài.
	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setInt(1, roomId);
	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                System.out.println("RoomDAO.getRoomByIdWithConnection: Room found - ID: " + rs.getInt("id") + ", Name: " + rs.getString("name")); // DEBUG
	                return mapResultSetToRoom(rs); // Gọi phương thức helper để tạo đối tượng Room
	            } else {
	                System.out.println("RoomDAO.getRoomByIdWithConnection: No room found with ID: " + roomId); // DEBUG
	                return null; // Không tìm thấy phòng
	            }
	        }
	    }
	    // SQLException từ PreparedStatement hoặc ResultSet sẽ được ném ra ngoài
	}
    /**
     * Thêm một phòng chiếu mới.
     * @param room Đối tượng Room.
     * @return true nếu thêm thành công, false nếu thất bại.
     */
    public boolean addRoom(Room room) {
        String sql = "INSERT INTO Room (name, seat_count) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, room.getName());
            pstmt.setInt(2, room.getSeatCount());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        room.setId(generatedKeys.getInt(1));
                    }
                }
                // Sau khi thêm phòng thành công, tự động tạo ghế cho phòng đó
                SeatDAO seatDAO = new SeatDAO();
                seatDAO.generateSeatsForRoom(room.getId(), room.getSeatCount());
                System.out.println("Đã thêm phòng thành công");
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                 System.err.println("Lỗi thêm phòng: Tên phòng '" + room.getName() + "' đã tồn tại.");
            } else {
                System.err.println("Lỗi khi thêm phòng: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Lấy thông tin phòng chiếu bằng ID.
     * @param roomId ID của phòng.
     * @return Đối tượng Room nếu tìm thấy, null nếu không.
     */
    public Room getRoomById(int roomId) {
        String sql = "SELECT * FROM Room WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy phòng bằng ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các phòng chiếu.
     * @return List các đối tượng Room.
     */
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM Room ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả phòng: " + e.getMessage());
            e.printStackTrace();
        }
        return rooms;
    }

    /**
     * Cập nhật thông tin phòng chiếu.
     * @param room Đối tượng Room với thông tin đã cập nhật.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean updateRoom(Room room) {
        String sqlUpdateRoom = "UPDATE Room SET name = ?, seat_count = ? WHERE id = ?";
        String sqlDeleteOldSeats = "DELETE FROM Seat WHERE room_id = ?";

        Connection conn = null;
        boolean success = false;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null || conn.isClosed()) {
                System.err.println("RoomDAO.updateRoom: Failed to get a valid database connection or connection is closed.");
                return false;
            }
            conn.setAutoCommit(false); // Bắt đầu transaction
            System.out.println("RoomDAO.updateRoom: Transaction started for room ID " + room.getId() + ". AutoCommit is " + conn.getAutoCommit()); // DEBUG

            // === SỬA ĐỔI QUAN TRỌNG Ở ĐÂY ===
            // 1. Lấy thông tin phòng cũ SỬ DỤNG CONNECTION HIỆN TẠI
            Room oldRoom = getRoomByIdWithConnection(room.getId(), conn);
            // ================================

            if (oldRoom == null) {
                System.err.println("RoomDAO.updateRoom: Room with ID " + room.getId() + " not found for update. Rolling back.");
                conn.rollback(); // Nếu không tìm thấy phòng, rollback ngay
                return false;
            }
            boolean seatCountChanged = oldRoom.getSeatCount() != room.getSeatCount();
            System.out.println("RoomDAO.updateRoom: Updating room ID " + room.getId() + ". Name: " + room.getName() + ", New SeatCount: " + room.getSeatCount() + ". Old SeatCount: " + oldRoom.getSeatCount() + ". SeatCountChanged: " + seatCountChanged);


            // 2. Cập nhật thông tin cơ bản của phòng
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateRoom)) {
                pstmtUpdate.setString(1, room.getName());
                pstmtUpdate.setInt(2, room.getSeatCount());
                pstmtUpdate.setInt(3, room.getId());

                int rowsAffected = pstmtUpdate.executeUpdate();
                if (rowsAffected == 0) {
                    System.err.println("RoomDAO.updateRoom: Update room table failed, no rows affected for ID " + room.getId() + ". Rolling back.");
                    conn.rollback();
                    return false;
                }
                System.out.println("RoomDAO.updateRoom: Room table updated successfully for ID " + room.getId());
            }

            // 3. Nếu seat_count thay đổi, xóa ghế cũ và tạo ghế mới
            if (seatCountChanged) {
                System.out.println("RoomDAO.updateRoom: Seat count changed. Proceeding to delete old seats and generate new ones for room ID " + room.getId());
                // 3a. Xóa tất cả ghế cũ của phòng này
                try (PreparedStatement pstmtDeleteSeats = conn.prepareStatement(sqlDeleteOldSeats)) {
                    pstmtDeleteSeats.setInt(1, room.getId());
                    int deletedSeatRows = pstmtDeleteSeats.executeUpdate();
                    System.out.println("RoomDAO.updateRoom: Deleted " + deletedSeatRows + " old seats for room ID " + room.getId());
                }

                // 3b. Tạo ghế mới với số lượng mới
                SeatDAO seatDAO = new SeatDAO();
                // Quan trọng: Truyền 'conn' (connection hiện tại của transaction) vào generateSeatsForRoom
                boolean seatsGeneratedSuccessfully = seatDAO.generateSeatsForRoom(room.getId(), room.getSeatCount(), conn);

                if (!seatsGeneratedSuccessfully) {
                    System.err.println("RoomDAO.updateRoom: Failed to generate new seats for room ID " + room.getId() + ". Rolling back entire transaction.");
                    conn.rollback(); // Rollback toàn bộ transaction nếu tạo ghế mới thất bại
                    return false;
                }
                System.out.println("RoomDAO.updateRoom: New seats generated successfully for room ID " + room.getId());
            }

            // 4. Nếu tất cả các bước thành công
            conn.commit(); // Hoàn tất transaction
            success = true;
            System.out.println("RoomDAO.updateRoom: Transaction committed successfully for room ID " + room.getId());

        } catch (SQLException e) { // Bắt SQLException từ getRoomByIdWithConnection hoặc các thao tác khác
            System.err.println("RoomDAO.updateRoom: SQLException occurred for room ID " + room.getId() + ". Error: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    // Kiểm tra lại xem conn có còn mở không trước khi rollback, mặc dù nó nên còn mở
                    if (!conn.isClosed()) {
                        System.err.println("RoomDAO.updateRoom: Rolling back transaction due to SQLException.");
                        conn.rollback();
                    } else {
                         System.err.println("RoomDAO.updateRoom: Connection was already closed before rollback attempt in catch block.");
                    }
                } catch (SQLException ex) {
                    System.err.println("RoomDAO.updateRoom: Error during rollback after SQLException: " + ex.getMessage());
                }
            }
            success = false;
        } finally {
            if (conn != null) {
                try {
                    // Chỉ khôi phục autoCommit nếu connection chưa bị đóng bởi một lỗi nghiêm trọng nào đó
                    if (!conn.isClosed()) {
                        conn.setAutoCommit(true);
                        System.out.println("RoomDAO.updateRoom: Restored autoCommit to true for connection.");
                    }
                } catch (SQLException ex) {
                    System.err.println("RoomDAO.updateRoom: Error restoring autoCommit: " + ex.getMessage());
                }
                // Không đóng connection ở đây
            }
        }
        System.out.println("RoomDAO.updateRoom: Finished for room ID " + room.getId() + ". Overall success: " + success);
        return success;
    }


    /**
     * Xóa một phòng chiếu.
     * Kéo theo xóa các Seat, Showtime và Ticket liên quan (do ON DELETE CASCADE).
     * @param roomId ID của phòng cần xóa.
     * @return true nếu xóa thành công, false nếu thất bại.
     */
    public boolean deleteRoom(int roomId) {
        String sql = "DELETE FROM Room WHERE id = ?";
        // Việc xóa phòng cũng sẽ tự động xóa các ghế, suất chiếu và vé liên quan
        // do ràng buộc khóa ngoại ON DELETE CASCADE.
        // Cân nhắc: Kiểm tra xem phòng có suất chiếu nào đang hoạt động không trước khi xóa.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa phòng: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("seat_count")
        );
    }
}