package edu.uvm.mecl.jointbot;



import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;

/**
 *
 * @author mwagy
 */
public class JointObjectiveFunction implements IObjectiveFunction {

    JointBotApplication jbApp;
    private final boolean IS_HEADLESS = true;
    
    public JointObjectiveFunction(JointBotApplication jbApp) throws IOException {
        this.jbApp = jbApp;
    }

    @Override
    public double valueOf(double[] x) {
        double currentDistance;
        try {
            jbApp.initPhysics();
            jbApp.resetWorld();
            currentDistance = JointBotApplication.run(jbApp, JointBotApplication.RUN_TIME, IS_HEADLESS);
            return 1/currentDistance;
        } catch (LWJGLException ex) {
            Logger.getLogger(JointObjectiveFunction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JointObjectiveFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    @Override
    public boolean isFeasible(double[] x) {
        for (double xval : x) {
            if (xval < 0 || xval > 2*Math.PI) {
                return false;
            }
        }
        return true;
    }
    
}
