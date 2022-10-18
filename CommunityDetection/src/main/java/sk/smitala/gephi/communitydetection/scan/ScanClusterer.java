/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.scan;

import com.google.common.collect.HashMultiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
//import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 * SCAN clustering
 *
 * @author smitalm
 *
 * Implementation of Structural Clustering Algorithm for Networks (SCAN)
 * proposed in <i>"SCAN: A Structural Clustering Algorithm for Networks (2007)"
 * by Xiaowei Xu, Nurcan Yuruk, Zhidan Feng and Thomas A. J. Schweiger</i>
 *
 */
public class ScanClusterer implements Statistics, LongTask {

    // constants
    private static final String MODULARITY_CLASS = "SCAN community";
    private static final int NOT_MEMBER = -1;
    private static int INITIAL_COLLECTION_SIZE = 10;
    // SCAN parameters defaults
    public static final double EPSILON_DEFAULT = 0.6d;
    public static final int MU_DEFAULT = 2;
    // SCAN parameters
    private double epsilon = EPSILON_DEFAULT;
    private int mu = MU_DEFAULT;
    private boolean groupHubs = false;
    private boolean groupOutliers = false;
    private boolean experimentalImprovement = true;
    // algorithm fields
    private GraphModel graphModel;
    private Graph graph;
    private int clustersCount = 0;
    private int hubsCount = 0;
    private int outliersCount = 0;
    // long task api fields
    private boolean cancelled;
    private ProgressTicket progressTicket;
    private HashMap<MyEdge, Double> BUFFER;
    private HashMap<String, String> CLUSTERS;

    public boolean isGroupHubs() {
	return groupHubs;
    }

    public boolean isGroupOutliers() {
	return groupOutliers;
    }

    public void setGroupHubs(boolean groupHubs) {
	this.groupHubs = groupHubs;
    }

    public void setGroupOutliers(boolean groupOutliers) {
	this.groupOutliers = groupOutliers;
    }

    public boolean isExperimentalImprovement() {
	return experimentalImprovement;
    }

    public void setExperimentalImprovement(boolean experimentalImprovement) {
	this.experimentalImprovement = experimentalImprovement;
    }

    protected int getCommonNeighboursCount(Node v, Node w) {
	Set<String> commonNeighbours = new HashSet<>();
	for (Node vNeighbour : graph.getNeighbors(v)) {
	    for (Node wNeighbour : graph.getNeighbors(w)) {
		if (vNeighbour.getId().equals(wNeighbour.getId())) {
		    commonNeighbours.add( (String)(vNeighbour.getId()));
		} else if (vNeighbour.getId().equals(w.getId())) {
		    commonNeighbours.add( (String)(vNeighbour.getId()));
		} else if (wNeighbour.getId().equals(v.getId())) {
		    commonNeighbours.add( (String)(wNeighbour.getId()));
		}
	    }
	}
	return commonNeighbours.size();
    }

    protected int getTotalNeighboursCount(Node v, Node w) {
	Set<String> totalNeighbours = new HashSet<>();
	for (Node vNeighbour : graph.getNeighbors(v)) {
	    totalNeighbours.add( (String)(vNeighbour.getId()));
	}
	for (Node wNeighbour : graph.getNeighbors(w)) {
	    totalNeighbours.add( (String)(wNeighbour.getId()));
	}

	totalNeighbours.add( (String)(v.getId()));
	totalNeighbours.add( (String)(w.getId()));

	return totalNeighbours.size();
    }

    protected double getJaccardIndex(Node n1, Node n2) {
	Integer totalNeighbours = getTotalNeighboursCount(n1, n2);
	Integer commonNeighbours = getCommonNeighboursCount(n1, n2);

	double index = (((double) commonNeighbours.intValue()) / ((double) totalNeighbours.intValue()));
	return index;
    }

    private double getStructuralSimilarity(Node v, Node w) {

	//check buffer if there is already calculated similarity for these nodes
	Double s = BUFFER.get(new MyEdge((String)v.getId(),(String) w.getId()));
	if (s == null) {
	    //similarity is symetric relation
	    s = BUFFER.get(new MyEdge((String)w.getId(), (String)v.getId()));
	}
	if (s != null) {
	    return s;
	}

	int commonNeighbours = getCommonNeighboursCount(v, w);
	int vNeighbours = graph.getNeighbors(v).toArray().length + 1;
	int wNeighbours = graph.getNeighbors(w).toArray().length + 1;
	int bothNeighbours = vNeighbours * wNeighbours;

	double structuralSimilarity;
	//structural similarity is formulated number of common neighbours
	//by geometric mean of sizes of both neighbourhoods
	if (experimentalImprovement) {
	    structuralSimilarity = ((double) commonNeighbours) / (Math.min(vNeighbours, wNeighbours));
	} else {
	    structuralSimilarity = ((double) commonNeighbours) / (Math.sqrt(bothNeighbours));
	}

	//put calculated similarity in buffer to prevent duplicate calculations
	BUFFER.put(new MyEdge((String)v.getId(), (String)w.getId()), new Double(structuralSimilarity));
	return structuralSimilarity;
    }

    private List<Node> getEpsilonNeighbourhood(Node n) {
	ArrayList<Node> neighbourhood = new ArrayList<Node>(INITIAL_COLLECTION_SIZE);
	neighbourhood.add(n);
	for (Node node : graph.getNeighbors(n)) {
	    if (getStructuralSimilarity(n, node) >= epsilon) {
		neighbourhood.add(node);
	    }
	}

	return neighbourhood;
    }

    private List<Node> getEpsilonNeighbourhoodJac(Node n) {
	ArrayList<Node> neighbourhood = new ArrayList<Node>(INITIAL_COLLECTION_SIZE);
	neighbourhood.add(n);
	for (Node node : graph.getNeighbors(n)) {
	    if (getJaccardIndex(n, node) >= epsilon) {
		neighbourhood.add(node);
	    }
	}

	return neighbourhood;
    }

    private boolean isCore(Node n) {
	return getEpsilonNeighbourhood(n).size() >= mu || getEpsilonNeighbourhoodJac(n).size() >= mu;
    }

    private boolean isDirectlyStructureReachable(Node v, Node w) {
	return isCore(v) && (getEpsilonNeighbourhood(v).contains(w) || getEpsilonNeighbourhoodJac(v).contains(w));
    }

    private boolean isSurroundedByCluster(String nodeID, String clusterID) {
	double neighboursInCluster = 0.0d;
	double neighboursCount = 0;
	for (Node n : graph.getNeighbors(graph.getNode(nodeID))) {
	    if (CLUSTERS.get((String)n.getId()).equals((clusterID))) {
		neighboursInCluster = neighboursInCluster + 1;
	    }
	    neighboursCount++;
	}

	return neighboursInCluster / neighboursCount > epsilon;
    }

    private boolean isUnclassified(Node node) {
	return !CLUSTERS.containsKey((String)node.getId());
    }

    private boolean isNotMember(Node node) {
	if (isUnclassified(node)) {
	    return false;
	}
	return CLUSTERS.get((String) node.getId()).equals("" + NOT_MEMBER);
    }

    private String getClusterID(Node node) {
	if (CLUSTERS.get((String) node.getId()) == null) {
	    return null;
	}

	String clusterID = CLUSTERS.get((String) node.getId());
//	if (clusterID.intValue() >= 0) {
	if (clusterID.compareTo("0") >= 0) {
	    return clusterID;
	} else {
	    return null;
	}
    }

    private boolean isHub(Node node) {
	String firstClusterID = null;
	String currentClusterID;
	for (Node v : graph.getNeighbors(node)) {
	    currentClusterID = getClusterID(v);
	    if (firstClusterID == null) {
		if (currentClusterID != null) {
//		    firstClusterID = new Integer(currentClusterID.intValue());
		    firstClusterID = currentClusterID;
		}
	    } else {
		if (currentClusterID != null && !currentClusterID.equals(firstClusterID)) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * @return the epsilon
     */
    public double getEpsilon() {
	return epsilon;
    }

    /**
     * @param epsilon the epsilon to set
     */
    public void setEpsilon(double epsilon) {
	this.epsilon = epsilon;
    }

    /**
     * @return the mu
     */
    public int getMu() {
	return mu;
    }

    /**
     * @param mu the mu to set
     */
    public void setMu(int mu) {
	this.mu = mu;
    }

    @Override
    public boolean cancel() {
	cancelled = true;
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
	this.progressTicket = pt;
    }

    /**
     * @return the graphModel
     */
    public GraphModel getGraphModel() {
	return graphModel;
    }

    /**
     * @param graphModel the graphModel to set
     */
    public void setGraphModel(GraphModel graphModel) {
	this.graphModel = graphModel;
    }

    @Override
    public void execute(GraphModel gm) {

	System.out.println();
	System.out.println("Started clustering with following parameters:");
	System.out.println("---------------------------------------------");
	System.out.println("epsilon = " + epsilon);
	System.out.println("mu = " + mu);
	System.out.println("---------------------------------------------");
	System.out.println();

	graph = gm.getGraphVisible();
	graph.readLock();
	if (!gm.getNodeTable().hasColumn(MODULARITY_CLASS)){
		gm.getNodeTable().addColumn(MODULARITY_CLASS, String.class);
	}

	//set buffer size to number of edges to prevent resizing
	BUFFER = new HashMap<MyEdge, Double>(graph.getEdgeCount());
	CLUSTERS = new HashMap<String, String>(graph.getNodeCount());
	INITIAL_COLLECTION_SIZE = graph.getEdgeCount() / graph.getNodeCount() * 2;
	try {
	    Progress.start(progressTicket, graph.getNodeCount());

	    System.out.println("Finding clusters...");
	    Queue<Node> Q = new LinkedList<Node>();
	    int clusterID;
	    for (Node v : graph.getNodes()) {
		if (cancelled) {
		    System.out.println("Force stopping clustering...");
		    return;
		}
		if (!isUnclassified(v)) {
		    continue;
		}

		Progress.progress(progressTicket);
		if (isCore(v)) {
		    // expand new cluster
		    clusterID = clustersCount++;
		    CLUSTERS.put((String)v.getId(), "" + clusterID);
		    Q.clear();
		    Q.addAll(getEpsilonNeighbourhood(v));
		    while (!Q.isEmpty()) {
			Node y = Q.poll();
			for (Node x : getEpsilonNeighbourhood(y)) {
			    if (!isDirectlyStructureReachable(y, x)) {
				if (getJaccardIndex(x, y) < epsilon) {
				    continue;
				}
			    }
			    Progress.progress(progressTicket);

			    if (isUnclassified(x) || isNotMember(x)) {
				if (isUnclassified(x)) {
				    Q.add(x);
				}
				CLUSTERS.put((String)x.getId(), "" + clusterID);
			    }
			}
		    }

		} else {
		    CLUSTERS.put((String)v.getId(), "" + NOT_MEMBER);
		}
	    }

	    // label each non member as hub or outlier
	    System.out.println("Marking hubs and outliers...");
	    for (Node v : graph.getNodes()) {
		if (!isNotMember(v)) {
		    v.setAttribute(MODULARITY_CLASS, "Cluster " + CLUSTERS.get((String) v.getId()));
		    continue;
		}

		if (isHub(v)) {
//		    findStrongestMembership(v);
		    hubsCount++;
		    v.setAttribute(MODULARITY_CLASS, ((groupHubs) ? "Hub" : ("Hub" + hubsCount)));
		} else {
//		    findStrongestMembership(v);
		    outliersCount++;
		    v.setAttribute(MODULARITY_CLASS, ((groupOutliers) ? "Outlier" : ("Outlier" + outliersCount)));
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	} finally {
	    //Unlock graph
	    graph.readUnlockAll();
	}

	System.out.println();
	System.out.println("Clustering finished with following results:");
	System.out.println("-------------------------------------------");
	System.out.println("Clusters found: " + clustersCount);
	System.out.println("Hubs found: " + hubsCount);
	System.out.println("Outliers found: " + outliersCount);
	System.out.println("-------------------------------------------");
	System.out.println();
    }

    public void findStrongestMembership(Node v) {

	HashMultiset<String> memberships = HashMultiset.create();
	int count = 0;
	for (Node n : graph.getNeighbors(v)) {
	    if (CLUSTERS.containsKey((String)n.getId())) {
		memberships.add(CLUSTERS.get((String)n.getId()));
	    }
	    count++;
	}

//	int max = 0;
//	for (Integer membership : memberships.elementSet()) {
//	    if (memberships.count(membership) > max) {
//		max = membership;
//	    }
//	}
    String max = null;
	for (String membership : memberships.elementSet()) {
	    if (memberships.count(membership) > (max == null ? 0 : memberships.count(max))) {
		max = membership;
	    }
	}

	if (memberships.count(max) < 2) {
	    return;
	}
	CLUSTERS.put((String)v.getId(), max);
	v.setAttribute(MODULARITY_CLASS, "Cluster " + max);
    }

    @Override
    public String getReport() {
	return "<HTML> <BODY> <h1> SCAN clustering </h1> "
		+ "<hr>"
		+ "<br />" + "<h2> Algorithm params: </h2>"
		+ "epsilon:  " + epsilon + "<br />"
		+ "mu:  " + mu + "<br />"
		+ "<br />" + "<h2> Results: </h2>"
		+ "<br />"
		+ "Clusters found: " + clustersCount + "<br />"
		+ "Hubs found: " + hubsCount + "<br />"
		+ "Outliers found: " + outliersCount + "<br />"
		+ "<br />" + "<h2> Algorithm: </h2>"
		+ "Xiaowei Xu, Nurcan Yuruk, Zhidan Feng, Thomas A. J. Schweiger, "
		+ "<i>SCAN: A Structural Clustering Algorithm for Networks</i>"
		+ ", 2007 <br />"
		+ "</BODY> </HTML>";
    }

    private class MyEdge {

	private String sourceID;
	private String targetID;

	public MyEdge(String sourceID, String targetID) {
	    this.sourceID = sourceID;
	    this.targetID = targetID;
	}

	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 53 * hash + this.sourceID.hashCode();
	    hash = 53 * hash + this.targetID.hashCode();
	    return hash;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final MyEdge other = (MyEdge) obj;
	    if (!this.sourceID.equals(other.sourceID)) {
		return false;
	    }
	    if (!this.targetID.equals(other.targetID)) {
		return false;
	    }
	    return true;
	}
    }
}
