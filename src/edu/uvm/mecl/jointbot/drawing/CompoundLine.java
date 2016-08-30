package edu.uvm.mecl.jointbot.drawing;

import edu.uvm.mecl.jointbot.Joint;
import edu.uvm.mecl.jointbot.JointBotInfo;
import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import edu.uvm.mecl.jointbot.JointGroup;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

/**
 *
 * @author mwagy
 */
public class CompoundLine {
    
    private final int DEFAULT_LINE_WIDTH = 10;
    
    private int lineWidth = DEFAULT_LINE_WIDTH;
    JointGroup jointGroup;
    ArrayList<Joint> joints = new ArrayList<Joint>();
    Point startPoint;
    Point currentPoint;
    
    public CompoundLine(Joint joint) {
        currentPoint = joint.getCircle().getCenter();
        joints.add(joint);
        this.jointGroup = joint.getGroup();
    }
    
    @Override
    public String toString() {
        String str = "[";
        for (Joint p : joints) {
            str += p + ", ";
        }
        return str + "]";
    }
    
    public void setLineWidth(int width) {
        lineWidth = width;
    }
    
    public void draw(Graphics2D g2, Point extendToPoint) {
        // we only minimize if this isn't an active drawing
        /*
        if (extendToPoint==null) {
            minimize();
        }
        */
        for (int i=1; i<joints.size(); i++) {
            Joint from = joints.get(i-1);
            Joint to   = joints.get(i);
            g2.setColor(jointGroup.getColor());
            g2.setStroke(new BasicStroke(lineWidth));
            g2.drawLine(from.getCircle().getCenter().x, from.getCircle().getCenter().y,
                    to.getCircle().getCenter().x, to.getCircle().getCenter().y);
        }
    }
    
    /**
     * minimize the length of line with given joints so that we are consistently
     * drawing the same shape with the given group of joints
     * TODO: this needs work
     */
    public void minimize() {
        ArrayList<Joint> betterJointOrder = new ArrayList<Joint>();
        ArrayList<Joint> restJoints = (ArrayList<Joint>) joints.clone();
        Joint prevJoint = getFurthestJointFromSpineJoint();
        betterJointOrder.add(prevJoint);
        restJoints.remove(prevJoint);
        while (!restJoints.isEmpty()) {
            Joint currJoint = getClosestJointTo(prevJoint, restJoints);
            betterJointOrder.add(currJoint);
            restJoints.remove(currJoint);
            prevJoint = currJoint;
        }
        
        joints = betterJointOrder;
    }
    
    private Joint getFurthestJointFromSpineJoint() {
        double furthestDistance = 0.0;
        Joint furthestJoint = null;
        for (Joint j : joints) {
            double currDistance = JointBotInfo.getJointDistance(j.getType(), 
                    Joints.Spine1_Spine2, 100, 100);
            if (currDistance > furthestDistance) {
                furthestJoint = j;
                furthestDistance = currDistance;
            } else if (j.getType().equals(Joints.Spine1_Spine2)) {
                furthestJoint = j;
            }
        }
        return furthestJoint;
    }
    
    private Joint getClosestJointTo(Joint thisJoint, ArrayList<Joint> restJoints) {
        double closestDist = Double.MAX_VALUE;
        Joint closestJoint = null;
        for (Joint currJoint : restJoints) {
            if (currJoint.equals(thisJoint)) {
                continue;
            }
            double currDist = JointBotInfo.getJointDistance(
                    thisJoint.getType(), currJoint.getType(), 100, 100);
            if (currDist < closestDist) {
                closestDist = currDist;
                closestJoint = currJoint;
            }
        }
        return closestJoint;
    }

    void removePoints() {
        joints.clear();
    }
    
    public void addJoint(Joint j) {
        joints.add(j);
        
        // remove from existing group
        JointGroup existingGroup = j.getGroup();
        if (existingGroup != null) {
            existingGroup.remove(j);
        }
        
        j.setGroup(jointGroup);
        jointGroup.add(j);
    }
    
    /*
     * don't mess with joint groups. just add this joint to this compound line
     */
    public void simplyAddJoint(Joint j) {
        joints.add(j);
    }
    
    Iterable<Joint> getJoints() {
        return joints;
    }
    
    public JointGroup getJointGroup() {
        return jointGroup;
    }
    
    public boolean contains(Joint j) {
        for (Joint joint : joints) {
            if (joint.equals(j)) {
                return true;
            }
        }
        return false;
    }

    public void removeJoint(Joint j) {
        /*
        boolean containsJoint = false;
        for (Joint lj : joints) {
            if (lj.equals(j)) {
                containsJoint = true;
            }
        }
        */
        joints.remove(j);
    }
}
