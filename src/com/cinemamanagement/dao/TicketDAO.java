package com.cinemamanagement.dao;

import com.cinemamanagement.model.Ticket;
import com.cinemamanagement.model.Seat; // Để trả về danh sách Seat
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.model.Room;
import com.cinemamanagement.model.Showtime;


import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TicketDAO {

    /**
     * Thêm một vé mới (đặt vé).
     * @param ticket Đối tượng Ticket.
     * @return true nếu đặt vé thành công.
     */
    public boolean addTicket(Ticket ticket) {
        String sql = "INSERT INTO Ticket (showtime_id, user_id, seat_id, booking_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, ticket.getShowtimeId());
            pstmt.setInt(2, ticket.getUserId());
            pstmt.setInt(3, ticket.getSeatId());
            // Nếu booking_time là null, CSDL sẽ tự dùng CURRENT_TIMESTAMP
            // Nếu muốn set từ Java:
            if (ticket.getBookingTime() != null) {
                 pstmt.setTimestamp(4, new java.sql.Timestamp(ticket.getBookingTime().getTime()));
            } else {
                 pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime())); // Thời gian hiện tại
            }


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ticket.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Unique constraint violation (showtime_id, seat_id)
                 System.err.println("Lỗi đặt vé: Ghế này đã được đặt cho suất chiếu này.");
            } else {
                System.err.println("Lỗi khi đặt vé: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Lấy thông tin vé bằng ID.
     * @param ticketId ID của vé.
     * @return Đối tượng Ticket nếu tìm thấy.
     */
    public Ticket getTicketById(int ticketId) {
        String sql = "SELECT * FROM Ticket WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ticketId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTicket(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy vé bằng ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các vé đã đặt bởi một người dùng.
     * @param userId ID của người dùng.
     * @return List các đối tượng Ticket.
     */
    public List<Ticket> getTicketsByUser(int userId) {
        List<Ticket> tickets = new ArrayList<>();
        // Sắp xếp theo thời gian đặt vé, mới nhất trước
        String sql = "SELECT * FROM Ticket WHERE user_id = ? ORDER BY booking_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy vé theo người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        return tickets;
    }
    
    /**
     * Lấy danh sách các vé đã đặt của một người dùng, bao gồm thông tin chi tiết về phim, phòng, suất chiếu.
     * Đây là một ví dụ về việc JOIN nhiều bảng để lấy thông tin đầy đủ cho hiển thị.
     * Bạn có thể tạo một lớp DTO (Data Transfer Object) riêng để chứa thông tin này.
     * Ví dụ: UserTicketDetails(Ticket ticket, Movie movie, Room room, Showtime showtime, Seat seat)
     * @param userId ID của người dùng.
     * @return List các đối tượng Ticket (hoặc DTO).
     */
    public List<TicketInfo> getTicketDetailsByUser(int userId) {
        List<TicketInfo> ticketInfos = new ArrayList<>();
        String sql = "SELECT t.id as ticket_id, t.booking_time, " +
                     "s.id as showtime_id, s.show_time, " +
                     "m.id as movie_id, m.title as movie_title, " +
                     "r.id as room_id, r.name as room_name, " +
                     "st.id as seat_id, st.seat_number " +
                     "FROM Ticket t " +
                     "JOIN Showtime s ON t.showtime_id = s.id " +
                     "JOIN Movie m ON s.movie_id = m.id " +
                     "JOIN Room r ON s.room_id = r.id " +
                     "JOIN Seat st ON t.seat_id = st.id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.booking_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Ticket ticket = new Ticket(
                            rs.getInt("ticket_id"),
                            rs.getInt("showtime_id"),
                            userId, // Đã biết userId
                            rs.getInt("seat_id"),
                            new Date(rs.getTimestamp("booking_time").getTime())
                    );
                    Movie movie = new Movie();
                    movie.setId(rs.getInt("movie_id"));
                    movie.setTitle(rs.getString("movie_title"));

                    Room room = new Room();
                    room.setId(rs.getInt("room_id"));
                    room.setName(rs.getString("room_name"));
                    
                    Showtime showtime = new Showtime();
                    showtime.setId(rs.getInt("showtime_id"));
                    showtime.setShowTime(new Date(rs.getTimestamp("show_time").getTime()));
                    showtime.setMovieId(movie.getId()); // Gán movie_id
                    showtime.setRoomId(room.getId()); // Gán room_id

                    Seat seat = new Seat(
                            rs.getInt("seat_id"),
                            room.getId(), // Gán room_id
                            rs.getString("seat_number")
                    );
                    
                    ticketInfos.add(new TicketInfo(ticket, movie, room, showtime, seat));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy chi tiết vé theo người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        return ticketInfos;
    }


    /**
     * Lấy danh sách các ghế đã được đặt cho một suất chiếu cụ thể.
     * @param showtimeId ID của suất chiếu.
     * @return List các đối tượng Seat đã được đặt.
     */
    public List<Seat> getBookedSeatsForShowtime(int showtimeId) {
        List<Seat> bookedSeats = new ArrayList<>();
        String sql = "SELECT s.id, s.room_id, s.seat_number " +
                     "FROM Seat s " +
                     "JOIN Ticket t ON s.id = t.seat_id " +
                     "WHERE t.showtime_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showtimeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookedSeats.add(new Seat(
                            rs.getInt("id"),
                            rs.getInt("room_id"),
                            rs.getString("seat_number")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy ghế đã đặt cho suất chiếu: " + e.getMessage());
            e.printStackTrace();
        }
        return bookedSeats;
    }

    /**
     * Hủy một vé (xóa vé).
     * @param ticketId ID của vé cần hủy.
     * @return true nếu hủy thành công.
     */
    public boolean deleteTicket(int ticketId) {
        String sql = "DELETE FROM Ticket WHERE id = ?";
        // Cân nhắc: Chỉ cho phép hủy vé nếu suất chiếu chưa bắt đầu.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ticketId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi hủy vé: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("id"),
                rs.getInt("showtime_id"),
                rs.getInt("user_id"),
                rs.getInt("seat_id"),
                new Date(rs.getTimestamp("booking_time").getTime())
        );
    }
    
    // Lớp nội bộ hoặc DTO để chứa thông tin chi tiết vé
    // Bạn có thể tạo file riêng cho lớp này nếu muốn
    public static class TicketInfo {
        public Ticket ticket;
        public Movie movie;
        public Room room;
        public Showtime showtime;
        public Seat seat;

        public TicketInfo(Ticket ticket, Movie movie, Room room, Showtime showtime, Seat seat) {
            this.ticket = ticket;
            this.movie = movie;
            this.room = room;
            this.showtime = showtime;
            this.seat = seat;
        }
    }
}