package edu.uvm.mecl.jointbot.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 *
 * @author mwagy
 */
public class Circle {

    private final int DEFAULT_RADIUS = 10;
    
    Color color;
    Point center;
    int radius;
    boolean mouseIsOver = false;
    
    /**
     *
     */
    public Circle() {
        center = new Point();
        radius = DEFAULT_RADIUS;
    }
    
    public Circle(Point center, int radius) { 
        this.center = center;
        this.radius = radius;
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1));
        if (mouseIsOver) {
//            g2.setColor(color.brighter());
//            System.out.println("mouse is over me");
            g2.setColor(Color.black);
        }
        g2.setColor(color);
        g2.fillOval(center.x-radius, center.y-radius, 2*radius, 2*radius);

        g2.setColor(Color.black);
        g2.drawOval(center.x-radius, center.y-radius, 2*radius, 2*radius);
    }
    
    public void setColor(Color col) {
        color = col;
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void checkMouseOver(Point mousePoint) {
        mouseIsOver = contains(mousePoint);
    }
    
    public boolean contains(Point p) {
        int xdist = p.x-center.x;
        int ydist = p.y-center.y;
        return Math.sqrt(xdist*xdist + ydist*ydist) <= radius;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }
    
    public void translate(int x, int y) {
        center.x += x;
        center.y += y;
    }
    
}
