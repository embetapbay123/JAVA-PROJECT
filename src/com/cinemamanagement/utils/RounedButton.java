package com.cinemamanagement.utils;
import javax.swing.*;
import java.awt.*;

public class RounedButton extends JButton {
    private Color bgColor;
    private static final long serialVersionUID = 1L;

    public RounedButton(String text, Color bgColor) {
        super(text);
        this.bgColor = bgColor;
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isRollover() ? bgColor.darker() : bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor.darker());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g2.dispose();
    }
}