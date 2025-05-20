// com.cinemamanagement.ui.BookTicketPanel.java
package com.cinemamanagement.ui;

import com.cinemamanagement.dao.*;
import com.cinemamanagement.model.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
// Bỏ ActionEvent và ActionListener không dùng trực tiếp ở đây nếu đã dùng lambda
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.math.BigDecimal; // QUAN TRỌNG
import java.text.NumberFormat; // QUAN TRỌNG
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale; // QUAN TRỌNG
// import java.util.Vector; // Không cần thiết nếu dùng List cho ComboBox Model
import java.util.stream.Collectors;

public class BookTicketPanel extends JPanel {
    private JComboBox<Movie> movieComboBox;
    private JComboBox<ShowtimeDisplay> showtimeComboBox;
    private JPanel seatGridPanel;
    private JButton bookButton;
    private JLabel selectedSeatLabel;
    private JLabel showtimePriceLabel; // Thêm để hiển thị giá

    private MovieDAO movieDAO;
    private ShowtimeDAO showtimeDAO;
    private RoomDAO roomDAO;
    private SeatDAO seatDAO;
    private TicketDAO ticketDAO;

    private List<JToggleButton> seatToggleButtons;
    private Seat currentlySelectedSeatObject;
    private Showtime currentShowtimeObject;
    private BigDecimal currentShowtimePrice = BigDecimal.ZERO; // Lưu giá hiện tại

    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")); // Định dạng VNĐ

    // Placeholders
    private final Movie MOVIE_PLACEHOLDER;
    private final ShowtimeDisplay SHOWTIME_PLACEHOLDER;


    // Lớp nội bộ để hiển thị thông tin Showtime trong JComboBox
    private static class ShowtimeDisplay {
        Showtime showtime;
        Room room;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));


        public ShowtimeDisplay(Showtime showtime, Room room) {
            this.showtime = showtime;
            this.room = room;
        }

        @Override
        public String toString() {
            if (showtime == null || room == null) {
                return "--- Chọn Suất Chiếu ---";
            }
            // Hiển thị thêm giá vé
            return String.format("Phòng: %s - Giờ: %s - Giá: %s",
                                 room.getName(),
                                 sdf.format(showtime.getShowTime()),
                                 currencyFormat.format(showtime.getPrice() != null ? showtime.getPrice() : BigDecimal.ZERO) );
        }
    }

    public BookTicketPanel() {
        // Khởi tạo placeholders
        MOVIE_PLACEHOLDER = new Movie("--- Chọn Phim ---", "", 0); // Đảm bảo constructor này tồn tại trong Movie.java
        // Placeholder cho ShowtimeDisplay, showtime và room bên trong nó có thể là null
        SHOWTIME_PLACEHOLDER = new ShowtimeDisplay(null, null);


        movieDAO = new MovieDAO();
        showtimeDAO = new ShowtimeDAO();
        roomDAO = new RoomDAO();
        seatDAO = new SeatDAO();
        ticketDAO = new TicketDAO();
        seatToggleButtons = new ArrayList<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initComponents();
        refreshMovieComboBox(); // Tải phim lần đầu
    }

    private void initComponents() {
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
        
        // Thêm label hiển thị giá vé
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; selectionPanel.add(new JLabel("Giá Vé Suất Chiếu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        showtimePriceLabel = new JLabel(currencyFormatter.format(0)); // Giá trị mặc định
        showtimePriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        showtimePriceLabel.setForeground(Color.BLUE);
        selectionPanel.add(showtimePriceLabel, gbc);

        add(selectionPanel, BorderLayout.NORTH);

        JPanel seatSelectionMainPanel = new JPanel(new BorderLayout(5,10));
        seatSelectionMainPanel.setBorder(BorderFactory.createTitledBorder(
                null, "Chọn Ghế", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), Color.BLUE
        ));
        
        JLabel screenLabel = new JLabel("MÀN HÌNH", SwingConstants.CENTER);
        screenLabel.setOpaque(true);
        screenLabel.setBackground(Color.DARK_GRAY);
        screenLabel.setForeground(Color.WHITE);
        screenLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        screenLabel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        seatSelectionMainPanel.add(screenLabel, BorderLayout.NORTH);

        seatGridPanel = new JPanel();
        JScrollPane seatScrollPane = new JScrollPane(seatGridPanel);
        seatScrollPane.setPreferredSize(new Dimension(500, 350)); // Tăng kích thước
        seatSelectionMainPanel.add(seatScrollPane, BorderLayout.CENTER);
        
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.add(createLegendItem("Còn Trống", Color.GREEN.darker()));
        legendPanel.add(createLegendItem("Đã Đặt", Color.RED.darker()));
        legendPanel.add(createLegendItem("Đang Chọn", Color.ORANGE));
        seatSelectionMainPanel.add(legendPanel, BorderLayout.SOUTH);
        add(seatSelectionMainPanel, BorderLayout.CENTER);

        JPanel bookingActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        selectedSeatLabel = new JLabel("Ghế đang chọn: Chưa có");
        selectedSeatLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bookingActionsPanel.add(selectedSeatLabel);

        bookButton = new JButton("Đặt Vé Ngay"); // Sửa lại thành JButton nếu không dùng RounedButton
        // bookButton = new com.cinemamanagement.utils.RounedButton("Đặt Vé Ngay", new Color(76,175,80)); // Nếu dùng
        bookButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bookButton.setEnabled(false);
        bookingActionsPanel.add(bookButton);
        add(bookingActionsPanel, BorderLayout.SOUTH);

        movieComboBox.addActionListener(e -> {
            if (movieComboBox.getSelectedItem() instanceof Movie) {
                Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();
                if (selectedMovie.getId() != MOVIE_PLACEHOLDER.getId()) { // Không phải placeholder
                    loadShowtimesForMovie(selectedMovie);
                } else { // Placeholder được chọn
                    showtimeComboBox.removeAllItems();
                    showtimeComboBox.addItem(SHOWTIME_PLACEHOLDER);
                    showtimePriceLabel.setText(currencyFormatter.format(0));
                    currentShowtimePrice = BigDecimal.ZERO;
                    clearSeatPanelAndSelection();
                }
            }
        });

        showtimeComboBox.addActionListener(e -> {
            if (showtimeComboBox.getSelectedItem() instanceof ShowtimeDisplay) {
                ShowtimeDisplay selectedShowtimeDisplay = (ShowtimeDisplay) showtimeComboBox.getSelectedItem();
                if (selectedShowtimeDisplay.showtime != null) { // Không phải placeholder
                    currentShowtimeObject = selectedShowtimeDisplay.showtime;
                    currentShowtimePrice = currentShowtimeObject.getPrice() != null ? currentShowtimeObject.getPrice() : BigDecimal.ZERO;
                    showtimePriceLabel.setText(currencyFormatter.format(currentShowtimePrice));
                    loadSeatsForShowtime(currentShowtimeObject);
                } else {
                    currentShowtimeObject = null;
                    currentShowtimePrice = BigDecimal.ZERO;
                    showtimePriceLabel.setText(currencyFormatter.format(0));
                    clearSeatPanelAndSelection();
                }
            }
        });
        bookButton.addActionListener(e -> handleBookTicket());
    }
    
    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel("<html><font color='" + String.format("#%06x", color.getRGB() & 0xFFFFFF) + "'>■</font> " + text + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    public void refreshMovieComboBox() {
        System.out.println("BookTicketPanel: Refreshing movie combo box...");
        Movie previouslySelectedMovie = null;
        if(movieComboBox.getSelectedItem() instanceof Movie && ((Movie)movieComboBox.getSelectedItem()).getId() != MOVIE_PLACEHOLDER.getId()){
            previouslySelectedMovie = (Movie) movieComboBox.getSelectedItem();
        }

        movieComboBox.removeAllItems();
        movieComboBox.addItem(MOVIE_PLACEHOLDER); // Thêm placeholder object

        List<Movie> movies = movieDAO.getAllMovies();
        if (movies != null) {
            for (Movie movie : movies) {
                movieComboBox.addItem(movie);
            }
        }

        boolean reSelected = false;
        if (previouslySelectedMovie != null) {
            for (int i = 0; i < movieComboBox.getItemCount(); i++) {
                 if (movieComboBox.getItemAt(i).getId() == previouslySelectedMovie.getId()) { // So sánh ID
                    movieComboBox.setSelectedIndex(i); 
                    reSelected = true;
                    break;
                }
            }
        }
        if (!reSelected) { // Nếu không chọn lại được phim cũ
            movieComboBox.setSelectedItem(MOVIE_PLACEHOLDER); // Chọn placeholder
            // Khi placeholder phim được chọn, các lựa chọn phụ thuộc cũng nên reset
            showtimeComboBox.removeAllItems();
            showtimeComboBox.addItem(SHOWTIME_PLACEHOLDER);
            showtimePriceLabel.setText(currencyFormatter.format(0));
            currentShowtimePrice = BigDecimal.ZERO;
            clearSeatPanelAndSelection();
        }
        System.out.println("BookTicketPanel: Movie combo box refreshed.");
    }

    private void loadShowtimesForMovie(Movie movie) {
        System.out.println("BookTicketPanel: Loading showtimes for movie: " + movie.getTitle());
        showtimeComboBox.removeAllItems();
        showtimeComboBox.addItem(SHOWTIME_PLACEHOLDER); // Thêm placeholder

        List<Showtime> showtimes = showtimeDAO.getShowtimesByMovie(movie.getId());
        if (showtimes != null && !showtimes.isEmpty()) {
            for (Showtime st : showtimes) {
                Room room = roomDAO.getRoomById(st.getRoomId());
                if (room != null) {
                    showtimeComboBox.addItem(new ShowtimeDisplay(st, room));
                } else {
                     System.err.println("BookTicketPanel: Room not found for showtime ID " + st.getId() + ", room ID " + st.getRoomId());
                }
            }
        } else {
             System.out.println("BookTicketPanel: No showtimes found for movie: " + movie.getTitle());
        }
        // Reset giá và ghế khi danh sách suất chiếu thay đổi
        showtimePriceLabel.setText(currencyFormatter.format(0));
        currentShowtimePrice = BigDecimal.ZERO;
        clearSeatPanelAndSelection();

        // Tự động chọn suất chiếu đầu tiên (nếu có và không phải placeholder)
        if (showtimeComboBox.getItemCount() > 1) { // Có item khác ngoài placeholder
            showtimeComboBox.setSelectedIndex(1);
        } else {
            showtimeComboBox.setSelectedItem(SHOWTIME_PLACEHOLDER);
        }
    }

    private void clearSeatPanelAndSelection() {
        System.out.println("BookTicketPanel: Clearing seat panel and selection.");
        seatGridPanel.removeAll();
        seatToggleButtons.clear();
        currentlySelectedSeatObject = null;
        selectedSeatLabel.setText("Ghế đang chọn: Chưa có");
        bookButton.setEnabled(false);
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
    }

    private void loadSeatsForShowtime(Showtime showtime) {
        System.out.println("BookTicketPanel: Attempting to load seats for showtime ID " + (showtime != null ? showtime.getId() : "null"));
        clearSeatPanelAndSelection();
        if (showtime == null) {
            System.out.println("BookTicketPanel: Showtime is null, cannot load seats.");
            return;
        }

        Room room = roomDAO.getRoomById(showtime.getRoomId());
        if (room == null) {
            JOptionPane.showMessageDialog(this, "Lỗi: Không tìm thấy thông tin phòng (ID: " + showtime.getRoomId() + ").", "Lỗi Phòng", JOptionPane.ERROR_MESSAGE);
            System.err.println("BookTicketPanel: Room not found for room ID " + showtime.getRoomId());
            return;
        }
        System.out.println("BookTicketPanel: Room found: " + room.getName() + ", Seat Count in DB: " + room.getSeatCount());

        List<Seat> allSeatsInRoom = seatDAO.getSeatsByRoom(room.getId());
        System.out.println("BookTicketPanel: Total physical seats retrieved for room " + room.getName() + ": " + allSeatsInRoom.size());

        if (allSeatsInRoom.isEmpty()) {
            String message = room.getSeatCount() > 0 ?
                             "Lỗi: Không có dữ liệu ghế cho phòng "+room.getName()+". Vui lòng kiểm tra (Admin cần tạo phòng và ghế)." :
                             "Phòng " + room.getName() + " không có ghế nào được cấu hình.";
            seatGridPanel.setLayout(new BorderLayout());
            seatGridPanel.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);
            System.err.println("BookTicketPanel: " + message);
            seatGridPanel.revalidate();
            seatGridPanel.repaint();
            return;
        }

        List<Seat> bookedSeatsForThisShowtime = ticketDAO.getBookedSeatsForShowtime(showtime.getId());
        List<Integer> bookedSeatIds = bookedSeatsForThisShowtime.stream().map(Seat::getId).collect(Collectors.toList());
        System.out.println("BookTicketPanel: Booked seat IDs for showtime " + showtime.getId() + ": " + bookedSeatIds);

        int numActualSeats = allSeatsInRoom.size();
        int numCols = 10;
        if (numActualSeats < numCols && numActualSeats > 0) numCols = numActualSeats;
        else if (numActualSeats == 0) numCols = 1;
        int numRows = (numCols > 0) ? (int) Math.ceil((double) numActualSeats / numCols) : 1;
        if (numActualSeats > 0 && numRows == 0) numRows = 1;

        System.out.println("BookTicketPanel: GridLayout - Rows: " + numRows + ", Cols: " + numCols + " for " + numActualSeats + " seats.");
        seatGridPanel.setLayout(new GridLayout(numRows, numCols, 8, 8));
        ButtonGroup seatGroup = new ButtonGroup();

        for (Seat seat : allSeatsInRoom) {
            JToggleButton seatButton = new JToggleButton(seat.getSeatNumber());
            seatButton.setPreferredSize(new Dimension(65, 45));
            seatButton.setFont(new Font("Arial", Font.BOLD, 12));
            seatToggleButtons.add(seatButton);
            seatGroup.add(seatButton);

            if (bookedSeatIds.contains(seat.getId())) {
                seatButton.setEnabled(false); seatButton.setSelected(true);
                seatButton.setBackground(Color.RED.darker()); seatButton.setForeground(Color.WHITE);
                seatButton.setToolTipText("Ghế đã đặt");
            } else {
                seatButton.setBackground(Color.GREEN.darker()); seatButton.setForeground(Color.WHITE);
                seatButton.setToolTipText("Ghế còn trống");
                seatButton.addActionListener(e -> {
                    for (JToggleButton btn : seatToggleButtons) {
                        if (btn.isEnabled() && btn != seatButton && btn.getBackground().equals(Color.ORANGE)) {
                             btn.setBackground(Color.GREEN.darker());
                        }
                    }
                    if (seatButton.isSelected()) {
                        currentlySelectedSeatObject = seat;
                        selectedSeatLabel.setText("Ghế đang chọn: " + seat.getSeatNumber());
                        bookButton.setEnabled(true);
                        seatButton.setBackground(Color.ORANGE);
                    } else {
                        currentlySelectedSeatObject = null;
                        selectedSeatLabel.setText("Ghế đang chọn: Chưa có");
                        bookButton.setEnabled(false);
                        seatButton.setBackground(Color.GREEN.darker());
                    }
                });
            }
            seatGridPanel.add(seatButton);
        }
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
        System.out.println("BookTicketPanel: Seat panel updated with " + seatGridPanel.getComponentCount() + " seat buttons.");
    }
    
    private void handleBookTicket() {
        if (LoginFrame.currentUser == null) { JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để đặt vé.", "Chưa Đăng Nhập", JOptionPane.WARNING_MESSAGE); return; }
        if (currentShowtimeObject == null || currentlySelectedSeatObject == null) { JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ suất chiếu và ghế.", "Thiếu Thông Tin", JOptionPane.WARNING_MESSAGE); return; }

        Movie movieForConfirmation = null;
        if(movieComboBox.getSelectedItem() instanceof Movie && ((Movie)movieComboBox.getSelectedItem()).getId() != MOVIE_PLACEHOLDER.getId()){
            movieForConfirmation = (Movie) movieComboBox.getSelectedItem();
        }
        Room roomForConfirmation = null;
        if(showtimeComboBox.getSelectedItem() instanceof ShowtimeDisplay && ((ShowtimeDisplay)showtimeComboBox.getSelectedItem()).showtime != null){
            ShowtimeDisplay display = (ShowtimeDisplay) showtimeComboBox.getSelectedItem();
            if(display.room != null) roomForConfirmation = display.room;
        }
        
        if (movieForConfirmation == null || roomForConfirmation == null) {
             JOptionPane.showMessageDialog(this, "Không thể xác định thông tin phim hoặc phòng. Vui lòng chọn lại.", "Lỗi", JOptionPane.ERROR_MESSAGE); return;
        }
        
        // Lấy giá từ currentShowtimePrice đã được set khi chọn suất chiếu
        BigDecimal priceToPay = currentShowtimePrice;
        if (priceToPay == null || priceToPay.compareTo(BigDecimal.ZERO) < 0) {
             JOptionPane.showMessageDialog(this, "Giá vé không hợp lệ cho suất chiếu này. Vui lòng chọn lại suất chiếu.", "Lỗi Giá Vé", JOptionPane.ERROR_MESSAGE); return;
        }

        String confirmationMessage = String.format(
                "Xác nhận đặt vé:\n\nPhim: %s\nPhòng: %s\nSuất chiếu: %s\nGhế: %s\nGiá vé: %s\n\nBạn có chắc chắn muốn đặt vé này không?",
                movieForConfirmation.getTitle(),
                roomForConfirmation.getName(),
                sdfDateTime.format(currentShowtimeObject.getShowTime()),
                currentlySelectedSeatObject.getSeatNumber(),
                currencyFormatter.format(priceToPay)
        );
        int confirm = JOptionPane.showConfirmDialog(this, confirmationMessage, "Xác Nhận Đặt Vé", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Ticket ticket = new Ticket();
            ticket.setShowtimeId(currentShowtimeObject.getId());
            ticket.setUserId(LoginFrame.currentUser.getId());
            ticket.setSeatId(currentlySelectedSeatObject.getId());
            ticket.setPricePaid(priceToPay); // GÁN GIÁ ĐÃ TRẢ

            if (ticketDAO.addTicket(ticket)) {
                JOptionPane.showMessageDialog(this, "Đặt vé thành công cho ghế " + currentlySelectedSeatObject.getSeatNumber() + "!", "Đặt Vé Thành Công", JOptionPane.INFORMATION_MESSAGE);
                loadSeatsForShowtime(currentShowtimeObject); // Làm mới sơ đồ ghế
            } else {
                JOptionPane.showMessageDialog(this, "Đặt vé thất bại. Ghế có thể đã được người khác đặt hoặc có lỗi CSDL.", "Đặt Vé Thất Bại", JOptionPane.ERROR_MESSAGE);
                loadSeatsForShowtime(currentShowtimeObject); // Luôn làm mới để cập nhật trạng thái
            }
        }
    }
    
    public void refreshDataOnTabSelect() { // Được gọi từ UserDashboardFrame
        System.out.println("BookTicketPanel: Tab selected, refreshing movie combo box...");
        refreshMovieComboBox();
        // Các ComboBox khác (suất chiếu) sẽ được làm mới theo logic đã có khi movieComboBox thay đổi.
        // Ghế cũng sẽ được làm mới khi suất chiếu thay đổi.
    }
}