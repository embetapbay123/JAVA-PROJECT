package com.cinemamanagement.dao;

import com.cinemamanagement.model.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    /**
     * Thêm một bộ phim mới vào CSDL.
     * @param movie Đối tượng Movie chứa thông tin phim.
     * @return true nếu thêm thành công, false nếu thất bại.
     */
    public boolean addMovie(Movie movie) {
        String sql = "INSERT INTO Movie (title, genre, duration, description, release_date, poster_url) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getGenre());
            pstmt.setInt(3, movie.getDuration());
            pstmt.setString(4, movie.getDescription());
            
            // Chuyển đổi java.util.Date sang java.sql.Date
            if (movie.getReleaseDate() != null) {
                pstmt.setDate(5, new java.sql.Date(movie.getReleaseDate().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            
            pstmt.setString(6, movie.getPosterUrl());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        movie.setId(generatedKeys.getInt(1)); // Gán ID được tạo cho đối tượng movie
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm phim: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy thông tin một bộ phim bằng ID.
     * @param movieId ID của phim.
     * @return Đối tượng Movie nếu tìm thấy, null nếu không.
     */
    public Movie getMovieById(int movieId) {
        String sql = "SELECT * FROM Movie WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMovie(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy phim bằng ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các bộ phim.
     * @return List các đối tượng Movie.
     */
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM Movie ORDER BY title ASC"; // Sắp xếp theo tên phim
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                movies.add(mapResultSetToMovie(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả phim: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }

    /**
     * Cập nhật thông tin một bộ phim.
     * @param movie Đối tượng Movie với thông tin đã cập nhật.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE Movie SET title = ?, genre = ?, duration = ?, description = ?, release_date = ?, poster_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getGenre());
            pstmt.setInt(3, movie.getDuration());
            pstmt.setString(4, movie.getDescription());
            
            if (movie.getReleaseDate() != null) {
                pstmt.setDate(5, new java.sql.Date(movie.getReleaseDate().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            
            pstmt.setString(6, movie.getPosterUrl());
            pstmt.setInt(7, movie.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật phim: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xóa một bộ phim khỏi CSDL.
     * @param movieId ID của phim cần xóa.
     * @return true nếu xóa thành công, false nếu thất bại.
     */
    public boolean deleteMovie(int movieId) {
        String sql = "DELETE FROM Movie WHERE id = ?";
        // CẢNH BÁO: Xóa phim sẽ kích hoạt ON DELETE CASCADE, có thể xóa các Showtime và Ticket liên quan.
        // Cân nhắc việc kiểm tra xem phim có suất chiếu nào không trước khi xóa,
        // hoặc đánh dấu phim là "không hoạt động" thay vì xóa hẳn.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa phim: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tìm kiếm phim theo tên (không phân biệt hoa thường).
     * @param titleSearchTerm Từ khóa tìm kiếm tên phim.
     * @return List các Movie khớp.
     */
    public List<Movie> searchMoviesByTitle(String titleSearchTerm) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM Movie WHERE LOWER(title) LIKE LOWER(?) ORDER BY title ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + titleSearchTerm + "%"); // Tìm kiếm chứa từ khóa
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm phim theo tên: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }

    /**
     * Tìm kiếm phim theo thể loại (không phân biệt hoa thường).
     * @param genreSearchTerm Từ khóa tìm kiếm thể loại.
     * @return List các Movie khớp.
     */
    public List<Movie> searchMoviesByGenre(String genreSearchTerm) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM Movie WHERE LOWER(genre) LIKE LOWER(?) ORDER BY title ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + genreSearchTerm + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm phim theo thể loại: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }
    
    /**
     * Helper method để map một dòng ResultSet thành đối tượng Movie.
     * @param rs ResultSet đang trỏ đến một dòng dữ liệu phim.
     * @return Đối tượng Movie.
     * @throws SQLException Nếu có lỗi khi truy cập ResultSet.
     */
    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        return new Movie(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("genre"),
                rs.getInt("duration"),
                rs.getString("description"),
                rs.getDate("release_date"), // Trả về java.sql.Date, có thể gán cho java.util.Date
                rs.getString("poster_url")
        );
    }
}