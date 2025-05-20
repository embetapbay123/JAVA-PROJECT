package com.cinemamanagement.ui;

import com.cinemamanagement.dao.TicketDAO;
import com.cinemamanagement.model.User; // Cần để lấy currentUser nếu làm chức năng liên quan đến user hiện tại
// Giả sử bạn có các lớp tiện ích này
import com.cinemamanagement.utils.CustomHeaderRenderer;
import com.cinemamanagement.utils.RounedButton;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TicketManagementPanel extends JPanel {
    private JTable ticketsTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    // Các thành phần cho tìm kiếm (tùy chọn đơn giản)
    private JTextField searchField;
    private JButton searchButton;
    private JComboBox<String> searchTypeComboBox;


    private TicketDAO ticketDAO;
    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TicketManagementPanel() {
        ticketDAO = new TicketDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadAllTickets(); // Tải danh sách vé khi panel được tạo
    }

    private void initComponents() {
        // Panel Top: Chứa nút làm mới và (tùy chọn) tìm kiếm
        JPanel topPanel = new JPanel(new BorderLayout(10,5));

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshButton = new RounedButton("Làm Mới Danh Sách Vé", new Color(33, 150, 243));
        refreshPanel.add(refreshButton);
        topPanel.add(refreshPanel, BorderLayout.WEST);

        // Panel tìm kiếm (ví dụ đơn giản: tìm theo username người đặt hoặc ID vé)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        searchPanel.add(new JLabel("Tìm theo:"));
        searchTypeComboBox = new JComboBox<>(new String[]{"Username Người Đặt", "ID Vé", "Tên Phim"});
        searchPanel.add(searchTypeComboBox);
        searchField = new JTextField(15);
        searchPanel.add(searchField);
        searchButton = new RounedButton("Tìm", new Color(0, 150, 136));
        searchPanel.add(searchButton);
        topPanel.add(searchPanel, BorderLayout.EAST);


        add(topPanel, BorderLayout.NORTH);

        // Bảng hiển thị danh sách vé
        String[] columnNames = {
                "ID Vé", "Tên Phim", "Phòng", "Ghế",
                "Suất Chiếu", "Người Đặt", "Giá Vé Trả", "Thời Gian Đặt"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa trực tiếp trên bảng
            }
        };
        ticketsTable = new JTable(tableModel);
        ticketsTable.setRowSorter(new TableRowSorter<>(tableModel));
        ticketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketsTable.getTableHeader().setReorderingAllowed(false);
        ticketsTable.getTableHeader().setPreferredSize(new Dimension(0, 35));
        ticketsTable.setRowHeight(28);
        ticketsTable.getTableHeader().setDefaultRenderer(
            new CustomHeaderRenderer(new Color(173, 216, 230), new Color(20, 20, 20))
        );
        // Thiết lập độ rộng cột (tùy chọn)
        ticketsTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID Vé
        ticketsTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên Phim
        ticketsTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Suất Chiếu
        ticketsTable.getColumnModel().getColumn(7).setPreferredWidth(150); // Thời Gian Đặt


        JScrollPane scrollPane = new JScrollPane(ticketsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Gán sự kiện
        refreshButton.addActionListener(e -> loadAllTickets());
        searchButton.addActionListener(e -> searchTickets());
        searchField.addActionListener(e -> searchTickets()); // Cho phép Enter để tìm

        // (Tùy chọn) Có thể thêm panel chi tiết ở dưới nếu muốn hiển thị thêm khi click vào vé
        // JPanel ticketDetailsDisplayPanel = new JPanel();
        // add(ticketDetailsDisplayPanel, BorderLayout.SOUTH);
    }

    public void loadAllTickets() {
        System.out.println("TicketManagementPanel: Loading all tickets...");
        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        searchField.setText(""); // Xóa trường tìm kiếm

        List<TicketDAO.TicketInfo> ticketInfos = ticketDAO.getAllTicketDetailsForAdmin();
        if (ticketInfos != null) {
            for (TicketDAO.TicketInfo info : ticketInfos) {
                tableModel.addRow(new Object[]{
                        info.ticket.getId(),
                        info.movie.getTitle(),
                        info.room.getName(),
                        info.seat.getSeatNumber(),
                        sdfDateTime.format(info.showtime.getShowTime()),
                        info.user.getUsername(),
                        currencyFormatter.format(info.ticket.getPricePaid()),
                        sdfDateTime.format(info.ticket.getBookingTime())
                });
            }
        }
        System.out.println("TicketManagementPanel: Loaded " + tableModel.getRowCount() + " tickets.");
    }

    private void searchTickets() {
        String searchTerm = searchField.getText().trim();
        String searchType = (String) searchTypeComboBox.getSelectedItem();

        if (searchTerm.isEmpty()) {
            loadAllTickets(); // Nếu không có từ khóa, tải lại tất cả
            return;
        }
        System.out.println("TicketManagementPanel: Searching tickets with term '" + searchTerm + "' by '" + searchType + "'");
        tableModel.setRowCount(0);
        
        // Hiện tại getAllTicketDetailsForAdmin không có tham số tìm kiếm.
        // Để tìm kiếm thực sự, bạn cần sửa getAllTicketDetailsForAdmin hoặc tạo phương thức mới
        // trong TicketDAO nhận tham số tìm kiếm và xây dựng câu lệnh WHERE tương ứng.
        // Dưới đây là cách lọc thủ công trên client-side (không hiệu quả với lượng dữ liệu lớn)
        // NHƯNG ĐỂ ĐƠN GIẢN CHO VÍ DỤ NÀY.
        // TỐT NHẤT LÀ LỌC Ở PHÍA DATABASE.

        List<TicketDAO.TicketInfo> allTicketInfos = ticketDAO.getAllTicketDetailsForAdmin();
        List<TicketDAO.TicketInfo> filteredTicketInfos = new ArrayList<>();

        if (allTicketInfos != null) {
            for (TicketDAO.TicketInfo info : allTicketInfos) {
                boolean match = false;
                if ("Username Người Đặt".equals(searchType)) {
                    if (info.user.getUsername().toLowerCase().contains(searchTerm.toLowerCase())) {
                        match = true;
                    }
                } else if ("ID Vé".equals(searchType)) {
                    try {
                        int ticketIdSearch = Integer.parseInt(searchTerm);
                        if (info.ticket.getId() == ticketIdSearch) {
                            match = true;
                        }
                    } catch (NumberFormatException ex) {
                        // Bỏ qua nếu không phải số
                    }
                } else if ("Tên Phim".equals(searchType)) {
                     if (info.movie.getTitle().toLowerCase().contains(searchTerm.toLowerCase())) {
                        match = true;
                    }
                }

                if (match) {
                    filteredTicketInfos.add(info);
                }
            }
        }

        if (!filteredTicketInfos.isEmpty()) {
            for (TicketDAO.TicketInfo info : filteredTicketInfos) {
                tableModel.addRow(new Object[]{
                        info.ticket.getId(),
                        info.movie.getTitle(),
                        info.room.getName(),
                        info.seat.getSeatNumber(),
                        sdfDateTime.format(info.showtime.getShowTime()),
                        info.user.getUsername(),
                        currencyFormatter.format(info.ticket.getPricePaid()),
                        sdfDateTime.format(info.ticket.getBookingTime())
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy vé nào khớp với tìm kiếm.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
         System.out.println("TicketManagementPanel: Search completed. Found " + tableModel.getRowCount() + " tickets.");
    }

    // Phương thức này có thể được gọi từ AdminDashboardFrame khi tab này được chọn
    public void refreshTicketList() {
        System.out.println("TicketManagementPanel: Tab selected. Refreshing ticket list.");
        loadAllTickets();
    }
}