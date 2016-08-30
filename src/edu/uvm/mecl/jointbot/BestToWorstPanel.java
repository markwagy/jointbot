package edu.uvm.mecl.jointbot;

import edu.uvm.mecl.jointbot.drawing.ArrowPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author mwagy
 */
public class BestToWorstPanel extends JPanel {
    private final String FONT = "TimesRoman";
    private final int FONT_SIZE = 10;
    private final float ARROW_PERC = 0.1f;
    
    private ArrowPanel bestArrow = new ArrowPanel();
    private ArrowPanel worstArrow = new ArrowPanel();
    private JPanel bestPanel = new JPanel();
    private JPanel worstPanel = new JPanel();
    private JPanel arrowPanel = new JPanel();
    private Font font;
    private Color color = Color.black;
    private Color backgroundColor = Color.white;
    
    public BestToWorstPanel() {
        setBackground(backgroundColor);
        font = new Font(FONT, Font.BOLD, FONT_SIZE);
        initPanels();
    }
    
    private void initPanels() {
//        setLayout(new GridLayout(1,3));
        setBackground(backgroundColor);
        setLayout(new BorderLayout());
        initBestPanel();
        initWorstPanel();
        initArrowPanel();
        add(worstPanel, BorderLayout.LINE_START);
        add(arrowPanel, BorderLayout.CENTER);
        add(bestPanel, BorderLayout.LINE_END);
    }
    
    private void initBestPanel() {
        JLabel bL = new JLabel("BEST");
        bL.setForeground(color);
        bL.setFont(font);
        bestPanel.setBackground(backgroundColor);
        bestPanel.add(bL);
    }
    
    private void initWorstPanel() {
        JLabel wL = new JLabel("WORST");
        wL.setForeground(color);
        wL.setFont(font);
        worstPanel.setBackground(backgroundColor);
        worstPanel.add(wL);
    }
    
    private void initArrowPanel() {
        worstArrow.flip();
        arrowPanel.setLayout(new GridLayout(1,2));
        arrowPanel.add(worstArrow);
        arrowPanel.add(bestArrow);
        setArrowPanelColor(Color.black);
        Dimension arrowPanelDim = new Dimension((int)(ARROW_PERC*getWidth()),getHeight());
        arrowPanel.setPreferredSize(arrowPanelDim);
    }
    
    private void setArrowPanelColor(Color col) {
        if (worstArrow != null && bestArrow != null) {
            worstArrow.setColor(backgroundColor);
            bestArrow.setColor(backgroundColor);
        }
    }
    
    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.add(new BestToWorstPanel());
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
    }
}
