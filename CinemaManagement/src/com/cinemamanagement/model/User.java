package com.cinemamanagement.model;

public class User {
    private int id;
    private String username;
    private String password; // Trong ứng dụng thực tế, trường này sẽ lưu trữ mật khẩu đã được hash
    private Role role;

    // Enum để định nghĩa vai trò người dùng
    public enum Role {
        ADMIN,
        USER
    }

    // Constructors
    public User() {
    }

    public User(int id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Constructor không có id (dùng khi tạo mới user, id sẽ do CSDL tự tăng)
    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", username='" + username + '\'' +
               // Không nên log hoặc hiển thị password
               ", role=" + role +
               '}';
    }
}