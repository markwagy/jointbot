package edu.uvm.mecl.jointbot.drawing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author mwagy
 */
public class ArrowPanel extends JPanel {
    private int h,w;
    private float bodyToArrowLengthRatio = 20f/21f;
    private float bodyToArrowHeightRatio = 1f/3f;
    private int rectBottomLeftX;
    private int rectBottomLeftY;
    private int rectWidth;
    private int rectHeight;
    private int[] arrowX = new int[3];
    private int[] arrowY = new int[3];
    private Color color = Color.black;
    private Color backgroundColor = Color.white;
    private boolean flip = false; // flips arrow direction
    
    public ArrowPanel() {
        h = getHeight();
        w = getWidth();
        this.setPreferredSize(new Dimension(800,200));
        setOpaque(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        setBackground(backgroundColor);
        g2.setColor(color);
        if (flip) {
            flipCoords(g2);
        }
        updatePoints();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRect(rectBottomLeftX, rectBottomLeftY, rectWidth, rectHeight);
        g2.fillPolygon(arrowX, arrowY, arrowX.length);
    }
    
    public void setColor(Color col) {
        backgroundColor = col;
    }
    
    public void flip() {
        flip = true;
    }
    
    public void flipCoords(Graphics2D g2) {
        g2.translate(getWidth(),0);
        g2.scale(-1, 1);
    }
    
    private void updatePoints() {
        h = getHeight();
        w = getWidth();
        rectBottomLeftX = 0;
        rectBottomLeftY = (int) (bodyToArrowHeightRatio*h);
        rectWidth = (int) (bodyToArrowLengthRatio*w);
        rectHeight = (int) (bodyToArrowHeightRatio*h);
        arrowX[0] = (int) (bodyToArrowLengthRatio*w);
        arrowY[0] = 0;
        arrowX[1] = w;
        arrowY[1] = (int) (h/2);
        arrowX[2] = (int) (bodyToArrowLengthRatio*w);
        arrowY[2] = h;
    }
    
    public static void main(String[] args) {
        JFrame jf = new JFrame();
        ArrowPanel ap = new ArrowPanel();
        ap.flip();
        jf.add(ap);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
    }
}
