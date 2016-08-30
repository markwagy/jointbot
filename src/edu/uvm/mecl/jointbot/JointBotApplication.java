package edu.uvm.mecl.jointbot;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.demos.opengl.DemoApplication;
import com.bulletphysics.demos.opengl.GLDebugDrawer;
import com.bulletphysics.demos.opengl.IGL;
import static com.bulletphysics.demos.opengl.IGL.*;
import com.bulletphysics.demos.opengl.LWJGL;
import com.bulletphysics.demos.opengl.LwjglGL;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.vecmath.Vector3f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

/**
 *
 * @author mwagy
 */
public class JointBotApplication extends DemoApplication {

    public static float RUN_TIME = 20; // seconds
    //public static float RUN_TIME = 1; // seconds
    
    private final float GROUND_DISTANCE = -15.0f;
    private final float GROUND_Y_DIM = 10f;
    private final float HINGE_FREQ = 2f;
    
    boolean oneStep;
    ArrayList<JointGroup> jointGroups;
    JLabel fitnessLabel;
    boolean resetSimulation = false;
    boolean simulationFinished = false;
    Robot robot;
    int timeStep = 0;
    int genNum = 1;
    JTextArea outputLabel;
    RigidBody groundBody;
    int configScenario;
    float maxImpulse = 0.2f;
    float rate = 1.0f;
    float totalTime = 0.0f;
    DrawingPanel progPanel;
    JProgressBar progressBar;
    float endingDistance = 0.0f;
    private FitnessSlider distanceSlider;
    
    private boolean alreadyWrote = false;
    
    //public static float simulationTimeEllapsed = 0f;
    private float simulationTimeEllapsed = 0f;
    
    private JointConfig jointConfig;
    
    public JointBotApplication (IGL gl, JLabel fitnessLabel, 
            DrawingPanel progPanel, JProgressBar progressBar) 
            throws IOException {
        super(gl);
        this.fitnessLabel = fitnessLabel;
        this.progPanel = progPanel;
        this.progressBar = progressBar;
        this.progressBar.setMinimum(0);
        this.progressBar.setMaximum((int) Math.ceil(RUN_TIME));
        // hack to get rid of help text
        super.keyboardCallback('h', 0, 0, 0);
    }
        
    public JointBotApplication (IGL gl, JLabel fitnessLabel, 
            DrawingPanel progPanel) 
            throws IOException {
        super(gl);
        this.fitnessLabel = fitnessLabel;
        this.progPanel = progPanel;

        // hack to get rid of help text
        super.keyboardCallback('h', 0, 0, 0);
    }
    
    public JointBotApplication(IGL gl, JointConfig jc) {
        super(gl);
        this.setConfig(jc);
    }
    
    public JointBotApplication(IGL gl) {
        super(gl);
    }
    
    public JointBotApplication(IGL gl, ArrayList<JointGroup> jg) {
        super(gl);
        ArrayList<Float> phases = JointGroup.getRandomAngles(jg.size());
        jointConfig = new JointConfig(jg, phases);
        this.setConfig(jointConfig);
    }
    
    public void setConfig(JointConfig config) {
        this.jointGroups = config.getGroups();
        updateGroupPhases(config.getPhases());
        this.jointConfig = config;
    }
    
    public JointConfig getConfig() {
        return jointConfig;
    }
    
    public void setDistanceSlider(FitnessSlider distanceSlider) {
        this.distanceSlider = distanceSlider;
    }
    
    /*
    private void updateGroupPhases(ArrayList<Float> phases) {
        for (int i=0; i<jointGroups.size(); i++) {
            (jointGroups.get(i)).setPhase(phases.get(i));
        }
    }
    */
    public final void updateGroupPhases(ArrayList<Float> phs) {
        ArrayList<Float> dedupedPhases = JointConfig.getDedupedPhases(phs);
        for (int i=0; i<jointGroups.size(); i++) {
            JointGroup jg = jointGroups.get(i);
            Float ph = dedupedPhases.get(i);
            jg.setPhase(ph);
        }
    }
    
    /* override callbacks with no behavior to prevent keyboard, mouse interaction */

    @Override
    public void keyboardCallback(char key, int x, int y, int modifiers) { }
    
    @Override
    public void specialKeyboard(int key, int x, int y, int modifiers) { }

    @Override
    public void mouseFunc(int button, int state, int x, int y) { }
    
    @Override
    public void mouseMotionFunc(int x, int y) { }
    
    /*
    public void runSimulation(ArrayList<JointGroup> jointGroups) {
        this.jointGroups = jointGroups;
        updateGroupPhases();
        resetSimulation = true;
    }
    */
    
    public void runSimulation(ArrayList<JointGroup> jointGroups, ArrayList<Float> phases) {
        this.jointGroups = jointGroups;
        setJointGroupPhases(jointGroups, phases);
//        System.err.println("running with the following configuration:");
//        System.err.println(JointGroup.summarizeJointGroups(jointGroups));
        alreadyWrote = false;
        resetSimulation = true;
    }
    
    private void setJointGroupPhases(ArrayList<JointGroup> jgs, ArrayList<Float> phases) {
        /*
        for (int i=0; i<jgs.size(); i++) {
            jgs.get(i).setPhase(phases.get(i));
        }
    }
    */
//    public final void updateGroupPhases() {
        ArrayList<Float> dedupedPhases = JointConfig.getDedupedPhases(phases);
        for (int i=0; i<jgs.size(); i++) {
            JointGroup jg = jgs.get(i);
            Float ph = dedupedPhases.get(i);
            jg.setPhase(ph);
        }
    }
    /*
    private void updateGroupPhases() {
        ArrayList<Float> phaseAngles = new ArrayList<Float>();
        JointGroup.updateGroupPhaseAngles(jointGroups, phaseAngles);
        for (int i=0; i<jointGroups.size(); i++) {
            (jointGroups.get(i)).setPhase(phaseAngles.get(i));
        }
    }
    */
    
    public float getEndingDistance() {
        return endingDistance;
    }
    
    public boolean isRunning() {
        return getPhysicsTime() < RUN_TIME;
    }
    
    private float getPhysicsTime() {
//        return JointBotApplication.simulationTimeEllapsed;
        return simulationTimeEllapsed;
    }
    
    private void writeToDBOnce() {
        if (!alreadyWrote) {
            //JointConfig jc = new JointConfig(this.jointGroups,getJointGroupPhases());
            JointConfig jc = (JointConfig) this.jointConfig.clone();
            //System.err.println("1 " + jc);
            Application.writeToDB(jc,
                    endingDistance, Application.IS_NOT_BACKGROUND);
            alreadyWrote = true;
        }
    }
    
    /*
    public void runSimulation() {
        jointGroups = progPanel.getJointGroups();
        JointGroup.summarizeJointGroups(jointGroups);
        updateGroupPhases();
        resetSimulation = true;
    }
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
        dynamicsWorld.setDebugDrawer(new GLDebugDrawer(gl));

        setUpGroundBox();
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
    
    public float[][] copyWeights(float[][] oldWeights) {
        float[][] newWeights = new float[oldWeights.length][oldWeights[0].length];
        for (int i=0; i<oldWeights.length; i++) {
            System.arraycopy(oldWeights[i], 0, newWeights[i], 0, oldWeights[i].length);
        }
        return newWeights;
    }
    
    private ArrayList<Float> getJointGroupPhases() {
        ArrayList<Float> phases = new ArrayList<Float>();
        for (JointGroup jg : jointGroups) {
            phases.add(jg.getPhase());
        }
        return phases;
    }
    
    @Override
    public void clientMoveAndDisplay() {
        
        if (resetSimulation) {
            resetWorld();
            resetSimulation = false;
        }
        
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        if (robot != null) {
            timeStep += 1;
            if (isRunning()) {
                tick();
                actuateJoints();
                if (progressBar != null) {
                    progressBar.setValue(Math.round(getPhysicsTime()));
                }
                if (distanceSlider != null) {
                    distanceSlider.setPointerValue(distanceFromOrigin());
                    //distanceSlider.setValue((int)(distanceFromOrigin()/distanceSlider.getMaximum()));
                }
            } else {
                endingDistance = distanceFromOrigin();
                writeToDBOnce();
                robot.delete();
            }
        }
        renderme();
    }
    
    private void tick() {

        float ms = getDeltaTimeMicroseconds();
        float minFPS = 1000000f/60f;
        if (ms > minFPS) {
            ms = minFPS;
        }

//        float ms = 1000000f/60f;
        simulationTimeEllapsed += ms/1000000f;
        dynamicsWorld.stepSimulation(ms/1000000f);
    }
    
    private void fineTick() {
        float ms = getDeltaTimeMicroseconds();
        float minFPS = 1000000f/100f;
        if (ms > minFPS) {
            ms = minFPS;
        }
//        JointBotApplication.simulationTimeEllapsed += ms/1000000f;
        simulationTimeEllapsed += ms/1000000f;
        dynamicsWorld.stepSimulation(ms, 1, .01f);
    }
    
    private void actuateJoints() {
        for (JointGroup jointGroup : jointGroups) {
            float phase = jointGroup.getPhase();
            for (Joint joint : jointGroup.getJointsInGroup()) {
                robot.actuateJointSine(joint.getType(), getPhysicsTime(), phase, HINGE_FREQ);
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
        dynamicsWorld.getConstraintSolver().reset();
        robot = null;
        spawnRobot();
        if (fitnessLabel != null) {
            fitnessLabel.setText("distance = " + distanceFromOrigin());
        }
        timeStep = 0;
//        JointBotApplication.simulationTimeEllapsed = 0f;
        simulationTimeEllapsed = 0f;
    }
    
    @Override
    public void displayCallback() {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        if (dynamicsWorld != null) {
            dynamicsWorld.debugDrawWorld();
        }
        renderme();
    }
    
    /*
    @Override
    public void keyboardCallback(char key, int x, int y, int modifiers) {
        switch (key) {
            case 'p':
                pause = !pause;
            case 'o':
                oneStep = true;
            case 'r':
                runSimulation(jointGroups);
            default:
                super.keyboardCallback(key, x, y, modifiers);
        }
    }
    */
    
    @Override
    public void renderme() {
        super.renderme();
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glTranslatef(0f, GROUND_DISTANCE + GROUND_Y_DIM, 0f);
        gl.drawCube(1f);
        if (robot!=null && fitnessLabel!=null && !simulationFinished) {
            float dist = distanceFromOrigin();
            String message = String.format("%.2f meters", dist);
            fitnessLabel.setText(message);
            distanceSlider.setPointerValue(dist);
            distanceSlider.repaint();
        }
    }
    
    public static float run(JointBotApplication demoApp, float runTime, boolean headless) 
            throws LWJGLException, IOException {
        //System.err.format("Joint Config: %s\n",demoApp.jointConfig.toString());
        demoApp.resetWorld();
//        while (JointBotApplication.simulationTimeEllapsed < runTime) {
        while (demoApp.getPhysicsTime() < runTime) {
            if (!headless) {
                demoApp.moveAndDisplay();
                Display.update();
            } else {
                demoApp.tick();
                demoApp.actuateJoints();
            }
        }
        return demoApp.distanceFromOrigin();
    }
    
    public static void runRandomSamples(boolean headless, JointBotApplication app)
            throws LWJGLException, FileNotFoundException, IOException {

        String testConfig = UUID.randomUUID().toString();
        
        JointConfig jc = JointConfig.getRandom();
        app.setConfig(jc);
        
        app.initPhysics();
        app.setCameraDistance(40f);
        app.resetWorld();
        
        int numSamples = 100;
        
        String debugFile = String.format("randomsamples_%s.csv",testConfig);
        BufferedWriter writer = new BufferedWriter(new FileWriter(debugFile,true));
        //writer.write("iter,endTime,distance,is.headless,test.config\n");
        for (int iter=0; iter<numSamples; iter++) {
            System.err.println("iter " + iter);
            System.err.println("joint config: " + app.getConfig().toString());
            float dist = JointBotApplication.run(app, RUN_TIME, headless);
            writer.write(String.format("%d,%f,%f,%s,%s\n",
                    iter,
                    app.getPhysicsTime(), dist, headless,
                    testConfig));
            app.resetWorld();
        }
        writer.close();
        System.exit(0);
    }
    
    public static void runRandomTestSamples(boolean headless, JointBotApplication app) throws FileNotFoundException, IOException, LWJGLException {
        String testConfig = UUID.randomUUID().toString();
        Random rnd = new Random();
        int testIdx = rnd.nextInt(3);
        
        JointConfig jc = JointConfig.getTestConfig(testIdx);
        app.setConfig(jc);
        
        app.initPhysics();
        app.setCameraDistance(40f);
        app.resetWorld();
        
        String debugFile = String.format("testsamples_%d_%s.csv",testIdx, testConfig);
        BufferedWriter writer = new BufferedWriter(new FileWriter(debugFile,true));

        float dist = JointBotApplication.run(app, RUN_TIME, headless);
        writer.write(String.format("%f,%f,%d,%s\n",
                app.getPhysicsTime(), dist,testIdx,testConfig));
        
        writer.close();
        System.exit(0);
    }
    
    public static void runTestSamples(boolean headless, JointBotApplication app) 
            throws FileNotFoundException, IOException, LWJGLException {
        int numSamples = 100;
        String testConfig = UUID.randomUUID().toString();
        String debugFile = String.format("testsamples_%s.csv", testConfig);
        BufferedWriter writer = new BufferedWriter(new FileWriter(debugFile,true));
        
        for (int testIdx=0; testIdx<3; testIdx++) {
            System.err.println("testidx="+testIdx);
            for (int sampleIdx=0; sampleIdx<numSamples; sampleIdx++) {
                System.err.println("sampleidx="+sampleIdx);
                JointConfig jc = JointConfig.getTestConfig(testIdx);
                app = new JointBotApplication(LWJGL.getGL());
                app.setConfig(jc);
                app.initPhysics();
                app.setCameraDistance(40f);
                app.resetWorld();
                float dist = JointBotApplication.run(app, RUN_TIME, headless);
                writer.write(String.format("%f,%f,%d,%s\n",
                        app.getPhysicsTime(), dist,testIdx,testConfig));
            }
        }
        writer.close();
        System.exit(0);
    }
        
    public static void main(String args[]) throws IOException, LWJGLException {
        JointBotApplication app = new JointBotApplication(LWJGL.getGL());
        boolean headless = true;
        if (!headless) {
            LwjglGL lwjgl = new LwjglGL();
            int w=800;
            int h=600;
            String title = "JointBotDemoApplication";
            Display.setDisplayMode(new DisplayMode(w, h));
            Display.setTitle(title);
            Display.create(new PixelFormat(0, 24, 0));
            
            Keyboard.create();
            Keyboard.enableRepeatEvents(true);
            Mouse.create();
            
            lwjgl.init();
            
            app.myinit();
            app.reshape(w, h);
        }
        //runRandomSamples(headless, app);
        //runTestSamples(headless, app);
        run(app, 10, false);
    }
   
}