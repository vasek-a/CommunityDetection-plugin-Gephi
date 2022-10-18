/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.linkcommunities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
//import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.*;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Link communities clustering
 *
 * @author smitalm
 *
 */
public class LinkClusterer implements Statistics, LongTask {

    public static final int ASDF = 3;
    public static final String EDGE_COMMUNITY = "Link community";
    public static final String EDGE_COMMUNITY_TRIVIAL = "Single link cluster";
    // buffers
    protected HashMap<NodePair, Integer> BUFFER_COMMON_NEIGHBOURS;
    protected HashMap<NodePair, Integer> BUFFER_TOTAL_NEIGHBOURS;
    protected HashMap<EdgePair, Double> BUFFER_SIMILARITIES;
    protected EdgePair[] SORTED_BUFFER;
    protected int alreadyMerged = 0;
    protected HashMap<String, String> edgeCommunities;
    // algorithm fields
    protected Graph graph;
    protected int MIN_COMMUNITY_SIZE = 2;
    protected int NODE_COUNT;
    protected int EDGE_COUNT;
    // algorithm results
    protected ArrayList<Double> densityGraph;
    protected HashMap<String, Community> communities;
    protected Community[] bestPartition = new Community[0];
    protected Community[][] bestPartitions = new Community[0][0];
    protected int dendrogramHeight;
    protected double bestPartitionDensity;
    protected int bestPartitionDensityCut;
    // long task api fields
    protected volatile boolean cancelled;
    protected ProgressTicket progressTicket;
    // params

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

    protected Node getKeystoneNode(Edge e1, Edge e2) {
	if (e1.getSource().getId().equals(e2.getSource().getId())) {
	    return e1.getSource();
	}
	if (e1.getTarget().getId().equals(e2.getTarget().getId())) {
	    return e1.getTarget();
	}
	if (e1.getSource().getId().equals(e2.getTarget().getId())) {
	    return e1.getSource();
	}
	if (e1.getTarget().getId().equals(e2.getSource().getId())) {
	    return e1.getTarget();
	}
	return null;
    }

    protected boolean sharingNodes(Edge e1, Edge e2) {
	return getKeystoneNode(e1, e2) != null;
    }

    protected NodePair getImpostNodes(Edge e1, Edge e2) {
	if (e1.getSource().getId().equals(e2.getSource().getId())) {
	    return new NodePair((String)e1.getTarget().getId(), (String)e2.getTarget().getId());
	}
	if (e1.getTarget().getId().equals(e2.getTarget().getId())) {
	    return new NodePair((String)e1.getSource().getId(), (String)e2.getSource().getId());
	}
	if (e1.getSource().getId().equals(e2.getTarget().getId())) {
	    return new NodePair((String)e1.getTarget().getId(), (String)e2.getSource().getId());
	}
	if (e1.getTarget().getId().equals(e2.getSource().getId())) {
	    return new NodePair((String)e1.getSource().getId(), (String)e2.getTarget().getId());
	}
	return null;
    }

    @Override
    public boolean cancel() {
	this.cancelled = true;
	System.out.println("CANCELLED!!!!");
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
	this.progressTicket = pt;
    }

    protected double getJaccardIndex(Edge e1, Edge e2) {
	NodePair impostNodes = getImpostNodes(e1, e2);
	if (impostNodes == null) {
	    return 0;
	}
//	 calculate size of conjunction
	Integer commonNeighbours = BUFFER_COMMON_NEIGHBOURS.get(new NodePair(impostNodes.node1, impostNodes.node2));
	if (commonNeighbours == null) {
	    commonNeighbours = BUFFER_COMMON_NEIGHBOURS.get(new NodePair(impostNodes.node2, impostNodes.node1));
	}
	if (commonNeighbours == null) {
	    commonNeighbours = getCommonNeighboursCount(graph.getNode(impostNodes.node1), graph.getNode(impostNodes.node2));
	    BUFFER_COMMON_NEIGHBOURS.put(new NodePair(impostNodes.node2, impostNodes.node1), commonNeighbours);
	}
	// calculate size of disjunction
	Integer totalNeighbours = BUFFER_TOTAL_NEIGHBOURS.get(new NodePair(impostNodes.node1, impostNodes.node2));
	if (totalNeighbours == null) {
	    totalNeighbours = BUFFER_TOTAL_NEIGHBOURS.get(new NodePair(impostNodes.node2, impostNodes.node1));
	}
	if (totalNeighbours == null) {
	    totalNeighbours = getTotalNeighboursCount(graph.getNode(impostNodes.node1), graph.getNode(impostNodes.node2));
	    BUFFER_TOTAL_NEIGHBOURS.put(new NodePair(impostNodes.node2, impostNodes.node1), totalNeighbours);
	}
	commonNeighbours = getCommonNeighboursCount(graph.getNode(impostNodes.node1), graph.getNode(impostNodes.node2));
	totalNeighbours = getTotalNeighboursCount(graph.getNode(impostNodes.node1), graph.getNode(impostNodes.node2));
//	double minNeighbours = Math.min(graph.getNeighbors(graph.getNode(impostNodes.node1)).toArray().length, graph.getNeighbors(graph.getNode(impostNodes.node2)).toArray().length);


	double index = (((double) commonNeighbours.intValue()) / ((double) totalNeighbours.intValue()));
//	double index = (((double) commonNeighbours.intValue()) / (minNeighbours));
	BUFFER_SIMILARITIES.put(new EdgePair((String)e1.getId(), (String)e2.getId()), new Double(index));
	return index;
    }

    protected boolean sameCommunity(EdgePair edgePair) {
	String community1 = edgeCommunities.get(edgePair.edge1);
	String community2 = edgeCommunities.get(edgePair.edge2);
	return (community1.equals(community2));
    }

    protected boolean sameCommunity(Integer edge1, Integer edge2) {
	String community1 = edgeCommunities.get(edge1);
	String community2 = edgeCommunities.get(edge2);
//	boolean sameCommunity = community1.equals(community2);
//	if (sameCommunity) {
//	    System.out.println("Same community: e[" + edge1 + "," + edge2 + "] c[" + community1 + "," + community2 + "]");
//	}
//	return sameCommunity;
	return community1.equals(community2);
    }

    protected List<EdgePair> mostSimilarLinks() {

	List<EdgePair> ties = new LinkedList<EdgePair>();
	Double bestSimilarity = null;
	EdgePair pair;
	for (int i = SORTED_BUFFER.length - 1 - alreadyMerged; i >= 0; i--) {
	    pair = SORTED_BUFFER[i];
	    if (!sameCommunity(pair)) {
		if (bestSimilarity == null) {
		    bestSimilarity = BUFFER_SIMILARITIES.get(pair);
		    ties.add(pair);
		} else if (bestSimilarity.doubleValue() <= BUFFER_SIMILARITIES.get(pair).doubleValue()) {
		    ties.add(pair);
		} else {
		    alreadyMerged += ties.size();
		    return ties;
		}
	    }
	}
	// this should only happen when clustering is done - all links are in single clusters
	return ties;
    }

    protected double getPartitionDensity() {
	double averageLinkDensity = 0;

	double linkDensity;
	for (Community c : communities.values()) {
	    linkDensity = c.getLinkDensity();
	    averageLinkDensity += linkDensity;
	}
	return averageLinkDensity * (2d / ((double) EDGE_COUNT));
    }

    protected void saveBestPartition() {
	bestPartition = new Community[communities.size()];
	int i = 0;
	for (Community c : communities.values()) {
	    bestPartition[i++] = new Community(c);
	}
    }

    @Override
    public void execute(GraphModel gm) {
	this.graph = gm.getGraphVisible();
	graph.readLock();

	// add column to datamodel
	if (!gm.getEdgeTable().hasColumn(EDGE_COMMUNITY)) {
//		gm.getEdgeTable().addColumn(EDGE_COMMUNITY, EDGE_COMMUNITY, String.class, Origin.DATA, "", false);
		gm.getEdgeTable().addColumn(EDGE_COMMUNITY, String.class);
	}
	if (!gm.getNodeTable().hasColumn(EDGE_COMMUNITY)) {
//		gm.getEdgeTable().addColumn(EDGE_COMMUNITY, EDGE_COMMUNITY, String.class, Origin.DATA, "", false);
		gm.getNodeTable().addColumn(EDGE_COMMUNITY, String.class);
	}



	EDGE_COUNT = graph.getEdgeCount();
	NODE_COUNT = graph.getNodeCount();

	Progress.start(progressTicket, NODE_COUNT);

	System.out.println("Started clustering...");

	long starttime = System.currentTimeMillis();
	long time = System.currentTimeMillis();
	System.out.print("Pre-Calculating all similarities... ");
	BUFFER_SIMILARITIES = new HashMap<EdgePair, Double>();
	BUFFER_COMMON_NEIGHBOURS = new HashMap<NodePair, Integer>();
	BUFFER_TOTAL_NEIGHBOURS = new HashMap<NodePair, Integer>();
	breadthFirst();
//	brute();
	BUFFER_COMMON_NEIGHBOURS.clear();
	BUFFER_TOTAL_NEIGHBOURS.clear();
	System.out.println("OK: " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
	System.out.println("BUFFER size after breadthfirst: " + BUFFER_SIMILARITIES.size());


	System.out.print("Sorting all edge pairs by their similarity... ");

	QuickSort qs = new QuickSort();
	SORTED_BUFFER = BUFFER_SIMILARITIES.keySet().toArray(new EdgePair[BUFFER_SIMILARITIES.size()]);
	qs.sort(SORTED_BUFFER);
	System.out.println("OK: " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
	System.out.println("Sorted buffer: ");

	System.out.print("Adding all links to their own community... ");
	communities = new HashMap<String, Community>(EDGE_COUNT);
	edgeCommunities = new HashMap<String, String>(EDGE_COUNT);
	for (Edge e : graph.getEdges()) {
	    if (cancelled) {
		return;
	    }
	    // set community ID as ID of its first edge
	    communities.put( (String)(e.getId()), new Community(e));
	}
	System.out.println("OK");

	densityGraph = new ArrayList<Double>();
	System.out.print("Building hierarchy... ");
	dendrogramHeight = 0;
	bestPartitionDensity = -1;
	bestPartitionDensityCut = -1;
	alreadyMerged = 0;
	double partitionDensity;
	List<EdgePair> edgePairs = mostSimilarLinks();

	while (!edgePairs.isEmpty()) {
	    if (cancelled) {
		return;
	    }
	    for (EdgePair edgePair : edgePairs) {
		if (cancelled) {
		    return;
		}
		String host = edgeCommunities.get(edgePair.edge1);
		String merge = edgeCommunities.get(edgePair.edge2);
		if (host.equals(merge)) {
		    continue;
		}
		Community hostCommunity = communities.get(host);
		Community mergedCommunity = communities.get(merge);
		// merge smaller community into larger 
		if (hostCommunity.edges.size() > mergedCommunity.edges.size()) {
		    hostCommunity.merge(mergedCommunity);
		    communities.remove(merge);
		} else {
		    mergedCommunity.merge(hostCommunity);
		    communities.remove(host);
		}
	    }
	    partitionDensity = getPartitionDensity();
	    densityGraph.add(partitionDensity);
//	    System.out.println("Communities = " + communities.size());
//	    System.out.println("Partition density = " + partitionDensity);
	    if (partitionDensity > bestPartitionDensity) {
		// remember this partition because its best so far. 
		// we do this because algorithm will only have 1 clustering result
//		if (communities.size() >= communityCount) {
		bestPartitionDensity = partitionDensity;
		bestPartitionDensityCut = dendrogramHeight;
		saveBestPartition();
//	    } else if (((bestPartitionDensity - partitionDensity) / ((double) bestPartitionDensity)) < 0.1) {
//		asd = 0;
//	    } else {
//		asd++;
//		if (asd == ASDF) {
//		    bestPartitionDensity = partitionDensity;
//		    bestPartitionDensityCut = dendrogramHeight;
//		    saveBestPartition();
//		}
	    }
	    // move to next edge pair
	    edgePairs = mostSimilarLinks();
//	    System.out.println("Most similar links number: " + edgePairs.size());
	    dendrogramHeight++;
	}
	System.out.println("OK");

	System.out.println("Communities number: " + bestPartition.length);


//	communities.clear();
//
////	refineCommunities();
//	for (int i = 0; i < bestPartition.length; i++) {
//	    communities.put(bestPartition[i].id, bestPartition[i]);
//	}
//
//	edgeCommunities.clear();
//	for (int i = bestPartition.length - 1; i >= 0; i--) {
//	    for (Integer e : bestPartition[i].edges) {
//		edgeCommunities.put(e, bestPartition[i].id);
//	    }
//	}
//
////	for (int i = 0; i < bestPartition.length; i++) {
////	    System.out.println("bestPartition[i].size=" + bestPartition[i].edges.size());
////	}
//	Arrays.sort(bestPartition, new Comparator<Community>() {
//	    @Override
//	    public int compare(Community o1, Community o2) {
//		return o1.numberOfLinks() - o2.numberOfLinks();
//	    }
//	});
//	double tmp;
//	System.out.println("Starting refinement...");
//
//	for (Integer c : communities.keySet().toArray(new Integer[communities.keySet().size()])) {
//	    if (cancelled) {
//		return;
//	    }
//	    if (!communities.containsKey(c)) {
//		continue;
//	    }
//	    for (Integer w : communities.keySet().toArray(new Integer[communities.keySet().size()])) {
//		if (cancelled) {
//		    return;
//		}
//		if (!communities.containsKey(w)) {
//		    continue;
//		}
//		Community hostCommunity = communities.get(c);
//		Community mergedCommunity = communities.get(w);
//		if (hostCommunity == null || mergedCommunity == null) {
//		    System.out.println("NULL IT IS!!");
//		    continue;
//		}
//		HashSet<Integer> connectors = hostCommunity.getConnectors(mergedCommunity);
//		System.out.println("");
//		System.out.println("connectors.size: " + connectors.size());
//		System.out.println("outside edges1: " + hostCommunity.countOutsideEdges());
//		System.out.println("outside edges2: " + mergedCommunity.countOutsideEdges());
//		if (connectors.size() >= hostCommunity.countOutsideEdges()
//			|| connectors.size() >= mergedCommunity.countOutsideEdges()) {
//		    communities.get(mergeCommunities(hostCommunity, mergedCommunity));
//		    System.out.println("Goin ");
////		    for (Integer e : connectors) {
////			Integer comID = edgeCommunities.get(e);
////			Community c = communities.get(comID);
////			if (c == null) {
////			    System.out.println("C is null");
////			    continue;
////			}
////			if (c.getConnectors(newCom).size() >= c.countOutsideEdges()) {
////			    newCom = communities.get(mergeCommunities(newCom, c));
////			}
////		    }
//		}
//
//	    }
//	}
//
////	    for (int i = 0; i < bestPartition.length; i++) {
////		System.out.println("Processing " + i + "th partition...");
////		for (int j = i + 1; j < bestPartition.length; j++) {
////		    if (cancelled) {
////			return;
////		    }
////		    if (sameCommunity(bestPartition[i].id, bestPartition[j].id)) {
//////		    System.out.println("sameCommunity " + bestPartition[i].id + "," + bestPartition[j].id);
////			continue;
////		    }
////		    Integer host = edgeCommunities.get(bestPartition[i].id);
////		    Integer merge = edgeCommunities.get(bestPartition[j].id);
////		    System.out.println("host: " + host + ", merge: " + merge);
////		    if (host.equals(merge)) {
////			continue;
////		    }
////		    Community hostCommunity = communities.get(host);
////		    Community mergedCommunity = communities.get(merge);
////		    if (hostCommunity == null || mergedCommunity == null) {
////			System.out.println("NULL IT IS!!");
////			continue;
////		    }
////		    HashSet<Integer> connectors = hostCommunity.getConnectors(mergedCommunity);
////		    if (connectors.size() >= hostCommunity.countOutsideEdges()
////			    || connectors.size() >= mergedCommunity.countOutsideEdges()) {
////			Community newCom = communities.get(mergeCommunities(hostCommunity, mergedCommunity));
////			for (Integer e : connectors) {
////			    Integer comID = edgeCommunities.get(e);
////			    Community c = communities.get(comID);
////			    if (c == null) {
////				System.out.println("C is null");
////				continue;
////			    }
////			    if (c.getConnectors(newCom).size() >= c.countOutsideEdges()) {
////				newCom = communities.get(mergeCommunities(newCom, c));
////			    }
////			}
////		    }
//////		tmp = hostCommunity.mergeScore(mergedCommunity);
//////		if (tmp > 0.5d) {
//////		    System.out.print(hostCommunity.id + "," + mergedCommunity.id);
//////		    System.out.println("; size1: " + hostCommunity.nodes.size()
//////			    + "; size2: " + mergedCommunity.nodes.size()
//////			    + "; mergeScore: " + tmp);
//////		    // merge smaller community into larger 
//////		    mergeCommunities(hostCommunity, mergedCommunity);
//////
//////		}
////		}
////	    }
//
//	saveBestPartition();


	Arrays.sort(bestPartition, new Comparator<Community>() {
	    @Override
	    public int compare(Community o1, Community o2) {
		return o1.numberOfLinks() - o2.numberOfLinks();
	    }
	});
	for (int i = 0; i < bestPartition.length; i++) {
		bestPartition[i].markEdges();
		bestPartition[i].markNodes();
//	    System.out.println("Best partition " + i + " has nodes: " + bestPartition[i].nodes.size() + " and edges: " + bestPartition[i].edges.size());
	}

	System.out.println("OK");
	System.out.println("Communities found: " + bestPartition.length);
	System.out.println("Dendrogram height: " + dendrogramHeight);
	System.out.println("Best partition density: " + bestPartitionDensity);
	System.out.println("Best partition density cut: " + bestPartitionDensityCut);

	graph.readUnlockAll();
	System.out.println("Finished clustering...");

    }

    private String mergeCommunities(Community hostCommunity, Community mergedCommunity) {
	if (hostCommunity.edges.size() > mergedCommunity.edges.size()) {
	    hostCommunity.merge(mergedCommunity);
	    communities.remove(mergedCommunity.id);
	    return hostCommunity.id;
	} else {
	    mergedCommunity.merge(hostCommunity);
	    communities.remove(hostCommunity.id);
	    return mergedCommunity.id;
	}
    }

    protected void refineCommunitiess() {
	double common;
	int count1;
	int count2;

	communities.clear();
	for (int i = 0; i < bestPartition.length; i++) {
	    if (cancelled) {
		return;
	    }
	    communities.put(bestPartition[i].id, bestPartition[i]);
	}
	for (int i = 0; i < bestPartition.length; i++) {
	    if (cancelled) {
		return;
	    }
	    System.out.println("i: " + i);
	    if (!communities.containsKey(bestPartition[i].id)) {
		continue;
	    }
	    for (int j = i + 1; j < bestPartition.length; j++) {
		if (cancelled) {
		    return;
		}
		if (!communities.containsKey(bestPartition[j].id)) {
		    continue;
		}
		count1 = bestPartition[i].getNodeCount();
		count2 = bestPartition[j].getNodeCount();
		double intersection = ((double) bestPartition[i].getIntersection(bestPartition[j]));
		if (intersection > 0) {
		    common = intersection / (Math.min(count1, count2));
		    if (common >= 0.6) {
			Community hostCommunity = bestPartition[i];
			Community mergedCommunity = bestPartition[j];
			System.out.println("Host: " + hostCommunity.id + ", merge: " + mergedCommunity.id + " has intersection: " + common);
			// merge smaller community into larger 
			if (hostCommunity.edges.size() > mergedCommunity.edges.size()) {
			    hostCommunity.merge(mergedCommunity);
			    communities.remove(mergedCommunity.id);
			} else {
			    mergedCommunity.merge(hostCommunity);
			    communities.remove(hostCommunity.id);
			}
		    }
		}
	    }
	}

	saveBestPartition();
    }

    protected void brute() {
	for (Edge e1 : graph.getEdges()) {
	    for (Edge e2 : graph.getEdges()) {
		if (BUFFER_SIMILARITIES.containsKey(new EdgePair((String)e1.getId(), (String)e2.getId()))) {
		    continue;
		} else if (BUFFER_SIMILARITIES.containsKey(new EdgePair((String)e2.getId(), (String)e1.getId()))) {
		    continue;
		}
		getJaccardIndex(e1, e2);
	    }
	}
    }

    protected void breadthFirst() {

	LinkedList<String> Q = new LinkedList<>();

	LinkedList<String> unvisited = new LinkedList<>();
	for (Node node : graph.getNodes()) {
	    unvisited.add((String) (node.getId()));
	}

	Node n;
	while (!unvisited.isEmpty()) {
	    if (cancelled) {
		return;
	    }
	    //remove node from unvisited, and add it to queue
	    //in connected graph, this will only be called once
	    Q.add(unvisited.poll());
	    while (!Q.isEmpty()) {
		if (cancelled) {
		    return;
		}
		//remove first node from queue
		n = graph.getNode(Q.poll());
		//and traverse all incident edges
		for (Edge e1 : graph.getEdges(n)) {
		    //if current neighbor is unvisited, add to Q
			String neighbor = (String)getOtherNode(e1, n).getId();
		    if (unvisited.contains(neighbor)) {
			Progress.progress(progressTicket);
			unvisited.remove(neighbor);
			Q.add(neighbor);
		    }
		    for (Edge e2 : graph.getEdges(n)) {
			if (e2.getId().equals(e1.getId())) {
			    continue;
			}
			if (BUFFER_SIMILARITIES.containsKey(new EdgePair((String)e1.getId(), (String)e2.getId()))) {
			    continue;
			} else if (BUFFER_SIMILARITIES.containsKey(new EdgePair((String)e2.getId(),(String) e1.getId()))) {
			    continue;
			}

			getJaccardIndex(e1, e2);
		    }
		}

	    }
	}
    }

    protected Node getOtherNode(Edge e, Node oneNode) {
	if (e.getSource().getId().equals(oneNode.getId())) {
	    return e.getTarget();
	} else {
	    return e.getSource();
	}
    }

    @Override
    public String getReport() {

	//distribution of values
	Map<Integer, Double> dist = new HashMap<Integer, Double>();
	int i = 0;
	for (Double d : densityGraph) {
	    dist.put(i++, d);
	}

	//Distribution series
	XYSeries dSeries = ChartUtils.createXYSeries(dist, "Density graph");
	XYSeriesCollection dataset = new XYSeriesCollection();
	dataset.addSeries(dSeries);

	JFreeChart chart = ChartFactory.createScatterPlot(
		"partition density",
		"height",
		"partition density",
		dataset,
		PlotOrientation.VERTICAL,
		true,
		false,
		false);
	chart.removeLegend();
	ChartUtils.decorateChart(chart);
	ChartUtils.scaleChart(chart, dSeries, false);
	String imageFile = ChartUtils.renderChart(chart, "partition-density.png");

	return "<HTML> <BODY> <h1> Link communities finished</h1> "
		+ "<hr>"
		+ "<br />"
		+ imageFile
		+ "<br />" + "Dendrogram height: " + dendrogramHeight
		+ "<br />" + "Communities found: " + bestPartition.length
		+ "<br />" + "Best partition density: " + bestPartitionDensity
		+ "<br />" + "Best partition density cut: " + bestPartitionDensityCut
		+ "<br />" + "<br />" + "Yong-Yeol Ahn, James P. Bagrow & Sune Lehmann, "
		+ "<i>Link communities reveal multi-scale complexity in networks</i>"
		+ ", 2009 <br />"
		+ "</BODY> </HTML>";
    }

    private boolean similarSizes(int size, int size0, double maxDifference) {
	double diff = Math.abs(size - size0);
	boolean similar = true;
	if (diff > ((double) (size * maxDifference))) {
	    similar = false;
	}
	if (diff > ((double) (size0 * maxDifference))) {
	    similar = false;
	}
	System.out.println(((similar) ? "Similar" : "NOT similar") + size + "," + size0);

	return similar;
    }

    protected class Community {

	String id;
	Set<String> edges;
	Set<String> nodes;

	public Community(Community community) {
	    this.id = community.id;
	    this.edges = new HashSet<>(community.edges);
	    this.nodes = new HashSet<>(community.nodes);
	}

	public Community(Edge edge) {
	    this.id = (String)edge.getId();
	    this.edges = new HashSet<>();
	    this.nodes = new HashSet<>();
	    this.edges.add((String)edge.getId());
	    nodes.add((String)edge.getSource().getId());
	    nodes.add((String)edge.getTarget().getId());
	    edgeCommunities.put(id, (String)edge.getId());
	}

	public void merge(Community community) {
	    for (String edge : community.edges) {
		edgeCommunities.put(edge, id);
	    }
	    edges.addAll(community.edges);
	    nodes.addAll(community.nodes);
	}

	public HashSet<String> getConnectors(Community other) {
	    HashSet<String> inside = new HashSet<>();
	    for (String node : nodes) {
		Node insider = graph.getNode(node);
		for (Node n : graph.getNeighbors(insider)) {
		    if (other.containsNode((String)n.getId())) {
			inside.add((String)graph.getEdge(graph.getNode(node), n).getId());
		    }
		}
//		System.out.println("Node " + insider.getLabel() + " has " + graph.getNeighbors(insider).toArray().length + " neighbors." + inside + " is inside");
	    }

	    return inside;
	}

	public double mergeScore(Community other) {
//	    double averageSize = (edges.size() + other.edges.size()) / 2.0d;
//	    double connectors = countConnectors(other);
	    double averageSize = (nodes.size() + other.nodes.size()) / 2.0d;
	    double minSize = Math.min(nodes.size(), other.nodes.size());
	    double connectors = getIntersection(other);
//	    System.out.println("Size1: " + edges.size() + ", size2: " + other.edges.size() + " Average size: " + averageSize + ", connectors: " + connectors);
	    return connectors / (minSize);
	}

	public int getNodeCount() {
	    return nodes.size();
	}

	public int countOutsideEdges() {
	    HashSet<String> outside = new HashSet<>();
	    for (String node : nodes) {
		Node insider = graph.getNode(node);
		for (Edge e : graph.getEdges(insider)) {
		    if (!edges.contains(e.getId())) {
			outside.add((String)e.getId());
		    }
		}
	    }
	    return outside.size();
	}

	public boolean containsNode(String node) {
//	    for (Edge e : graph.getEdges(graph.getNode(node))) {
//		if (edgeCommunities.get(e.getId()).equals(id)) {
//		    return true;
//		}
//	    }
//	    return false;
	    return nodes.contains(node);
	}

	public int getIntersection(Community other) {
	    int commonNodes = 0;
	    for (String n : nodes) {
		if (other.containsNode(n)) {
		    commonNodes++;
		}
	    }
	    return commonNodes;
	}

	public void markNodes() {
//	    if (edges.size() <= MIN_COMMUNITY_SIZE) {
//		return;
//	    }
		Edge e;
	    for (String edge : edges) {
			e = graph.getEdge(edge);
			e.getSource().setAttribute(EDGE_COMMUNITY, id);
			e.getTarget().setAttribute(EDGE_COMMUNITY, id);
	    }
	}

	public void markEdges() {
//	    if (edges.size() > MIN_COMMUNITY_SIZE) {
	    for (String edge : edges) {
			graph.getEdge(edge).setAttribute(EDGE_COMMUNITY, id);
	    }
//	    } else {
	    //FIXME throwing null exception here for no apparent reason
//		for (Integer edge : edges) {
//		    graph.getEdge(edge.intValue()).setAttribute(EDGE_COMMUNITY, EDGE_COMMUNITY_TRIVIAL);
//		}
//	    }
	}

	public int numberOfLinks() {
	    return edges.size();
	}

	public int numberOfNodes() {
	    //TODO optimize performance of this
	    Set<String> nodes = new TreeSet<>();
	    for (String edge : edges) {
		nodes.add( (String)(graph.getEdge(edge).getSource().getId()));
		nodes.add( (String)(graph.getEdge(edge).getTarget().getId()));
	    }
	    return nodes.size();
	}

	public double getLinkDensity() {
	    double links = numberOfLinks();
	    if (links == 1) {
		return 0;
	    }
	    double nodes = numberOfNodes();
	    if (((nodes - 2) * (nodes - 1)) == 0) {
		return 0;
	    }
	    return (links * (links - nodes + 1)) / ((nodes - 2) * (nodes - 1));
	}
    }

    protected class NodePair {

	String node1;
	String node2;

	public NodePair(String node1, String node2) {
	    this.node1 = node1;
	    this.node2 = node2;
	}

	@Override
	public String toString() {
	    return "[" + getNode1().getLabel() + "," + getNode2().getLabel() + "]";
	}

	public Node getNode1() {
	    return graph.getNode(node1);
	}

	public Node getNode2() {
	    return graph.getNode(node2);
	}

	@Override
	public int hashCode() {
	    int hash = 5;
	    hash = 59 * hash + (this.node1 != null ? this.node1.hashCode() : 0);
	    hash = 59 * hash + (this.node2 != null ? this.node2.hashCode() : 0);
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
	    final NodePair other = (NodePair) obj;
	    if (this.node1 != other.node1 && (this.node1 == null || !this.node1.equals(other.node1))) {
		return false;
	    }
	    if (this.node2 != other.node2 && (this.node2 == null || !this.node2.equals(other.node2))) {
		return false;
	    }
	    return true;
	}
    }

    protected class EdgePair {

	String edge1;
	String edge2;

	public EdgePair(String edge1, String edge2) {
	    this.edge1 = edge1;
	    this.edge2 = edge2;
	}

	private EdgePair(Edge e1, Edge e2) {
	    this.edge1 = (String)e1.getId();
	    this.edge2 = (String)e2.getId();
	}

	Edge getEdge1() {
	    return graph.getEdge(edge1);
	}

	Edge getEdge2() {
	    return graph.getEdge(edge2);
	}

	@Override
	public String toString() {
	    return "[ (" + getEdge1().getSource().getLabel() + "," + getEdge1().getTarget().getLabel() + ") ; (" + getEdge2().getSource().getLabel() + "," + getEdge2().getTarget().getLabel() + ")]";
	}

	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 17 * hash + (this.edge1 != null ? this.edge1.hashCode() : 0);
	    hash = 17 * hash + (this.edge2 != null ? this.edge2.hashCode() : 0);
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
	    final EdgePair other = (EdgePair) obj;
	    if (this.edge1 != other.edge1 && (this.edge1 == null || !this.edge1.equals(other.edge1))) {
		return false;
	    }
	    if (this.edge2 != other.edge2 && (this.edge2 == null || !this.edge2.equals(other.edge2))) {
		return false;
	    }
	    return true;
	}
    }

    class QuickSort {

	private EdgePair[] pairs;
	private int number;

	public void sort(EdgePair[] values) {
	    // Check for empty or null array
	    if (values == null || values.length == 0) {
		return;
	    }
	    this.pairs = values;
	    number = values.length;
	    quicksort(0, number - 1);
	}

	private void quicksort(int low, int high) {
	    int i = low, j = high;
	    // Get the pivot element from the middle of the list
	    double pivot = BUFFER_SIMILARITIES.get(pairs[low + (high - low) / 2]).doubleValue();

	    // Divide into two lists
	    while (i <= j) {
		// If the current value from the left list is smaller then the pivot
		// element then get the next element from the left list
		while (BUFFER_SIMILARITIES.get(pairs[i]).doubleValue() < pivot) {
		    i++;
		}
		// If the current value from the right list is larger then the pivot
		// element then get the next element from the right list
		while (BUFFER_SIMILARITIES.get(pairs[j]).doubleValue() > pivot) {
		    j--;
		}

		// If we have found a values in the left list which is larger then
		// the pivot element and if we have found a value in the right list
		// which is smaller then the pivot element then we exchange the
		// values.
		// As we are done we can increase i and j
		if (i <= j) {
		    exchange(i, j);
		    i++;
		    j--;
		}
	    }
	    // Recursion
	    if (low < j) {
		quicksort(low, j);
	    }
	    if (i < high) {
		quicksort(i, high);
	    }
	}

	private void exchange(int i, int j) {
	    EdgePair temp = pairs[i];
	    pairs[i] = pairs[j];
	    pairs[j] = temp;
	}
    }
}
