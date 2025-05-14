package com.cinemamanagement.ui;

import com.cinemamanagement.dao.*;
import com.cinemamanagement.model.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class BookTicketPanel extends JPanel {
    private JComboBox<Movie> movieComboBox;
    private JComboBox<ShowtimeDisplay> showtimeComboBox;
    private JPanel seatGridPanel; // Panel để chứa các nút ghế
    private JButton bookButton;
    private JLabel selectedSeatLabel; // Hiển thị ghế đang chọn

    private MovieDAO movieDAO;
    private ShowtimeDAO showtimeDAO;
    private RoomDAO roomDAO;
    private SeatDAO seatDAO;
    private TicketDAO ticketDAO;

    private List<JToggleButton> seatToggleButtons; // Danh sách các nút ghế
    private Seat currentlySelectedSeatObject; // Đối tượng Seat đang được chọn
    private Showtime currentShowtimeObject; // Suất chiếu hiện tại đang được xem ghế

    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Lớp nội bộ để hiển thị thông tin Showtime trong JComboBox dễ đọc hơn
    private static class ShowtimeDisplay {
        Showtime showtime;
        Room room; // Giữ thông tin phòng để hiển thị
        Movie movie; // Giữ thông tin phim để hiển thị (tùy chọn)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        public ShowtimeDisplay(Showtime showtime, Room room, Movie movie) {
            this.showtime = showtime;
            this.room = room;
            this.movie = movie; // Không bắt buộc dùng trong toString nhưng có thể hữu ích
        }

        @Override
        public String toString() {
            if (showtime == null || room == null) {
                return "--- Chọn Suất Chiếu ---";
            }
            return "Phòng: " + room.getName() + " - Giờ: " + sdf.format(showtime.getShowTime());
        }
    }

    public BookTicketPanel() {
        movieDAO = new MovieDAO();
        showtimeDAO = new ShowtimeDAO();
        roomDAO = new RoomDAO();
        seatDAO = new SeatDAO();
        ticketDAO = new TicketDAO();
        seatToggleButtons = new ArrayList<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initComponents();
        loadMoviesForComboBox();
    }

    private void initComponents() {
        // Panel chọn phim và suất chiếu (NORTH)
        JPanel selectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; selectionPanel.add(new JLabel("Chọn Phim:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        movieComboBox = new JComboBox<>();
        selectionPanel.add(movieComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; selectionPanel.add(new JLabel("Chọn Suất Chiếu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        showtimeComboBox = new JComboBox<>();
        selectionPanel.add(showtimeComboBox, gbc);
        
        add(selectionPanel, BorderLayout.NORTH);

        // Panel hiển thị sơ đồ ghế (CENTER)
        JPanel seatSelectionMainPanel = new JPanel(new BorderLayout(5,10));
        seatSelectionMainPanel.setBorder(BorderFactory.createTitledBorder(
                null, "Chọn Ghế", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), Color.BLUE
        ));
        
        // Khu vực màn hình (giả lập)
        JLabel screenLabel = new JLabel("MÀN HÌNH", SwingConstants.CENTER);
        screenLabel.setOpaque(true);
        screenLabel.setBackground(Color.DARK_GRAY);
        screenLabel.setForeground(Color.WHITE);
        screenLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        screenLabel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        seatSelectionMainPanel.add(screenLabel, BorderLayout.NORTH);

        seatGridPanel = new JPanel(); // Layout sẽ được set khi load ghế
        JScrollPane seatScrollPane = new JScrollPane(seatGridPanel);
        seatScrollPane.setPreferredSize(new Dimension(400, 300)); // Kích thước gợi ý
        seatSelectionMainPanel.add(seatScrollPane, BorderLayout.CENTER);
        
        // Chú thích ghế
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.add(createLegendItem("Còn Trống", Color.GREEN.darker()));
        legendPanel.add(createLegendItem("Đã Đặt", Color.RED.darker()));
        legendPanel.add(createLegendItem("Đang Chọn", Color.ORANGE));
        seatSelectionMainPanel.add(legendPanel, BorderLayout.SOUTH);

        add(seatSelectionMainPanel, BorderLayout.CENTER);


        // Panel thông tin đặt vé và nút đặt (SOUTH)
        JPanel bookingActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        selectedSeatLabel = new JLabel("Ghế đang chọn: Chưa có");
        selectedSeatLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bookingActionsPanel.add(selectedSeatLabel);

        bookButton = new JButton("Đặt Vé Ngay");
        bookButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bookButton.setEnabled(false); // Ban đầu không cho đặt khi chưa chọn ghế
        bookingActionsPanel.add(bookButton);
        
        add(bookingActionsPanel, BorderLayout.SOUTH);


        // Action Listeners
        movieComboBox.addActionListener(e -> {
            Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();
            if (selectedMovie != null && selectedMovie.getId() != 0) { // Bỏ qua item placeholder
                loadShowtimesForMovie(selectedMovie);
            } else {
                showtimeComboBox.removeAllItems();
                showtimeComboBox.addItem(new ShowtimeDisplay(null, null, null)); // Placeholder
                clearSeatPanelAndSelection();
            }
        });

        showtimeComboBox.addActionListener(e -> {
            ShowtimeDisplay selectedShowtimeDisplay = (ShowtimeDisplay) showtimeComboBox.getSelectedItem();
            if (selectedShowtimeDisplay != null && selectedShowtimeDisplay.showtime != null) {
                currentShowtimeObject = selectedShowtimeDisplay.showtime;
                loadSeatsForShowtime(currentShowtimeObject);
            } else {
                currentShowtimeObject = null;
                clearSeatPanelAndSelection();
            }
        });
        
        bookButton.addActionListener(e -> handleBookTicket());
    }
    
    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel("<html><font color='" + String.format("#%06x", color.getRGB() & 0xFFFFFF) + "'>■</font> " + text + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    private void loadMoviesForComboBox() {
        movieComboBox.removeAllItems();
        movieComboBox.addItem(new Movie("--- Chọn Phim ---", null, 0)); // Placeholder item
        List<Movie> movies = movieDAO.getAllMovies(); // Nên lấy phim có suất chiếu trong tương lai
        if (movies != null) {
            for (Movie movie : movies) {
                movieComboBox.addItem(movie);
            }
        }
    }

    private void loadShowtimesForMovie(Movie movie) {
        showtimeComboBox.removeAllItems();
        showtimeComboBox.addItem(new ShowtimeDisplay(null, null, null)); // Placeholder
        List<Showtime> showtimes = showtimeDAO.getShowtimesByMovie(movie.getId());
        if (showtimes != null) {
            for (Showtime st : showtimes) {
                Room room = roomDAO.getRoomById(st.getRoomId());
                showtimeComboBox.addItem(new ShowtimeDisplay(st, room, movie));
            }
        }
        clearSeatPanelAndSelection();
    }

    private void clearSeatPanelAndSelection() {
        seatGridPanel.removeAll();
        seatToggleButtons.clear();
        currentlySelectedSeatObject = null;
        selectedSeatLabel.setText("Ghế đang chọn: Chưa có");
        bookButton.setEnabled(false);
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
    }

    private void loadSeatsForShowtime(Showtime showtime) {
        clearSeatPanelAndSelection();
        if (showtime == null) return;

        Room room = roomDAO.getRoomById(showtime.getRoomId());
        if (room == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin phòng cho suất chiếu này.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Seat> allSeatsInRoom = seatDAO.getSeatsByRoom(room.getId());
        List<Seat> bookedSeatsForThisShowtime = ticketDAO.getBookedSeatsForShowtime(showtime.getId());
        List<Integer> bookedSeatIds = bookedSeatsForThisShowtime.stream().map(Seat::getId).collect(Collectors.toList());

        // Ước lượng số cột, ví dụ 10 cột, để tính số hàng
        int numCols = Math.min(10, room.getSeatCount()); // Không quá 10 cột, hoặc ít hơn nếu phòng nhỏ
        if (numCols == 0) numCols = 1; // Tránh chia cho 0
        int numRows = (int) Math.ceil((double) allSeatsInRoom.size() / numCols);

        seatGridPanel.setLayout(new GridLayout(numRows, numCols, 8, 8)); // Khoảng cách giữa các ghế

        ButtonGroup seatGroup = new ButtonGroup(); // Đảm bảo chỉ một ghế được chọn

        for (Seat seat : allSeatsInRoom) {
            JToggleButton seatButton = new JToggleButton(seat.getSeatNumber());
            seatButton.setPreferredSize(new Dimension(65, 45)); // Kích thước nút ghế
            seatButton.setFont(new Font("Arial", Font.BOLD, 12));
            seatToggleButtons.add(seatButton);
            seatGroup.add(seatButton); // Thêm vào group

            if (bookedSeatIds.contains(seat.getId())) {
                seatButton.setEnabled(false);
                seatButton.setBackground(Color.RED.darker());
                seatButton.setForeground(Color.WHITE);
                seatButton.setToolTipText("Ghế đã được đặt");
            } else {
                seatButton.setBackground(Color.GREEN.darker());
                seatButton.setForeground(Color.WHITE);
                seatButton.setToolTipText("Ghế còn trống");
                seatButton.addActionListener(e -> {
                    // Bỏ chọn ghế cũ nếu có
                    for (JToggleButton btn : seatToggleButtons) {
                        if (btn.isEnabled() && btn != seatButton) { // Chỉ reset màu ghế trống khác
                             btn.setBackground(Color.GREEN.darker());
                        }
                    }
                    
                    if (seatButton.isSelected()) {
                        currentlySelectedSeatObject = seat;
                        selectedSeatLabel.setText("Ghế đang chọn: " + seat.getSeatNumber());
                        bookButton.setEnabled(true);
                        seatButton.setBackground(Color.ORANGE); // Màu ghế đang chọn
                    } else { // Nếu người dùng bỏ chọn
                        currentlySelectedSeatObject = null;
                        selectedSeatLabel.setText("Ghế đang chọn: Chưa có");
                        bookButton.setEnabled(false);
                        seatButton.setBackground(Color.GREEN.darker()); // Trả lại màu ghế trống
                    }
                });
            }
            seatGridPanel.add(seatButton);
        }
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
    }
    
    private void handleBookTicket() {
        if (LoginFrame.currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để đặt vé.", "Chưa Đăng Nhập", JOptionPane.WARNING_MESSAGE);
            // Có thể mở lại LoginFrame ở đây nếu cần
            return;
        }

        if (currentShowtimeObject == null || currentlySelectedSeatObject == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ suất chiếu và ghế.", "Thiếu Thông Tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();
        ShowtimeDisplay selectedShowtimeDisplay = (ShowtimeDisplay) showtimeComboBox.getSelectedItem();
        Room selectedRoom = (selectedShowtimeDisplay != null && selectedShowtimeDisplay.room != null) ? selectedShowtimeDisplay.room : null;
        
        if (selectedMovie == null || selectedMovie.getId() == 0 || selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Thông tin phim hoặc phòng không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String confirmationMessage = String.format(
                "Xác nhận đặt vé:\n\nPhim: %s\nPhòng: %s\nSuất chiếu: %s\nGhế: %s\n\nBạn có chắc chắn muốn đặt vé này không?",
                selectedMovie.getTitle(),
                selectedRoom.getName(),
                sdfDateTime.format(currentShowtimeObject.getShowTime()),
                currentlySelectedSeatObject.getSeatNumber()
        );

        int confirm = JOptionPane.showConfirmDialog(this, confirmationMessage, "Xác Nhận Đặt Vé", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Ticket ticket = new Ticket();
            ticket.setShowtimeId(currentShowtimeObject.getId());
            ticket.setUserId(LoginFrame.currentUser.getId());
            ticket.setSeatId(currentlySelectedSeatObject.getId());
            ticket.setBookingTime(new Date()); // Thời gian đặt vé là hiện tại

            if (ticketDAO.addTicket(ticket)) {
                JOptionPane.showMessageDialog(this, "Đặt vé thành công cho ghế " + currentlySelectedSeatObject.getSeatNumber() + "!", "Đặt Vé Thành Công", JOptionPane.INFORMATION_MESSAGE);
                // Làm mới lại sơ đồ ghế để cập nhật ghế vừa đặt
                loadSeatsForShowtime(currentShowtimeObject);
                // Có thể tự động chuyển sang tab "Vé Của Tôi" hoặc làm mới nó
                // ((UserDashboardFrame) SwingUtilities.getWindowAncestor(this)).switchToMyTicketsTabAndRefresh();
            } else {
                JOptionPane.showMessageDialog(this, "Đặt vé thất bại. Ghế có thể đã được người khác đặt hoặc có lỗi xảy ra.", "Đặt Vé Thất Bại", JOptionPane.ERROR_MESSAGE);
                // Làm mới lại sơ đồ ghế để đảm bảo thông tin chính xác
                loadSeatsForShowtime(currentShowtimeObject);
            }
        }
    }
    
    /**
     * Phương thức này có thể được gọi từ UserDashboardFrame để làm mới dữ liệu khi tab được chọn.
     */
    public void refreshData() {
        loadMoviesForComboBox(); // Tải lại danh sách phim
        // showtimeComboBox và seatPanel sẽ tự động được làm mới khi phim được chọn
    }
}