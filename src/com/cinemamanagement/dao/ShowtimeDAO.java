package com.cinemamanagement.dao;

import com.cinemamanagement.model.Showtime;
import com.cinemamanagement.model.Movie;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ShowtimeDAO {

    public boolean hasTimeConflict(int roomId, Date proposedStartTime, Date proposedEndTime, int excludeShowtimeId) {
        String sql = "SELECT COUNT(s.id) " +
                     "FROM Showtime s " +
                     "JOIN Movie m ON s.movie_id = m.id " +
                     "WHERE s.room_id = ? " +
                     "AND s.id != ? " +
                     "AND (? < DATE_ADD(s.show_time, INTERVAL m.duration MINUTE) " +
                     "     AND ? > s.show_time)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, excludeShowtimeId);
            pstmt.setTimestamp(3, new java.sql.Timestamp(proposedStartTime.getTime()));
            pstmt.setTimestamp(4, new java.sql.Timestamp(proposedEndTime.getTime()));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int conflictCount = rs.getInt(1);
                    System.out.println("ShowtimeDAO.hasTimeConflict: Room " + roomId + ", Start " + proposedStartTime + ", End " + proposedEndTime + ", Exclude " + excludeShowtimeId + " -> Conflicts: " + conflictCount);
                    return conflictCount > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra xung đột thời gian suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }
        System.err.println("ShowtimeDAO.hasTimeConflict: Error occurred, assuming conflict for safety.");
        return true;
    }

    public boolean addShowtime(Showtime showtime) {
        MovieDAO movieDAO = new MovieDAO();
        Movie movie = movieDAO.getMovieById(showtime.getMovieId());
        if (movie == null) {
            System.err.println("ShowtimeDAO.addShowtime: Không tìm thấy phim ID " + showtime.getMovieId());
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(showtime.getShowTime());
        cal.add(Calendar.MINUTE, movie.getDuration());
        Date proposedEndTime = cal.getTime();

        if (hasTimeConflict(showtime.getRoomId(), showtime.getShowTime(), proposedEndTime, 0)) {
            System.err.println("ShowtimeDAO.addShowtime: Xung đột thời gian cho phòng " + showtime.getRoomId());
            return false;
        }

        String sql = "INSERT INTO Showtime (movie_id, room_id, show_time, price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, showtime.getMovieId());
            pstmt.setInt(2, showtime.getRoomId());
            pstmt.setTimestamp(3, new java.sql.Timestamp(showtime.getShowTime().getTime()));
            pstmt.setBigDecimal(4, showtime.getPrice());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        showtime.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("ShowtimeDAO.addShowtime: Showtime added successfully with ID " + showtime.getId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi thêm suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateShowtime(Showtime showtime) {
        MovieDAO movieDAO = new MovieDAO();
        Movie movie = movieDAO.getMovieById(showtime.getMovieId());
        if (movie == null) {
            System.err.println("ShowtimeDAO.updateShowtime: Không tìm thấy phim ID " + showtime.getMovieId());
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(showtime.getShowTime());
        cal.add(Calendar.MINUTE, movie.getDuration());
        Date proposedEndTime = cal.getTime();

        if (hasTimeConflict(showtime.getRoomId(), showtime.getShowTime(), proposedEndTime, showtime.getId())) {
            System.err.println("ShowtimeDAO.updateShowtime: Xung đột thời gian cho phòng " + showtime.getRoomId() + " khi cập nhật suất chiếu ID " + showtime.getId());
            return false;
        }

        String sql = "UPDATE Showtime SET movie_id = ?, room_id = ?, show_time = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showtime.getMovieId());
            pstmt.setInt(2, showtime.getRoomId());
            pstmt.setTimestamp(3, new java.sql.Timestamp(showtime.getShowTime().getTime()));
            pstmt.setBigDecimal(4, showtime.getPrice());
            pstmt.setInt(5, showtime.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("ShowtimeDAO.updateShowtime: Showtime updated successfully for ID " + showtime.getId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi cập nhật suất chiếu ID " + showtime.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Showtime mapResultSetToShowtime(ResultSet rs) throws SQLException {
        return new Showtime(
                rs.getInt("id"),
                rs.getInt("movie_id"),
                rs.getInt("room_id"),
                new Date(rs.getTimestamp("show_time").getTime()),
                rs.getBigDecimal("price")
        );
    }

    public Showtime getShowtimeById(int showtimeId) {
        String sql = "SELECT id, movie_id, room_id, show_time, price FROM Showtime WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showtimeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToShowtime(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy suất chiếu bằng ID " + showtimeId + ": " + e.getMessage());
        }
        return null;
    }

    public List<Showtime> getAllShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String sql = "SELECT id, movie_id, room_id, show_time, price FROM Showtime ORDER BY show_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả suất chiếu: " + e.getMessage());
        }
        return showtimes;
    }

    public List<Showtime> getShowtimesByMovie(int movieId) {
        List<Showtime> showtimes = new ArrayList<>();
        // Chỉ lấy các suất chiếu từ hôm nay trở đi
        String sql = "SELECT id, movie_id, room_id, show_time, price FROM Showtime WHERE movie_id = ? AND show_time >= CURDATE() ORDER BY show_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    showtimes.add(mapResultSetToShowtime(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy suất chiếu theo phim ID " + movieId + ": " + e.getMessage());
        }
        return showtimes;
    }
    
    public boolean deleteShowtime(int showtimeId) {
        String sqlCheckTickets = "SELECT COUNT(*) FROM Ticket WHERE showtime_id = ?";
        String sqlDelete = "DELETE FROM Showtime WHERE id = ?";
        Connection conn = null; // Quản lý transaction thủ công nếu cần kiểm tra trước khi xóa
        try {
            conn = DatabaseConnection.getConnection();
            // Nếu bạn muốn đảm bảo không xóa suất chiếu đã có vé mà không muốn CSDL tự CASCADE,
            // bạn có thể bỏ autoCommit, kiểm tra, rồi mới quyết định commit/rollback.
            // conn.setAutoCommit(false); 
            
            // Kiểm tra xem có vé nào liên quan không (tùy chọn, vì CSDL đã có ON DELETE CASCADE)
            // Nếu bạn muốn ngăn xóa nếu có vé, hãy xử lý ở đây
            // try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckTickets)) {
            //     pstmtCheck.setInt(1, showtimeId);
            //     try (ResultSet rs = pstmtCheck.executeQuery()) {
            //         if (rs.next() && rs.getInt(1) > 0) {
            //             System.err.println("ShowtimeDAO.deleteShowtime: Suất chiếu ID " + showtimeId + " đã có vé đặt. Hiện tại CSDL sẽ xóa vé theo (CASCADE).");
            //             // if (conn != null && !conn.getAutoCommit()) conn.rollback();
            //             // return false; // Uncomment nếu muốn chặn xóa
            //         }
            //     }
            // }

            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDelete)) {
                pstmtDelete.setInt(1, showtimeId);
                int affectedRows = pstmtDelete.executeUpdate();
                if (affectedRows > 0) {
                    // if (conn != null && !conn.getAutoCommit()) conn.commit();
                    System.out.println("ShowtimeDAO.deleteShowtime: Deleted showtime ID " + showtimeId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi xóa suất chiếu ID " + showtimeId + ": " + e.getMessage());
            e.printStackTrace();
            // if (conn != null && !conn.getAutoCommit()) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
        } finally {
            // if (conn != null && !conn.getAutoCommit()) { try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
        return false;
    }
}