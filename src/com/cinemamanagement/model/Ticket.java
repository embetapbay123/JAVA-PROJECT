package com.cinemamanagement.model;

import java.util.Date; // Sử dụng java.util.Date cho bookingTime (hoặc java.sql.Timestamp)

public class Ticket {
    private int id;
    private int showtimeId;
    private int userId;
    private int seatId;
    private Date bookingTime; // Thời gian đặt vé (java.util.Date map với TIMESTAMP trong SQL)

    // Constructors
    public Ticket() {
    }

    public Ticket(int id, int showtimeId, int userId, int seatId, Date bookingTime) {
        this.id = id;
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.seatId = seatId;
        this.bookingTime = bookingTime;
    }
    
    public Ticket(int showtimeId, int userId, int seatId) {
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.seatId = seatId;
        // bookingTime có thể được set bởi CSDL (DEFAULT CURRENT_TIMESTAMP) hoặc trong DAO
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public Date getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(Date bookingTime) {
        this.bookingTime = bookingTime;
    }

    @Override
    public String toString() {
        return "Ticket{" +
               "id=" + id +
               ", showtimeId=" + showtimeId +
               ", userId=" + userId +
               ", seatId=" + seatId +
               ", bookingTime=" + (bookingTime != null ? bookingTime.toString() : "N/A") +
               '}';
    }
}