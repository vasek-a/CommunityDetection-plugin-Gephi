/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation.internal;

import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import sk.smitala.gephi.communitydetection.utils.traverse.TraverseListener;

import java.util.Arrays;

/**
 *
 * @author smitalm
 */
public class IntraEdgesRatio implements TraverseListener<Node> {

    Column attribute;
    Graph graph;
    double ratioSum = 0.0d;

    public IntraEdgesRatio(Graph graph, Column attribute) {
	this.attribute = attribute;
	this.graph = graph;
    }

    @Override
    public void onElementVisited(Node e) {
	int intraCommunityEdges = 0;
	int totalEdges = 0;
	Object o = e.getAttribute(attribute.getId());
	for (Node n : graph.getNeighbors(e)) {
	    totalEdges++;
	    if (n.getAttribute(attribute.getId()) != null && n.getAttribute(attribute.getId()).equals(o)) {
		intraCommunityEdges++;
	    }
	}
	if (totalEdges != 0) {
		ratioSum += ((double) intraCommunityEdges) / totalEdges;
	}
	}

    public double getRatio() {
	return ratioSum / graph.getNodeCount();
    }
}
