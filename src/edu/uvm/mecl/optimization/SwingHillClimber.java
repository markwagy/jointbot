package edu.uvm.mecl.optimization;

import edu.uvm.mecl.jointbot.Application;
import edu.uvm.mecl.jointbot.JointConfig;
import edu.uvm.mecl.jointbot.JointGroup;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.swing.SwingWorker;

/**
 * TODO: this is a hack... cut and paste from HillClimber rather than extending
 * because i don't know how to also extend SwingWorker
 * @author mwagy
 */
public class SwingHillClimber extends SwingWorker<Void, Void> {
    
    int numGens, dimension;
    ArrayList<Float> minVals;
    ArrayList<Float> maxVals;
    IObjectiveFunction fitFun;
    float mutateProb;
    double bestFitness;
//    JLabel valueLabel;
    Solution bestSolution;
    ArrayList<JointGroup> jointGroups;
    
    public SwingHillClimber(int numGens,
            ArrayList<Float> minVals, ArrayList<Float> maxVals,
            IObjectiveFunction fitFun, float mutateProb, //JLabel valueLabel,
            ArrayList<JointGroup> jointGroups) {
        assert minVals.size() == maxVals.size();
        this.dimension = maxVals.size();
        this.numGens = numGens;
        this.minVals = minVals;
        this.maxVals = maxVals;
        this.fitFun = fitFun;
        this.mutateProb = mutateProb;
//        this.valueLabel = valueLabel;
        this.jointGroups = jointGroups;
    }
    
    // run hill climber
    @Override
    public Void doInBackground() {
        Float[] parent = getRandomIndividual();
        Float[] child;
        double parentFitness = fitFun.valueOf(toDoubleArray(parent));
        double childFitness;
        
        System.err.println("parent.fitness, child.fitness");
        Solution sol = null;
        publish();
        // eval generations
        for (int gen=0; gen<numGens && !isCancelled(); gen++) {
            child = JointConfig.getMutatedVersion(parent, mutateProb);
            childFitness = fitFun.valueOf(toDoubleArray(child));
            if (childFitness < parentFitness) {
                parent = child;
                parentFitness = childFitness;
            }
            
            System.err.println(String.format("%.2f,\t%.2f",1/parentFitness,1/childFitness));
           
            sol = new Solution<Float, Float>(
                    new Float(parentFitness),
                    new ArrayList<Float>(Arrays.asList(parent)));
            //publish(sol);
            //valueLabel.setText(sol.getValue().toString());
        }
        bestFitness = parentFitness;
        bestSolution = sol;
        //return sol;
        return null;
    }
    
    private double[] toDoubleArray(Float[] vals) {
        double[] rtn = new double[vals.length];
        for (int i=0; i<vals.length; i++) {
            rtn[i] = (double) vals[i];
        }
        return rtn;
    }
    /*
    @Override
    protected void process(List<Solution> solutions) {
        // get last solution
        if (solutions.size()>0) {
            Solution lastSolution = solutions.get(solutions.size()-1);
            float distance = (Float) lastSolution.getValue();
            //valueLabel.setText(String.format("%.3f",distance));
        }
    }
    */
    
    @Override
    protected void done() {
        try {
            get();
            JointConfig jointConfig = new JointConfig(
                    jointGroups, bestSolution.getElements());
            Application.writeToDB(
                    jointConfig, 
                    (Float) bestSolution.getValue(), 
                    Application.IS_BACKGROUND);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public double getBestFitness() {
        return bestFitness;
    }
    
    private ArrayList<Float> floatArrayListFromDoubleArr(double[] arr) {
        ArrayList<Float> rtn = new ArrayList<Float>();
         for (int dim=0; dim<arr.length; dim++) {
                rtn.add(new Float(arr[dim]));
         }
         return rtn;
    }
    
    public Float[] getRandomIndividual() {
        Random rnd = new Random();
        Float[] ind = new Float[dimension];
        for (int dim=0; dim<dimension; dim++) {
            // scale value
            double val = rnd.nextDouble()*(maxVal(dim) - minVal(dim)) + minVal(dim);
            ind[dim] = (float) val;
        }
        return ind;
    }
    
    private float minVal(int dimension) {
        return minVals.get(dimension);
    }
    
    private float maxVal(int dimension) {
        return maxVals.get(dimension);
    }

    public Solution getSolution() {
        return bestSolution;
    }
}
