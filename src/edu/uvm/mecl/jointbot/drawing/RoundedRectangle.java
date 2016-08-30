package edu.uvm.mecl.jointbot.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 *
 * @author mwagy
 */
public class RoundedRectangle {
    private final int ROUNDING_RADIUS = 10;
    
    private Point center;
    private int width;
    private int height;
    private final Color color = new Color(210,210,210);
    
    public RoundedRectangle() {    }
    
    public void setWidth(int w) {
        this.width = w;
    }
    
    public void setHeight(int h) {
        this.height = h;
    }
    
    public void setCenter(Point center) {
        this.center = center;
    }
    
    public void draw(Graphics2D g2) {
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillRoundRect(center.x - width/2, center.y - height/2, 
                width, height, 
                ROUNDING_RADIUS, ROUNDING_RADIUS);
        g2.setColor(new Color(110,110,110));
        g2.drawRoundRect(center.x - width/2, center.y - height/2,
                width, height,
                ROUNDING_RADIUS, ROUNDING_RADIUS);
    }
}
