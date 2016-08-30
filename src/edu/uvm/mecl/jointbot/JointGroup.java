package edu.uvm.mecl.jointbot;

import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import java.awt.Color;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;



public class JointGroup implements Comparable {
  
    public ArrayList<Joint>joints = new ArrayList<Joint>();
    
    private Color color;
    
    private float phase = 0.0f;
    
    private boolean jointsFixed = false;
    
    public JointGroup(Color color) {
        this.color = color;
    }
    
    public JointGroup() { }

    @Override
    public String toString() {
        //String str = getHash() + "\n";
        String str = "";
        str += String.format("(phase=%.2f)[%d]{",phase,joints.size());
        for (Joint j : joints) {
            str += j + ", ";
        }
        str += "}";
        return str;
    }
    
    @Override
    public JointGroup clone() {
        JointGroup newJG = new JointGroup();
        newJG.setColor(color);
        newJG.setPhase(phase);
        newJG.addJoints(joints);
        return newJG;
    }
    
    public void addJoints(ArrayList<Joint> otherJoints) {
        for (Joint j : otherJoints) {
            joints.add(j.clone());
        }
    }
    
    /**
     * get a hash that represents this joint group
     */
    public String getHash() {
        String jointsString = "";
        for (Joint j : joints) {
            jointsString += joints.toString();
        }
        return MD5.getHash(jointsString);
    }
    
    public boolean isFixed() {
        return jointsFixed;
    }
    
    public void add(Joint j) { 
        joints.add(j); 
    }
    
    public void addAll(ArrayList<Joint> joints) { 
        this.joints.addAll(joints); 
    }
    
    public void remove(Joint j) { 
        joints.remove(j); 
    }
    
    public ArrayList<Joint> getAll() { 
        return joints; 
    }
    
    public int numJoints() { 
        return joints.size(); 
    }
    
    public Color getColor() { 
        return color; 
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }
    
    public float getPhase() {
        return phase;
    }
    
    public ArrayList<Joint> getJointsInGroup() {
        return joints;
    }
    
    public boolean containsJoint(Joints j) {
        for (Joint mine : joints) {
            JointBotInfo.Joints jointType = mine.getType();
            if (jointType.equals(j)) {
                return true;
            }
        }
        return false;
    }
        
    public static ArrayList<Float> getAngles(int n) {
        ArrayList<Float> possibleAngles = new ArrayList<Float>();
        for (float ang=0.0f; ang<2*Math.PI; ang+=Math.PI/n) {
            possibleAngles.add(ang);
        }
        return possibleAngles;
    }
    
    public static ArrayList<Float> getRandomAngles(int n) {
        ArrayList<Float> angs = new ArrayList<Float>();
        Random rand = new Random();
        for (int i=0; i<n; i++) {
            angs.add((float) (rand.nextFloat()*Math.PI*2));
        }
        return angs;
    }
        
    /**
     * updates phase angle assigned to each group. should be called after each
     * group joint or split!
     */
    public static void updateGroupPhaseAngles(
            ArrayList<JointGroup> jointGroups,
            ArrayList<Float> phaseAngles) {
        ArrayList<Float> angles = getAngles(jointGroups.size());
        Collections.shuffle(angles);
        phaseAngles.clear();
        for (Float ang : angles) {
            phaseAngles.add(ang);
        }
    }
    
    /*
    public static float[][] getPossiblePhaseAssignments(int numGroups) {
        int numPossibleAssignments = numPermutations(numGroups);
        float[][] assignments = new float[numPossibleAssignments][numGroups];
        for (int assignmentIdx=0; assignmentIdx<numPossibleAssignments; assignmentIdx++) {
            float[] currentPhaseAssignment = new float[numGroups];
            // TODO
        }
        return assignments;
    }
    */
    
    public static ArrayList<JointGroup> getRandomJointGroup() {
        Random rnd = new Random();
        int numGroups = rnd.nextInt(Joints.Count.ordinal());
        if (numGroups>=Joints.Count.ordinal()) {
            numGroups = Joints.Count.ordinal();
        }
        ArrayList<JointGroup> jointGroups = new ArrayList<JointGroup>();
        for (int i=0; i<numGroups; i++) {
            jointGroups.add(new JointGroup());
        }
        for (Joints jType : JointBotInfo.Joints.values()) {
            Joint j = new Joint(jType);
            int jgId = rnd.nextInt(numGroups);
            if (jgId >= numGroups) {
                jgId = numGroups-1;
            }
            jointGroups.get(jgId).add(j);
        }
        return jointGroups;
    }
    
    private static int numPermutations(int n) {
        int res = 1;
        for (int i=1; i<=n ;i++) {
            res *= i;
        }
        return res;
    }

    void fixJoints() {
        jointsFixed = true;
    }
    
    void unfixJoints() {
        jointsFixed = false;
    }

    public boolean containsJoint(Joint joint) {
        for (Joint myjoint : joints) {
            if (myjoint.equals(joint)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return joints.isEmpty();
    }
    
    public static String summarizeJointGroups(ArrayList<JointGroup> jointGroups) {
        String s = jointGroups.size() + " joint groups\n";
        for (JointGroup jointGroup : jointGroups) {
            s += "JG" + jointGroup + "\n";
        }
        return s;
    }

    @Override
    public int compareTo(Object other) {
        String thisHash = getHash();
        String thatHash = ((JointGroup) other).getHash();
        return thisHash.compareTo(thatHash);
    }

    void setColor(Color color) {
        this.color = color;
    }
}
class MD5 {
    public static String getHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(text.getBytes());
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            String hashtext = bigInt.toString(16);
            /*
            // Now we need to zero pad it if you actually want the full 32 chars.
            while(hashtext.length() < 32 ){
                hashtext = "0"+hashtext;
            }
            */
            return hashtext;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MD5.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}