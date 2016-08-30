package edu.uvm.mecl.jointbot;

import com.bulletphysics.demos.opengl.IGL;
import javax.swing.SwingWorker;

/**
 * Run a joint bot simulation in a background thread when using Swing.
 * @author mwagy
 */
public class BackgroundRunner extends SwingWorker<Void, Void> {

    JointConfig jointConfig;
    int numRuns;
    IGL igl;
            
    public BackgroundRunner(int numRuns, JointConfig jc, IGL igl) {
        jointConfig = jc;
        this.numRuns = numRuns;
        this.igl = igl;
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        System.err.println("starting a run in the background");
        JointBotApplication jbApp = new JointBotApplication(igl, jointConfig);
        jbApp.initPhysics();
        jbApp.resetWorld();
        float distance = JointBotApplication.run(jbApp, JointBotApplication.RUN_TIME, true);
        //System.err.println("2 " + jointConfig);
        Application.writeToDB(
                    jointConfig, 
                    distance,
                    Application.IS_BACKGROUND);
        return null;
    }
    
}
