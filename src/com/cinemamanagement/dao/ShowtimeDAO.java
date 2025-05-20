package com.cinemamanagement.dao;

import com.cinemamanagement.model.Showtime;
import com.cinemamanagement.model.Movie; // Cần để lấy duration

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ShowtimeDAO {

    /**
     * Kiểm tra xem có suất chiếu nào khác trong cùng một phòng bị chồng chéo thời gian
     * với suất chiếu được đề xuất hay không.
     *
     * @param roomId ID của phòng.
     * @param proposedStartTime Thời gian bắt đầu của suất chiếu đề xuất.
     * @param proposedEndTime Thời gian kết thúc của suất chiếu đề xuất.
     * @param excludeShowtimeId ID của suất chiếu cần loại trừ khỏi kiểm tra (dùng khi cập nhật).
     *                          Đặt là 0 hoặc giá trị không tồn tại (ví dụ: -1) nếu không cần loại trừ (khi thêm mới).
     * @return true nếu có xung đột, false nếu không.
     */
    public boolean hasTimeConflict(int roomId, Date proposedStartTime, Date proposedEndTime, int excludeShowtimeId) {
        // SQL kiểm tra xung đột: (StartTimeA < EndTimeB) AND (EndTimeA > StartTimeB)
        String sql = "SELECT COUNT(s.id) " +
                     "FROM Showtime s " +
                     "JOIN Movie m ON s.movie_id = m.id " + // Join với Movie để lấy duration
                     "WHERE s.room_id = ? " +
                     "AND s.id != ? " + // Loại trừ suất chiếu hiện tại nếu đang cập nhật
                     "AND (? < DATE_ADD(s.show_time, INTERVAL m.duration MINUTE) " + // proposedStartTime < oldShowtimeEndTime
                     "     AND ? > s.show_time)";                                  // proposedEndTime > oldShowtimeStartTime

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            pstmt.setInt(2, excludeShowtimeId);
            pstmt.setTimestamp(3, new java.sql.Timestamp(proposedStartTime.getTime()));
            pstmt.setTimestamp(4, new java.sql.Timestamp(proposedEndTime.getTime()));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int conflictCount = rs.getInt(1);
                    System.out.println("Time conflict check for room " + roomId +
                                       ", start: " + proposedStartTime +
                                       ", end: " + proposedEndTime +
                                       ", excluding: " + excludeShowtimeId +
                                       " -> Found conflicts: " + conflictCount); // Debug
                    return conflictCount > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra xung đột thời gian suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }
        // Nếu có lỗi trong quá trình kiểm tra, coi như có xung đột để đảm bảo an toàn
        System.err.println("Time conflict check resulted in error, assuming conflict."); // Debug
        return true;
    }


    public boolean addShowtime(Showtime showtime) {
        // 1. Lấy thông tin phim để biết thời lượng
        MovieDAO movieDAO = new MovieDAO(); // Tạo instance mới hoặc inject nếu dùng DI
        Movie movie = movieDAO.getMovieById(showtime.getMovieId());
        if (movie == null) {
            System.err.println("Lỗi thêm suất chiếu: Không tìm thấy phim với ID " + showtime.getMovieId());
            return false;
        }

        // 2. Tính thời gian kết thúc dự kiến của suất chiếu mới
        Calendar cal = Calendar.getInstance();
        cal.setTime(showtime.getShowTime());
        cal.add(Calendar.MINUTE, movie.getDuration());
        Date proposedEndTime = cal.getTime();

        // 3. Kiểm tra xung đột thời gian (excludeShowtimeId = 0 hoặc -1 vì đang thêm mới)
        if (hasTimeConflict(showtime.getRoomId(), showtime.getShowTime(), proposedEndTime, 0)) {
            System.err.println("Lỗi thêm suất chiếu: Xung đột thời gian với suất chiếu khác trong cùng phòng.");
            return false; // Trả về false để UI biết và thông báo
        }

        // 4. Nếu không có xung đột, tiến hành thêm suất chiếu
        String sql = "INSERT INTO Showtime (movie_id, room_id, show_time) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, showtime.getMovieId());
            pstmt.setInt(2, showtime.getRoomId());
            pstmt.setTimestamp(3, new java.sql.Timestamp(showtime.getShowTime().getTime()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        showtime.setId(generatedKeys.getInt(1));
                    }
                }
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
            System.err.println("Lỗi cập nhật suất chiếu: Không tìm thấy phim với ID " + showtime.getMovieId());
            return false;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(showtime.getShowTime());
        cal.add(Calendar.MINUTE, movie.getDuration());
        Date proposedEndTime = cal.getTime();

        // Khi cập nhật, exclude chính showtimeId đang được sửa
        if (hasTimeConflict(showtime.getRoomId(), showtime.getShowTime(), proposedEndTime, showtime.getId())) {
            System.err.println("Lỗi cập nhật suất chiếu: Xung đột thời gian với suất chiếu khác trong cùng phòng.");
            return false;
        }

        String sql = "UPDATE Showtime SET movie_id = ?, room_id = ?, show_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showtime.getMovieId());
            pstmt.setInt(2, showtime.getRoomId());
            pstmt.setTimestamp(3, new java.sql.Timestamp(showtime.getShowTime().getTime()));
            pstmt.setInt(4, showtime.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi cập nhật suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Các phương thức khác (getShowtimeById, getAllShowtimes, getShowtimesByMovie, etc.) giữ nguyên
    // Hoặc thêm các System.out.println để debug nếu cần
    public Showtime getShowtimeById(int showtimeId) {
        String sql = "SELECT * FROM Showtime WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showtimeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToShowtime(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy suất chiếu bằng ID: " + e.getMessage());
        }
        return null;
    }

    public List<Showtime> getAllShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String sql = "SELECT * FROM Showtime ORDER BY show_time DESC";
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
        String sql = "SELECT * FROM Showtime WHERE movie_id = ? AND show_time >= CURDATE() ORDER BY show_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    showtimes.add(mapResultSetToShowtime(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy suất chiếu theo phim: " + e.getMessage());
        }
        return showtimes;
    }
    
     public List<Showtime> getShowtimesByRoom(int roomId) {
        List<Showtime> showtimes = new ArrayList<>();
        String sql = "SELECT * FROM Showtime WHERE room_id = ? AND show_time >= CURDATE() ORDER BY show_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    showtimes.add(mapResultSetToShowtime(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy suất chiếu theo phòng: " + e.getMessage());
            e.printStackTrace();
        }
        return showtimes;
    }
    
    public List<Showtime> getUpcomingShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String sql = "SELECT * FROM Showtime WHERE show_time >= NOW() AND show_time <= DATE_ADD(NOW(), INTERVAL 7 DAY) ORDER BY show_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy các suất chiếu sắp tới: " + e.getMessage());
            e.printStackTrace();
        }
        return showtimes;
    }

    public boolean deleteShowtime(int showtimeId) {
        String sql = "DELETE FROM Showtime WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showtimeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa suất chiếu: " + e.getMessage());
        }
        return false;
    }

    private Showtime mapResultSetToShowtime(ResultSet rs) throws SQLException {
        return new Showtime(
                rs.getInt("id"),
                rs.getInt("movie_id"),
                rs.getInt("room_id"),
                new Date(rs.getTimestamp("show_time").getTime())
        );
    }
}