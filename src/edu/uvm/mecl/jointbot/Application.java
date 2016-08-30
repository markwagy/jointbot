package edu.uvm.mecl.jointbot;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import edu.uvm.mecl.jointbot.JointBotInfo.Joints;
import edu.uvm.mecl.optimization.Solution;
import edu.uvm.mecl.optimization.SwingHillClimber;
import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mwagy
 */
public class Application extends javax.swing.JFrame implements ActionListener {
    
    private final static String VERSION = "0.0.1";
    
    private static enum OptimizationMethod {CMAES, HillClimber, ContributeRuns};
            
    private boolean USE_BACKGROUND_RUNS = true;
    //private boolean USE_BACKGROUND_RUNS = false;
    
    /* optimization parameters */
    private final OptimizationMethod optMethod = OptimizationMethod.ContributeRuns;
    //private final int NUM_GENERATIONS = 10;
    private final int NUM_BACKGROUND_RUNS = 4;
    private final float MUTATE_PROB = 0.1f;
    
    /* display parameters */
    private final float CAMERA_DISTANCE = -60f;
    private final int NUM_SCHEM_PANELS = 10;
    public static String IS_BACKGROUND = "T";
    public static String IS_NOT_BACKGROUND = "F";
    private final boolean IS_SPECTRUM_VIEW = true;
    //private final SchematicPanel[] schematicPanels = new SchematicPanel[NUM_SCHEM_PANELS];
    
    /* database parameters */
    private static String CONN_STRING 
            = "jdbc:mysql://localhost/jointbot?user=webuser&password=webber";
//            = "jdbc:mysql://23.239.11.215/jointbot?user=webuser&password=webber";

    /* experiment parameters */
    private boolean isControlGroup = false;
    
    JointBotApplication demoApp;
    static UUID SESSION_ID = UUID.randomUUID();
    ArrayList<JointGroup> jointGroups;
    private float mutateProb = MUTATE_PROB;
    
    private double bestDistance = Float.MIN_VALUE;
    private static String ipAddr;
    
    /**
     * Creates new form AppForm
     */
    public Application() {
        getIPAddress();
        setUserGroup(); // must be called after we obtain IP
        //System.err.println("User is in control group: " + isControlGroup);
        initComponents();
        setupHistoryPanel();
        runButton.addActionListener(this);
        this.setBackground(Color.darkGray);
    }
    
    public static void setDBServerIP(String dbServerIP) {
        CONN_STRING = String.format(
                "jdbc:mysql://%s/jointbot?user=webuser&password=webber", dbServerIP);
    }
    
    private void getIPAddress() {
        try {
            ipAddr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void runDemo() {
        try {
            demoApp = new JointBotApplication(demoPanel.getGL(),
                    fitnessLabel, drawingPanel, progressBar);
            demoApp.initPhysics();
            demoApp.setCameraDistance(CAMERA_DISTANCE);
            demoApp.setDistanceSlider(distanceSlider);
            demoPanel.runDemo(demoApp);
            repaint();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        
        setupHistoryPanel();
        jointGroups = drawingPanel.getJointGroups();
        //System.err.println(JointGroup.summarizeJointGroups(jointGroups));
        //statusLabel.setText("Loading model..."); // this doesn't work for some reason. probably threads
        //            ArrayList<Float> bestPhases = runPreSimulations(NUM_GENERATIONS);
        
        Float[] phasesArr = getBestPhasesFromDB(jointGroups);
        // mutate
        phasesArr = JointConfig.getMutatedVersion(phasesArr, mutateProb);
        ArrayList<Float> phases = new ArrayList<Float>(Arrays.asList(phasesArr));
        JointConfig config = new JointConfig(jointGroups, phases);
        demoApp.setConfig(config);
        if (USE_BACKGROUND_RUNS) {
            try {
                //runPreSimulations(NUM_GENERATIONS);
                contributeRuns(NUM_BACKGROUND_RUNS, config);
            } catch (InterruptedException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        demoApp.runSimulation(jointGroups, phases);
        
        //            bestDistance = JointBotApplication.run(demoApp, 1000);
        //writeToDB(config, bestDistance, IS_NOT_BACKGROUND);
        //setupHistoryPanel();
    }
    
    public static void writeToDB(JointConfig config, double dist, String isBackgroundRun) {
        Connection conn;
        try {
            conn = (Connection) DriverManager.getConnection(CONN_STRING);
            
            CallableStatement cStmt = conn.prepareCall("{call add_config(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
            
            cStmt.setFloat("phase_s1_s2", config.getJointPhase(JointBotInfo.Joints.Spine1_Spine2));
            
            cStmt.setFloat("phase_s1_u1", config.getJointPhase(JointBotInfo.Joints.Spine1_UpperLeg1));
            cStmt.setFloat("phase_s1_u2", config.getJointPhase(JointBotInfo.Joints.Spine1_UpperLeg2));
            cStmt.setFloat("phase_s2_u3", config.getJointPhase(JointBotInfo.Joints.Spine2_UpperLeg3));
            cStmt.setFloat("phase_s2_u4", config.getJointPhase(JointBotInfo.Joints.Spine2_UpperLeg4));
            
            cStmt.setFloat("phase_u1_l1", config.getJointPhase(JointBotInfo.Joints.UpperLeg1_LowerLeg1));
            cStmt.setFloat("phase_u2_l2", config.getJointPhase(JointBotInfo.Joints.UpperLeg2_LowerLeg2));
            cStmt.setFloat("phase_u3_l3", config.getJointPhase(JointBotInfo.Joints.UpperLeg3_LowerLeg3));
            cStmt.setFloat("phase_u4_l4", config.getJointPhase(JointBotInfo.Joints.UpperLeg4_LowerLeg4));
            
            int[] groupIds = JointConfig.getGroupIds(config.getGroups());
            
            cStmt.setInt("group_s1_s2", groupIds[Joints.Spine1_Spine2.ordinal()]);
            
            cStmt.setInt("group_s1_u1", groupIds[Joints.Spine1_UpperLeg1.ordinal()]);
            cStmt.setInt("group_s1_u2", groupIds[Joints.Spine1_UpperLeg2.ordinal()]);
            cStmt.setInt("group_s2_u3", groupIds[Joints.Spine2_UpperLeg3.ordinal()]);
            cStmt.setInt("group_s2_u4", groupIds[Joints.Spine2_UpperLeg4.ordinal()]);
            
            cStmt.setInt("group_u1_l1", groupIds[Joints.UpperLeg1_LowerLeg1.ordinal()]);
            cStmt.setInt("group_u2_l2", groupIds[Joints.UpperLeg2_LowerLeg2.ordinal()]);
            cStmt.setInt("group_u3_l3", groupIds[Joints.UpperLeg3_LowerLeg3.ordinal()]);
            cStmt.setInt("group_u4_l4", groupIds[Joints.UpperLeg4_LowerLeg4.ordinal()]);
            
            cStmt.setFloat("distance", (float) dist);
            cStmt.setString("session_id", SESSION_ID.toString());
            cStmt.setString("background_run", isBackgroundRun);
            cStmt.setString("ip_address", ipAddr);
            cStmt.setString("version_id", VERSION);
            
            cStmt.execute();
            
        } catch (SQLException ex) {
            System.err.println("connection error");
            System.err.println(ex);
        }
    }
    
    private Float[] getBestPhasesFromDB(ArrayList<JointGroup> jointGrps) {
        
        // initially populate with random phases in case this group doesn't exist in the DB yet
        int[] groupIds = JointConfig.getGroupIds(jointGrps);
        Float[] phaseValues = JointConfig.getRandomPhaseValues(groupIds);
        try {
            Connection con = (Connection) DriverManager.getConnection(CONN_STRING);
            CallableStatement cStmt = con.prepareCall("{call get_best_phase_vals_by_groupids(?,?,?,?,?,?,?,?,?,?,?)}");
            cStmt.setInt("group_s1_s2", groupIds[Joints.Spine1_Spine2.ordinal()]);
            
            cStmt.setInt("group_s1_u1", groupIds[Joints.Spine1_UpperLeg1.ordinal()]);
            cStmt.setInt("group_s1_u2", groupIds[Joints.Spine1_UpperLeg2.ordinal()]);
            cStmt.setInt("group_s2_u3", groupIds[Joints.Spine2_UpperLeg3.ordinal()]);
            cStmt.setInt("group_s2_u4", groupIds[Joints.Spine2_UpperLeg4.ordinal()]);
            
            cStmt.setInt("group_u1_l1", groupIds[Joints.UpperLeg1_LowerLeg1.ordinal()]);
            cStmt.setInt("group_u2_l2", groupIds[Joints.UpperLeg2_LowerLeg2.ordinal()]);
            cStmt.setInt("group_u3_l3", groupIds[Joints.UpperLeg3_LowerLeg3.ordinal()]);
            cStmt.setInt("group_u4_l4", groupIds[Joints.UpperLeg4_LowerLeg4.ordinal()]);
            
            String isControlStr = "N";
            if (this.isControlGroup) {
                isControlStr = "Y";
            }
            cStmt.setString("p_is_control", isControlStr);
            cStmt.setString("p_ip_address", ipAddr);

            cStmt.execute();
            
            ResultSet results = cStmt.getResultSet();
            
            while (results.next()) {
                phaseValues[JointBotInfo.Joints.Spine1_Spine2.ordinal()]       = results.getFloat("s1_s2");
                phaseValues[JointBotInfo.Joints.Spine1_UpperLeg1.ordinal()]    = results.getFloat("s1_u1");
                phaseValues[JointBotInfo.Joints.Spine1_UpperLeg2.ordinal()]    = results.getFloat("s1_u2");
                phaseValues[JointBotInfo.Joints.Spine2_UpperLeg3.ordinal()]    = results.getFloat("s2_u3");
                phaseValues[JointBotInfo.Joints.Spine2_UpperLeg4.ordinal()]    = results.getFloat("s2_u4");
                phaseValues[JointBotInfo.Joints.UpperLeg1_LowerLeg1.ordinal()] = results.getFloat("u1_l1");
                phaseValues[JointBotInfo.Joints.UpperLeg2_LowerLeg2.ordinal()] = results.getFloat("u2_l2");
                phaseValues[JointBotInfo.Joints.UpperLeg3_LowerLeg3.ordinal()] = results.getFloat("u3_l3");
                phaseValues[JointBotInfo.Joints.UpperLeg4_LowerLeg4.ordinal()] = results.getFloat("u4_l4");
            }
            return phaseValues;
        } catch (SQLException ex) {
            System.err.println("unable to get best phases from DB");
            System.err.println(ex);
        }
        System.err.println("ERROR: did not find best phases");
        return JointConfig.getRandom().getGroupPhases();
    }
    
//    private ArrayList<Float> runPreSimulations(int howMany) throws IOException {
    private void runPreSimulations(int howMany) throws IOException {
        ArrayList<JointGroup> currJointGroups = drawingPanel.getJointGroups();
        JointBotApplication jbApp
                = new JointBotApplication(demoPanel.getGL(), currJointGroups);
        jbApp.initPhysics();
        IObjectiveFunction fitfun = new JointObjectiveFunction(jbApp);
//        ArrayList<Float> bestPhases = optimizePhaseAngles(fitfun, howMany);
        optimizePhaseAngles(fitfun, howMany);
//        jbApp.destroy();
//        return bestPhases;
    }
    
//    private ArrayList<Float> optimizePhaseAngles(IObjectiveFunction fitfun, int numGens) {
    private void optimizePhaseAngles(IObjectiveFunction fitfun, int numGens) {
        Solution bestSolution = null;
        switch (optMethod) {
            case CMAES:
                bestSolution = runCMAES(fitfun, numGens);
                break;
            case HillClimber:
                try {
                    //bestSolution = runHillClimber(fitfun, numGens);
                    runHillClimber(fitfun,numGens);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            default:
                try {
//                    bestSolution = runHillClimber(fitfun, numGens);
                    runHillClimber(fitfun,numGens);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
//        return bestSolution.getElements();
    }
    
    private void contributeRuns(int numRuns, JointConfig jc)
        throws InterruptedException, ExecutionException {
        for (int i=0; i<numRuns; i++) {
            BackgroundRunner br = new BackgroundRunner(numRuns, jc, demoPanel.getGL());
            br.execute();
        }
    }
    
    private Solution runCMAES(IObjectiveFunction fitfun, int numGens) {
        ArrayList<Float> bestPhases = new ArrayList<Float>();
        
        CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
        cma.setDimension(drawingPanel.getJointGroups().size());
        cma.setInitialX(0.0);
        cma.setInitialStandardDeviation(0.1);
        cma.options.stopMaxIter = numGens;
        double[] fitness = cma.init();
        while (cma.stopConditions.getNumber() == 0) {
            double[][] pop = cma.samplePopulation();
            for (int i=0; i<pop.length; i++) {
                while (!fitfun.isFeasible(pop[i])) {
                    pop[i] = cma.resampleSingle(i);
                }
                fitness[i] = fitfun.valueOf(pop[i]);
            }
            cma.updateDistribution(fitness);
            int outmod = 150;
            if (cma.getCountIter() % (15*outmod) == 1) {
                cma.printlnAnnotation();
            }
            if (cma.getCountIter() % outmod == 1) {
                cma.println();
            }
        }
        cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX()));
        cma.println("Terminated due to");
        for (String s : cma.stopConditions.getMessages()) {
            cma.println(" " + s);
        }
        cma.println("best function value " + cma.getBestFunctionValue()
                + " at evaluation " + cma.getBestEvaluationNumber());
        bestDistance = cma.getBestFunctionValue();
        double[] bestX = cma.getBestX();
        for (double x : bestX) {
            bestPhases.add(new Float(x));
        }
        return new Solution(bestDistance, bestPhases);
    }
    
    private void runHillClimber(IObjectiveFunction fitfun, int numGens) 
            throws InterruptedException, ExecutionException {

        ArrayList<Float> minVals = new ArrayList<Float>();
        ArrayList<Float> maxVals = new ArrayList<Float>();
        for (int i=0; i<jointGroups.size(); i++) {
            minVals.add(0f);
            maxVals.add((float) (2*Math.PI));
        }
        SwingHillClimber hc = new SwingHillClimber(numGens, minVals, 
                maxVals, fitfun, mutateProb, jointGroups);
        hc.execute();
//        hc.get(); // block
        //Solution bestSolution = hc.run();
        /*
        Solution bestSolution = hc.getSolution();
        ArrayList<Float> bestPhases = bestSolution.getElements();
        bestDistance = 1/hc.getBestFitness();
        return new Solution(bestDistance, bestPhases);
        * */
    }
    
    private void printPhases(ArrayList<Float> phases) {
        for (Float p : phases) {
            System.err.print(" " + p + " ");
        }
        System.err.println();
    }
    
    private void setupHistoryPanel() {
        // refresh things
        resetHistoryPanel();
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;

        try {
            conn = (Connection) DriverManager.getConnection(CONN_STRING);
              
            CallableStatement cStmt = conn.prepareCall("{call get_history_panel_info(?,?,?)}");
            String isControlStr;
            if (isControlGroup) {
                isControlStr = "Y";
            } else {
                isControlStr = "N";
            }

            cStmt.setString("p_is_control", isControlStr);
            cStmt.setString("p_ip_address", ipAddr);
            cStmt.setInt("p_num_panels", NUM_SCHEM_PANELS);
            cStmt.execute();
            
            results = cStmt.getResultSet();
            int i=0;
            Float dist = 0f;
            while(results.next()) { 
                SchematicPanel schematicPanel = schematicPanelFromDBResult(results);
                schematicPanel.cleanup();
                dist = results.getFloat("distance");
                //Float perc = results.getFloat("percent");
                Integer tries = results.getInt("tries");
                distanceSlider.addSegment(dist, tries, schematicPanel);
                //distanceSlider.addSchematicPanel(schematicPanel);
                i++;
            }
            repaint();
        } catch (SQLException ex) {
            System.err.println("select didn't work on configs: " + ex);
        } finally {
            if (results != null) {
                try {
                    results.close(); 
                } catch (SQLException ex) {
                    System.err.println("unable to close results: " + ex);
                }
                results = null;
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    System.err.println("unable to close statement: " + ex);
                }
                stmt = null;
            }
        }
    }
    
    private void resetHistoryPanel() {
        distanceSlider.reset();
    }
    
    private void setUserGroup() {
        try {
            Connection conn = (Connection) DriverManager.getConnection(CONN_STRING);
            CallableStatement stmt = conn.prepareCall("{call get_usergroup(?,?)}");
            stmt.setString(1, ipAddr);
            stmt.registerOutParameter(2, java.sql.Types.INTEGER);
            stmt.execute();
            int group = stmt.getInt(2);
            if (group==0) {
                isControlGroup = false;
            } else {
                // control group has a period after the word "moved"
                isControlGroup = true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private SchematicPanel schematicPanelFromDBResult(ResultSet res) throws SQLException {
        SchematicPanel sp = new SchematicPanel();
        JointConfig jointConfig = configFromResultSet(res);
        sp.init(jointConfig);
        return sp;
    }
    
    public static JointConfig configFromResultSet(ResultSet res) throws SQLException {
        HashMap<Float, ArrayList<Joints>> hm = new HashMap<Float, ArrayList<Joints>>();
        
        putJoint(hm, res.getFloat("grp_s1_s2"), Joints.Spine1_Spine2);
        
        putJoint(hm, res.getFloat("grp_s1_u1"), Joints.Spine1_UpperLeg1);
        putJoint(hm, res.getFloat("grp_s1_u2"), Joints.Spine1_UpperLeg2);
        putJoint(hm, res.getFloat("grp_s2_u3"), Joints.Spine2_UpperLeg3);
        putJoint(hm, res.getFloat("grp_s2_u4"), Joints.Spine2_UpperLeg4);

        putJoint(hm, res.getFloat("grp_u1_l1"), Joints.UpperLeg1_LowerLeg1);
        putJoint(hm, res.getFloat("grp_u2_l2"), Joints.UpperLeg2_LowerLeg2);
        putJoint(hm, res.getFloat("grp_u3_l3"), Joints.UpperLeg3_LowerLeg3);
        putJoint(hm, res.getFloat("grp_u4_l4"), Joints.UpperLeg4_LowerLeg4);

        return new JointConfig(hm);
    }
    
    /**
     * changes hm
     * 
     * @param hm
     * @param phase
     * @param joint 
     */
    private static void putJoint(HashMap<Float, ArrayList<Joints>> hm, Float phase, Joints joint) {
        // already in hashmap?
        if (hm.containsKey(phase)) {
            ArrayList<Joints> js = hm.get(phase);
            js.add(joint);
        } else {
            ArrayList<Joints> js = new ArrayList<Joints>();
            js.add(joint);
            hm.put(phase, js);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        simulationPanel = new javax.swing.JPanel();
        demoPanel = new com.bulletphysics.demos.applet.DemoPanel();
        designPanel = new javax.swing.JPanel();
        drawingPanel = new edu.uvm.mecl.jointbot.DrawingPanel();
        runButton = new javax.swing.JButton();
        resetButton = new javax.swing.JButton();
        quitButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        distanceLabel = new javax.swing.JLabel();
        fitnessLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        distanceSlider = new edu.uvm.mecl.jointbot.FitnessSlider();
        bestToWorstPanel1 = new edu.uvm.mecl.jointbot.BestToWorstPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(102, 102, 102));
        setPreferredSize(new java.awt.Dimension(1010, 870));

        simulationPanel.setBackground(new java.awt.Color(102, 102, 102));
        simulationPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        demoPanel.setToolTipText("This is the simulation window, where your robot will move after clicking the \"GO\" button.");

        org.jdesktop.layout.GroupLayout demoPanelLayout = new org.jdesktop.layout.GroupLayout(demoPanel);
        demoPanel.setLayout(demoPanelLayout);
        demoPanelLayout.setHorizontalGroup(
            demoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 602, Short.MAX_VALUE)
        );
        demoPanelLayout.setVerticalGroup(
            demoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        designPanel.setBackground(new java.awt.Color(102, 102, 102));
        designPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Design (top view of robot)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), new java.awt.Color(255, 255, 255))); // NOI18N
        designPanel.setToolTipText("Click and drag on circles to connect robot leg joints. \nThis will cause the joints to move together in the simulation.");

        drawingPanel.setToolTipText("Click and drag on circles to connect robot leg joints. \nThis will cause the joints to move together in the simulation.");

        org.jdesktop.layout.GroupLayout drawingPanelLayout = new org.jdesktop.layout.GroupLayout(drawingPanel);
        drawingPanel.setLayout(drawingPanelLayout);
        drawingPanelLayout.setHorizontalGroup(
            drawingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 274, Short.MAX_VALUE)
        );
        drawingPanelLayout.setVerticalGroup(
            drawingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 338, Short.MAX_VALUE)
        );

        runButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/uvm/mecl/jointbot/go.png"))); // NOI18N
        runButton.setToolTipText("Click here to run your robot design in the simulation window.");
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        resetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/uvm/mecl/jointbot/reset.png"))); // NOI18N
        resetButton.setToolTipText("Reset the drawing to the default.");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout designPanelLayout = new org.jdesktop.layout.GroupLayout(designPanel);
        designPanel.setLayout(designPanelLayout);
        designPanelLayout.setHorizontalGroup(
            designPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(designPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(designPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(designPanelLayout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(drawingPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 276, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(designPanelLayout.createSequentialGroup()
                        .add(runButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(resetButton)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        designPanelLayout.setVerticalGroup(
            designPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(designPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(designPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(runButton)
                    .add(resetButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(drawingPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 340, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        quitButton.setText("Quit");
        quitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                quitButtonMouseClicked(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(102, 102, 102));

        statusLabel.setForeground(new java.awt.Color(255, 255, 255));
        statusLabel.setText("TIME LEFT:");
        statusLabel.setToolTipText("This is how much time is left in the simulation for your robot to move.");

        progressBar.setToolTipText("This is how much time is left in the simulation for your robot to move.");

        distanceLabel.setForeground(new java.awt.Color(255, 255, 255));
        distanceLabel.setText("DISTANCE:");
        distanceLabel.setToolTipText("This is how much your robot has moved so far.");

        fitnessLabel.setBackground(new java.awt.Color(153, 153, 153));
        fitnessLabel.setForeground(new java.awt.Color(255, 255, 255));

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(11, 11, 11)
                .add(statusLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 216, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 39, Short.MAX_VALUE)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(77, 77, 77)
                .add(distanceLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fitnessLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 108, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jSeparator1)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, fitnessLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, distanceLabel)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusLabel))))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout simulationPanelLayout = new org.jdesktop.layout.GroupLayout(simulationPanel);
        simulationPanel.setLayout(simulationPanelLayout);
        simulationPanelLayout.setHorizontalGroup(
            simulationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(simulationPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(simulationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(demoPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(simulationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(designPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, quitButton))
                .addContainerGap())
        );
        simulationPanelLayout.setVerticalGroup(
            simulationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, simulationPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(simulationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(simulationPanelLayout.createSequentialGroup()
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(demoPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(designPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(quitButton))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel4.setBackground(new java.awt.Color(204, 204, 204));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        jLabel1.setText("<html><b>Can you design a robot that moves farther?</b></html>");

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(16, 16, 16)
                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBackground(new java.awt.Color(204, 204, 204));

        jLabel2.setText("<html><b>1. Design the robot:</b></html>");

        jLabel5.setText("<html>Click and drag on the circles in the <b>Design</b> panel on the right. These circles represent robot leg joints and will make the joints in the same color group move together. Clicking on them with the right mouse button splits the groupings.</html>");

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(16, 16, 16)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 237, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 129, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel7.setBackground(new java.awt.Color(204, 204, 204));

        jLabel3.setText("<html><b>2. Run the simulation:</b></html>");

        jLabel4.setText("<html>Click the <b>GO</b> button to see your robot design move in the simulation window on the left in the time allotted. You'll be able to see how how it moves compared to some past designs by watching the red pointer.\n<p><i>Then try again!</i>");

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel3)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(0, 1, Short.MAX_VALUE)
                        .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 365, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(16, 16, 16))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Past Designs"));

        distanceSlider.setBackground(new java.awt.Color(255, 255, 255));
        distanceSlider.setToolTipText("These are past robot designs showing how the leg joints were grouped together to get the robot to move in different ways. \nDesigns are ranked from \"worst\" (shortest distance traveled) to \"best\" (longest distance traveled).");

        org.jdesktop.layout.GroupLayout distanceSliderLayout = new org.jdesktop.layout.GroupLayout(distanceSlider);
        distanceSlider.setLayout(distanceSliderLayout);
        distanceSliderLayout.setHorizontalGroup(
            distanceSliderLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        distanceSliderLayout.setVerticalGroup(
            distanceSliderLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 149, Short.MAX_VALUE)
        );

        bestToWorstPanel1.setForeground(new java.awt.Color(255, 255, 255));

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(distanceSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(bestToWorstPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(bestToWorstPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(distanceSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(simulationPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(simulationPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_runButtonActionPerformed

    private void quitButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_quitButtonMouseClicked
        System.exit(0);
    }//GEN-LAST:event_quitButtonMouseClicked

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        drawingPanel.reset();
    }//GEN-LAST:event_resetButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Application appForm = new Application();
                appForm.setVisible(true);
                appForm.runDemo();
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private edu.uvm.mecl.jointbot.BestToWorstPanel bestToWorstPanel1;
    private com.bulletphysics.demos.applet.DemoPanel demoPanel;
    private javax.swing.JPanel designPanel;
    private javax.swing.JLabel distanceLabel;
    private edu.uvm.mecl.jointbot.FitnessSlider distanceSlider;
    private edu.uvm.mecl.jointbot.DrawingPanel drawingPanel;
    private javax.swing.JLabel fitnessLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton quitButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton runButton;
    private javax.swing.JPanel simulationPanel;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}
