package com.cinemamanagement.dao;

import com.cinemamanagement.model.Ticket;
import com.cinemamanagement.model.Seat;
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.model.Room;
import com.cinemamanagement.model.Showtime;
import com.cinemamanagement.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TicketDAO {

    // Phương thức addTicket giữ nguyên như phiên bản trước (đã xử lý pricePaid)
    public boolean addTicket(Ticket ticket) {
        String sql = "INSERT INTO Ticket (showtime_id, user_id, seat_id, price_paid, booking_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, ticket.getShowtimeId());
            pstmt.setInt(2, ticket.getUserId());
            pstmt.setInt(3, ticket.getSeatId());
            pstmt.setBigDecimal(4, ticket.getPricePaid());

            if (ticket.getBookingTime() != null) {
                 pstmt.setTimestamp(5, new java.sql.Timestamp(ticket.getBookingTime().getTime()));
            } else {
                 pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ticket.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("TicketDAO.addTicket: Ticket booked successfully with ID " + ticket.getId());
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) {
                 System.err.println("TicketDAO.addTicket: Lỗi đặt vé: Ghế ID " + ticket.getSeatId() + " đã được đặt cho suất chiếu ID " + ticket.getShowtimeId() + ". " + e.getMessage());
            } else {
                System.err.println("Lỗi SQL khi đặt vé: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    // Phương thức mapResultSetToTicket giữ nguyên
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("id"),
                rs.getInt("showtime_id"),
                rs.getInt("user_id"),
                rs.getInt("seat_id"),
                rs.getBigDecimal("price_paid"),
                new Date(rs.getTimestamp("booking_time").getTime())
        );
    }

    // Phương thức getTicketsByUser giữ nguyên
    public List<Ticket> getTicketsByUser(int userId) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT id, showtime_id, user_id, seat_id, price_paid, booking_time FROM Ticket WHERE user_id = ? ORDER BY booking_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy vé theo người dùng ID " + userId + ": " + e.getMessage());
        }
        return tickets;
    }
    
    // --- LỚP DTO (Data Transfer Object) ĐỂ CHỨA THÔNG TIN VÉ CHI TIẾT ---
    // Đảm bảo các lớp Model (Movie, Room, Showtime, Seat, Ticket) có constructor mặc định
    // và các setters nếu bạn khởi tạo đối tượng rỗng rồi set giá trị.
    public static class TicketInfo {
        public Ticket ticket;
        public Movie movie;
        public Room room;
        public Showtime showtime; // Chứa giá gốc của suất chiếu
        public Seat seat;
        public User user; // Thêm thông tin người đặt vé

        public TicketInfo(Ticket ticket, Movie movie, Room room, Showtime showtime, Seat seat, User user) {
            this.ticket = ticket;
            this.movie = movie;
            this.room = room;
            this.showtime = showtime;
            this.seat = seat;
            this.user = user;
        }
        // Có thể thêm các getters ở đây nếu cần truy cập từ bên ngoài DTO này
    }
    // --- KẾT THÚC LỚP DTO ---


    // Phương thức getTicketDetailsByUser đã cập nhật để bao gồm giá
    public List<TicketInfo> getTicketDetailsByUser(int userId) {
        List<TicketInfo> ticketInfos = new ArrayList<>();
        String sql = "SELECT t.id as ticket_id, t.booking_time, t.price_paid, " +
                     "s.id as showtime_id, s.show_time, s.price as showtime_price, s.movie_id as showtime_movie_id, s.room_id as showtime_room_id, " +
                     "m.id as movie_id, m.title as movie_title, m.genre as movie_genre, m.duration as movie_duration, " +
                     "r.id as room_id, r.name as room_name, r.seat_count as room_seat_count, " +
                     "st.id as seat_id, st.seat_number, " +
                     "u.id as user_id, u.username as user_username, u.role as user_role " + // Lấy thêm thông tin user
                     "FROM Ticket t " +
                     "JOIN Showtime s ON t.showtime_id = s.id " +
                     "JOIN Movie m ON s.movie_id = m.id " +
                     "JOIN Room r ON s.room_id = r.id " +
                     "JOIN Seat st ON t.seat_id = st.id " +
                     "JOIN User u ON t.user_id = u.id " + // Join với bảng User
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.booking_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Ticket ticket = new Ticket();
                    ticket.setId(rs.getInt("ticket_id"));
                    ticket.setShowtimeId(rs.getInt("showtime_id"));
                    ticket.setUserId(rs.getInt("user_id")); // Đã có userId từ tham số
                    ticket.setSeatId(rs.getInt("seat_id"));
                    ticket.setPricePaid(rs.getBigDecimal("price_paid"));
                    ticket.setBookingTime(new Date(rs.getTimestamp("booking_time").getTime()));
                    
                    Movie movie = new Movie();
                    movie.setId(rs.getInt("movie_id"));
                    movie.setTitle(rs.getString("movie_title"));
                    movie.setGenre(rs.getString("movie_genre"));
                    movie.setDuration(rs.getInt("movie_duration"));
                    
                    Room room = new Room();
                    room.setId(rs.getInt("room_id"));
                    room.setName(rs.getString("room_name"));
                    room.setSeatCount(rs.getInt("room_seat_count"));
                    
                    Showtime showtime = new Showtime();
                    showtime.setId(rs.getInt("showtime_id"));
                    showtime.setMovieId(rs.getInt("showtime_movie_id"));
                    showtime.setRoomId(rs.getInt("showtime_room_id"));
                    showtime.setShowTime(new Date(rs.getTimestamp("show_time").getTime()));
                    showtime.setPrice(rs.getBigDecimal("showtime_price"));

                    Seat seat = new Seat();
                    seat.setId(rs.getInt("seat_id"));
                    seat.setRoomId(room.getId()); // Lấy room_id từ đối tượng room đã tạo
                    seat.setSeatNumber(rs.getString("seat_number"));
                    
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("user_username"));
                    user.setRole(User.Role.valueOf(rs.getString("user_role").toUpperCase()));
                    
                    ticketInfos.add(new TicketInfo(ticket, movie, room, showtime, seat, user));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy chi tiết vé theo người dùng ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return ticketInfos;
    }

    // --- PHƯƠNG THỨC MỚI CHO ADMIN QUẢN LÝ VÉ ---
    /**
     * Lấy tất cả thông tin chi tiết của các vé trong hệ thống cho Admin.
     * Có thể thêm các tham số để lọc/tìm kiếm sau này.
     * @return List các đối tượng TicketInfo.
     */
    public List<TicketInfo> getAllTicketDetailsForAdmin() {
        List<TicketInfo> ticketInfos = new ArrayList<>();
        // Câu lệnh SQL tương tự getTicketDetailsByUser nhưng không có WHERE user_id
        String sql = "SELECT t.id as ticket_id, t.booking_time, t.price_paid, " +
                     "s.id as showtime_id, s.show_time, s.price as showtime_price, s.movie_id as showtime_movie_id, s.room_id as showtime_room_id, " +
                     "m.id as movie_id, m.title as movie_title, m.genre as movie_genre, m.duration as movie_duration, " +
                     "r.id as room_id, r.name as room_name, r.seat_count as room_seat_count, " +
                     "st.id as seat_id, st.seat_number, " +
                     "u.id as user_id, u.username as user_username, u.role as user_role " +
                     "FROM Ticket t " +
                     "JOIN Showtime s ON t.showtime_id = s.id " +
                     "JOIN Movie m ON s.movie_id = m.id " +
                     "JOIN Room r ON s.room_id = r.id " +
                     "JOIN Seat st ON t.seat_id = st.id " +
                     "JOIN User u ON t.user_id = u.id " +
                     "ORDER BY t.booking_time DESC"; // Sắp xếp theo thời gian đặt vé mới nhất

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); // Dùng Statement vì không có tham số động
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket ticket = new Ticket();
                ticket.setId(rs.getInt("ticket_id"));
                ticket.setShowtimeId(rs.getInt("showtime_id"));
                ticket.setUserId(rs.getInt("user_id"));
                ticket.setSeatId(rs.getInt("seat_id"));
                ticket.setPricePaid(rs.getBigDecimal("price_paid"));
                ticket.setBookingTime(new Date(rs.getTimestamp("booking_time").getTime()));
                
                Movie movie = new Movie();
                movie.setId(rs.getInt("movie_id"));
                movie.setTitle(rs.getString("movie_title"));
                movie.setGenre(rs.getString("movie_genre"));
                movie.setDuration(rs.getInt("movie_duration"));
                
                Room room = new Room();
                room.setId(rs.getInt("room_id"));
                room.setName(rs.getString("room_name"));
                room.setSeatCount(rs.getInt("room_seat_count"));
                
                Showtime showtime = new Showtime();
                showtime.setId(rs.getInt("showtime_id"));
                showtime.setMovieId(rs.getInt("showtime_movie_id"));
                showtime.setRoomId(rs.getInt("showtime_room_id"));
                showtime.setShowTime(new Date(rs.getTimestamp("show_time").getTime()));
                showtime.setPrice(rs.getBigDecimal("showtime_price"));

                Seat seat = new Seat();
                seat.setId(rs.getInt("seat_id"));
                seat.setRoomId(room.getId());
                seat.setSeatNumber(rs.getString("seat_number"));
                
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("user_username"));
                user.setRole(User.Role.valueOf(rs.getString("user_role").toUpperCase()));
                
                ticketInfos.add(new TicketInfo(ticket, movie, room, showtime, seat, user));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả chi tiết vé cho Admin: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("TicketDAO.getAllTicketDetailsForAdmin: Loaded " + ticketInfos.size() + " tickets."); // DEBUG
        return ticketInfos;
    }
    // --- KẾT THÚC PHƯƠNG THỨC MỚI ---


    // Phương thức getBookedSeatsForShowtime giữ nguyên
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
            System.err.println("Lỗi khi lấy ghế đã đặt cho suất chiếu ID " + showtimeId + ": " + e.getMessage());
        }
        return bookedSeats;
    }
    
    // Phương thức deleteTicket giữ nguyên
    public boolean deleteTicket(int ticketId) {
        String sql = "DELETE FROM Ticket WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi hủy vé ID " + ticketId + ": " + e.getMessage());
        }
        return false;
    }
    // --- PHƯƠNG THỨC MỚI ĐỂ TÍNH DOANH THU ---
    /**
     * Tính tổng doanh thu từ các vé đã bán trong một khoảng thời gian nhất định.
     * Doanh thu được tính dựa trên cột `price_paid` của bảng `Ticket`
     * và `booking_time` của vé.
     *
     * @param startDate Ngày bắt đầu (bao gồm).
     * @param endDate   Ngày kết thúc (KHÔNG bao gồm - tức là lấy đến hết ngày trước endDate).
     *                  Để bao gồm cả ngày endDate, khi truyền vào, bạn có thể cộng thêm 1 ngày cho endDate.
     * @return Tổng doanh thu dưới dạng BigDecimal, hoặc BigDecimal.ZERO nếu không có doanh thu/có lỗi.
     */
    public BigDecimal calculateTotalRevenue(Date startDate, Date endDate) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        // Câu lệnh SQL tính tổng price_paid từ bảng Ticket
        // Dựa trên booking_time.
        // Lưu ý: Xử lý khoảng thời gian với DATETIME/TIMESTAMP cần cẩn thận.
        // Ví dụ: Nếu startDate là '2023-01-01' và endDate là '2023-01-03',
        // chúng ta muốn lấy các vé đặt từ '2023-01-01 00:00:00' đến '2023-01-02 23:59:59'.
        // Hoặc, điều kiện là: booking_time >= startDate AND booking_time < (endDate được cộng thêm 1 ngày)
        String sql = "SELECT SUM(price_paid) AS total_revenue FROM Ticket WHERE booking_time >= ? AND booking_time < ?";

        System.out.println("TicketDAO.calculateTotalRevenue: Calculating revenue from " + startDate + " to " + endDate); // DEBUG

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Chuyển đổi java.util.Date sang java.sql.Timestamp
            pstmt.setTimestamp(1, new java.sql.Timestamp(startDate.getTime()));

            // Để bao gồm tất cả các vé trong ngày endDate, chúng ta cần đặt giới hạn trên là bắt đầu ngày tiếp theo
            Calendar c = Calendar.getInstance();
            c.setTime(endDate);
            c.add(Calendar.DATE, 1); // Thêm 1 ngày vào endDate
            c.set(Calendar.HOUR_OF_DAY, 0); // Đặt giờ, phút, giây, ms về 0 của ngày tiếp theo
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Date adjustedEndDate = c.getTime();
            pstmt.setTimestamp(2, new java.sql.Timestamp(adjustedEndDate.getTime()));
            System.out.println("TicketDAO.calculateTotalRevenue: Adjusted endDate for query: " + adjustedEndDate); // DEBUG


            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal revenue = rs.getBigDecimal("total_revenue");
                    if (revenue != null) {
                        totalRevenue = revenue;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tính tổng doanh thu: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("TicketDAO.calculateTotalRevenue: Calculated total revenue: " + totalRevenue.toPlainString()); // DEBUG
        return totalRevenue;
    }
    // --- KẾT THÚC PHƯƠNG THỨC MỚI ---
}