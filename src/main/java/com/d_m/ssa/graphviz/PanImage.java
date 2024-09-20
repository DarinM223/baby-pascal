package com.d_m.ssa.graphviz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.Serial;

public class PanImage extends JPanel implements MouseListener, MouseMotionListener {
    @Serial
    private static final long serialVersionUID = 686236038272685724L;
    private final BufferedImage image;
    private int x;
    private int y;
    private Integer prevMouseX = null;
    private Integer prevMouseY = null;

    public PanImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, x, y, this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        prevMouseX = null;
        prevMouseY = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (prevMouseX != null && prevMouseY != null) {
            int diffX = e.getX() - prevMouseX;
            int diffY = e.getY() - prevMouseY;
            x += diffX;
            y += diffY;
        }

        prevMouseX = e.getX();
        prevMouseY = e.getY();
        this.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
