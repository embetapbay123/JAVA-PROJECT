package com.cinemamanagement.model;

import java.util.Date;
import java.math.BigDecimal; // Sử dụng BigDecimal
import java.math.RoundingMode;

public class Ticket {
    private int id;
    private int showtimeId;
    private int userId;
    private int seatId;
    private BigDecimal pricePaid; // Giá đã trả cho vé này
    private Date bookingTime;

    public Ticket() {
        this.pricePaid = BigDecimal.ZERO;
    }

    public Ticket(int id, int showtimeId, int userId, int seatId, BigDecimal pricePaid, Date bookingTime) {
        this.id = id;
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.seatId = seatId;
        this.pricePaid = (pricePaid == null) ? BigDecimal.ZERO : pricePaid.setScale(2, RoundingMode.HALF_UP);
        this.bookingTime = bookingTime;
    }

    // Constructor không có id và bookingTime (sẽ được set tự động)
    public Ticket(int showtimeId, int userId, int seatId, BigDecimal pricePaid) {
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.seatId = seatId;
        this.pricePaid = (pricePaid == null) ? BigDecimal.ZERO : pricePaid.setScale(2, RoundingMode.HALF_UP);
    }

    // Getters
    public int getId() { return id; }
    public int getShowtimeId() { return showtimeId; }
    public int getUserId() { return userId; }
    public int getSeatId() { return seatId; }
    public BigDecimal getPricePaid() { return pricePaid; }
    public Date getBookingTime() { return bookingTime; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public void setPricePaid(BigDecimal pricePaid) {
        this.pricePaid = (pricePaid == null) ? BigDecimal.ZERO : pricePaid.setScale(2, RoundingMode.HALF_UP);
    }
    public void setBookingTime(Date bookingTime) { this.bookingTime = bookingTime; }

    @Override
    public String toString() {
        return "Ticket{" +
               "id=" + id +
               ", showtimeId=" + showtimeId +
               ", userId=" + userId +
               ", seatId=" + seatId +
               ", pricePaid=" + (pricePaid != null ? pricePaid.toPlainString() : "0.00") +
               ", bookingTime=" + (bookingTime != null ? bookingTime.toString() : "N/A") +
               '}';
    }
}