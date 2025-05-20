package com.cinemamanagement.model;

public class Room {
    private int id;
    private String name;
    private int seatCount;

    // Constructors
    public Room() {
    }

    public Room(int id, String name, int seatCount) {
        this.id = id;
        this.name = name;
        this.seatCount = seatCount;
    }

    public Room(String name, int seatCount) {
        this.name = name;
        this.seatCount = seatCount;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    @Override
    public String toString() {
        // Quan trọng cho JComboBox hiển thị tên phòng
        return name != null ? name : "N/A";
    }

	public int getTotalSeats() {
		// TODO Auto-generated method stub
		return 0;
	}
}