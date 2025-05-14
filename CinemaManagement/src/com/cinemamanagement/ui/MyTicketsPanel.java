package com.cinemamanagement.ui;

import com.cinemamanagement.dao.TicketDAO;
import com.cinemamanagement.dao.MovieDAO; // Để lấy tên phim
import com.cinemamanagement.dao.RoomDAO;   // Để lấy tên phòng
import com.cinemamanagement.dao.ShowtimeDAO; // Để lấy thời gian chiếu
import com.cinemamanagement.dao.SeatDAO;     // Để lấy số ghế
import com.cinemamanagement.model.Ticket;
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.model.Room;
import com.cinemamanagement.model.Showtime;
import com.cinemamanagement.model.Seat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class MyTicketsPanel extends JPanel {
    private JTable ticketsTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    // private JButton cancelButton; // Tùy chọn: Nút hủy vé

    private TicketDAO ticketDAO;
    // Các DAO khác để lấy thông tin chi tiết cho vé
    private MovieDAO movieDAO;
    private RoomDAO roomDAO;
    private ShowtimeDAO showtimeDAO;
    private SeatDAO seatDAO;

    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private SimpleDateFormat sdfBookingTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");


    public MyTicketsPanel() {
        ticketDAO = new TicketDAO();
        movieDAO = new MovieDAO();
        roomDAO = new RoomDAO();
        showtimeDAO = new ShowtimeDAO();
        seatDAO = new SeatDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        // Không load vé ở đây, sẽ load khi tab được chọn hoặc khi có sự kiện
        // Hoặc có thể gọi loadUserTickets() nếu muốn load ngay
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Làm Mới Danh Sách Vé");
        topPanel.add(refreshButton);
        // cancelButton = new JButton("Hủy Vé Đã Chọn"); // Nếu có chức năng hủy vé
        // topPanel.add(cancelButton);
        add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID Vé", "Tên Phim", "Phòng Chiếu", "Số Ghế", "Thời Gian Chiếu", "Thời Gian Đặt"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ticketsTable = new JTable(tableModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        ticketsTable.setRowSorter(sorter);
        ticketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketsTable.getTableHeader().setReorderingAllowed(false);
        ticketsTable.getTableHeader().setPreferredSize(new Dimension(ticketsTable.getTableHeader().getWidth(), 30));
        ticketsTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(ticketsTable);
        add(scrollPane, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> loadUserTickets());
        
        // if (cancelButton != null) {
        //     cancelButton.addActionListener(e -> handleCancelTicket());
        // }
    }

    public void loadUserTickets() {
        if (LoginFrame.currentUser == null) {
            tableModel.setRowCount(0); // Xóa bảng nếu không có user
            return;
        }

        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        
        // Sử dụng phương thức đã tạo trong TicketDAO để lấy thông tin chi tiết
        List<TicketDAO.TicketInfo> userTicketInfos = ticketDAO.getTicketDetailsByUser(LoginFrame.currentUser.getId());

        if (userTicketInfos != null) {
            for (TicketDAO.TicketInfo ticketInfo : userTicketInfos) {
                tableModel.addRow(new Object[]{
                        ticketInfo.ticket.getId(),
                        ticketInfo.movie.getTitle(),
                        ticketInfo.room.getName(),
                        ticketInfo.seat.getSeatNumber(),
                        sdfDateTime.format(ticketInfo.showtime.getShowTime()),
                        sdfBookingTime.format(ticketInfo.ticket.getBookingTime())
                });
            }
        }
         if (userTicketInfos == null || userTicketInfos.isEmpty()) {
            // Optional: Hiển thị thông báo nếu không có vé nào
            // JOptionPane.showMessageDialog(this, "Bạn chưa đặt vé nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // private void handleCancelTicket() {
    //     int selectedRow = ticketsTable.getSelectedRow();
    //     if (selectedRow == -1) {
    //         JOptionPane.showMessageDialog(this, "Vui lòng chọn một vé để hủy.", "Chưa chọn vé", JOptionPane.WARNING_MESSAGE);
    //         return;
    //     }
    //     int modelRow = ticketsTable.convertRowIndexToModel(selectedRow);
    //     int ticketId = (int) tableModel.getValueAt(modelRow, 0);
    //     String movieName = (String) tableModel.getValueAt(modelRow, 1);
    //     String seatNumber = (String) tableModel.getValueAt(modelRow, 3);

    //     // Kiểm tra xem suất chiếu đã qua chưa (logic phức tạp hơn)
    //     // Ticket selectedTicketFullInfo = ticketDAO.getTicketById(ticketId); // Lấy thông tin đầy đủ của vé
    //     // Showtime showtimeOfTicket = showtimeDAO.getShowtimeById(selectedTicketFullInfo.getShowtimeId());
    //     // if (showtimeOfTicket.getShowTime().before(new Date())) {
    //     //     JOptionPane.showMessageDialog(this, "Không thể hủy vé cho suất chiếu đã qua.", "Lỗi Hủy Vé", JOptionPane.ERROR_MESSAGE);
    //     //     return;
    //     // }


    //     int confirm = JOptionPane.showConfirmDialog(this,
    //             "Bạn có chắc chắn muốn hủy vé cho phim '" + movieName + "', ghế " + seatNumber + "?",
    //             "Xác Nhận Hủy Vé",
    //             JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

    //     if (confirm == JOptionPane.YES_OPTION) {
    //         if (ticketDAO.deleteTicket(ticketId)) {
    //             JOptionPane.showMessageDialog(this, "Hủy vé thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
    //             loadUserTickets(); // Tải lại danh sách vé
    //         } else {
    //             JOptionPane.showMessageDialog(this, "Hủy vé thất bại. Có lỗi xảy ra.", "Lỗi", JOptionPane.ERROR_MESSAGE);
    //         }
    //     }
    // }
}