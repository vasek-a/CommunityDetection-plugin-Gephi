/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation.internal;

import org.gephi.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import sk.smitala.gephi.communitydetection.utils.Community;
import sk.smitala.gephi.communitydetection.utils.traverse.TraversePairListener;

/**
 *
 * @author smitalm
 */
public class DaviesBouldin
	implements TraversePairListener<Community> {

    Column attribute;
    Graph graph;
    int count = 0;
    double sum = 0;
    double worstCase = 0;
    String lastCommunity = "";

    public DaviesBouldin(Graph graph, Column attribute) {
	this.attribute = attribute;
	this.graph = graph;
    }

    @Override
    public void onElementPairVisited(Community c1, Community c2) {
	if (c1.getId().equals(c2.getId())) {
	    return;
	}
	if (c1.getId().equals(lastCommunity)) {
	    double r = calculateR(c1, c2);
	    if (r > worstCase) {
		worstCase = r;
	    }
	} else {
	    //zvysime pocet komunit
	    count++;
	    sum += worstCase;
	    //zapamatame poslednu komunitu
	    lastCommunity = c1.getId();
	    worstCase = 0;
	}
    }

    public double calculate() {
	return sum / count;
    }

    private double calculateR(Community c1, Community c2) {
	double intraSimilarity1 = c1.averagePathLengthBetweenMembers(graph);
	double intraSimilarity2 = c2.averagePathLengthBetweenMembers(graph);
	double interSimilarity = c1.averagePathLengthBetweenCommunities(graph, c2);
	return (intraSimilarity1 + intraSimilarity2) / interSimilarity;
    }
}
