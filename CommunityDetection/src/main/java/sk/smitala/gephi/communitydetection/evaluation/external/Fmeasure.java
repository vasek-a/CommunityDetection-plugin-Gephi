/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation.external;

import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import sk.smitala.gephi.communitydetection.utils.traverse.TraversePairListener;

import java.util.Objects;

/**
 * @author smitalm
 */
public class Fmeasure implements TraversePairListener<Node> {

    Column groundTruth;
    Column targetClustering;
    int tp = 0;
    int fp = 0;
    int tn = 0;
    int fn = 0;

    public Fmeasure(Column groundTruth, Column targetClustering) {
        this.groundTruth = groundTruth;
        this.targetClustering = targetClustering;
    }

    public static double calculate(double penalty, Graph graph, Column classAttr, Column clusterAttr) {
        // first create communities data structure for convenient computation
        Node[] nodes = graph.getNodes().toArray();
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        String iClass;
        String jClass;
        String iCluster;
        String jCluster;
        for (int i = 0; i < nodes.length - 1; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                if (nodes[i].getAttribute(classAttr.getId()) instanceof String) {
                    iClass = (String) nodes[i].getAttribute(classAttr.getId());
                    jClass = (String) nodes[j].getAttribute(classAttr.getId());
                } else if (nodes[i].getAttribute(classAttr.getId()) instanceof Integer) {
                    iClass = ((Integer) nodes[i].getAttribute(classAttr.getId())) + "";
                    jClass = ((Integer) nodes[j].getAttribute(classAttr.getId())) + "";
                } else {
//		    throw new ClassCastException("Clustering attribute has to be type String, Integer!");
//		    System.err.println("Class attribute not found!");
                    tp++;
                    continue;
                }
                if (nodes[i].getAttribute(clusterAttr.getId()) instanceof String) {
                    iCluster = (String) nodes[i].getAttribute(clusterAttr.getId());
                    jCluster = (String) nodes[j].getAttribute(clusterAttr.getId());
                } else if (nodes[i].getAttribute(clusterAttr.getId()) instanceof Integer) {
                    iCluster = ((Integer) nodes[i].getAttribute(clusterAttr.getId())) + "";
                    jCluster = ((Integer) nodes[j].getAttribute(clusterAttr.getId())) + "";
                } else {
//		    throw new ClassCastException("Clustering attribute has to be type String, Integer!");
//		    System.err.println("Cluster attribute not found!");
                    fn++;
                    continue;
                }

                if (iClass.equals(jClass) && iCluster.equals(jCluster)) {
                    tp++;
                } else if (!iClass.equals(jClass) && iCluster.equals(jCluster)) {
                    fp++;
                } else if (!iClass.equals(jClass) && !iCluster.equals(jCluster)) {
                    tn++;
                } else {
                    fn++;
                }
            }
        }

        double P = ((double) tp) / (tp + fp);
        double R = ((double) tp) / (tp + fn);
        return ((penalty * penalty + 1) * P * R) / ((penalty * penalty * P) + R);
    }

    /*
        @Override
        public void onElementPairVisited(Node n1, Node n2) {
        String iClass;
        String jClass;
        String iCluster;
        String jCluster;
        if (n1.getAttribute(groundTruth.getId()) instanceof String) {
            iClass = (String) n1.getAttribute(groundTruth.getId());
            jClass = (String) n2.getAttribute(groundTruth.getId());
        } else if (n1.getAttribute(groundTruth.getId()) instanceof Integer) {
            iClass = ((Integer) n1.getAttribute(groundTruth.getId())) + "";
            jClass = ((Integer) n2.getAttribute(groundTruth.getId())) + "";
        } else {
    //		    throw new ClassCastException("Clustering attribute has to be type String, Integer!");
    //	    System.err.println("Class attribute not found!");
            tp++;
            return;
        }
        if (n1.getAttribute(targetClustering.getId()) instanceof String) {
            iCluster = (String) n1.getAttribute(targetClustering.getId());
            jCluster = (String) n2.getAttribute(targetClustering.getId());
        } else if (n1.getAttribute(targetClustering.getId()) instanceof Integer) {
            iCluster = ((Integer) n1.getAttribute(targetClustering.getId())) + "";
            jCluster = ((Integer) n2.getAttribute(targetClustering.getId())) + "";
        } else {
    //		    throw new ClassCastException("Clustering attribute has to be type String, Integer!");
    //	    System.err.println("Cluster attribute not found!");
            fn++;
            return;
        }

        if (iClass.equals(jClass) && iCluster.equals(jCluster)) {
            tp++;
        } else if (!iClass.equals(jClass) && iCluster.equals(jCluster)) {
            fp++;
        } else if (!iClass.equals(jClass) && !iCluster.equals(jCluster)) {
            tn++;
        } else {
            fn++;
        }
        }
    */
    @Override
    public void onElementPairVisited(Node n1, Node n2) {
        Object iClass;
        Object jClass;
        Object iCluster;
        Object jCluster;

        iClass = n1.getAttribute(groundTruth);
        jClass = n2.getAttribute(groundTruth);

        iCluster = n1.getAttribute(targetClustering);
        jCluster = n2.getAttribute(targetClustering);

        if (Objects.equals(iClass, jClass) && Objects.equals(iCluster, jCluster)) {
            tp++;
        } else if (!Objects.equals(iClass, jClass) && Objects.equals(iCluster, jCluster)) {
            fp++;
        } else if (!Objects.equals(iClass, jClass) && !Objects.equals(iCluster, jCluster)) {
            tn++;
        } else {
            fn++;
        }

    }

    public double calculate(double penalty) {

        double P = ((double) tp) / (tp + fp);
        double R = ((double) tp) / (tp + fn);
        return ((penalty * penalty + 1) * P * R) / ((penalty * penalty * P) + R);
    }
}
