package com.cinemamanagement.ui;

import com.cinemamanagement.dao.TicketDAO;
// Giả sử bạn có các lớp tiện ích này
import com.cinemamanagement.utils.RounedButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RevenuePanel extends JPanel {
    private JTextField startDateField;
    private JTextField endDateField;
    private JButton viewRevenueButton;
    private JButton todayButton, thisWeekButton, thisMonthButton;
    private JLabel totalRevenueLabel;
    private JLabel dateRangeLabel; // Hiển thị khoảng ngày đã chọn

    private TicketDAO ticketDAO;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public RevenuePanel() {
        ticketDAO = new TicketDAO();
        dateFormat.setLenient(false); // Parse ngày tháng nghiêm ngặt

        setLayout(new BorderLayout(10, 20)); // Tăng khoảng cách dọc
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding lớn hơn

        initComponents();
        // Đặt ngày mặc định là ngày hôm nay khi mở panel
        setDefaultDateRangeToToday();
    }

    private void initComponents() {
        // Panel chọn ngày và các nút chọn nhanh
        JPanel dateSelectionPanel = new JPanel(new GridBagLayout());
        dateSelectionPanel.setBorder(BorderFactory.createTitledBorder("Chọn Khoảng Thời Gian"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Ngày bắt đầu
        gbc.gridx = 0; gbc.gridy = 0; dateSelectionPanel.add(new JLabel("Từ ngày (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        startDateField = new JTextField(10);
        dateSelectionPanel.add(startDateField, gbc);

        // Ngày kết thúc
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; // Reset fill
        dateSelectionPanel.add(new JLabel("Đến ngày (dd/MM/yyyy):"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        endDateField = new JTextField(10);
        dateSelectionPanel.add(endDateField, gbc);
        gbc.weightx = 0; // Reset weightx

        // Nút Xem Doanh Thu
        viewRevenueButton = new RounedButton("Xem Doanh Thu", new Color(0, 123, 255));
        gbc.gridx = 4; gbc.gridy = 0; gbc.gridheight = 2; gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(5, 15, 5, 5); // Thêm khoảng cách trái
        dateSelectionPanel.add(viewRevenueButton, gbc);
        gbc.gridheight = 1; gbc.fill = GridBagConstraints.HORIZONTAL; // Reset
        gbc.insets = new Insets(5, 5, 5, 5);


        // Các nút chọn nhanh
        JPanel quickSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        todayButton = new RounedButton("Hôm Nay", new Color(23, 162, 184));
        thisWeekButton = new RounedButton("Tuần Này", new Color(40, 167, 69));
        thisMonthButton = new RounedButton("Tháng Này", new Color(255, 193, 7));
        quickSelectPanel.add(todayButton);
        quickSelectPanel.add(thisWeekButton);
        quickSelectPanel.add(thisMonthButton);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        dateSelectionPanel.add(quickSelectPanel, gbc);
        gbc.gridwidth = 1; // Reset gridwidth


        // Panel hiển thị kết quả
        JPanel resultPanel = new JPanel(new BorderLayout(5,5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Kết Quả Doanh Thu"));
        resultPanel.setPreferredSize(new Dimension(400, 100)); // Kích thước cho panel kết quả

        dateRangeLabel = new JLabel("Doanh thu cho: [Chưa chọn khoảng ngày]", SwingConstants.CENTER);
        dateRangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultPanel.add(dateRangeLabel, BorderLayout.NORTH);
        
        totalRevenueLabel = new JLabel(currencyFormatter.format(0), SwingConstants.CENTER);
        totalRevenueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalRevenueLabel.setForeground(new Color(0, 100, 0)); // Màu xanh lá đậm
        resultPanel.add(totalRevenueLabel, BorderLayout.CENTER);


        // Thêm các panel vào panel chính
        add(dateSelectionPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER); // Đặt ở giữa để dễ nhìn

        // Gán sự kiện
        viewRevenueButton.addActionListener(e -> calculateAndDisplayRevenue());
        todayButton.addActionListener(e -> setDateRangeToToday());
        thisWeekButton.addActionListener(e -> setDateRangeToThisWeek());
        thisMonthButton.addActionListener(e -> setDateRangeToThisMonth());
    }

    private void setDefaultDateRangeToToday() {
        Date today = new Date();
        startDateField.setText(dateFormat.format(today));
        endDateField.setText(dateFormat.format(today));
        calculateAndDisplayRevenue(); // Tự động tính khi mở
    }

    private void setDateRangeToToday() {
        Date today = new Date();
        startDateField.setText(dateFormat.format(today));
        endDateField.setText(dateFormat.format(today));
        calculateAndDisplayRevenue();
    }

    private void setDateRangeToThisWeek() {
        Calendar cal = Calendar.getInstance();
        // Đặt ngày đầu tuần là Thứ Hai (tùy theo Locale, ở Việt Nam thường là Thứ Hai)
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date firstDayOfWeek = cal.getTime();

        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        // Nếu Chủ Nhật là ngày cuối tuần theo cài đặt, nó có thể là của tuần trước.
        // Để chắc chắn, cộng 6 ngày từ ngày đầu tuần.
        Calendar endOfWeekCal = Calendar.getInstance();
        endOfWeekCal.setTime(firstDayOfWeek);
        endOfWeekCal.add(Calendar.DATE, 6);
        Date lastDayOfWeek = endOfWeekCal.getTime();


        startDateField.setText(dateFormat.format(firstDayOfWeek));
        endDateField.setText(dateFormat.format(lastDayOfWeek));
        calculateAndDisplayRevenue();
    }

    private void setDateRangeToThisMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1); // Ngày đầu tiên của tháng hiện tại
        Date firstDayOfMonth = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); // Ngày cuối cùng của tháng
        Date lastDayOfMonth = cal.getTime();

        startDateField.setText(dateFormat.format(firstDayOfMonth));
        endDateField.setText(dateFormat.format(lastDayOfMonth));
        calculateAndDisplayRevenue();
    }

    private void calculateAndDisplayRevenue() {
        String startDateStr = startDateField.getText().trim();
        String endDateStr = endDateField.getText().trim();

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày bắt đầu và ngày kết thúc.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            if (endDate.before(startDate)) {
                JOptionPane.showMessageDialog(this, "Ngày kết thúc không được trước ngày bắt đầu.", "Lỗi Ngày", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Gọi DAO để tính doanh thu
            BigDecimal revenue = ticketDAO.calculateTotalRevenue(startDate, endDate);

            // Hiển thị kết quả
            dateRangeLabel.setText("Doanh thu từ ngày " + dateFormat.format(startDate) + " đến ngày " + dateFormat.format(endDate) + ":");
            totalRevenueLabel.setText(currencyFormatter.format(revenue));
            System.out.println("RevenuePanel: Displaying revenue " + currencyFormatter.format(revenue) + " for " + startDateStr + " - " + endDateStr);


        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Định dạng ngày không hợp lệ. Vui lòng nhập theo dd/MM/yyyy.", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Phương thức này có thể được gọi từ AdminDashboardFrame khi tab được chọn
    public void onTabSelected() {
        System.out.println("RevenuePanel: Tab selected. Setting default date range to today.");
        setDefaultDateRangeToToday(); // Tự động hiển thị doanh thu hôm nay khi tab được chọn
    }
}