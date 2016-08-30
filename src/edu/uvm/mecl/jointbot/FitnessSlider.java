package edu.uvm.mecl.jointbot;

import com.mysql.jdbc.Connection;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.CallableStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author mwagy
 */
public class FitnessSlider extends JPanel {
    private final int PREFERRED_HEIGHT = 200;
    
    private final int LINE_WIDTH      = 3;
    private final int LINE_BOTTOM_OFFSET = 10;
    private final int TICK_HEIGHT     = 20;
    private final int TICK_WIDTH      = 2;
    private final int POINTER_WIDTH   = 30;
    private final Color POINTER_COLOR = Color.red;
    private final float POINTER_ALPHA_VALUE = 0.6f;
    
    private final int TOP_LABEL_Y_OFFSET = 10;
    private final int TOP_LABEL_X_OFFSET = 5;
    
    private final int SEGMENT_STOP_SHORT_AMT = 10;
    private final int SEGMENT_LINE_OFFSET = TICK_HEIGHT;
    private final int SEGMENT_LINEWIDTH = 2;
    private final int SEGMENT_LABEL_OFFSET_Y = 5;
    private final int SEGMENT_LABEL_OFFSET_X = 2;
    private final Color SEGMENT_INACTIVE_COLOR = new Color(240,240,240);
    private final Color SEGMENT_ACTIVE_COLOR = SEGMENT_INACTIVE_COLOR.brighter();

    private final int SCHEMATIC_PANEL_Y_OFFSET = 7;
    private final int SCHEMATIC_PANEL_WIDTH = 100;
    private final int SCHEMATIC_PANEL_HEIGHT = 100;

    private final int PIXELS_PER_LETTER = 7;
    
    ArrayList<Segment> segments = new ArrayList<Segment>();
    Pointer pointer = new Pointer(
            getHeight(), POINTER_WIDTH, 
            getHeight(), POINTER_COLOR);
    
    private boolean drawTicks = true;
    private float pointerValue = 0f;
    private Color color = Color.BLACK;
    private Segment activeSegment;
    //private ArrayList<SchematicPanel> schematicPanels;
    private int schematicPanelHeight;
    
    public FitnessSlider() {
        //schematicPanels = new ArrayList<SchematicPanel>();
        this.getPreferredSize().height = PREFERRED_HEIGHT;
    }
    
    private AlphaComposite makeComposite(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return(AlphaComposite.getInstance(type, alpha));
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        drawLine(g2);
        g2.setComposite(makeComposite(POINTER_ALPHA_VALUE));
        drawPointer(g2);
    }
    
    public void setTicksOn() {
        drawTicks = true;
    }
    
    public void setTicksOff() {
        drawTicks = false;
    }
    
    public void reset() {
        segments.clear();
        //schematicPanels.clear();
    }
    
    /*
    public void addSchematicPanel(SchematicPanel sp) {
        schematicPanels.add(sp);
    }
    */
    
    private void drawLine(Graphics2D g2) {
        int h = getHeight();
        int w = getWidth();
        g2.setStroke(new BasicStroke(LINE_WIDTH));
        g2.drawLine(0, getLineYPos(), w, getLineYPos());
        if (drawTicks) {
            drawSegments(g2);
        }
    }
    
    private void drawSegments(Graphics2D g2) {
        float w = (float) getWidth();
        float numSegments = segments.size();
        int segmentWidth = (int) (w/numSegments);
        
        for (int segmentIdx=0; segmentIdx<segments.size(); segmentIdx++) {
            Segment currentSegment = segments.get(segmentIdx);
            
            // segments must be evenly spaced
            int segmentStartPos = segmentIdx*segmentWidth + TICK_WIDTH;
            int segmentEndPos   = (segmentIdx+1)*segmentWidth - SEGMENT_STOP_SHORT_AMT;
            //schematicPanelHeight = (int) (SCHEMATIC_PANEL_HEIGHT_PROP*segmentWidth);
            schematicPanelHeight = SCHEMATIC_PANEL_HEIGHT;
                        
            // tick
            g2.setColor(color);
            g2.setStroke(new BasicStroke(LINE_WIDTH));
            //g2.drawLine(segmentStartPos, getLineYPos(), segmentStartPos, schematicPanelHeight+LINE_WIDTH);
            g2.drawLine(segmentStartPos, getLineYPos(), segmentStartPos, 0);

            // segment line
            g2.setStroke(new BasicStroke(SEGMENT_LINEWIDTH));
            g2.setColor(Color.gray);
            g2.drawLine(
                    segmentStartPos, getLineYPos()- SEGMENT_LINE_OFFSET, 
                    segmentEndPos, getLineYPos() - SEGMENT_LINE_OFFSET);
            
            // segment rectangle
            if (currentSegment.isActive) {
                g2.setColor(SEGMENT_ACTIVE_COLOR);
            } else {
                g2.setColor(SEGMENT_INACTIVE_COLOR);
            }
            /*
            g2.fillRect(
                    segmentStartPos, 
                    schematicPanelHeight,
                    segmentWidth - SEGMENT_STOP_SHORT_AMT + TICK_WIDTH, 
                    getHeight() - (getHeight() - getLineYPos()) - schematicPanelHeight - SEGMENT_LINE_OFFSET);
                    */
            g2.fillRect(
                    segmentStartPos, 
                    0,
                    segmentWidth - SEGMENT_STOP_SHORT_AMT + TICK_WIDTH, 
                    getHeight() - (getHeight() - getLineYPos()) - SEGMENT_LINE_OFFSET);
                        
            // labels
            g2.setColor(Color.black);
            String bottomLabel = currentSegment.getBottomLabel();
            int labelPosX = getLabelPosX(segmentStartPos, segmentEndPos, bottomLabel);
            int labelPosY = getLabelPosY();
            g2.drawString(bottomLabel, labelPosX, labelPosY);
            String topLabel = currentSegment.getTopLabel();
            labelPosX += TOP_LABEL_X_OFFSET;
            labelPosY = TOP_LABEL_Y_OFFSET;
            g2.drawString(topLabel, labelPosX, labelPosY);
            
            // schematic panel
            SchematicPanel sp = currentSegment.getSchematicPanel();
            sp.drawFixedSize(g2, 
//                    segmentStartPos + ((int) (.001*(segmentWidth-SCHEMATIC_PANEL_WIDTH))), 
                    segmentStartPos,
                    SCHEMATIC_PANEL_Y_OFFSET, 
                    SCHEMATIC_PANEL_WIDTH, 
                    schematicPanelHeight);
        }
    }
    
    private int getLabelPosX(int segmentStartPos, int segmentEndPos, String labelText) {
        /*
        return segmentStartPos + 
                (segmentEndPos-segmentStartPos)/2 - getPixels(labelText)/2;
                */
        return segmentStartPos + SEGMENT_LABEL_OFFSET_X;
    }
    
    private int getLabelPosY() {
        return getLineYPos() - SEGMENT_LINE_OFFSET - SEGMENT_LABEL_OFFSET_Y;
    }
    
    private int getPixels(String str) {
        return PIXELS_PER_LETTER * str.length();
    }
    
    private int getLineXStart() {
        return 0;
    }
    
    private int getLineXEnd() {
        int w = getWidth();
        return w;
    }
    
    private int getLineYPos() {
        return getHeight() - LINE_BOTTOM_OFFSET;
    }
    
    private float getRange() {
        return lastSegment().endValue - firstSegment().startValue;
    }
    
    private Segment firstSegment() {
        Collections.sort(segments);
        return segments.get(0);
    }
    
    private Segment lastSegment() {
        Collections.sort(segments);
        return segments.get(segments.size()-1);
    }
    
    public void setPointerValue(float val) {
        pointerValue = val;
        if (segments.size()>0) {
            updatePointerPosition();
        }
    }
    
    private void updatePointerPosition() {
        if (activeSegment != null) {
            activeSegment.toggleActive();
        }
        activeSegment = getActiveSegment(pointerValue);
        activeSegment.toggleActive();
        int posWithinSegment = getPositionWithinSegment(
                pointerValue, 
                activeSegment.getStartValue(), activeSegment.getEndValue(),
                getSegmentStartPos(activeSegment), getSegmentEndPos(activeSegment));
        int segmentStartPos = getSegmentStartPos(activeSegment);
        pointer.setPositionX(segmentStartPos + posWithinSegment);

        pointer.setHeight(getHeight()-schematicPanelHeight);
        pointer.setLinePosY(getHeight());
    }
    
    private int getLineXRange() {
        return getLineXEnd() - getLineXStart();        
    }
    
    private int getSegmentDrawLength() {
        return getLineXRange()/segments.size();
    }
    
    private int getSegmentStartPos(Segment seg) {
        Collections.sort(segments);
        int segIdx = segments.indexOf(seg);
        int segmentDrawLength = getSegmentDrawLength();
        return segmentDrawLength*segIdx;
    }
    
    private int getSegmentEndPos(Segment seg) {
        int segStartPos = getSegmentStartPos(seg);
        return segStartPos + getSegmentDrawLength();
    }
    
    private int getPositionWithinSegment(
            float val, float segMin, float segMax, 
            int segStartPos, int segEndPos) {
        // percent value is of segment bounds
        float valPercOfSegment;
        // it is possible that (in the single segment case) the pointer is outside the bounds
        // in which case we just want to place it at the far left of the segment
        if (val >= segMax) {
            valPercOfSegment = 1;
        } else if (val < segMin) {
            valPercOfSegment = 0;
        } else {
            valPercOfSegment = (val-segMin)/(segMax-segMin);            
        } 

        int segPosRange = segEndPos-segStartPos;
        int valPos = (int) (valPercOfSegment*segPosRange);
        return valPos;
    }
    
    private Segment getActiveSegment(float value) {
        for (Segment s : segments) {
            if (s.contains(value)) {
                return s;
            }
        }
        Collections.sort(segments);
        if (value > segments.get(segments.size()-1).endValue) {
            return segments.get(segments.size()-1);
        } else {
            // this is only when the pointer hasn't been set yet
            // or when value is less than the min segment bound
            return segments.get(0);
        }
    }
    
    private void drawPointer(Graphics2D g2) {
        if (segments.size()>0) {
            updatePointerPosition();
        }
        pointer.draw(g2);
    }
    
    // segment is defined by its starting position
    public void addSegment(float value, int tries, SchematicPanel sp) {
        Segment newSegment = new Segment(value, tries, sp);
        //newSegment.setTries(tries);
        // if there aren't any segments yet
        if (segments.isEmpty()) {
            newSegment.setEndValue(2*value);
            segments.add(newSegment);
            return;
        } 

        // if there are already segments
        Segment lastSegment = lastSegment();
        Segment firstSegment = firstSegment();
        if (value > lastSegment.getStartValue()) {
            // we have a new last segment
            lastSegment.setEndValue(value);
            // copy range of last end value as an approximation for a good end value
            newSegment.setEndValue(value + lastSegment.getRange());
        } else if (value < firstSegment.getStartValue()) {
            // this should be the first segment
            newSegment.setEndValue(firstSegment.getStartValue());
        } else {
            Segment leftSegment = getSegmentLeft(value);
            Segment rightSegment = getSegmentRight(value);
            // squeeze into its proper place
            newSegment.setEndValue(rightSegment.getStartValue());
            leftSegment.setEndValue(newSegment.getStartValue());
        }
        segments.add(newSegment);
        Collections.sort(segments);
    }
    
    private Segment getSegmentLeft(float value) {
        Collections.sort(segments);
        Segment prev = segments.get(0);
        for (Segment s : segments) {
            if (s.getStartValue()>value) {
                return prev;
            }
            prev = s;
        }
        return null;
    }
    
    private Segment getSegmentRight(float value) {
        Collections.sort(segments);
        for (Segment s : segments) {
            if (s.getEndValue() >= value) {
                return s;
            }
        }
        return null;
    }
    
    private class Pointer {
        private final int DEFAULT_HEIGHT = 30;
        private final int DEFAULT_WIDTH  = 5;
        int positionX = 0;
        int linePosY  = 0;
        int height = DEFAULT_HEIGHT;
        int width  = DEFAULT_WIDTH;
        Color color;
        
        public Pointer(int h, int w, int y, Color col) {
            this(h, w, y);
            this.color = col;
        }
        
        public Pointer(int height, int width, int linePosY) {
            this.width = width;
            this.height = height;
            this.linePosY = linePosY;
        }
        
        public void setPositionX(int pos) {
            positionX = pos;
        }
        
        public void setLinePosY(int pos) {
            linePosY = pos;
        }
        
        public void setHeight(int h) {
            height = h;
        }
        
        public void setWidth(int w) {
            width = w;
        }
        
        public void draw(Graphics2D g2) {
            g2.setColor(color);
            int x[] = {positionX-width/2, positionX+width/2, positionX};
            int y[] = {linePosY, linePosY, linePosY - height};
            g2.fillPolygon(x, y, x.length);
            // shadow
            int shadowOffset=1;
            g2.setColor(color.darker().darker());
            x[0] = positionX-width/2-shadowOffset;
            x[1] = positionX+width/2-shadowOffset;
            x[2] = positionX-shadowOffset;
            y[0] = linePosY+shadowOffset;
            y[1] = linePosY + shadowOffset;
            y[2] = linePosY - height+shadowOffset;
            g2.fillPolygon(x, y, x.length);

        }
    }
    
    private class Segment implements Comparable {
        
        private float startValue, endValue;
        private boolean isActive = false;
        private int tries = 0;
        private SchematicPanel schematicPanel;
        
        public Segment(float value, int tries, SchematicPanel sp) {
            startValue = value;
            schematicPanel = sp;
            this.tries = tries;
        }
        
        @Override
        public String toString() {
            return String.format("[%.2f %.2f]",startValue,endValue);
        }
        
        public void toggleActive() {
            isActive = !isActive;
        }
        
        public boolean isActive() {
            return isActive;
        }

        public float getInterpolateProportion(float value) {
            return value/getRange();
        }
        
        public SchematicPanel getSchematicPanel() {
            return schematicPanel;
        }
        
        public boolean contains(float value) {
            // include lower bound, exclude upper bound
            return value >= startValue && value < endValue;
        }
        
        public float getEndValue() {
            return endValue;
        }
        
        public void setEndValue(float value) {
            endValue = value;
        }
        
        public float getStartValue() {
            return startValue;
        }
        
        public void setStartValue(float value) {
            startValue = value;
        }
        
        public float getRange() {
            return endValue-startValue;
        }
        
        private String getDistanceString() {
            return String.format("%.1f meters", startValue);
        }
        
        private String getTriesString() {
            if (tries==1) {
                return String.format("run 1 time");
            }
            return String.format("run %d times", tries);
        }
        
        public String getTopLabel() {
            return getTriesString();
        }
        
        public String getBottomLabel() {
            return getDistanceString();
        }
        
        /*
        public void setTries(int tries) {
            this.tries = tries;
        }
        */
        
        @Override
        public int compareTo(Object t) {
            Segment other = (Segment) t;
            if (other.startValue < startValue) {
                return 1;
            } else if (other.startValue > startValue) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    
    public static ResultSet callableStatementSchematicPanels() throws SQLException {
        String CONN_STRING
                = "jdbc:mysql://localhost/jointbot?user=webuser&password=webber";
        Connection conn = (Connection) DriverManager.getConnection(CONN_STRING);
        
        CallableStatement cStmt = conn.prepareCall("{call get_history_panel_info(?,?,?,?)}");
        String isControlStr = "N";
        String useSpectrumView = "Y";
        int NUM_SCHEM_PANELS = 10;
        cStmt.setString("is_control", isControlStr);
        cStmt.setString("ip_addr", "1.1.1.1");
        cStmt.setInt("num_panels", NUM_SCHEM_PANELS);
        cStmt.setString("is_spectrum_view", useSpectrumView);
        cStmt.execute();
        
        ResultSet results = cStmt.getResultSet();
        return results;
    }
    
    public static void main(String args[]) throws SQLException {
        JFrame jf = new JFrame();
        final FitnessSlider fs = new FitnessSlider();

        ResultSet results = callableStatementSchematicPanels();
        int i=0;
        while(results.next()) {
            SchematicPanel sp = new SchematicPanel();
            JointConfig jointConfig = Application.configFromResultSet(results);
            sp.init(jointConfig);
            //f.add(sp);
            float distance = results.getFloat("dist");
            fs.addSegment(distance, 25, sp);
            //fs.addSchematicPanel(sp);
            i++;
        }
        
//        fs.addSegment(1.5f,10);
//        fs.addSegment(0.0f,50);
//        fs.addSegment(2.0f,40);
        fs.setPointerValue(11.4f);

        jf.add(fs);
        jf.setSize(new Dimension(1000,80));
        jf.setVisible(true);
        
        jf.addMouseListener(new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent me) {
                float pos = ((float) me.getX())/((float) fs.getWidth());
                System.err.println(pos);
                fs.setPointerValue(pos);
                fs.repaint();
            }
            @Override
            public void mousePressed(MouseEvent me) {            }
            @Override
            public void mouseReleased(MouseEvent me) {            }
            @Override
            public void mouseEntered(MouseEvent me) {            }
            @Override
            public void mouseExited(MouseEvent me) {            }
        });
    }
}
