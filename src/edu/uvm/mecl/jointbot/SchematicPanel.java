package edu.uvm.mecl.jointbot;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import edu.uvm.mecl.jointbot.drawing.Body;
import edu.uvm.mecl.jointbot.drawing.CompoundLine;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.CallableStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A drawing (only) of a joint configuration on a robot.
 * @author mwagy
 */
public class SchematicPanel extends JPanel {

    private final float JOINT_RADIUS_PERC = 0.03f;
    private final int LINE_WIDTH = 3;

    private boolean drawBodies = true;
    
    protected JointConfig jointConfig;
    protected ArrayList<Joint> joints = new ArrayList<Joint>();
    protected Body[] bodies = new Body[JointBotInfo.Bodies.Count.ordinal()];
    
    ArrayList<CompoundLine> compoundLines = new ArrayList<CompoundLine>();
    
    public SchematicPanel() {   
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.black));
        initBodies();
    }
    
    public void init(JointConfig jointConfig) {
        this.jointConfig = jointConfig;
        // reassign colorings according to JG order so that we are consistent
        this.jointConfig.reassignColors();
        if (jointConfig != null) {
            initSymbolComponents(jointConfig);
        }
    }
    
    public void setDrawBody(boolean doDraw) {
        drawBodies = doDraw;
    }
    
    private void initSymbolComponents(JointConfig jc) {
        ArrayList<JointGroup> jgs = (ArrayList<JointGroup>) (jc.getGroups()).clone();
        ArrayList<Float> phs = jc.getPhases();
        for (int i=0; i<jgs.size(); i++) {
            JointGroup jg = jgs.get(i);
            jg.setPhase(phs.get(i));
            ArrayList<Joint> js = (ArrayList<Joint>) (jg.getJointsInGroup()).clone();
            CompoundLine cL = null;
            for (int j=0; j<js.size(); j++) {
                Joint currJoint = js.get(j);
                currJoint.setDrawRadiusPerc(JOINT_RADIUS_PERC);
                if (j==0) {
                    cL = new CompoundLine(currJoint);
                } else {
                    cL.simplyAddJoint(currJoint);
                }
                joints.add(currJoint);
            }
            cL.setLineWidth(LINE_WIDTH);
            compoundLines.add(cL);
        }
    }
    
    private void initBodies() {
        for (int i=0; i<JointBotInfo.Bodies.Count.ordinal(); i++) {
            bodies[i] = new Body(JointBotInfo.Bodies.values()[i]);
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        draw(g2);
    }
    
    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (drawBodies) {
            drawBodies(g2, getWidth(), getHeight());
        }
        
        for (Joint joint : joints) {
            joint.draw(g2, getWidth(), getHeight());
        }
        
        for (CompoundLine cL : compoundLines) {
            cL.draw(g2, null);
        }
    }
    
    public void drawFixedSize(Graphics2D g2, int x, int y, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (drawBodies) {
            drawBodies(g2, x, y, w, h);
        }
        
        for (Joint joint : joints) {
            joint.draw(g2, x, y, w, h);
        }
        
        for (CompoundLine cL : compoundLines) {
            cL.draw(g2, null);
        }
    }
    
    public void cleanup() {
        for (CompoundLine cl : compoundLines) {
            cl.minimize();
        }
    }
    
    private void drawBodies(Graphics2D g2, int w, int h) {
        drawBodies(g2, 0, 0, w, h);
    }
    
    private void drawBodies(Graphics2D g2, int x, int y, int w, int h) {
        for (Body body : bodies) {
            body.draw(g2, x, y, w, h);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100,100);
    }
    
    public static ResultSet callableStatementSchematicPanels() throws SQLException {
        String CONN_STRING
                = "jdbc:mysql://localhost/jointbot?user=webuser&password=webber";
        Connection conn = (Connection) DriverManager.getConnection(CONN_STRING);
        
        CallableStatement cStmt = conn.prepareCall("{call get_history_panel_info(?,?,?)}");
        String isControlStr = "N";
        int NUM_SCHEM_PANELS = 40;
        cStmt.setString("p_is_control", isControlStr);
        cStmt.setString("p_ip_address", "");
        cStmt.setInt("p_num_panels", NUM_SCHEM_PANELS);
        cStmt.execute();
        
        ResultSet results = cStmt.getResultSet();
        return results;
    }
    
    public static ResultSet selectSchematicPanels(boolean isControl) throws SQLException {
        String control = "0";
        if (isControl) {
            control = "1";
        }
        String selectString
                = "SELECT " +
                "grp_s1_s2," +
                "grp_s1_u1," +
                "grp_s1_u2," +
                "grp_s2_u3," +
                "grp_s2_u4," +
                "grp_u1_l1," +
                "grp_u2_l2," +
                "grp_u3_l3," +
                "grp_u4_l4," +
                "max(distance) as max, is_control, count(*) as count " +
                "FROM tbl_configs a JOIN tbl_usergroup b ON a.ip_address = b.ip_address " +
                "WHERE is_control = '" + control + "' " + 
                "GROUP BY grp_s1_s2, grp_s1_u1, grp_s1_u2, grp_s2_u3, grp_s2_u4, grp_u1_l1, grp_u2_l2, grp_u3_l3, grp_u4_l4 " +
                "ORDER BY count desc limit 10";
        //               "FROM tbl_configs WHERE id = 58";
        //                "FROM tbl_configs WHERE id = 622";
        System.err.println(selectString);
        Connection conn = (Connection) DriverManager.getConnection("jdbc:mysql://patton/jointbot?user=webuser&password=webber");
        Statement stmt = (Statement) conn.createStatement();
        ResultSet results = stmt.executeQuery(selectString);
        return results;
    }

    public static void main(String args[]) throws SQLException {
        boolean IS_CONTROL = false;
        String title;
        if (IS_CONTROL) {
            title = "  Control";
        } else {
            title = "  Experimental";
        }
        JFrame f = new JFrame(title);
        f.add(new JLabel(title));
        GridLayout gl = new GridLayout();
        ResultSet resultsCtl = selectSchematicPanels(true);
        ResultSet resultsExp = selectSchematicPanels(false);
//        ResultSet results = callableStatementSchematicPanels();
        int i=0;

        ResultSet results;
        if (IS_CONTROL) {
            results = resultsCtl;
        } else {
            results = resultsExp;
        }
        while(results.next()) {
            SchematicPanel sp = new SchematicPanel();
            JointConfig jointConfig = Application.configFromResultSet(results);
            sp.init(jointConfig);
            String panelStr =  String.format("%.1f (%d runs)", 
                    new Float(results.getString("max")), new Integer(results.getString("count")));
            SchematicPanelWithInfo spi = new SchematicPanelWithInfo(sp, panelStr);
            f.add(spi);
            i++;
        }        

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gl.setRows((int)Math.sqrt(i/2));
        f.setLayout(gl);
        f.pack();
        f.setVisible(true);
    }

    void reset() {
        joints.clear();
        compoundLines.clear();
    }

}
