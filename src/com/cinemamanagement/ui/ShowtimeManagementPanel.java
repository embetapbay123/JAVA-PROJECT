package com.cinemamanagement.ui;

import com.cinemamanagement.dao.MovieDAO;
import com.cinemamanagement.dao.RoomDAO;
import com.cinemamanagement.dao.ShowtimeDAO;
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.model.Room;
import com.cinemamanagement.model.Showtime;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector; // Dùng cho JComboBox model
import com.cinemamanagement.utils.*;

public class ShowtimeManagementPanel extends JPanel {
    private JTable showtimeTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, refreshButton, clearFormButton;

    // Components cho form
    private JComboBox<Movie> movieComboBox;
    private JComboBox<Room> roomComboBox;
    private JSpinner timeSpinner; // Dùng JSpinner để chọn giờ và phút
    private JTextField dateField; // Dùng JTextField để nhập ngày (dd/MM/yyyy)

    private ShowtimeDAO showtimeDAO;
    private MovieDAO movieDAO;
    private RoomDAO roomDAO;

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    // private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm"); // Không cần thiết vì JSpinner xử lý

    public ShowtimeManagementPanel() {
        showtimeDAO = new ShowtimeDAO();
        movieDAO = new MovieDAO(); // Đảm bảo đã khởi tạo
        roomDAO = new RoomDAO();   // Đảm bảo đã khởi tạo

        // Cấu hình SimpleDateFormat
        dateFormat.setLenient(false);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadShowtimes(); // Tải danh sách suất chiếu ban đầu
        loadMovieAndRoomComboBoxes(); // Tải dữ liệu cho ComboBoxes lần đầu
    }

    private void initComponents() {
        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        addButton = new RounedButton("Thêm Suất Chiếu", new Color(76, 175, 80)); 
        editButton = new RounedButton("Sửa Suất Chiếu",new Color(33, 150, 243)); 
        deleteButton = new RounedButton("Xóa Suất Chiếu", new Color(244, 67, 54));
        refreshButton = new RounedButton("Làm Mới Bảng", new Color(255, 193, 7));
        
        mainButtonsPanel.add(addButton);
        mainButtonsPanel.add(editButton);
        mainButtonsPanel.add(deleteButton);
        mainButtonsPanel.add(refreshButton);
        add(mainButtonsPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID Suất", "Tên Phim", "Tên Phòng", "Thời Gian Chiếu"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        showtimeTable = new JTable(tableModel);
        showtimeTable.setBackground(new Color(255, 255, 240)); // Very light yellow
        showtimeTable.setSelectionBackground(new Color(255, 153, 102)); // Light orange for selected row
        showtimeTable.setSelectionForeground(Color.WHITE);   
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        showtimeTable.setRowSorter(sorter);
        showtimeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showtimeTable.getTableHeader().setReorderingAllowed(false);
        showtimeTable.getTableHeader().setPreferredSize(new Dimension(showtimeTable.getTableHeader().getWidth(), 30));
        showtimeTable.setRowHeight(25);
        showtimeTable.setPreferredScrollableViewportSize(new Dimension(800, 300));
        showtimeTable.getTableHeader().setDefaultRenderer(
        	    new CustomHeaderRenderer(new Color(173, 216, 230), new Color(60, 60, 60))
        	);
        showtimeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int selectedRow = showtimeTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int modelRow = showtimeTable.convertRowIndexToModel(selectedRow);
                        populateFormFromTable(modelRow);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(showtimeTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin chi tiết suất chiếu"));
        formPanel.setBackground(new Color(204, 229, 255)); // Light yellow
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Chọn Phim:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        movieComboBox = new JComboBox<>(); // Khởi tạo JComboBox
        formPanel.add(movieComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; formPanel.add(new JLabel("Chọn Phòng:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        roomComboBox = new JComboBox<>(); // Khởi tạo JComboBox
        formPanel.add(roomComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; formPanel.add(new JLabel("Ngày Chiếu (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        dateField = new JTextField(10);
        formPanel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; formPanel.add(new JLabel("Giờ Chiếu (HH:mm):"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        timeSpinner.setValue(cal.getTime());
        formPanel.add(timeSpinner, gbc);

        clearFormButton =  new RounedButton("Xóa Form", new Color(244, 67, 54));
        gbc.gridx = 1; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        formPanel.add(clearFormButton, gbc);

        add(formPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addShowtime());
        editButton.addActionListener(e -> editShowtime());
        deleteButton.addActionListener(e -> deleteShowtime());
        refreshButton.addActionListener(e -> {
            loadShowtimes();
            clearForm();
        });
        clearFormButton.addActionListener(e -> clearForm());
    }

    // === PHẦN SỬA ĐỔI QUAN TRỌNG: Phương thức làm mới ComboBox Phim ===
    /**
     * Tải lại danh sách phim cho movieComboBox trong form.
     * Được gọi khi panel này được focus hoặc khi có sự thay đổi dữ liệu phim.
     */
    public void refreshMovieSelectionInForm() {
        System.out.println("ShowtimeManagementPanel: Refreshing movie combo box for form...");

        Movie previouslySelectedMovie = null;
        if (movieComboBox.getSelectedItem() instanceof Movie) {
            previouslySelectedMovie = (Movie) movieComboBox.getSelectedItem();
        }

        movieComboBox.removeAllItems();
        // Bạn có thể thêm một item placeholder nếu muốn, ví dụ:
         movieComboBox.addItem(new Movie("--- Chọn Phim ---", null, 0));
        // Điều này hữu ích nếu bạn muốn người dùng phải chủ động chọn.
        // Nếu không có placeholder, ComboBox sẽ tự động chọn item đầu tiên (nếu có).

        List<Movie> movies = movieDAO.getAllMovies();
        if (movies != null) {
            for (Movie movie : movies) {
                movieComboBox.addItem(movie);
            }
        }

        if (previouslySelectedMovie != null && previouslySelectedMovie.getId() != 0) {
            boolean found = false;
            for (int i = 0; i < movieComboBox.getItemCount(); i++) {
                // Cẩn thận khi getItemAt(i) có thể không phải là Movie nếu có placeholder
                if (movieComboBox.getItemAt(i) instanceof Movie) {
                    Movie item = (Movie) movieComboBox.getItemAt(i);
                    if (item.getId() == previouslySelectedMovie.getId()) {
                        movieComboBox.setSelectedIndex(i);
                        found = true;
                        break;
                    }
                }
            }
            if (!found && movieComboBox.getItemCount() > 0) {
                // Nếu phim cũ không còn và có item khác, chọn item đầu tiên
                // (Hoặc không làm gì nếu bạn có placeholder và muốn nó được chọn)
                // movieComboBox.setSelectedIndex(0);
            }
        } else if (movieComboBox.getItemCount() > 0) {
            // Nếu không có phim nào được chọn trước đó, và không có placeholder
            // movieComboBox.setSelectedIndex(0);
        }
        System.out.println("ShowtimeManagementPanel: Movie combo box for form refreshed.");
    }
 // === PHƯƠNG THỨC MỚI: Làm mới ComboBox Phòng ===
    /**
     * Tải lại danh sách phòng cho roomComboBox trong form.
     */
    public void refreshRoomSelectionInForm() {
        System.out.println("ShowtimeManagementPanel: Refreshing room combo box for form...");

        Room previouslySelectedRoom = null;
        if (roomComboBox.getSelectedItem() instanceof Room) {
            previouslySelectedRoom = (Room) roomComboBox.getSelectedItem();
        }

        roomComboBox.removeAllItems();
        // Tương tự movieComboBox, bạn có thể thêm placeholder cho Room nếu muốn:
        // roomComboBox.addItem(new Room(0, "--- Chọn Phòng ---", 0));

        List<Room> rooms = roomDAO.getAllRooms(); // Lấy danh sách phòng mới nhất
        if (rooms != null) {
            for (Room room : rooms) {
                roomComboBox.addItem(room);
            }
        }

        if (previouslySelectedRoom != null && previouslySelectedRoom.getId() != 0) {
            boolean found = false;
            for (int i = 0; i < roomComboBox.getItemCount(); i++) {
                if (roomComboBox.getItemAt(i) instanceof Room) {
                    Room item = (Room) roomComboBox.getItemAt(i);
                    if (item.getId() == previouslySelectedRoom.getId()) {
                        roomComboBox.setSelectedIndex(i);
                        found = true;
                        break;
                    }
                }
            }
            if (!found && roomComboBox.getItemCount() > 0) {
                // roomComboBox.setSelectedIndex(0); // Chọn item đầu nếu phòng cũ không còn
            }
        } else if (roomComboBox.getItemCount() > 0) {
            // roomComboBox.setSelectedIndex(0); // Chọn item đầu nếu không có gì được chọn trước
        }
        System.out.println("ShowtimeManagementPanel: Room combo box for form refreshed.");
    }
    // === KẾT THÚC PHƯƠNG THỨC MỚI ===

    // === KẾT THÚC PHẦN SỬA ĐỔI ===


    private void loadMovieAndRoomComboBoxes() {
        // === PHẦN SỬA ĐỔI: Gọi phương thức làm mới phim ===
        refreshMovieSelectionInForm(); // Thay vì load trực tiếp vào model ở đây
        refreshRoomSelectionInForm();
        // === KẾT THÚC PHẦN SỬA ĐỔI ===
        
        // Load rooms (giữ nguyên)
//        List<Room> rooms = roomDAO.getAllRooms();
//        Vector<Room> roomVector = new Vector<>(rooms);
//        roomComboBox.setModel(new DefaultComboBoxModel<>(roomVector));
        // Nếu bạn muốn có placeholder cho Room ComboBox:
        // Room placeholderRoom = new Room(0, "--- Chọn Phòng ---", 0);
        // roomVector.insertElementAt(placeholderRoom, 0);
        // roomComboBox.setModel(new DefaultComboBoxModel<>(roomVector));
        // roomComboBox.setSelectedIndex(0);
    }

    private void loadShowtimes() {
        tableModel.setRowCount(0);
        List<Showtime> showtimes = showtimeDAO.getAllShowtimes();
        for (Showtime st : showtimes) {
            Movie movie = movieDAO.getMovieById(st.getMovieId());
            Room room = roomDAO.getRoomById(st.getRoomId());
            tableModel.addRow(new Object[]{
                    st.getId(),
                    (movie != null) ? movie.getTitle() : "Lỗi: Phim ID " + st.getMovieId(),
                    (room != null) ? room.getName() : "Lỗi: Phòng ID " + st.getRoomId(),
                    dateTimeFormat.format(st.getShowTime())
            });
        }
    }

    private void populateFormFromTable(int modelRow) {
        int showtimeId = (int) tableModel.getValueAt(modelRow, 0);
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeId);

        if (showtime != null) {
            Movie movie = movieDAO.getMovieById(showtime.getMovieId());
            if (movie != null) {
                boolean movieFoundInComboBox = false;
                for (int i = 0; i < movieComboBox.getItemCount(); i++) {
                     if (movieComboBox.getItemAt(i) instanceof Movie && ((Movie)movieComboBox.getItemAt(i)).getId() == movie.getId()) {
                        movieComboBox.setSelectedIndex(i);
                        movieFoundInComboBox = true;
                        break;
                    }
                }
                if(!movieFoundInComboBox) movieComboBox.setSelectedItem(null); // Hoặc set placeholder
            } else {
                movieComboBox.setSelectedItem(null);
            }

            Room room = roomDAO.getRoomById(showtime.getRoomId());
            if (room != null) {
                boolean roomFoundInComboBox = false;
                for (int i = 0; i < roomComboBox.getItemCount(); i++) {
                     if (roomComboBox.getItemAt(i) instanceof Room && ((Room)roomComboBox.getItemAt(i)).getId() == room.getId()) {
                        roomComboBox.setSelectedIndex(i);
                        roomFoundInComboBox = true;
                        break;
                    }
                }
                if(!roomFoundInComboBox) roomComboBox.setSelectedItem(null);
            } else {
                roomComboBox.setSelectedItem(null);
            }

            Date showDateTime = showtime.getShowTime();
            dateField.setText(dateFormat.format(showDateTime));
            timeSpinner.setValue(showDateTime);
        }
    }

    private void clearForm() {
        // Nếu có placeholder, chọn nó, nếu không thì setSelectedItem(null)
        if (movieComboBox.getItemCount() > 0 && !(movieComboBox.getItemAt(0) instanceof Movie && ((Movie)movieComboBox.getItemAt(0)).getId() == 0 )) {
             movieComboBox.setSelectedIndex(0); // Giả sử item đầu không phải placeholder
        } else {
             movieComboBox.setSelectedItem(null); // Hoặc setSelectedIndex(0) nếu có placeholder
        }

        if (roomComboBox.getItemCount() > 0 && !(roomComboBox.getItemAt(0) instanceof Room && ((Room)roomComboBox.getItemAt(0)).getId() == 0)) {
            roomComboBox.setSelectedIndex(0);
        } else {
            roomComboBox.setSelectedItem(null);
        }

        dateField.setText("");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        timeSpinner.setValue(cal.getTime());
        showtimeTable.clearSelection();
    }

    private Showtime getShowtimeFromForm() {
        // Kiểm tra xem có item nào được chọn không
        if (!(movieComboBox.getSelectedItem() instanceof Movie)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phim.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();

        if (!(roomComboBox.getSelectedItem() instanceof Room)) {
             JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        Room selectedRoom = (Room) roomComboBox.getSelectedItem();

        String dateStr = dateField.getText().trim();
        Date timeFromSpinner = (Date) timeSpinner.getValue();

        if (dateStr.isEmpty()) { // selectedMovie và selectedRoom đã được kiểm tra ở trên
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày chiếu.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }


        Date parsedDate;
        try {
            parsedDate = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Ngày chiếu không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.", "Lỗi Định Dạng Ngày", JOptionPane.ERROR_MESSAGE);
            dateField.requestFocusInWindow();
            return null;
        }

        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(parsedDate);
        Calendar calendarTime = Calendar.getInstance();
        calendarTime.setTime(timeFromSpinner);
        calendarDate.set(Calendar.HOUR_OF_DAY, calendarTime.get(Calendar.HOUR_OF_DAY));
        calendarDate.set(Calendar.MINUTE, calendarTime.get(Calendar.MINUTE));
        calendarDate.set(Calendar.SECOND, 0);
        calendarDate.set(Calendar.MILLISECOND, 0);
        Date finalShowTime = calendarDate.getTime();

        Showtime showtime = new Showtime();
        showtime.setMovieId(selectedMovie.getId());
        showtime.setRoomId(selectedRoom.getId());
        showtime.setShowTime(finalShowTime);

        return showtime;
    }

    private void addShowtime() {
        Showtime showtime = getShowtimeFromForm();
        if (showtime == null) return;

        if (showtime.getShowTime().before(new Date())) {
            JOptionPane.showMessageDialog(this, "Không thể thêm suất chiếu cho thời gian trong quá khứ.", "Lỗi Thời Gian", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (showtimeDAO.addShowtime(showtime)) {
            JOptionPane.showMessageDialog(this, "Thêm suất chiếu thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadShowtimes();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Thêm suất chiếu thất bại. Có lỗi xảy ra (có thể do trùng suất chiếu hoặc ID không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editShowtime() {
        int selectedRow = showtimeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một suất chiếu từ bảng để sửa.", "Chưa chọn suất chiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = showtimeTable.convertRowIndexToModel(selectedRow);
        int showtimeId = (int) tableModel.getValueAt(modelRow, 0);

        Showtime showtimeToUpdate = getShowtimeFromForm();
        if (showtimeToUpdate == null) return;

        showtimeToUpdate.setId(showtimeId);

        if (showtimeDAO.updateShowtime(showtimeToUpdate)) {
            JOptionPane.showMessageDialog(this, "Cập nhật suất chiếu thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadShowtimes();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật suất chiếu thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteShowtime() {
        int selectedRow = showtimeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một suất chiếu từ bảng để xóa.", "Chưa chọn suất chiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = showtimeTable.convertRowIndexToModel(selectedRow);
        int showtimeId = (int) tableModel.getValueAt(modelRow, 0);
        String movieName = String.valueOf(tableModel.getValueAt(modelRow, 1)); // Dùng String.valueOf để tránh NullPointerException
        String roomName = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String time = String.valueOf(tableModel.getValueAt(modelRow, 3));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa suất chiếu:\n" +
                "Phim: " + movieName + "\n" +
                "Phòng: " + roomName + "\n" +
                "Thời gian: " + time + " (ID: " + showtimeId + ") không?\n" +
                "LƯU Ý: Việc này có thể xóa các vé đã đặt cho suất chiếu này.",
                "Xác nhận xóa suất chiếu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (showtimeDAO.deleteShowtime(showtimeId)) {
                JOptionPane.showMessageDialog(this, "Xóa suất chiếu thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                loadShowtimes();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa suất chiếu thất bại. Có lỗi xảy ra.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}