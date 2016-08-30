package edu.uvm.mecl.jointbot;

import javax.swing.JApplet;

/**
 *
 * @author mwagy
 */ 
public class AppletWrapper extends JApplet {

    Application app;
//    private final String DB_SERVER_IP_PARAM = "DBServerIp";
    
    @Override
    public void init() {
//        dbServerIp = this.getParameter(DB_SERVER_IP_PARAM);
        app = new Application();
    }
    
    @Override
    public void start() {
//        System.err.format("db: %s\n",dbServerIp);
        app.setVisible(true);
        app.runDemo();
    }
}
