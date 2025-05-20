package com.cinemamanagement.model;

import java.util.Date; // Sử dụng java.util.Date cho showTime

public class Showtime {
    private int id;
    private int movieId;
    private int roomId;
    private Date showTime; // Thời gian bắt đầu suất chiếu (java.util.Date map với DATETIME trong SQL)

    // Constructors
    public Showtime() {
    }

    public Showtime(int id, int movieId, int roomId, Date showTime) {
        this.id = id;
        this.movieId = movieId;
        this.roomId = roomId;
        this.showTime = showTime;
    }
    
    public Showtime(int movieId, int roomId, Date showTime) {
        this.movieId = movieId;
        this.roomId = roomId;
        this.showTime = showTime;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public Date getShowTime() {
        return showTime;
    }

    public void setShowTime(Date showTime) {
        this.showTime = showTime;
    }

    @Override
    public String toString() {
        return "Showtime{" +
               "id=" + id +
               ", movieId=" + movieId +
               ", roomId=" + roomId +
               ", showTime=" + (showTime != null ? showTime.toString() : "N/A") + // Tránh NullPointerException
               '}';
    }
}