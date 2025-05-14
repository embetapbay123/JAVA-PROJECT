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
    -- (Có thể thêm giá vé cho suất chiếu này nếu giá khác nhau)
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
    -- (Có thể thêm giá vé đã thanh toán, trạng thái vé, v.v.)
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

-- Thêm một vài phòng chiếu mẫu
INSERT INTO Room (name, seat_count) VALUES
('Room 1', 50),
('Room 2', 30),
('VIP Room', 20)
ON DUPLICATE KEY UPDATE seat_count=VALUES(seat_count);

-- Thêm ghế cho Phòng 1 (id=1, giả sử) - Bạn sẽ cần code để tạo ghế tự động khi thêm phòng
-- Ví dụ này chỉ tạo một vài ghế cho Room 1
-- INSERT INTO Seat (room_id, seat_number) VALUES (1, 'A1'), (1, 'A2'), (1, 'A3'), (1, 'A4'), (1, 'A5');
-- INSERT INTO Seat (room_id, seat_number) VALUES (1, 'B1'), (1, 'B2'), (1, 'B3'), (1, 'B4'), (1, 'B5');
-- (Việc insert ghế thủ công như này không hiệu quả. Nên có logic trong ứng dụng để tạo ghế khi tạo phòng)

-- Thêm một vài suất chiếu mẫu (thời gian nên ở tương lai để có thể đặt vé)
-- Giả sử Movie ID 1 là Avengers, Room ID 1 là Room 1
-- Lưu ý: Chỉnh sửa ngày giờ cho phù hợp với thời điểm bạn chạy script này để chúng là các suất chiếu trong tương lai
-- INSERT INTO Showtime (movie_id, room_id, show_time) VALUES
-- (1, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:00:00')), -- Avengers, Room 1, 10 AM ngày mai
-- (1, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 13:00:00')), -- Avengers, Room 1, 1 PM ngày mai
-- (2, 2, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 11:00:00')); -- Joker, Room 2, 11 AM ngày mai

-- (Không thêm vé mẫu vì vé được tạo khi người dùng đặt)

-- Kết thúc script