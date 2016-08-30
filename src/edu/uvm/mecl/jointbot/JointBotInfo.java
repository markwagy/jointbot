package edu.uvm.mecl.jointbot;

import java.awt.Point;

/**
 *
 * @author mwagy
 */
public class JointBotInfo {
     
    public enum Bodies {
        Spine1,
        Spine2,
        
        UpperLeg1,
        UpperLeg2,
        UpperLeg3,
        UpperLeg4,
        
        LowerLeg1,
        LowerLeg2,
        LowerLeg3,
        LowerLeg4,

        Count
    }
    
    public enum Joints {
        Spine1_Spine2,
        
        Spine1_UpperLeg1,
        Spine1_UpperLeg2,
        Spine2_UpperLeg3,
        Spine2_UpperLeg4,
        
        UpperLeg1_LowerLeg1,
        UpperLeg2_LowerLeg2,
        UpperLeg3_LowerLeg3,
        UpperLeg4_LowerLeg4,
        
        Count;
    }
    
    public enum Dimensions {X, Y, Z}
    
    public static final double CYL_LENGTH = 0.2;
    public static final double CYL_WIDTH = 0.03;
    public static final double CENTER = 0.5;
    public static final double JOINT_SIZE = 0.025;
    
    /* 
     * we're using TINYAMT so that joints are consistently slightly different in 
     * the way they are drawn since some algorithms draw based on which are
     * further away from the spine1/spine2 joint. this doesn't affect the 
     * actual robot - just the drawing, and it is not perceptible by the user
     */
    private static final double TINYAMT = 0.0001;
    
    public static final double XPOS_SPINE1 = CENTER-CYL_LENGTH/2;
    public static final double YPOS_SPINE1 = CENTER + TINYAMT;
    public static final double XPOS_SPINE2 = CENTER+CYL_LENGTH/2;
    public static final double YPOS_SPINE2 = CENTER;
    
    public static final double XPOS_UPPERLEG1 = CENTER-CYL_LENGTH;
    public static final double YPOS_UPPERLEG1 = CENTER+CYL_LENGTH/2+CYL_WIDTH/2 + TINYAMT;
    public static final double XPOS_UPPERLEG2 = CENTER-CYL_LENGTH;
    public static final double YPOS_UPPERLEG2 = CENTER-CYL_LENGTH/2-CYL_WIDTH/2 + TINYAMT*2;
    public static final double XPOS_UPPERLEG3 = CENTER+CYL_LENGTH;
    public static final double YPOS_UPPERLEG3 = CENTER-CYL_LENGTH/2-CYL_WIDTH/2 + TINYAMT*3;
    public static final double XPOS_UPPERLEG4 = CENTER+CYL_LENGTH;
    public static final double YPOS_UPPERLEG4 = CENTER+CYL_LENGTH/2+CYL_WIDTH/2 + TINYAMT*4;
    
    public static final double XPOS_LOWERLEG1 = CENTER-CYL_LENGTH;
    public static final double YPOS_LOWERLEG1 = CENTER+CYL_LENGTH+CYL_LENGTH/2 + TINYAMT;
    public static final double XPOS_LOWERLEG2 = CENTER-CYL_LENGTH;
    public static final double YPOS_LOWERLEG2 = CENTER-CYL_LENGTH-CYL_LENGTH/2 + TINYAMT*5;
    public static final double XPOS_LOWERLEG3 = CENTER+CYL_LENGTH;
    public static final double YPOS_LOWERLEG3 = CENTER-CYL_LENGTH-CYL_LENGTH/2 + TINYAMT*6;
    public static final double XPOS_LOWERLEG4 = CENTER+CYL_LENGTH;
    public static final double YPOS_LOWERLEG4 = CENTER+CYL_LENGTH+CYL_LENGTH/2 + TINYAMT*7;

    
    public static Point getJointCanvasPosition(Joints joint, int canvasWidth, int canvasHeight) {
        Point pos = new Point();
        switch(joint) {
            case Spine1_Spine2:
                pos.x = percToX(XPOS_SPINE1+CYL_LENGTH/2, canvasWidth);
                pos.y = percToY(CENTER, canvasHeight);
                break;
            case Spine1_UpperLeg1:
                pos.x = percToX(XPOS_SPINE1-CYL_LENGTH/2, canvasWidth);
                pos.y = percToY(CENTER + CYL_WIDTH/2 + JOINT_SIZE/2, canvasHeight);
                break;
            case Spine1_UpperLeg2:
                pos.x = percToX(XPOS_SPINE1-CYL_LENGTH/2, canvasWidth);
                pos.y = percToY(CENTER - CYL_WIDTH/2 - JOINT_SIZE/2, canvasHeight);
                break;
            case Spine2_UpperLeg3:
                pos.x = percToX(XPOS_SPINE2+CYL_LENGTH/2, canvasWidth);
                pos.y = percToY(CENTER - CYL_WIDTH/2 - JOINT_SIZE/2, canvasHeight);
                break;
            case Spine2_UpperLeg4:
                pos.x = percToX(XPOS_SPINE2+CYL_LENGTH/2, canvasWidth);
                pos.y = percToY(CENTER + CYL_WIDTH/2 + JOINT_SIZE/2, canvasHeight);
                break;
            case UpperLeg1_LowerLeg1:
                pos.x = percToX(XPOS_UPPERLEG1, canvasWidth);
                pos.y = percToY(YPOS_UPPERLEG1+CYL_LENGTH/2, canvasHeight);
                break;
            case UpperLeg2_LowerLeg2:
                pos.x = percToX(XPOS_UPPERLEG2, canvasWidth);
                pos.y = percToY(YPOS_UPPERLEG2-CYL_LENGTH/2, canvasHeight);
                break;
            case UpperLeg3_LowerLeg3:
                pos.x = percToX(XPOS_UPPERLEG3, canvasWidth);
                pos.y = percToY(YPOS_UPPERLEG3-CYL_LENGTH/2, canvasHeight);
                break;
            case UpperLeg4_LowerLeg4:
                pos.x = percToX(XPOS_UPPERLEG4, canvasWidth);
                pos.y = percToY(YPOS_UPPERLEG4+CYL_LENGTH/2, canvasHeight);
                break;
        }
        return pos;
    }
    
    public static double getJointDistance(Joints joint1, Joints joint2, int w, int h) {
        Point p1 = getJointCanvasPosition(joint1,w,h);
        Point p2 = getJointCanvasPosition(joint2,w,h);
        double dist = Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
        return dist;
    }
    
    public static Point getBodyCanvasPosition(Bodies body, int canvasWidth, int canvasHeight) {
        Point pos = new Point();
        double x = 0;
        double y = 0;
        switch(body) {
            case Spine1:
                x = JointBotInfo.XPOS_SPINE1;
                y = JointBotInfo.YPOS_SPINE1;
                break;
            case Spine2:
                x = JointBotInfo.XPOS_SPINE2;
                y = JointBotInfo.YPOS_SPINE2;
                break;
            case UpperLeg1:
                x = JointBotInfo.XPOS_UPPERLEG1;
                y = JointBotInfo.YPOS_UPPERLEG1;
                break;
            case UpperLeg2:
                x = JointBotInfo.XPOS_UPPERLEG2;
                y = JointBotInfo.YPOS_UPPERLEG2;
                break;
            case UpperLeg3:
                x = JointBotInfo.XPOS_UPPERLEG3;
                y = JointBotInfo.YPOS_UPPERLEG3;
                break;
            case UpperLeg4:
                x = JointBotInfo.XPOS_UPPERLEG4;
                y = JointBotInfo.YPOS_UPPERLEG4;
                break;
            case LowerLeg1:
                x = JointBotInfo.XPOS_LOWERLEG1;
                y = JointBotInfo.YPOS_LOWERLEG1;
                break;
            case LowerLeg2:
                x = JointBotInfo.XPOS_LOWERLEG2;
                y = JointBotInfo.YPOS_LOWERLEG2;
                break;
            case LowerLeg3:
                x = JointBotInfo.XPOS_LOWERLEG3;
                y = JointBotInfo.YPOS_LOWERLEG3;
                break;
            case LowerLeg4:
                x = JointBotInfo.XPOS_LOWERLEG4;
                y = JointBotInfo.YPOS_LOWERLEG4;
                break;
        }
        pos.x = percToX(x, canvasWidth);
        pos.y = percToY(y, canvasHeight);
        return pos;
    }
    
    public static String getJointString(Joints type) {
        String s = "";
        switch(type) {
            case Spine1_Spine2:
                s = "Spine1.Spine2";
                break;
            case Spine1_UpperLeg1:
                s = "Spine1.UpperLeg1";
                break;
            case Spine1_UpperLeg2:
                s = "Spine1.UpperLeg2";
                break;
            case Spine2_UpperLeg3:
                s = "Spine2.UpperLeg3";
                break;
            case Spine2_UpperLeg4:
                s = "Spine2.UpperLeg4";
                break;
            case UpperLeg1_LowerLeg1:
                s = "UpperLeg1.LowerLeg1";
                break;
            case UpperLeg2_LowerLeg2:
                s = "UpperLeg2.LowerLeg2";
                break;
            case UpperLeg3_LowerLeg3:
                s = "UpperLeg3.LowerLeg3";
                break;
            case UpperLeg4_LowerLeg4:
                s = "UpperLeg4.LowerLeg4";
                break;
        }
        return s;
    }
    
    public static int getBodyCanvasSize(Bodies body, Dimensions dim, int canvasDimension) {
        if (body.equals(Bodies.Spine1) || body.equals(Bodies.Spine2)) {
            if (dim == Dimensions.X) {
                return percToX(JointBotInfo.CYL_LENGTH, canvasDimension);
            } else {
                return percToY(JointBotInfo.CYL_WIDTH, canvasDimension);
            }
        } else {
            if (dim == Dimensions.X) {
                return percToX(JointBotInfo.CYL_WIDTH, canvasDimension);
            } else {
                return percToY(JointBotInfo.CYL_LENGTH, canvasDimension);
            }
        }
    }
    
    
    private static int percToX(double perc, int canvasWidth) {
        return (int) (perc*canvasWidth);
    }
    
    private static int percToY(double perc, int canvasHeight) {
        return (int) (perc*canvasHeight);
    }
}
