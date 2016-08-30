package edu.uvm.mecl.jointbot.drawing;

import edu.uvm.mecl.jointbot.JointBotInfo;
import edu.uvm.mecl.jointbot.JointBotInfo.Bodies;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 *
 * @author mwagy
 */
public class Body {
    Bodies type;
    RoundedRectangle rect;
    
    public Body(Bodies type) {
        this.type = type;
        rect = new RoundedRectangle();
    }
    
    /*
    public void draw(Graphics2D g2, int canvasWidth, int canvasHeight) {
        Point center = JointBotInfo.getBodyCanvasPosition(type, canvasWidth, canvasHeight);
        int w = JointBotInfo.getBodyCanvasSize(type, JointBotInfo.Dimensions.X, canvasWidth);
        int h = JointBotInfo.getBodyCanvasSize(type, JointBotInfo.Dimensions.Y, canvasHeight);
        rect.setWidth(w);
        rect.setHeight(h);
        rect.setCenter(center);
        rect.draw(g2);
    }
    */
    public void draw(Graphics2D g2, int canvasWidth, int canvasHeight) {
        draw(g2, 0, 0, canvasWidth, canvasHeight);
    }
    
    public void draw(Graphics2D g2, int x, int y, int canvasWidth, int canvasHeight) {
        Point center = JointBotInfo.getBodyCanvasPosition(type, canvasWidth, canvasHeight);
        int w = JointBotInfo.getBodyCanvasSize(type, JointBotInfo.Dimensions.X, canvasWidth);
        int h = JointBotInfo.getBodyCanvasSize(type, JointBotInfo.Dimensions.Y, canvasHeight);
        rect.setWidth(w);
        rect.setHeight(h);
        center.x += x;
        center.y += y;
        rect.setCenter(center);
        rect.draw(g2);
    }
}
