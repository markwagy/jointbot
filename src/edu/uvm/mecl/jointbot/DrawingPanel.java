package edu.uvm.mecl.jointbot;

import edu.uvm.mecl.jointbot.JointBotInfo.Bodies;
import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import edu.uvm.mecl.jointbot.drawing.Body;
import edu.uvm.mecl.jointbot.drawing.CompoundLine;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

/**
 *
 * @author mwagy
 */
public class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener {

    private int dimX, dimY;
    private boolean drawCoords = false;
    
    protected final int CIRCLE_MARGIN = 3;
    
    protected ArrayList<JointGroup> jointGroups = new ArrayList<JointGroup>();
//    protected Circle[] jointCircles = new Circle[JointBotInfo.Joints.Count.ordinal()];
    protected Joint[] joints = new Joint[JointBotInfo.Joints.Count.ordinal()];
    /*
    protected RoundedRectangle[] bodyRects 
            = new RoundedRectangle[JointBot.Bodies.Count.ordinal()];
    */
    protected Body[] bodies = new Body[JointBotInfo.Bodies.Count.ordinal()];
                
    public static Color[] colors = {
        new Color(150,150,255), Color.GREEN, Color.ORANGE,
        Color.PINK, Color.RED, Color.YELLOW,
        Color.GRAY, Color.CYAN, Color.MAGENTA};
        
    private static ArrayList<Float> phaseAngles = new ArrayList<Float>();
    
    private static boolean[] colorInUse = new boolean[Joints.Count.ordinal()];
    
    
    ArrayList<CompoundLine> compoundLines = new ArrayList<CompoundLine>();
    int currentLineIdx = 0;
    CompoundLine currentLine;
    
    Point currentPoint;
    Point activePoint; // point at which the last mouse activity took place (click or drag over active joint)
    
    JointGroup selectedGroup;
    Color currentLineColor = Color.lightGray;
    boolean drawing = false;
    boolean mouseSwitch = false;
    
    ArrayList<Joint> alreadyAddedJoints = new ArrayList<Joint>();
    
    public DrawingPanel() {
        this(500,500);
    }
    
    public DrawingPanel(int dimX, int dimY) {
        this.dimX = dimX;
        this.dimY = dimY;
        setPreferredSize(new Dimension(dimX, dimY));
        joints = new Joint[JointBotInfo.Joints.Count.ordinal()];
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        initColorUsage();
        initJoints();
        initJointGroups();
        initBodies();
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        setBorder(BorderFactory.createLineBorder(Color.lightGray));
        setBackground(Color.white);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    public void reset() {
        initColorUsage();
        initJoints();
        initJointGroups();
        initBodies();
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        compoundLines.clear();
        repaint();
    }
    
    public void initColorUsage() {
        for (int i=0; i<colorInUse.length; i++) {
            colorInUse[i] = false;
        }
    }
    
    public Color claimAvailableColor() {
        for (int i=0; i<colors.length; i++) {
            if (!colorInUse[i]) {
                colorInUse[i] = true;
                return colors[i];
            }
        }
        return null;
    }
        
    public void initJointGroups() {
        jointGroups.clear();
        for (int i=0; i<Joints.Count.ordinal(); i++) {
            Color col = claimAvailableColor();
            JointGroup jg = new JointGroup(col);
            jg.add(joints[i]);
            jointGroups.add(jg);
            joints[i].setGroup(jg);
        }
    }
    
    private void initJoints() {
        for (int i=0; i<Joints.Count.ordinal(); i++) {
            Point center = JointBotInfo.getJointCanvasPosition(Joints.values()[i], dimX, dimY);
            joints[i] = new Joint(Joints.values()[i]);
        }
    }
    
    private void initBodies() {
        for (int i=0; i<JointBotInfo.Bodies.Count.ordinal(); i++) {
            bodies[i] = new Body(Bodies.values()[i]);
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (drawCoords) {
            drawCoords(g2);
        }
        updateGroupPhases();
        drawBodies(g2);
        
        for (CompoundLine cl : compoundLines) {
            cl.draw(g2, currentPoint);
        }
        for (Joint joint : joints) {
            joint.draw(g2, getWidth(), getHeight());
        }
        if (drawing) {
            drawExtendLine(g2);
        }
    }
    
    private void drawBodies(Graphics2D g2) {
        for (Body body : bodies) {
            body.draw(g2, getWidth(), getHeight());
        }

    }
    
    private void drawCoords(Graphics2D g2) {
        g2.setColor(new Color(200,200,200));
        g2.drawLine(percToX(.5), percToY(0), percToX(.5), percToY(1));
        g2.drawLine(percToX(0), percToY(.5), percToX(1), percToY(.5));
    }

    private int percToX(double perc) {
        return (int) (perc*dimX);
    }
    
    private int percToY(double perc) {
        return (int) (perc*dimY);
    }
    
    public ArrayList<JointGroup> getJointGroups() {
        return jointGroups;
    }
        
    protected static void unclaimColor(Color col) {
        for (int i=0; i<colors.length; i++) {
            if (colors[i].equals(col)) {
                colorInUse[i] = false;
                return;
            }
        }
    }
        
    public JointGroup splitJointGroup(JointGroup jointGroup) {
        JointGroup newGroup = new JointGroup(claimAvailableColor());
        ArrayList<Joint> currJoints = jointGroup.getJointsInGroup();
        Collections.shuffle(currJoints);
        int half = (int) (currJoints.size()/2);
        for (int i=0; i<half; i++) {
            newGroup.add(currJoints.get(i));
            currJoints.remove(i);
        }
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        return newGroup;
    }
    
    public void mergeJointGroups(JointGroup jg1, JointGroup jg2) {
        jg1.addAll(jg2.getAll());
        jointGroups.remove(jg2);
        unclaimColor(jg2.getColor());
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
    }
    
    public float getGroupPhase(JointGroup jg) {
        updateGroupPhases();
        for (int i=0; i<jointGroups.size(); i++) {
            if (jg.equals(jointGroups.get(i))) {
                return phaseAngles.get(i);
            }
        }
        return 0.0f;
    }
    
    public void updateGroupPhases() {
        for (int i=0; i<jointGroups.size(); i++) {
            (jointGroups.get(i)).setPhase(phaseAngles.get(i));
        }
    }
    
    protected void addNewJointGroup(Joint j) {
        JointGroup newGroup = new JointGroup(claimAvailableColor());
        newGroup.add(j);
        j.setGroup(newGroup);
        jointGroups.add(newGroup);
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        updateGroupPhases();
    }

    public void setPhases(ArrayList<Float> phases) {
        phaseAngles = phases;
    }
    
    public void drawExtendLine(Graphics2D g2) {
        if (currentLine != null) {
            g2.setColor(currentLine.getJointGroup().getColor());
        } else {
            g2.setColor(Color.lightGray);
        }
        float[] dashPattern = {30,10,10,10};
	g2.setStroke(
                new BasicStroke(8,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,4,dashPattern,0));
        g2.drawLine(
                activePoint.x, activePoint.y, 
                currentPoint.x, currentPoint.y);
    }
    
    private CompoundLine getExistingCompoundLine(Joint joint) {
        for (CompoundLine line : compoundLines) {
            if (line.contains(joint)) {
                return line;
            }
        }
        return null;
    }
      
    @Override
    public void mouseClicked(MouseEvent me) {
        if (me.getButton()==MouseEvent.BUTTON3) {
            ArrayList<Joint> activeJoints = getActiveJoints(me.getPoint());
            if (!activeJoints.isEmpty()) {
                Joint active = activeJoints.get(0);
                 for (CompoundLine cL : compoundLines) {
                     if (cL.contains(active)) {
                         cL.removeJoint(active);
                         removeJointFromJointGroup(active);
                         addNewJointGroup(active);
                     }
                 }
            }
            cleanJointGroups();
            System.err.println(JointGroup.summarizeJointGroups(jointGroups));
            repaint();
        }
    }

    private ArrayList<Joint> getActiveJoints(Point mousePoint) {
        ArrayList<Joint> activeJoints = new ArrayList<Joint>();
        for (Joint joint : joints) {
            if (joint.isActive(mousePoint)) {
                activeJoints.add(joint);
            }
        }
        return activeJoints;
    }
    
    private void removeJointFromJointGroup(Joint joint) {
        for (JointGroup jg : jointGroups) {
            if (jg.containsJoint(joint)) {
                jg.remove(joint);
                return;
            }
        }
    }
        
    @Override
    public void mousePressed(MouseEvent me) {
        currentPoint = me.getPoint();
        ArrayList<Joint> activeJoints = getActiveJoints(currentPoint);
        if (!activeJoints.isEmpty()) {

            drawing = true;
            
            // assuming that we only have one joint here (can't click on multiple)
            // but keeping with structure that allows for multiple for now
            Joint activeJoint = activeJoints.get(0);
            
            // check if this joint alread belongs to a compound line
            CompoundLine existingCompoundLine = getExistingCompoundLine(activeJoint);
            if (existingCompoundLine==null) {
                currentLine = new CompoundLine(activeJoint);
            } else {
                currentLine = existingCompoundLine;
            }
            
            alreadyAddedJoints.add(activeJoint);
            activePoint = activeJoint.getCircle().getCenter();
            compoundLines.add(currentLine);
        }
        repaint();
    }
        
    @Override
    public void mouseDragged(MouseEvent me) {
        currentPoint = me.getPoint();
        ArrayList<Joint> activeJoints = getActiveJoints(currentPoint);
        if (!activeJoints.isEmpty()) {
            Joint currentActiveJoint = activeJoints.get(0);
            if (!alreadyAddedJoints.contains(currentActiveJoint)) {
                Joint currentJoint = activeJoints.get(0);
                removeJointFromOtherLines(currentJoint);
                currentLine.addJoint(currentJoint);

                alreadyAddedJoints.add(activeJoints.get(0));
                activePoint = activeJoints.get(0).getCircle().getCenter();
            }
        }
        repaint();
    }
    
    @Override
    public void mouseReleased(MouseEvent me) {
        ArrayList<Joint> activeJoints = getActiveJoints(me.getPoint());
        if (activeJoints.isEmpty() && 
                drawing && compoundLines != null) {
            // no active endpoint, so scrap this line
            compoundLines.remove(compoundLines.get(compoundLines.size()-1));
            currentLine = null;
        } else if (currentLine != null && activeJoints.size()>0) {
            // we have successfully added a joint group by drawing a line
            currentLine.addJoint(activeJoints.get(0));
        }
        alreadyAddedJoints.clear();
        drawing = false;
        repaint();
        //summarizeJointGroups();
        // get rid of empty groups, etc.
        cleanJointGroups();
        System.err.println(JointGroup.summarizeJointGroups(jointGroups));
        /*
        Application.writeToDB(new JointConfig(jointGroups, 
                JointGroup.getRandomAngles(jointGroups.size())), 0, "F");
                */
    }
    
    private void cleanJointGroups() {
        ArrayList<JointGroup> emptyGroups = new ArrayList<JointGroup>();
        for (JointGroup jg : jointGroups) {
            if (jg.isEmpty()) {
                emptyGroups.add(jg);
            }
        }
        for (JointGroup emptyGroup : emptyGroups) {
            jointGroups.remove(emptyGroup);
            unclaimColor(emptyGroup.getColor());
        }
    }
    
    private void removeJointFromOtherLines(Joint j) {
        if (compoundLines != null) {
            for (CompoundLine line : compoundLines) {
                line.removeJoint(j);
            }
        }
    }

    public void mouseEntered(MouseEvent me) {    }

    public void mouseExited(MouseEvent me) {    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600,600);
    }
    
    public void mouseMoved(MouseEvent me) {
        repaint();
    }
    
    public static void main(String args[]) {
        JFrame f = new JFrame("Swing Painting");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new DrawingPanel(400,400));
        f.pack();
        f.setVisible(true);
    }

}
