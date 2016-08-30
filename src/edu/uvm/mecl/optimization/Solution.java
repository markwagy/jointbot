package edu.uvm.mecl.optimization;

import java.util.ArrayList;

/**
 * A Solution is a solution to an optimization problem
 * @author mwagy
 */
public class Solution <SolutionType, ElementType> {
    private SolutionType solutionValue;
    private ArrayList<ElementType> elements = new ArrayList<ElementType>();
    
    public Solution(SolutionType sol, ArrayList<ElementType> els) {
        this.elements.addAll(els);
        this.solutionValue = sol;
    }
    
    public ArrayList<ElementType> getElements() {
        return elements;
    }
    
    public SolutionType getValue() {
        return solutionValue;
    }
    
}
