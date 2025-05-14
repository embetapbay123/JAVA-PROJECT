package com.cinemamanagement.model;

public class Seat {
    private int id;
    private int roomId;         // ID của phòng chứa ghế này
    private String seatNumber;  // Ví dụ: A1, B5

    // Constructors
    public Seat() {
    }

    public Seat(int id, int roomId, String seatNumber) {
        this.id = id;
        this.roomId = roomId;
        this.seatNumber = seatNumber;
    }

    public Seat(int roomId, String seatNumber) {
        this.roomId = roomId;
        this.seatNumber = seatNumber;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Override
    public String toString() {
        return "Seat{" +
               "id=" + id +
               ", roomId=" + roomId +
               ", seatNumber='" + seatNumber + '\'' +
               '}';
    }
}