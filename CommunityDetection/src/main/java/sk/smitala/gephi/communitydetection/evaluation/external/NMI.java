/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation.external;

import sk.smitala.gephi.communitydetection.utils.Community;
import sk.smitala.gephi.communitydetection.utils.traverse.TraverseListener;

import java.util.Map;

/**
 * @author smitalm
 */
public class NMI implements TraverseListener<Community> {

    double mutualInformation = 0;
    double groundTruthEntropy;
    double targetClusteringEntropy;
    int nodeCount;
    Map<String, Community> targetClustering;
    int visited = 0;

    public NMI(int nodeCount, double groundTruthEntropy, Map<String, Community> targetClustering) {
        this.nodeCount = nodeCount;
        this.groundTruthEntropy = groundTruthEntropy;
        this.targetClustering = targetClustering;

        System.out.println("Ground entropy: " + groundTruthEntropy);
        System.out.println("NMI Target size: " + targetClustering.size());

        double tmp;
        targetClusteringEntropy = 0;
        for (Community c : targetClustering.values()) {
            tmp = ((double) c.size()) / nodeCount;
            targetClusteringEntropy += tmp * Math.log(tmp);
        }
    }

    public double calculate() {
        return mutualInformation / (((-groundTruthEntropy) + (-targetClusteringEntropy)) / 2);
    }

    @Override
    public void onElementVisited(Community c) {
        for (Community w : targetClustering.values()) {
            double tmp;
            tmp = c.intersectionSize(w);
            if (tmp == 0) {
                continue;
            }
            mutualInformation += (tmp / nodeCount)
                    * Math.log((nodeCount * tmp)
                    / (c.size() * w.size()));
        }
    }

//    @Override
    public void onElementPairVisited(Community c, Community w) {
        double tmp;
        tmp = c.intersectionSize(w);
        if (tmp == 0) {
            return;
        }
        mutualInformation += (tmp / nodeCount)
                * Math.log(
                (nodeCount * tmp)
                        / (c.size() * w.size()));
    }
}
