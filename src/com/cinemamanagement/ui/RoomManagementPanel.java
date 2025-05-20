package com.cinemamanagement.ui;

import com.cinemamanagement.dao.RoomDAO;
import com.cinemamanagement.dao.SeatDAO; // Có thể cần để xử lý ghế khi thay đổi số lượng
import com.cinemamanagement.model.Room;
import com.cinemamanagement.utils.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class RoomManagementPanel extends JPanel {
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, refreshButton, clearFormButton;
    private JTextField nameField, seatCountField; // Trường cho form

    private RoomDAO roomDAO;
    // private SeatDAO seatDAO; // Nếu cần xử lý logic ghế phức tạp khi cập nhật phòng

    public RoomManagementPanel() {
        roomDAO = new RoomDAO();
        // seatDAO = new SeatDAO(); 
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadRooms();
    }

    private void initComponents() {
        // Panel các nút điều khiển chính (không có tìm kiếm cho Room ở ví dụ này)
        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5)); // Căn trái
        addButton = new RounedButton("Thêm Mới Phòng", new Color(76, 175, 80)); 
        editButton = new RounedButton("Sửa Thông Tin",new Color(33, 150, 243));
        deleteButton = new RounedButton("Xóa Phòng", new Color(244, 67, 54));
        refreshButton = new RounedButton("Làm Mới Bảng", new Color(255, 193, 7));
        mainButtonsPanel.add(addButton);
        mainButtonsPanel.add(editButton);
        mainButtonsPanel.add(deleteButton);
        mainButtonsPanel.add(refreshButton);
        
        add(mainButtonsPanel, BorderLayout.NORTH);

        // Bảng hiển thị danh sách phòng
        String[] columnNames = {"ID Phòng", "Tên Phòng", "Số Lượng Ghế"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setBackground(new Color(255, 255, 240)); // Very light yellow
        roomTable.setSelectionBackground(new Color(255, 153, 102)); // Light orange for selected row
        roomTable.setSelectionForeground(Color.WHITE);      
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        roomTable.setRowSorter(sorter);   
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setPreferredScrollableViewportSize(new Dimension(800, 300));
        roomTable.setRowHeight(25);
        roomTable.getTableHeader().setPreferredSize(new Dimension(roomTable.getTableHeader().getWidth(), 30));
        roomTable.getTableHeader().setReorderingAllowed(false);
        roomTable.getTableHeader().setDefaultRenderer(
        	    new CustomHeaderRenderer(new Color(173, 216, 230), new Color(60, 60, 60))
        	);
        roomTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int selectedRow = roomTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int modelRow = roomTable.convertRowIndexToModel(selectedRow);
                        populateFormFromTable(modelRow);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(roomTable);
        add(scrollPane, BorderLayout.CENTER);

        // Panel form để thêm/sửa phòng
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin chi tiết phòng chiếu"));
        formPanel.setBackground(new Color(204, 229, 255)); // Light blue
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Tên Phòng:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; nameField = new JTextField(20); formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; formPanel.add(new JLabel("Số Lượng Ghế:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; seatCountField = new JTextField(10); formPanel.add(seatCountField, gbc);

        clearFormButton =  new RounedButton("Xóa Form", new Color(244, 67, 54));
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0; // Reset weight
        formPanel.add(clearFormButton, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // Action Listeners
        addButton.addActionListener(e -> addRoom());
        editButton.addActionListener(e -> editRoom());
        deleteButton.addActionListener(e -> deleteRoom());
        refreshButton.addActionListener(e -> {
            loadRooms();
            clearForm();
        });
        clearFormButton.addActionListener(e -> clearForm());
    }

    private void loadRooms() {
        tableModel.setRowCount(0);
        List<Room> rooms = roomDAO.getAllRooms();
        for (Room room : rooms) {
            tableModel.addRow(new Object[]{
                    room.getId(),
                    room.getName(),
                    room.getSeatCount()
            });
        }
    }

    private void populateFormFromTable(int modelRow) {
        int roomId = (int) tableModel.getValueAt(modelRow, 0);
        Room room = roomDAO.getRoomById(roomId);

        if (room != null) {
            nameField.setText(room.getName());
            seatCountField.setText(String.valueOf(room.getSeatCount()));
        }
    }

    private void clearForm() {
        nameField.setText("");
        seatCountField.setText("");
        roomTable.clearSelection();
        nameField.requestFocusInWindow();
    }

    private Room getRoomFromForm() {
        String name = nameField.getText().trim();
        String seatCountStr = seatCountField.getText().trim();

        if (name.isEmpty() || seatCountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên phòng và số lượng ghế không được để trống.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        int seatCount;
        try {
            seatCount = Integer.parseInt(seatCountStr);
            if (seatCount <= 0) throw new NumberFormatException("Số ghế phải > 0");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng ghế phải là một số nguyên dương.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            seatCountField.requestFocusInWindow();
            return null;
        }
        
        Room room = new Room();
        room.setName(name);
        room.setSeatCount(seatCount);
        return room;
    }

    private void addRoom() {
        Room room = getRoomFromForm();
        if (room == null) return;

        if (roomDAO.addRoom(room)) {
            JOptionPane.showMessageDialog(this, "Thêm phòng chiếu thành công! Ghế đã được tự động tạo.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadRooms();
            clearForm();
        } else {
            // RoomDAO đã in lỗi cụ thể (vd: tên phòng trùng)
            JOptionPane.showMessageDialog(this, "Thêm phòng chiếu thất bại. Tên phòng có thể đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng từ bảng để sửa.", "Chưa chọn phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = roomTable.convertRowIndexToModel(selectedRow);
        int roomId = (int) tableModel.getValueAt(modelRow, 0);

        Room roomToUpdate = getRoomFromForm();
        if (roomToUpdate == null) return;

        roomToUpdate.setId(roomId);

        // Lấy thông tin phòng cũ để kiểm tra thay đổi số ghế
        Room oldRoom = roomDAO.getRoomById(roomId);
        if (oldRoom == null) {
             JOptionPane.showMessageDialog(this, "Không tìm thấy phòng để cập nhật.", "Lỗi", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // Thông báo nếu số ghế thay đổi (logic xóa/tạo ghế đã nằm trong RoomDAO.updateRoom)
        if (oldRoom.getSeatCount() != roomToUpdate.getSeatCount()) {
            int confirmChangeSeats = JOptionPane.showConfirmDialog(this,
                    "Số lượng ghế đã thay đổi. Các ghế cũ sẽ bị xóa và ghế mới sẽ được tạo.\n" +
                    "LƯU Ý: Điều này có thể ảnh hưởng đến các suất chiếu và vé đã đặt cho các ghế không còn tồn tại.\n" +
                    "Bạn có chắc chắn muốn tiếp tục?",
                    "Xác nhận thay đổi số ghế",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmChangeSeats != JOptionPane.YES_OPTION) {
                // Khôi phục lại giá trị seatCount cũ vào form nếu người dùng hủy
                seatCountField.setText(String.valueOf(oldRoom.getSeatCount()));
                return;
            }
        }


        if (roomDAO.updateRoom(roomToUpdate)) {
            JOptionPane.showMessageDialog(this, "Cập nhật thông tin phòng thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadRooms();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật phòng thất bại. Tên phòng có thể đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng từ bảng để xóa.", "Chưa chọn phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = roomTable.convertRowIndexToModel(selectedRow);
        int roomId = (int) tableModel.getValueAt(modelRow, 0);
        String roomName = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa phòng '" + roomName + "' (ID: " + roomId + ") không?\n" +
                "LƯU Ý: Việc này sẽ xóa TẤT CẢ ghế, suất chiếu và vé liên quan đến phòng này.",
                "Xác nhận xóa phòng",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (roomDAO.deleteRoom(roomId)) {
                JOptionPane.showMessageDialog(this, "Xóa phòng thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                loadRooms();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa phòng thất bại. Có lỗi xảy ra.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}