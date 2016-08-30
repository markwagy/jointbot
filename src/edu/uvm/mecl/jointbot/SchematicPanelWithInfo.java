package edu.uvm.mecl.jointbot;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author mwagy
 */
public class SchematicPanelWithInfo extends JPanel {
       
    public SchematicPanelWithInfo(SchematicPanel sp, String infoString) {
        this.setBackground(Color.lightGray);
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        JLabel label = new JLabel(infoString);
        this.setLayout(new BorderLayout());
        this.add(sp, BorderLayout.NORTH);
        this.add(label, BorderLayout.SOUTH);
    }
    
}
