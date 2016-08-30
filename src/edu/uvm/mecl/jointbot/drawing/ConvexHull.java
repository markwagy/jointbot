package edu.uvm.mecl.jointbot.drawing;

import edu.uvm.mecl.jointbot.Joint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * based on:
 * https://code.google.com/p/convex-hull/source/browse/Convex%20Hull/src/algorithms/FastConvexHull.java?r=4
 */
public class ConvexHull {
    
    public ArrayList<Joint> execute(ArrayList<Joint> points)
    {
        if (points.size()<2) {
            return points;
        }
        ArrayList<Joint> xSorted = (ArrayList<Joint>) points.clone();
        Collections.sort(xSorted, new XCompare());
        
        int n = xSorted.size();
        
        Joint[] lUpper = new Joint[n];
        
        lUpper[0] = xSorted.get(0);
        lUpper[1] = xSorted.get(1);
        
        int lUpperSize = 2;
        
        for (int i = 2; i < n; i++)
        {
            lUpper[lUpperSize] = xSorted.get(i);
            lUpperSize++;
            
            while (lUpperSize > 2 && !rightTurn(lUpper[lUpperSize - 3], lUpper[lUpperSize - 2], lUpper[lUpperSize - 1]))
            {
                // Remove the middle point of the three last
                lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
                lUpperSize--;
            }
        }
        
        Joint[] lLower = new Joint[n];
        
        lLower[0] = xSorted.get(n - 1);
        lLower[1] = xSorted.get(n - 2);
        
        int lLowerSize = 2;
        
        for (int i = n - 3; i >= 0; i--)
        {
            lLower[lLowerSize] = xSorted.get(i);
            lLowerSize++;
            
            while (lLowerSize > 2 && !rightTurn(lLower[lLowerSize - 3], lLower[lLowerSize - 2], lLower[lLowerSize - 1]))
            {
                // Remove the middle point of the three last
                lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
                lLowerSize--;
            }
        }
        
        ArrayList<Joint> result = new ArrayList<Joint>();
        
        for (int i = 0; i < lUpperSize; i++)
        {
            result.add(lUpper[i]);
        }
        
        for (int i = 1; i < lLowerSize; i++)
        {
            result.add(lLower[i]);
        }
        
        return result;
    }
    
    private boolean rightTurn(Joint a, Joint b, Joint c)
    {
        return (b.getCircle().center.x - a.getCircle().center.x)*(c.getCircle().center.y - a.getCircle().center.y) - 
                (b.getCircle().center.y - a.getCircle().center.y)*(c.getCircle().center.x - a.getCircle().center.x) > 0;
    }
    
    private class XCompare implements Comparator<Joint>
    {
        @Override
        public int compare(Joint o1, Joint o2)
        {
            return (new Integer(o1.getCircle().center.x)).compareTo(new Integer(o2.getCircle().center.x));
        }
    }
}