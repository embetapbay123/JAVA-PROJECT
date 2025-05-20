// Bởi vì trong Look and Feel mặc định của Java Swing, các tiêu đề bảng không chỉnh màu header được
// do đó cần phải tạo một lớp renderer tùy chỉnh để thay đổi màu sắc của tiêu đề bảng.
package com.cinemamanagement.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomHeaderRenderer extends DefaultTableCellRenderer {
    private Color backgroundColor;
    private Color foregroundColor;

    public CustomHeaderRenderer(Color backgroundColor, Color foregroundColor) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        setHorizontalAlignment(CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBackground(backgroundColor);
        setForeground(foregroundColor);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw right border (vertical line)
        g.setColor(new Color(200, 200, 200)); // Light gray for border
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
    }
}
/*Cách dùng ví dụ: 
movieTable.getTableHeader().setDefaultRenderer(
new CustomHeaderRenderer(new Color(255, 204, 102), new Color(60, 60, 60))
);
*/