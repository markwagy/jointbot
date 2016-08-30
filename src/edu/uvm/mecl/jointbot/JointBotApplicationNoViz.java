package edu.uvm.mecl.jointbot;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import edu.uvm.mecl.DemoApplicationNoViz;
import edu.uvm.mecl.jointbot.JointBotInfo.Bodies;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.vecmath.Vector3f;

/**
 *
 * @author mwagy
 */
public class JointBotApplicationNoViz extends DemoApplicationNoViz {

    private final float GROUND_DISTANCE = -15.0f;
    private final float GROUND_Y_DIM = 10f;
    
    public static int runNumber = 1;
    
    private JointConfig jointConfig;
    
    ArrayList<JointGroup> jointGroups;
    boolean resetSimulation = false;
    boolean simulationFinished = false;
    Robot robot;
    int timeStep = 0;
    int genNum = 1;
    RigidBody groundBody;
    int configScenario;
    float maxImpulse = 0.2f;
    float rate = 1.0f;
    float totalTime = 0.0f;
    //BufferedWriter writer;
    
    public static float simulationTimeEllapsed = 0f;
    
    public JointBotApplicationNoViz(JointConfig jointConfig) {
        super();
        this.jointConfig = jointConfig;
    }
    
    public JointBotApplicationNoViz(ArrayList<JointGroup> jg) {
        super();
        ArrayList<Float> phases = JointGroup.getRandomAngles(jg.size());
        jointConfig = new JointConfig(jg, phases);
    }

    public void runSimulation(ArrayList<JointGroup> jointGroups) {
        this.jointGroups = jointGroups;
        resetSimulation = true;
    }
    
    public void runSimulation(ArrayList<JointGroup> jgs, ArrayList<Float> phases) {
        this.jointGroups = jgs;
        updateGroupPhases(phases);
    }
    
    private void randomizeGroupPhases() {
        ArrayList<Float> phaseAngles = new ArrayList<Float>();
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        for (int i=0; i<jointGroups.size(); i++) {
            (jointGroups.get(i)).setPhase(phaseAngles.get(i));
        }
    }
    
    public JointConfig getJointConfig() {
        return jointConfig;
    }
    
    private void updateGroupPhases(ArrayList<Float> phases) {
        for (int i=0; i<jointGroups.size(); i++) {
            (jointGroups.get(i)).setPhase(phases.get(i));
        }
    }
    
    public void setPhases(double[] phases) {
        for (int i=0; i<jointGroups.size(); i++) {
            jointGroups.get(i).setPhase((float) phases[i]);
        }
    }
    
    public void runSimulation() {
        jointGroups = jointConfig.getGroups();
        JointGroup.summarizeJointGroups(jointGroups);
        randomizeGroupPhases();
        resetSimulation = true;
    }
   
    /**
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Override
    public void initPhysics() throws FileNotFoundException, IOException {
        // Setup the basic world
        DefaultCollisionConfiguration collision_config = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collision_config);
        BroadphaseInterface overlappingPairCache = new DbvtBroadphase();
        ConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();
        
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, constraintSolver, collision_config);
        dynamicsWorld.setGravity(new Vector3f(0f, -30f, 0f));

        setUpGroundBox();
        runSimulation(jointConfig.getGroups());
        
        clientResetScene();
    }
    
    private void setUpGroundBox() {
        CollisionShape groundShape = new BoxShape(new Vector3f(200f, GROUND_Y_DIM, 200f));
        Transform groundTransform = new Transform();
        groundTransform.setIdentity();
        groundTransform.origin.set(0f, GROUND_DISTANCE, 0f);
        groundBody = localCreateRigidBody(0f, groundTransform, groundShape);
    }
    
    private void spawnRobot() {
        robot = new JointBot(dynamicsWorld);
        robot.setGroundObjectID(groundBody);
    }
    
    @Override
    public void clientMoveAndDisplay() {

        if (resetSimulation) {
            resetWorld();
            resetSimulation = false;
        }
        
        if (robot != null) {
            timeStep += 1;
            float ms = getDeltaTimeMicroseconds();
            float minFPS = 1000000f/60f;
            if (ms > minFPS) {
                ms = minFPS;
            }
            JointBotApplicationNoViz.simulationTimeEllapsed += ms/1000000;
            dynamicsWorld.stepSimulation(ms/1000000f);
            actuateJoints();
        }
    }
    
    private String getEndpointsString()  {
        Vector3f gcom = new Vector3f();
        Vector3f g2 = new Vector3f();
        groundBody.getCenterOfMassPosition(gcom);
        g2.x = gcom.x + 10; // arbitrary 10 length vector to show plane vector
        g2.y = gcom.y;
        g2.z = gcom.z;
        Vector3f groundEndpoints[] = new Vector3f[2];
        groundEndpoints[0] = gcom;
        groundEndpoints[1] = g2;
        
        String rtn = lineVectorString(groundEndpoints);
        
        for (int bodiesIdx=0; bodiesIdx < Bodies.Count.ordinal(); bodiesIdx++) {
            Vector3f[] aabb = ((JointBot) robot).getAABB(Bodies.values()[bodiesIdx]);
            rtn += "," + lineVectorString(aabb);
        }
        return rtn;
    }
    
    private String getAABBString() {
        Vector3f groundMinAABB = new Vector3f();
        Vector3f groundMaxAABB = new Vector3f();
        groundBody.getAabb(groundMinAABB,groundMaxAABB);
        Vector3f groundAABB[] = new Vector3f[2];
        groundAABB[0] = groundMinAABB;
        groundAABB[1] = groundMaxAABB;
        
        String rtn = lineVectorString(groundAABB);
        
        for (int bodiesIdx=0; bodiesIdx < Bodies.Count.ordinal(); bodiesIdx++) {
            Vector3f[] aabb = ((JointBot) robot).getAABB(Bodies.values()[bodiesIdx]);
            rtn += "," + lineVectorString(aabb);
        }
        return rtn;
    }
    
    private String lineVectorString(Vector3f[] aabb) {
        String rtn = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                aabb[0].x,aabb[0].y,aabb[0].z,
                aabb[1].x,aabb[1].y,aabb[1].z);
        return rtn;
    }
    
    private void actuateJoints() {
        for (JointGroup jointGroup : jointGroups) {
            float phase = jointGroup.getPhase();
            for (Joint joint : jointGroup.getJointsInGroup()) {
                robot.actuateJointSine(joint.getType(), JointBotApplicationNoViz.simulationTimeEllapsed, phase, 100f);
            }
        }
    }
    
    public float distanceFromOrigin() {
        float x = robot.getPosition().x;
        float z = robot.getPosition().z;
        return (float) Math.sqrt(x*x + z*z);
    }
    
    public void resetWorld() {
        if (robot!=null) {
         robot.delete();
        }
        dynamicsWorld.clearForces();
        robot = null;
        spawnRobot();
        timeStep = 0;
        JointBotApplicationNoViz.simulationTimeEllapsed = 0f;
    }
    
    @Override
    public void destroy() {
        super.destroy();
    }
    
    public static void main(String[] args) throws IOException, FileNotFoundException {
        BufferedWriter w = new BufferedWriter(new FileWriter("headlessDebug.csv"));
        w.write("iter,endTime,distance,test.config\n");
        int testConfig = 1;
        JointConfig jc = JointConfig.getTestConfig(testConfig);
        JointBotApplicationNoViz demoApp = new JointBotApplicationNoViz(jc);
        demoApp.initPhysics();
        int numSamples = 100;
        
        System.out.println(String.format("running %d samples for %f seconds", numSamples, JointBotApplication.RUN_TIME));
        for (int sampleIdx = 0; sampleIdx < numSamples; sampleIdx++) {
            System.out.println("iteration " + sampleIdx);
            long startTime = System.nanoTime();
            double dist = run(demoApp, JointBotApplication.RUN_TIME);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            //                System.out.println(String.format("%f,%d,%l",dist,numTimeSteps,duration));
            w.write(String.format("%d,%f,%f,%d\n",sampleIdx,JointBotApplicationNoViz.simulationTimeEllapsed,dist,testConfig));
        }
        
        w.close();
    }
    
    public static double run(JointBotApplicationNoViz demoApp, float simulationTime) {
        long lastTime = System.currentTimeMillis();
        int frames = 0;
        demoApp.resetWorld();
        while (JointBotApplicationNoViz.simulationTimeEllapsed < simulationTime) {
            demoApp.moveAndDisplay();
            long time = System.currentTimeMillis();
            if (time - lastTime < 1000) {
                frames++;
            }
            else {
                lastTime = time;
                frames = 0;
            }
        }
        float dist = demoApp.distanceFromOrigin();
        Application.writeToDB(demoApp.getJointConfig(), dist, Application.IS_BACKGROUND);
        return dist;
    }

}