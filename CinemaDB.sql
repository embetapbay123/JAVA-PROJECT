-- Tạo cơ sở dữ liệu nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS cinema_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Sử dụng cơ sở dữ liệu vừa tạo hoặc đã tồn tại
USE cinema_db;

-- 1. Bảng Người dùng (User)
-- Lưu trữ thông tin người dùng và vai trò của họ
CREATE TABLE IF NOT EXISTS User (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    username VARCHAR(50) NOT NULL UNIQUE,      -- Tên đăng nhập, không được null, duy nhất
    password VARCHAR(255) NOT NULL,            -- Mật khẩu 
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER' -- Vai trò: ADMIN hoặc USER, mặc định là USER
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng Phim (Movie)
-- Lưu trữ thông tin về các bộ phim
CREATE TABLE IF NOT EXISTS Movie (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    title VARCHAR(255) NOT NULL,               -- Tên phim, không được null
    genre VARCHAR(100),                        -- Thể loại phim
    duration INT,                              -- Thời lượng phim (tính bằng phút)
    description TEXT,                          -- Mô tả chi tiết về phim (tùy chọn)
    release_date DATE,                         -- Ngày phát hành (tùy chọn)
    poster_url VARCHAR(255)                    -- Đường dẫn đến ảnh poster (tùy chọn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng Phòng chiếu (Room)
-- Lưu trữ thông tin về các phòng chiếu trong rạp
CREATE TABLE IF NOT EXISTS Room (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    name VARCHAR(50) NOT NULL UNIQUE,          -- Tên phòng chiếu (ví dụ: Room 1, IMAX), duy nhất
    seat_count INT NOT NULL                    -- Tổng số ghế trong phòng
    -- (Có thể thêm các thuộc tính khác như loại phòng, công nghệ chiếu, v.v.)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng Ghế (Seat)
-- Lưu trữ thông tin chi tiết về từng ghế trong mỗi phòng chiếu
-- Bảng này giúp quản lý trạng thái của từng ghế (đã đặt, còn trống) cho một suất chiếu cụ thể
-- và cũng để tạo sơ đồ ghế.
CREATE TABLE IF NOT EXISTS Seat (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    room_id INT NOT NULL,                      -- Khóa ngoại, liên kết đến bảng Room
    seat_number VARCHAR(10) NOT NULL,          -- Số ghế (ví dụ: A1, B5, VIP1)
    -- (Có thể thêm loại ghế: 'NORMAL', 'VIP', v.v.)
    CONSTRAINT fk_seat_room FOREIGN KEY (room_id) REFERENCES Room(id) ON DELETE CASCADE ON UPDATE CASCADE, -- Ràng buộc khóa ngoại
    UNIQUE KEY unique_seat_in_room (room_id, seat_number) -- Đảm bảo mỗi số ghế là duy nhất trong một phòng
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng Suất chiếu (Showtime)
-- Liên kết một bộ phim với một phòng chiếu tại một thời điểm cụ thể
CREATE TABLE IF NOT EXISTS Showtime (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    movie_id INT NOT NULL,                     -- Khóa ngoại, liên kết đến bảng Movie
    room_id INT NOT NULL,                      -- Khóa ngoại, liên kết đến bảng Room
    show_time DATETIME NOT NULL,               -- Thời gian bắt đầu suất chiếu
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES Movie(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_showtime_room FOREIGN KEY (room_id) REFERENCES Room(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng Vé (Ticket)
-- Lưu trữ thông tin về các vé đã được đặt bởi người dùng
CREATE TABLE IF NOT EXISTS Ticket (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- ID tự tăng, khóa chính
    showtime_id INT NOT NULL,                  -- Khóa ngoại, liên kết đến bảng Showtime
    user_id INT NOT NULL,                      -- Khóa ngoại, liên kết đến bảng User (người đặt vé)
    seat_id INT NOT NULL,                      -- Khóa ngoại, liên kết đến bảng Seat (ghế được đặt)
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Thời gian đặt vé, mặc định là thời điểm hiện tại
    price_paid DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_ticket_showtime FOREIGN KEY (showtime_id) REFERENCES Showtime(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES User(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_seat FOREIGN KEY (seat_id) REFERENCES Seat(id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY unique_ticket_for_showtime_seat (showtime_id, seat_id) -- Đảm bảo mỗi ghế trong một suất chiếu chỉ được đặt 1 lần
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu mẫu (Tùy chọn, nhưng hữu ích cho việc kiểm thử ban đầu)

-- Tạo tài khoản admin mặc định (mật khẩu 'admin123' - CẦN HASH TRONG THỰC TẾ)
INSERT INTO User (username, password, role) VALUES ('admin', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE password='admin123', role='ADMIN'; -- Nếu user 'admin' đã tồn tại thì cập nhật

-- Tạo tài khoản user mẫu (mật khẩu 'user123' - CẦN HASH TRONG THỰC TẾ)
INSERT INTO User (username, password, role) VALUES ('user', 'user123', 'USER')
ON DUPLICATE KEY UPDATE password='user123', role='USER';

-- Thêm một vài bộ phim mẫu
INSERT INTO Movie (title, genre, duration, description, release_date) VALUES
('Avengers: Endgame', 'Action, Sci-Fi', 181, 'The Avengers assemble once more in order to reverse Thanos'' actions and restore balance to the universe.', '2019-04-26'),
('Joker', 'Drama, Thriller', 122, 'In Gotham City, mentally troubled comedian Arthur Fleck is disregarded and mistreated by society.', '2019-10-04'),
('Spider-Man: No Way Home', 'Action, Adventure', 148, 'With Spider-Man''s identity now revealed, Peter asks Doctor Strange for help.', '2021-12-17')
ON DUPLICATE KEY UPDATE genre=VALUES(genre), duration=VALUES(duration); -- Cập nhật nếu phim đã tồn tại dựa trên title (cần thêm UNIQUE constraint cho title nếu muốn làm vậy)

-- Không thêm phòng mẫu nữa vì lỗi, tự thêm trong app ^^

-- Kết thúc script