package edu.uvm.mecl.jointbot;

import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * A configuration of joint groups. e.g. a set of groups to which a partition
 * of joints are assigned and the phase angles associated with each of those groups.
 * @author mwagy
 */
public class JointConfig implements Comparable {
    
    private ArrayList<Float> phases;
    private ArrayList<JointGroup> jointGroups;
    
    public JointConfig(ArrayList<JointGroup> jointGroups, ArrayList<Float> phases) {
        // deep copy!
        this.phases = new ArrayList<Float>();
        for (Float ph : phases) {
            this.phases.add(new Float(ph.floatValue()));
        }
        this.jointGroups = new ArrayList<JointGroup>();
        for (JointGroup jg : jointGroups) {
            this.jointGroups.add(jg.clone());
        }
        updateGroupPhases();
    }

    public JointConfig(HashMap<Float, ArrayList<Joints>> hm) {
        jointGroups = new ArrayList<JointGroup>();
        phases = new ArrayList<Float>();
        int i=0; 
        for (Iterator<Float> it = hm.keySet().iterator(); it.hasNext();) {
            Float key = it.next();
            phases.add(key);
            JointGroup jg = new JointGroup(DrawingPanel.colors[i]);
            for (Joints j : hm.get(key)) {
                Joint currJoint = new Joint(j);
                currJoint.setGroup(jg);
                jg.add(currJoint);
            }
            jointGroups.add(jg);
            i++;
        }
        updateGroupPhases();
    }
    
    @Override
    public String toString() {
        String s = "";
        for (JointGroup jg : jointGroups) {
            s += jg.toString();
        }
        return s;
    }
        
    public static Float[] getMutatedVersion(Float[] ind, float mutateProb) {
        Float[] newInd = new Float[ind.length];
        int[] groupIds = getGroupIdsFromPhases(new ArrayList<Float>(Arrays.asList(ind)));
        
        HashMap<Integer, Float> mutateMap = getMutationMap(ind, mutateProb);
        for (int i=0; i<ind.length; i++) {
            if (mutateMap.containsKey(groupIds[i])) {
                newInd[i] = mutateMap.get(groupIds[i]);
            } else {
                newInd[i] = ind[i];
            }
        }
        return newInd;
    }
    
    private static HashMap<Integer, Float> getMutationMap(Float[] ind, float mutateProb) {
        int[] groupIds = getGroupIdsFromPhases(new ArrayList(Arrays.asList(ind)));
        ArrayList<Integer> uids = getUniqueGroupIds(groupIds);
        // integer is the group id and float is the mutated value
        HashMap<Integer, Float> mutateMap = new HashMap<Integer, Float>();
        Random rnd = new Random();
        // walk through each unique group id and determine if it should be mutated
        for (Integer uid : uids) {
            if (rnd.nextFloat() < mutateProb) {
                mutateMap.put(uid, JointConfig.getRandomPhaseValue());
            }
        }
        return mutateMap;
    }
    
    private static ArrayList<Integer> getUniqueGroupIds(int[] groupIds) {
        ArrayList<Integer> uids = new ArrayList<Integer>();
        for (int i=0; i<groupIds.length; i++) {
            if (!uids.contains(groupIds[i])) {
                uids.add(groupIds[i]);
            }
        }
        return uids;
    }
    
    private static int[] getGroupIdsFromPhases(ArrayList<Float> phaseValues) {
        // map every phase seen to a unique id
        HashMap<Float, Integer> phaseIdMap = new HashMap<Float, Integer>();
        // var to keep track of each new id
        int currId = 0;
        // consistent group ids for each of the joints
        int[] ids = new int[Joints.Count.ordinal()];
        // we know that the first phase value is new...
        ids[0] = currId;
        phaseIdMap.put(phaseValues.get(0), currId);
        // iterate through all of the other joint phases
        for (int i=1; i<phaseValues.size(); i++) {
            float currPhase = phaseValues.get(i);
            if (!phaseIdMap.containsKey(currPhase)) {
                // this is a new phase value
                currId++;
                phaseIdMap.put(currPhase, currId);
            } 
            ids[i] = phaseIdMap.get(currPhase);
        }
        return ids;
    }
    
    /**
     * 
     * @return a consistent array of integers that represents group phases
     */
    public static int[] getGroupIds(ArrayList<JointGroup> jointGrps) {
        // get phases ordered by joint order in jointinfo
        // (so that we are consistent each time this is called)
        ArrayList<Joint> joints = getAllJointsInOrder(jointGrps);
        ArrayList<Float> orderedPhases = new ArrayList<Float>();
        for (Joint j : joints) {
            orderedPhases.add(j.getGroup().getPhase());
        }
        int[] ids = getGroupIdsFromPhases(orderedPhases);
        return ids; 
    }
    
    private static ArrayList<Joint> getAllJointsInOrder(ArrayList<JointGroup> jointGrps) {
        ArrayList<Joint> joints = new ArrayList<Joint>();
        for (JointGroup jg : jointGrps) {
            ArrayList<Joint> thisGroupJoints = jg.getJointsInGroup();
            joints.addAll(thisGroupJoints);
        }
        assert joints.size() == Joints.Count.ordinal();
        Collections.sort(joints);
        return joints;
    }
    
    public Float[] getGroupPhases() {
        Float[] phs = ((Float[]) phases.toArray());
        return phs;
    }
    
    public static JointConfig getRandom() {
        Random rnd = new Random();
        int numGroups = rnd.nextInt(JointBotInfo.Joints.Count.ordinal());
        ArrayList<JointGroup> jointGroups = new ArrayList<JointGroup>();
        for (int jgIdx=0; jgIdx < numGroups; jgIdx++) {
            JointGroup jg = new JointGroup();
            jointGroups.add(jg);
        }
        for (int i=0; i<Joints.Count.ordinal(); i++) {
            int groupIdx = rnd.nextInt(numGroups);
            (jointGroups.get(groupIdx)).add(new Joint(Joints.values()[i]));
        }
        ArrayList<Float> randPhases = JointGroup.getRandomAngles(numGroups);
        JointConfig jc = new JointConfig(jointGroups, randPhases);
        return jc;
    }
    
    public ArrayList<Float> getPhases() {
        return phases;
    }
    
    public ArrayList<JointGroup> getGroups() {
        return jointGroups;
    }
    
    @Override
    public JointConfig clone() {
        ArrayList<Float> ps = new ArrayList<Float>();
        for (Float p : phases) {
            ps.add(p);
        }
        ArrayList<JointGroup> gs = new ArrayList<JointGroup>();
        for (JointGroup g : jointGroups) {
            gs.add(g);
        }
        return new JointConfig(gs,ps);
    }
    
    /**
     * sort this configuration's joint groups by their md5 hashes (i.e. their natural ordering)
     */
    private void sortGroups() {
        Collections.sort(jointGroups);
    }
    
    /**
     * reassign colors to joint groups given their natural ordering
     */
    public void reassignColors() {
        sortGroups();
        for (int i=0; i<jointGroups.size(); i++) {
            jointGroups.get(i).setColor(DrawingPanel.colors[i]);
        }
    }
    
    public final void updateGroupPhases() {
        ArrayList<Float> dedupedPhases = getDedupedPhases(phases);
        for (int i=0; i<jointGroups.size(); i++) {
            JointGroup jg = jointGroups.get(i);
            Float ph = dedupedPhases.get(i);
            jg.setPhase(ph);
        }
    }
    
    public static ArrayList<Float> getDedupedPhases(ArrayList<Float> phs) {
        ArrayList dedPhs = new ArrayList<Float>();
        for (Float p : phs) {
            if (!dedPhs.contains(p)) {
                dedPhs.add(p);
            }
        }
        return dedPhs;
    }
    
    public float getJointPhase(Joints type) {
        ArrayList<JointGroup> jgs = getGroups();
        for (JointGroup jg : jgs) {
            if (jg.containsJoint(type)) {
                return jg.getPhase();
            }
        }
        System.err.println("ERROR: this shouldn't happen");
        return -1f; /* error */
    }
    
    public static JointConfig getTestConfig(int configNum) {
        ArrayList<JointGroup> jointGroups = new ArrayList<JointGroup>();
        ArrayList<Float> phases = new ArrayList<Float>();
        int numGroups;
        switch (configNum) {
            case 0:
                numGroups = 3;
                for (int jgIdx=0; jgIdx<numGroups; jgIdx++) {
                    JointGroup jg = new JointGroup();
                    jointGroups.add(jg);
                }
                for (int i=0; i<JointBotInfo.Joints.Count.ordinal(); i++) {
                    if (i%3==0) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(0);
                        j.setGroup(jg);
                        jg.setPhase(3.0f);
                        jg.add(j);
                    } else if (i==0 || i==1) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(1);
                        j.setGroup(jg);
                        jg.setPhase(1.0f);
                        jg.add(j);
                    } else {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(2);
                        j.setGroup(jg);
                        jg.setPhase(0.0f);
                        jg.add(j);
                    }
                }
                phases.add(3f);
                phases.add(1f);
                phases.add(0f);
                return new JointConfig(jointGroups, phases);
            case 1:
                numGroups = 3;
                for (int jgIdx=0; jgIdx<numGroups; jgIdx++) {
                    JointGroup jg = new JointGroup();
                    jointGroups.add(jg);
                }
                for (int i=0; i<JointBotInfo.Joints.Count.ordinal(); i++) {
                    if (i%3==0) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(0);
                        j.setGroup(jg);
                        jg.setPhase(3.0f);
                        jg.add(j);
                    } else if (i==1 || i==2 || i==3 || i==5) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(1);
                        j.setGroup(jg);
                        jg.setPhase(0.0f);
                        jg.add(j);
                    } else {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(2);
                        j.setGroup(jg);
                        jg.setPhase(5.0f);
                        jg.add(j);
                    }
                }
                phases.add(3f);
                phases.add(0f);
                phases.add(5f);
                return new JointConfig(jointGroups, phases);
            case 2:
                numGroups = 2;
                
                for (int jgIdx=0; jgIdx < numGroups; jgIdx++) {
                    JointGroup jg = new JointGroup();
                    jointGroups.add(jg);
                }
                for (int i=0; i<JointBotInfo.Joints.Count.ordinal(); i++) {
                    if (i%2==0) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(0);
                        j.setGroup(jg);
                        jg.setPhase(3.1415f);
                        jg.add(j);
                    } else {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(1);
                        j.setGroup(jg);
                        jg.setPhase(2*3.1415f);
                        jg.add(j);
                    }
                }
                phases.add(3.1415f);
                phases.add(2*3.1415f);
                return new JointConfig(jointGroups, phases);
            default:
                numGroups = 2;

                for (int jgIdx=0; jgIdx < numGroups; jgIdx++) {
                    JointGroup jg = new JointGroup();
                    jointGroups.add(jg);
                }
                for (int i=0; i<JointBotInfo.Joints.Count.ordinal(); i++) {
                    if (i%2==0) {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(0);
                        j.setGroup(jg);
                        jg.setPhase(3.1415f);
                        jg.add(j);
                    } else {
                        Joint j = new Joint(JointBotInfo.Joints.values()[i]);
                        JointGroup jg = jointGroups.get(1);
                        j.setGroup(jg);
                        jg.setPhase(2*3.1415f);
                        jg.add(j);
                    }
                }
                phases.add(3.1415f);
                phases.add(2*3.1415f);
                return new JointConfig(jointGroups, phases);
        }
    }

    // compare unique joint configs ids to determine if they are same
    private boolean compareConfigIds(int[] ids1, int[] ids2) {
        // assert
        if (ids1.length != ids2.length) {
            System.err.println("ERROR: config id list must be the same length");
            return false;
        }
        for (int i=0; i<ids1.length; i++) {
            if (ids1[i] != ids2[i]) {
                return false;
            }
        }
        return true;
    }
    
    public static float getRandomPhaseValue() {
        Random rnd = new Random();
        return rnd.nextFloat() * 2f * (float) Math.PI;
    }
    
    public static Float[] getRandomPhaseValues(int[] groupIds) {
        HashMap<Integer,Float> map = new HashMap<Integer, Float>();
        Float[] phaseVals = new Float[groupIds.length];

        for (int i=0; i<groupIds.length; i++) {
            Integer groupIdInteger = new Integer(groupIds[i]);
            if (!map.containsKey(groupIdInteger)) {
                map.put(groupIdInteger, new Float(JointConfig.getRandomPhaseValue()));
            } 
            phaseVals[i] = map.get(groupIdInteger);
        }
        return phaseVals;
    }
    
    @Override
    public int compareTo(Object t) {
        JointConfig other = (JointConfig) t;
        int[] otherIds = other.getGroupIds(other.getGroups());
        int[] thisIds  = other.getGroupIds(this.getGroups());
        if (compareConfigIds(thisIds, otherIds)) {
            return 0;
        }
        return -1;
    }
}
