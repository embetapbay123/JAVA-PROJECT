package com.cinemamanagement.model;

import java.util.Date; // Sử dụng java.util.Date cho release_date

public class Movie {
    private int id;
    private String title;
    private String genre;
    private int duration;         // Thời lượng tính bằng phút
    private String description;
    private Date releaseDate;     // Ngày phát hành
    private String posterUrl;     // URL của ảnh poster

    // Constructors
    public Movie() {
    }

    public Movie(int id, String title, String genre, int duration, String description, Date releaseDate, String posterUrl) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.description = description;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
    }
    
    // Constructor tiện lợi hơn khi chỉ cần các trường cơ bản (tùy nhu cầu)
    public Movie(String title, String genre, int duration) {
        this.title = title;
        this.genre = genre;
        this.duration = duration;
    }


    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    @Override
    public String toString() {
        // Quan trọng cho JComboBox hiển thị tên phim
        return title != null ? title : "N/A";
    }
}