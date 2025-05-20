package com.cinemamanagement.ui;

import com.cinemamanagement.dao.MovieDAO;
import com.cinemamanagement.model.Movie;
import com.cinemamanagement.utils.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter; // Để sắp xếp bảng
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date; // java.util.Date

public class MovieManagementPanel extends JPanel {
    private JTable movieTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton, refreshButton, clearSearchButton;
    private JTextField searchField;
    private JComboBox<String> searchTypeComboBox;

    private JTextField titleField, genreField, durationField, posterUrlField, descriptionField;
    private JTextField releaseDateField; // Sử dụng JTextField để nhập ngày
    
    private MovieDAO movieDAO;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public MovieManagementPanel() {
        movieDAO = new MovieDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Cấu hình cho SimpleDateFormat để parse ngày tháng nghiêm ngặt
        dateFormat.setLenient(false);

        initComponents();
        loadMovies();
    }

    private void initComponents() {
        // Panel tìm kiếm và các nút điều khiển chính
        JPanel topControlsPanel = new JPanel(new BorderLayout(10, 5));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("Tìm theo:"));
        searchTypeComboBox = new JComboBox<>(new String[]{"Tên phim", "Thể loại"});
        searchPanel.add(searchTypeComboBox);
        searchField = new JTextField(20);
        searchPanel.add(searchField);
        searchButton = new RounedButton("Tìm kiếm",  new Color(156, 39, 176));
        searchPanel.add(searchButton);
        clearSearchButton = new RounedButton("Xóa tìm",  new Color(121, 85, 72));
        searchPanel.add(clearSearchButton);
        
        topControlsPanel.add(searchPanel, BorderLayout.WEST);

        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        addButton = new RounedButton("Thêm Mới", new Color(76, 175, 80)); 
        editButton = new RounedButton("Sửa",new Color(33, 150, 243)); 
        deleteButton = new RounedButton("Xóa", new Color(244, 67, 54));
        refreshButton = new RounedButton("Làm Mới Bảng", new Color(255, 193, 7));
        mainButtonsPanel.add(addButton);
        mainButtonsPanel.add(editButton);
        mainButtonsPanel.add(deleteButton);
        mainButtonsPanel.add(refreshButton);
        topControlsPanel.add(mainButtonsPanel, BorderLayout.EAST);

        add(topControlsPanel, BorderLayout.NORTH);

        // Bảng hiển thị danh sách phim
        String[] columnNames = {"ID", "Tên phim", "Thể loại", "Thời lượng (phút)", "Ngày phát hành", "Mô tả", "Poster URL"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movieTable = new JTable(tableModel);
        movieTable.setBackground(new Color(255, 255, 240)); // Very light yellow
        movieTable.setSelectionBackground(new Color(255, 153, 102)); // Light orange for selected row
        movieTable.setSelectionForeground(Color.WHITE);
        movieTable.setPreferredScrollableViewportSize(new Dimension(800, 300));
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        movieTable.setRowSorter(sorter);
        movieTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movieTable.getTableHeader().setReorderingAllowed(false);
        movieTable.getTableHeader().setDefaultRenderer(
        	    new CustomHeaderRenderer(new Color(173, 216, 230), new Color(60, 60, 60))
        	);
        movieTable.getTableHeader().setPreferredSize(new Dimension(movieTable.getTableHeader().getWidth(), 30));
        movieTable.getTableHeader().setBackground(new Color(255, 204, 102)); // Light orange
        movieTable.getTableHeader().setForeground(new Color(60, 60, 60));    // Dark text
        movieTable.setRowHeight(25);

        movieTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int selectedRow = movieTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int modelRow = movieTable.convertRowIndexToModel(selectedRow);
                        populateFormFromTable(modelRow);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(movieTable);
        add(scrollPane, BorderLayout.CENTER);

        // Panel form để thêm/sửa phim
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin chi tiết phim"));
        formPanel.setBackground(new Color(204, 229, 255)); // Light yellow
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Các trường trong form
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Tên phim:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; titleField = new JTextField(30); formPanel.add(titleField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; formPanel.add(new JLabel("Thể loại:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0; genreField = new JTextField(20); formPanel.add(genreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Thời lượng (phút):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; durationField = new JTextField(); formPanel.add(durationField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; formPanel.add(new JLabel("Ngày phát hành (dd/MM/yyyy):"), gbc); // Label đã sửa
        gbc.gridx = 3; gbc.gridy = 1; 
        releaseDateField = new JTextField(10); // Sử dụng JTextField
        formPanel.add(releaseDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Poster URL:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; posterUrlField = new JTextField(); formPanel.add(posterUrlField, gbc);
        gbc.gridwidth = 1; 

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3;
        descriptionField = new JTextField(); 
        formPanel.add(descriptionField, gbc);
        gbc.gridwidth = 1;

        JButton clearFormButton =  new RounedButton("Xóa", new Color(244, 67, 54));
        gbc.gridx = 3; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(clearFormButton, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // Action Listeners
        addButton.addActionListener(e -> addMovie());
        editButton.addActionListener(e -> editMovie());
        deleteButton.addActionListener(e -> deleteMovie());
        searchButton.addActionListener(e -> searchMovies());
        refreshButton.addActionListener(e -> {
            searchField.setText(""); 
            loadMovies(); 
            clearForm(); 
        });
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadMovies();
        });
        clearFormButton.addActionListener(e -> clearForm());
    }

    private void loadMovies() {
        tableModel.setRowCount(0); 
        List<Movie> movies = movieDAO.getAllMovies();
        for (Movie movie : movies) {
            tableModel.addRow(new Object[]{
                    movie.getId(),
                    movie.getTitle(),
                    movie.getGenre(),
                    movie.getDuration(),
                    movie.getReleaseDate() != null ? dateFormat.format(movie.getReleaseDate()) : "",
                    movie.getDescription(),
                    movie.getPosterUrl()
            });
        }
    }

    private void searchMovies() {
        String searchTerm = searchField.getText().trim();
        String searchType = (String) searchTypeComboBox.getSelectedItem();
        
        if (searchTerm.isEmpty()) {
            loadMovies(); 
            return;
        }

        tableModel.setRowCount(0);
        List<Movie> movies;
        if ("Tên phim".equals(searchType)) {
            movies = movieDAO.searchMoviesByTitle(searchTerm);
        } else { 
            movies = movieDAO.searchMoviesByGenre(searchTerm);
        }

        for (Movie movie : movies) {
            tableModel.addRow(new Object[]{
                    movie.getId(),
                    movie.getTitle(),
                    movie.getGenre(),
                    movie.getDuration(),
                    movie.getReleaseDate() != null ? dateFormat.format(movie.getReleaseDate()) : "",
                    movie.getDescription(),
                    movie.getPosterUrl()
            });
        }
        if (movies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy phim nào khớp.", "Thông báo tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void populateFormFromTable(int modelRow) {
        int movieId = (int) tableModel.getValueAt(modelRow, 0); 
        Movie movie = movieDAO.getMovieById(movieId); 

        if (movie != null) {
            titleField.setText(movie.getTitle());
            genreField.setText(movie.getGenre());
            durationField.setText(String.valueOf(movie.getDuration()));
            if (movie.getReleaseDate() != null) {
                releaseDateField.setText(dateFormat.format(movie.getReleaseDate())); // Hiển thị ngày đã định dạng
            } else {
                releaseDateField.setText("");
            }
            posterUrlField.setText(movie.getPosterUrl());
            descriptionField.setText(movie.getDescription());
        }
    }
    
    private void clearForm() {
        titleField.setText("");
        genreField.setText("");
        durationField.setText("");
        releaseDateField.setText(""); // Xóa JTextField ngày
        posterUrlField.setText("");
        descriptionField.setText("");
        movieTable.clearSelection(); 
        titleField.requestFocusInWindow();
    }


    private Movie getMovieFromForm() {
        String title = titleField.getText().trim();
        String genre = genreField.getText().trim();
        String durationStr = durationField.getText().trim();
        String releaseDateStr = releaseDateField.getText().trim(); // Lấy chuỗi ngày từ JTextField
        String posterUrl = posterUrlField.getText().trim();
        String description = descriptionField.getText().trim();

        if (title.isEmpty() || genre.isEmpty() || durationStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên phim, thể loại và thời lượng không được để trống.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) throw new NumberFormatException("Thời lượng phải > 0");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Thời lượng phải là một số nguyên dương.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            durationField.requestFocusInWindow();
            return null;
        }
        
        Date releaseDate = null;
        if (!releaseDateStr.isEmpty()) {
            try {
                // dateFormat.setLenient(false); // Đã set ở constructor
                releaseDate = dateFormat.parse(releaseDateStr);
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this,
                        "Ngày phát hành không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.",
                        "Lỗi Định Dạng Ngày", JOptionPane.ERROR_MESSAGE);
                releaseDateField.requestFocusInWindow();
                return null;
            }
        }
        
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setDuration(duration);
        movie.setReleaseDate(releaseDate);
        movie.setPosterUrl(posterUrl);
        movie.setDescription(description);
        
        return movie;
    }

    private void addMovie() {
        Movie movie = getMovieFromForm();
        if (movie == null) return; 

        if (movieDAO.addMovie(movie)) {
            JOptionPane.showMessageDialog(this, "Thêm phim thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadMovies(); 
            clearForm();  
        } else {
            JOptionPane.showMessageDialog(this, "Thêm phim thất bại. Có lỗi xảy ra.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editMovie() {
        int selectedRow = movieTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phim từ bảng để sửa.", "Chưa chọn phim", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = movieTable.convertRowIndexToModel(selectedRow);
        int movieId = (int) tableModel.getValueAt(modelRow, 0);

        Movie movieToUpdate = getMovieFromForm();
        if (movieToUpdate == null) return;

        movieToUpdate.setId(movieId); 

        if (movieDAO.updateMovie(movieToUpdate)) {
            JOptionPane.showMessageDialog(this, "Cập nhật thông tin phim thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadMovies();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật phim thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMovie() {
        int selectedRow = movieTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phim từ bảng để xóa.", "Chưa chọn phim", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = movieTable.convertRowIndexToModel(selectedRow);
        int movieId = (int) tableModel.getValueAt(modelRow, 0);
        String movieTitle = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa phim '" + movieTitle + "' (ID: " + movieId + ") không?\n" +
                "LƯU Ý: Việc này có thể xóa các suất chiếu và vé liên quan.",
                "Xác nhận xóa phim",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (movieDAO.deleteMovie(movieId)) {
                JOptionPane.showMessageDialog(this, "Xóa phim thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                loadMovies();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa phim thất bại. Phim có thể đang được tham chiếu hoặc có lỗi xảy ra.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    
    
}