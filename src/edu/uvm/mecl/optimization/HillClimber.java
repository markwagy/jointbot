package edu.uvm.mecl.optimization;

import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JLabel;

/**
 *
 * @author mwagy
 */
public class HillClimber {
    
    int numGens, dimension;
    ArrayList<Float> minVals;
    ArrayList<Float> maxVals;
    IObjectiveFunction fitFun;
    double mutateProb;
    double bestFitness;
    JLabel valueLabel;
    Solution bestSolution;
    
    public HillClimber(int numGens,
            ArrayList<Float> minVals, ArrayList<Float> maxVals,
            IObjectiveFunction fitFun, double mutateProb, JLabel valueLabel) {
        assert minVals.size() == maxVals.size();
        this.dimension = maxVals.size();
        this.numGens = numGens;
        this.minVals = minVals;
        this.maxVals = maxVals;
        this.fitFun = fitFun;
        this.mutateProb = mutateProb;
        this.valueLabel = valueLabel;
    }
    
    public Solution run() {
        double[] parent = getRandomIndividual();
        double[] child;
        double parentFitness = fitFun.valueOf(parent);
        double childFitness;
        
        System.err.println("parent.fitness, child.fitness");
        // eval generations
        for (int gen=0; gen<numGens; gen++) {
            child = getMutatedVersion(parent);
            childFitness = fitFun.valueOf(child);
            if (childFitness < parentFitness) {
                parent = child;
                parentFitness = childFitness;
            }
            
            System.err.println(String.format("%.2f,\t%.2f",1/parentFitness,1/childFitness));
            valueLabel.setText(String.format(".3f",1/parentFitness));
        }
        bestFitness = parentFitness;
        // build arraylist to return
        ArrayList<Float> rtn = new ArrayList<Float>();
        for (int dim=0; dim<parent.length; dim++) {
            rtn.add(new Float(parent[dim]));
        }
        return new Solution(bestFitness, rtn);
    }
    
    public double getBestFitness() {
        return bestFitness;
    }
    
    public double[] getRandomIndividual() {
        Random rnd = new Random();
        double[] ind = new double[dimension];
        for (int dim=0; dim<dimension; dim++) {
            // scale value
            double val = rnd.nextDouble()*(maxVal(dim) - minVal(dim)) + minVal(dim);
            ind[dim] = val;
        }
        return ind;
    }
    
    private double[] getMutatedVersion(double[] ind) {
        Random rnd = new Random();
        double[] newInd = new double[ind.length];
        for (int i=0; i<ind.length; i++) {
            if (rnd.nextDouble() < mutateProb) {
                newInd[i] = rnd.nextDouble();
            } else {
                newInd[i] = ind[i];
            }
        }
        return newInd;
    }
    
    private float minVal(int dimension) {
        return minVals.get(dimension);
    }
    
    private float maxVal(int dimension) {
        return maxVals.get(dimension);
    }
}
