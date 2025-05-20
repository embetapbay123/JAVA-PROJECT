package com.cinemamanagement.model;

import java.util.Date;
import java.math.BigDecimal; // Sử dụng BigDecimal cho tiền tệ
import java.math.RoundingMode;

public class Showtime {
    private int id;
    private int movieId;
    private int roomId;
    private Date showTime;
    private BigDecimal price; // Giá vé cho suất chiếu này

    public Showtime() {
        this.price = BigDecimal.ZERO; // Giá trị mặc định
    }

    public Showtime(int id, int movieId, int roomId, Date showTime, BigDecimal price) {
        this.id = id;
        this.movieId = movieId;
        this.roomId = roomId;
        this.showTime = showTime;
        this.price = (price == null) ? BigDecimal.ZERO : price.setScale(2, RoundingMode.HALF_UP); // Làm tròn 2 chữ số
    }

    public Showtime(int movieId, int roomId, Date showTime, BigDecimal price) {
        this.movieId = movieId;
        this.roomId = roomId;
        this.showTime = showTime;
        this.price = (price == null) ? BigDecimal.ZERO : price.setScale(2, RoundingMode.HALF_UP);
    }

    // Getters
    public int getId() { return id; }
    public int getMovieId() { return movieId; }
    public int getRoomId() { return roomId; }
    public Date getShowTime() { return showTime; }
    public BigDecimal getPrice() { return price; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setShowTime(Date showTime) { this.showTime = showTime; }
    public void setPrice(BigDecimal price) {
        this.price = (price == null) ? BigDecimal.ZERO : price.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Showtime{" +
               "id=" + id +
               ", movieId=" + movieId +
               ", roomId=" + roomId +
               ", showTime=" + (showTime != null ? showTime.toString() : "N/A") +
               ", price=" + (price != null ? price.toPlainString() : "0.00") +
               '}';
    }
}