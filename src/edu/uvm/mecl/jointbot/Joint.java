package edu.uvm.mecl.jointbot;

import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import edu.uvm.mecl.jointbot.drawing.Circle;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 *
 * @author mwagy
 */
public class Joint implements Comparable {

    private final float DEFAULT_RADIUS_PERC = 0.03f;
    
    private float drawRadiusPerc = DEFAULT_RADIUS_PERC;
    private Joints type;
    private Circle circle;
    private JointGroup group;
    
    public Joint(JointBotInfo.Joints jointType) {
        this.type  = jointType;
        this.circle = new edu.uvm.mecl.jointbot.drawing.Circle();
    }
    
    @Override
    public Joint clone() {
        Joint newJ = new Joint(type);
        newJ.setGroup(group);
        newJ.setCircle(circle);
        return newJ;
    }
    
    public void setCircle(Circle c) {
        circle = c;
    }
    
    @Override
    public String toString() {
        String s = JointBotInfo.getJointString(type);
        return s;
    }
    
    public void setGroup(JointGroup jg) {
        this.group = jg;
    }
    
    public void setDrawRadiusPerc(float perc) {
        drawRadiusPerc = perc;
    }
    
    /*
    public void draw(Graphics2D g2, int canvasWidth, int canvasHeight) {
        circle.setCenter(
                JointBotInfo.getJointCanvasPosition(type,canvasWidth,canvasHeight));
        circle.setRadius((int) (canvasHeight*drawRadiusPerc));
        circle.setColor(group.getColor());
        circle.draw(g2);
    }
    */
    public void draw(Graphics2D g2, int canvasWidth, int canvasHeight) {
        draw(g2,0,0,canvasWidth,canvasHeight);
    }
    
    public void draw(Graphics2D g2, int x, int y, int canvasWidth, int canvasHeight) {
        circle.setCenter(
                JointBotInfo.getJointCanvasPosition(type,canvasWidth,canvasHeight));
        circle.translate(x,y);
        circle.setRadius((int) (canvasHeight*drawRadiusPerc));
        circle.setColor(group.getColor());
        circle.draw(g2);
    }
        
    public boolean isActive(Point mousePoint) {
        return circle.contains(mousePoint);
    }
    
    public JointGroup getGroup() {
        return group;
    }

    public Circle getCircle() {
        return circle;
    }
    
    public Joints getType() {
        return type;
    }

    /**
     * order by joint type in the order of the JointInfo type list
     * @param t
     * @return 
     */
    @Override
    public int compareTo(Object t) {
        Joint other = (Joint) t;
        int otherIdx = getJointTypeIndex(other.getType());
        int myIdx    = getJointTypeIndex(type);
        if (otherIdx == myIdx) {
            return 0;
        } else if (otherIdx < myIdx) {
            return 1;
        } else {
            return -1;
        }
    }
    
    private int getJointTypeIndex(Joints typeVal) {
        for (int i=0; i<Joints.Count.ordinal(); i++) {
            if (typeVal==Joints.values()[i]) {
                return i;
            }
        }
        return -1;
    }
}
