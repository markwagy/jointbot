package edu.uvm.mecl.jointbot.drawing;

import edu.uvm.mecl.jointbot.Joint;
import edu.uvm.mecl.jointbot.JointGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 *
 * @author mwagy
 */
public class JoinLine {

    private final int LINE_WIDTH = 10;
    
//    Point from, to;
//    JointGroup jointGroup;
    
    /*
    public JoinLine(Point from, Point to, JointGroup jg) {
        this.from       = from;
        this.to         = to;
        this.jointGroup = jg;
    }
    * */

    JoinLine() {   }
  /*  
    @Override
    public String toString() {
        return String.format("(%d,%d)--(%d,%d)", from.x, from.y, to.x, to.y);
    }
    */
    
    public void draw(Graphics2D g2, Joint from, Joint to, Color col) {
        g2.setColor(col);
        g2.setStroke(new BasicStroke(LINE_WIDTH));
        g2.drawLine(
                from.getCircle().getCenter().x, from.getCircle().getCenter().y, 
                to.getCircle().getCenter().x, to.getCircle().getCenter().y);
    }
    /*
    public void set(Point from, Point to) {
        this.to = to;
        this.from = from;
    }
    
    public Point getFrom() {
        return from;
    }
    
    public Point getTo() {
        return to;
    }
    * */
}
