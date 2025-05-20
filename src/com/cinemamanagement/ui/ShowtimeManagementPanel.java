// com.cinemamanagement.ui.ShowtimeManagementPanel.java
package com.cinemamanagement.ui;

import com.cinemamanagement.dao.MovieDAO;
import com.cinemamanagement.dao.RoomDAO;
import com.cinemamanagement.dao.ShowtimeDAO;
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.model.Room;
import com.cinemamanagement.model.Showtime;
import com.cinemamanagement.utils.CustomHeaderRenderer; // Giả sử bạn có package này
import com.cinemamanagement.utils.RounedButton;   // Giả sử bạn có package này

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal; // QUAN TRỌNG: Import BigDecimal
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class ShowtimeManagementPanel extends JPanel {
    private JTable showtimeTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, refreshButton, clearFormButton;

    private JComboBox<Movie> movieComboBox;
    private JComboBox<Room> roomComboBox;
    private JSpinner timeSpinner;
    private JTextField dateField;
    private JTextField priceField; // Thêm trường nhập giá vé

    private ShowtimeDAO showtimeDAO;
    private MovieDAO movieDAO;
    private RoomDAO roomDAO;

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));


    public ShowtimeManagementPanel() {
        showtimeDAO = new ShowtimeDAO();
        movieDAO = new MovieDAO();
        roomDAO = new RoomDAO();
        dateFormat.setLenient(false);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        // loadShowtimes(); // Sẽ được gọi trong refreshAllSelectionsInForm
        refreshAllSelectionsInForm(); // Gọi để tải dữ liệu ban đầu cho ComboBoxes và bảng
    }

    private void initComponents() {
        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        // Sử dụng lớp RounedButton của bạn
        addButton = new RounedButton("Thêm Suất Chiếu", new Color(76, 175, 80));
        editButton = new RounedButton("Sửa Suất Chiếu", new Color(33, 150, 243));
        deleteButton = new RounedButton("Xóa Suất Chiếu", new Color(244, 67, 54));
        refreshButton = new RounedButton("Làm Mới", new Color(255, 193, 7)); // Đổi tên nút cho ngắn gọn

        mainButtonsPanel.add(addButton);
        mainButtonsPanel.add(editButton);
        mainButtonsPanel.add(deleteButton);
        mainButtonsPanel.add(refreshButton);
        add(mainButtonsPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID Suất", "Tên Phim", "Tên Phòng", "Thời Gian Chiếu", "Giá Vé"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        showtimeTable = new JTable(tableModel);
        showtimeTable.setBackground(new Color(255, 255, 240));
        showtimeTable.setSelectionBackground(new Color(255, 153, 102));
        showtimeTable.setSelectionForeground(Color.WHITE);
        showtimeTable.setRowSorter(new TableRowSorter<>(tableModel));
        showtimeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showtimeTable.getTableHeader().setReorderingAllowed(false);
        showtimeTable.getTableHeader().setPreferredSize(new Dimension(0, 35)); // Tăng chiều cao header
        showtimeTable.setRowHeight(28); // Tăng chiều cao dòng
        showtimeTable.getTableHeader().setDefaultRenderer(new CustomHeaderRenderer(new Color(173, 216, 230), new Color(20, 20, 20))); // Màu chữ đậm hơn
        showtimeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && showtimeTable.getSelectedRow() != -1) {
                    populateFormFromTable(showtimeTable.convertRowIndexToModel(showtimeTable.getSelectedRow()));
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(showtimeTable);
        scrollPane.setPreferredSize(new Dimension(800, 300)); // Set kích thước ưu tiên cho bảng
        add(scrollPane, BorderLayout.CENTER);


        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin chi tiết suất chiếu"));
        formPanel.setBackground(new Color(240, 248, 255)); // Màu nền nhẹ nhàng hơn
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8); // Tăng padding
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Chọn Phim:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; movieComboBox = new JComboBox<>(); formPanel.add(movieComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; formPanel.add(new JLabel("Chọn Phòng:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; roomComboBox = new JComboBox<>(); formPanel.add(roomComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; formPanel.add(new JLabel("Ngày Chiếu (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; dateField = new JTextField(12); formPanel.add(dateField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; formPanel.add(new JLabel("Giờ Chiếu (HH:mm):"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
        timeSpinner.setValue(cal.getTime());
        formPanel.add(timeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0; formPanel.add(new JLabel("Giá Vé (VNĐ):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        priceField = new JTextField(12);
        formPanel.add(priceField, gbc);

        clearFormButton =  new RounedButton("Xóa Form", new Color(108, 117, 125)); // Màu xám cho nút clear
        gbc.gridx = 1; gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0;
        formPanel.add(clearFormButton, gbc);
        add(formPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addShowtime());
        editButton.addActionListener(e -> editShowtime());
        deleteButton.addActionListener(e -> deleteShowtime());
        refreshButton.addActionListener(e -> {
            loadShowtimes(); // Tải lại bảng
            refreshAllSelectionsInForm(); // Tải lại comboboxes
            clearForm(); // Xóa form
        });
        clearFormButton.addActionListener(e -> clearForm());
    }

    public void refreshMovieSelectionInForm() {
        System.out.println("ShowtimeManagementPanel: Refreshing movie combo box for form...");
        Movie previouslySelectedMovie = null;
        if (movieComboBox.getSelectedItem() instanceof Movie && ((Movie) movieComboBox.getSelectedItem()).getId() != 0) {
            previouslySelectedMovie = (Movie) movieComboBox.getSelectedItem();
        }

        movieComboBox.removeAllItems();
        // Thêm placeholder với ID = 0 (hoặc một giá trị đặc biệt)
        movieComboBox.addItem(new Movie("--- Chọn Phim ---", "", 0));

        List<Movie> movies = movieDAO.getAllMovies();
        if (movies != null) {
            for (Movie movie : movies) {
                movieComboBox.addItem(movie);
            }
        }

        boolean reSelected = false;
        if (previouslySelectedMovie != null) {
            for (int i = 0; i < movieComboBox.getItemCount(); i++) {
                if (movieComboBox.getItemAt(i) instanceof Movie) {
                    Movie item = (Movie) movieComboBox.getItemAt(i);
                    if (item.getId() == previouslySelectedMovie.getId()) {
                        movieComboBox.setSelectedIndex(i);
                        reSelected = true;
                        break;
                    }
                }
            }
        }
        if (!reSelected) {
            movieComboBox.setSelectedIndex(0); // Chọn placeholder nếu không tìm thấy phim cũ
        }
        System.out.println("ShowtimeManagementPanel: Movie combo box for form refreshed.");
    }

    public void refreshRoomSelectionInForm() {
        System.out.println("ShowtimeManagementPanel: Refreshing room combo box for form...");
        Room previouslySelectedRoom = null;
        if (roomComboBox.getSelectedItem() instanceof Room && ((Room) roomComboBox.getSelectedItem()).getId() != 0) {
            previouslySelectedRoom = (Room) roomComboBox.getSelectedItem();
        }

        roomComboBox.removeAllItems();
        roomComboBox.addItem(new Room(0, "--- Chọn Phòng ---", 0)); // Placeholder

        List<Room> rooms = roomDAO.getAllRooms();
        if (rooms != null) {
            for (Room room : rooms) {
                roomComboBox.addItem(room);
            }
        }

        boolean reSelected = false;
        if (previouslySelectedRoom != null) {
            for (int i = 0; i < roomComboBox.getItemCount(); i++) {
                if (roomComboBox.getItemAt(i) instanceof Room) {
                    Room item = (Room) roomComboBox.getItemAt(i);
                    if (item.getId() == previouslySelectedRoom.getId()) {
                        roomComboBox.setSelectedIndex(i);
                        reSelected = true;
                        break;
                    }
                }
            }
        }
        if (!reSelected) {
            roomComboBox.setSelectedIndex(0);
        }
        System.out.println("ShowtimeManagementPanel: Room combo box for form refreshed.");
    }

    public void refreshAllSelectionsInForm() {
        System.out.println("ShowtimeManagementPanel: Refreshing ALL selections (movies and rooms)...");
        refreshMovieSelectionInForm();
        refreshRoomSelectionInForm();
        loadShowtimes(); // Tải lại bảng suất chiếu khi làm mới tất cả
    }
        
    private void loadShowtimes() {
        System.out.println("ShowtimeManagementPanel: Loading showtimes into table...");
        tableModel.setRowCount(0);
        List<Showtime> showtimes = showtimeDAO.getAllShowtimes();
        if (showtimes != null) {
            for (Showtime st : showtimes) {
                Movie movie = movieDAO.getMovieById(st.getMovieId());
                Room room = roomDAO.getRoomById(st.getRoomId());
                tableModel.addRow(new Object[]{
                        st.getId(),
                        (movie != null) ? movie.getTitle() : "Phim ID " + st.getMovieId(),
                        (room != null) ? room.getName() : "Phòng ID " + st.getRoomId(),
                        dateTimeFormat.format(st.getShowTime()),
                        currencyFormatter.format(st.getPrice())
                });
            }
        }
        System.out.println("ShowtimeManagementPanel: Showtimes table loaded with " + tableModel.getRowCount() + " rows.");
    }

    private void populateFormFromTable(int modelRowIndex) {
        System.out.println("ShowtimeManagementPanel: Populating form from table row " + modelRowIndex);
        int showtimeId = (int) tableModel.getValueAt(modelRowIndex, 0);
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeId);
        if (showtime == null) {
            System.err.println("ShowtimeManagementPanel: Could not find showtime with ID " + showtimeId);
            clearForm();
            return;
        }

        Movie movie = movieDAO.getMovieById(showtime.getMovieId());
        Room room = roomDAO.getRoomById(showtime.getRoomId());

        boolean movieSetInComboBox = false;
        if (movie != null) {
            for (int i = 0; i < movieComboBox.getItemCount(); i++) {
                if (movieComboBox.getItemAt(i) instanceof Movie && ((Movie)movieComboBox.getItemAt(i)).getId() == movie.getId()) {
                     movieComboBox.setSelectedIndex(i);
                     movieSetInComboBox = true;
                     break;
                }
            }
        }
        if (!movieSetInComboBox) movieComboBox.setSelectedIndex(0); // Chọn placeholder nếu không tìm thấy
        
        boolean roomSetInComboBox = false;
        if (room != null) {
            for (int i = 0; i < roomComboBox.getItemCount(); i++) {
                if (roomComboBox.getItemAt(i) instanceof Room && ((Room)roomComboBox.getItemAt(i)).getId() == room.getId()) {
                     roomComboBox.setSelectedIndex(i);
                     roomSetInComboBox = true;
                     break;
                }
            }
        }
        if (!roomSetInComboBox) roomComboBox.setSelectedIndex(0);

        dateField.setText(dateFormat.format(showtime.getShowTime()));
        timeSpinner.setValue(showtime.getShowTime());
        priceField.setText(showtime.getPrice().toPlainString()); // Sử dụng toPlainString() cho BigDecimal
        System.out.println("ShowtimeManagementPanel: Form populated for showtime ID " + showtimeId);
    }

    private void clearForm() {
        movieComboBox.setSelectedIndex(0); // Chọn placeholder
        roomComboBox.setSelectedIndex(0);  // Chọn placeholder
        dateField.setText("");
        priceField.setText("");
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0);
        timeSpinner.setValue(cal.getTime());
        showtimeTable.clearSelection();
        System.out.println("ShowtimeManagementPanel: Form cleared.");
    }

    private Showtime getShowtimeFromForm() {
        if (!(movieComboBox.getSelectedItem() instanceof Movie) || ((Movie)movieComboBox.getSelectedItem()).getId() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phim hợp lệ.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE); return null;
        }
        Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();

        if (!(roomComboBox.getSelectedItem() instanceof Room) || ((Room)roomComboBox.getSelectedItem()).getId() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng hợp lệ.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE); return null;
        }
        Room selectedRoom = (Room) roomComboBox.getSelectedItem();

        String dateStr = dateField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (dateStr.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày chiếu.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE); return null; }
        if (priceStr.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập giá vé.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE); return null; }

        Date timeFromSpinner = (Date) timeSpinner.getValue();
        Date parsedDate;
        BigDecimal price;
        try {
            parsedDate = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Ngày chiếu không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.", "Lỗi Định Dạng Ngày", JOptionPane.ERROR_MESSAGE); return null;
        }
        try {
            price = new BigDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "Giá vé không được âm.", "Lỗi Giá Vé", JOptionPane.ERROR_MESSAGE); return null;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá vé không hợp lệ. Vui lòng nhập số.", "Lỗi Định Dạng Giá", JOptionPane.ERROR_MESSAGE); return null;
        }
        
        Calendar calDate = Calendar.getInstance(); calDate.setTime(parsedDate);
        Calendar calTime = Calendar.getInstance(); calTime.setTime(timeFromSpinner);
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        calDate.set(Calendar.SECOND, 0); calDate.set(Calendar.MILLISECOND, 0);
        
        Showtime showtime = new Showtime();
        showtime.setMovieId(selectedMovie.getId());
        showtime.setRoomId(selectedRoom.getId());
        showtime.setShowTime(calDate.getTime());
        showtime.setPrice(price);
        return showtime;
    }

    private void addShowtime() {
        Showtime showtime = getShowtimeFromForm();
        if (showtime == null) return;
        
        Calendar showCal = Calendar.getInstance(); showCal.setTime(showtime.getShowTime());
        Calendar nowCal = Calendar.getInstance();
        
        boolean isPastDate = showCal.get(Calendar.YEAR) < nowCal.get(Calendar.YEAR) ||
                             (showCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && 
                              showCal.get(Calendar.DAY_OF_YEAR) < nowCal.get(Calendar.DAY_OF_YEAR));
                              
        boolean isTodayButPastTime = showCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                                     showCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
                                     showtime.getShowTime().before(new Date(System.currentTimeMillis() - 60000 * 5)); // Cho phép trễ 5 phút

        if (isPastDate || isTodayButPastTime) {
            JOptionPane.showMessageDialog(this, "Không thể thêm suất chiếu cho thời gian trong quá khứ.", "Lỗi Thời Gian", JOptionPane.ERROR_MESSAGE); return;
        }

        if (showtimeDAO.addShowtime(showtime)) {
            JOptionPane.showMessageDialog(this, "Thêm suất chiếu thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadShowtimes(); clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Thêm suất chiếu thất bại. Có thể do xung đột thời gian hoặc lỗi CSDL.", "Lỗi Thêm Suất Chiếu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editShowtime() {
        int selectedRow = showtimeTable.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một suất chiếu từ bảng để sửa.", "Chưa chọn suất chiếu", JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = showtimeTable.convertRowIndexToModel(selectedRow);
        int showtimeId = (int) tableModel.getValueAt(modelRow, 0);
        Showtime showtimeToUpdate = getShowtimeFromForm();
        if (showtimeToUpdate == null) return;
        showtimeToUpdate.setId(showtimeId);

        if (showtimeDAO.updateShowtime(showtimeToUpdate)) {
            JOptionPane.showMessageDialog(this, "Cập nhật suất chiếu thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadShowtimes(); clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật suất chiếu thất bại. Có thể do xung đột thời gian hoặc lỗi CSDL.", "Lỗi Cập Nhật Suất Chiếu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteShowtime() {
        int selectedRow = showtimeTable.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this,"Vui lòng chọn suất chiếu để xóa.","Lỗi",JOptionPane.ERROR_MESSAGE); return; }
        int modelRow = showtimeTable.convertRowIndexToModel(selectedRow);
        int showtimeId = (int) tableModel.getValueAt(modelRow, 0);
        String movieName = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String roomName = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String time = String.valueOf(tableModel.getValueAt(modelRow, 3));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa suất chiếu:\n" +
                "Phim: " + movieName + "\n" +
                "Phòng: " + roomName + "\n" +
                "Thời gian: " + time + " (ID: " + showtimeId + ") không?\n" +
                "LƯU Ý: Vé đã đặt cho suất chiếu này CŨNG SẼ BỊ XÓA.",
                "Xác nhận xóa suất chiếu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (showtimeDAO.deleteShowtime(showtimeId)) {
                 JOptionPane.showMessageDialog(this,"Xóa suất chiếu thành công!","Thông báo",JOptionPane.INFORMATION_MESSAGE);
                 loadShowtimes(); clearForm();
            } else {
                 JOptionPane.showMessageDialog(this,"Xóa suất chiếu thất bại.","Lỗi",JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}