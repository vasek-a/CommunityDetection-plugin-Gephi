/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.infomap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
//import org.gephi.data.attributes.api.AttributeModel;
//import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.graph.api.Origin;
//import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
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
public class InfomapClusterer implements Statistics, LongTask {

    // constants
    public static final String PAGERANK = "pageranks";
    public static final String COMMUNITY_ID = "Infomap community";
    public static final int NO_COMMUNITY = -1;
    // params
    public static double TELEPORT_CHANCE_DEFAULT = 0.15;
    protected double teleportChance = TELEPORT_CHANCE_DEFAULT;
    // algorithm fields
    protected Graph graph;
    protected int NODE_COUNT;
    protected int EDGE_COUNT;
    // long task api fields
    protected boolean cancelled;
    protected ProgressTicket progressTicket;
    //
    protected HashMap<String, Double> NODE_FREQUENCIES;
    protected HashMap<String, String> NODE_COMMUNITIES;
    protected HashMap<String, Community> COMMUNITIES;
    protected double bestCommunitiesValue;

    protected double mapEquation() {
//	System.out.println("Map equation, communities count=" + communities.size());
	double redPart;
	double bluePart;
	double blackPart = 0;
	//
	double exitProbability;
	double moduleSwitchProbability = 0;
	for (Community community : COMMUNITIES.values()) {
	    exitProbability = community.getExitProbability();
	    moduleSwitchProbability += exitProbability;
	    //
	    blackPart += exitProbability * Math.log(exitProbability);
	}
//	System.out.println("Module switch probability: " + moduleSwitchProbability);
	redPart = moduleSwitchProbability * Math.log(moduleSwitchProbability);

	double bluePartNegative = 0;
	for (Double f : NODE_FREQUENCIES.values()) {
	    bluePartNegative += f * Math.log(f);
	}

	double bluePartPositive = 0;
	double tmp;
	for (Community community : COMMUNITIES.values()) {
	    tmp = community.getExitProbability() + community.getModuleFrequency();
	    bluePartPositive += tmp * Math.log(tmp);
	}

	bluePart = -bluePartNegative + bluePartPositive;

	return redPart - (1 + 1) * blackPart + bluePart;

    }

    // TESTED OK
    protected Collection<String> getConnectedCommunities(Community community) {
	Set<String> communities = new HashSet<>();
	for (String id : community.members) {
	    for (Node neighbor : graph.getNeighbors(graph.getNode(id))) {
		if (!NODE_COMMUNITIES.get(neighbor.getId()).equals(community.id)) {
		    communities.add(NODE_COMMUNITIES.get(neighbor.getId()));

		}
	    }
	}
	return communities;
    }

    @Override
    public void execute(GraphModel gm) {
	Progress.start(progressTicket);
	System.out.println("INFOMAP started...");
	System.out.println("Loading graph...");
	// graph
	graph = gm.getGraphVisible();
	graph.readLock();
//	if (!am.getNodeTable().hasColumn(COMMUNITY_ID)) {
//	    am.getNodeTable().addColumn(COMMUNITY_ID, COMMUNITY_ID, AttributeType.STRING, AttributeOrigin.COMPUTED, "");
//	}
	// add column to datamodel
	if (!gm.getNodeTable().hasColumn(COMMUNITY_ID)) {
	    gm.getNodeTable().addColumn(COMMUNITY_ID, COMMUNITY_ID, String.class, Origin.DATA, "", false);
	}
	// init variables
	EDGE_COUNT = graph.getEdgeCount();
	NODE_COUNT = graph.getNodeCount();
	NODE_FREQUENCIES = new HashMap<>(NODE_COUNT);
	NODE_COMMUNITIES = new HashMap<>(NODE_COUNT);
	COMMUNITIES = new HashMap<>(NODE_COUNT);
	System.out.println("Loading PageRank values...");
	for (Node n : graph.getNodes()) {
	    NODE_FREQUENCIES.put((String)n.getId(), (Double) n.getAttribute(PAGERANK));
	    NODE_COMMUNITIES.put((String)n.getId(), (String)n.getId());
	    COMMUNITIES.put((String)n.getId(), new Community((String)n.getId()));
	}
	boolean done = false;

	CommunityPair best;
	double bestValue = Double.MAX_VALUE;
	double currentValue;

	Community backup1;
	Community backup2;
	Community host;
	Community peer;

	int iteration = 0;
	while (!done) {
	    System.out.println("Iteration: " + iteration++);
		String[] communities = COMMUNITIES.keySet().toArray(new String[0]);
	    done = true;
	    for (int i = 0; i < communities.length; i++) {
		host = COMMUNITIES.get(communities[i]);
		if (host == null) {
		    continue;
		}
		best = null;

		System.out.println("");
		System.out.println("Host: " + host.id);
//		System.out.println("Communities size before =" + COMMUNITIES.values().size());
		for (String id : getConnectedCommunities(host)) {
		    peer = COMMUNITIES.get(id);
		    backup1 = new Community(host);
		    backup2 = new Community(peer);
		    // try merge
		    if (host.size() >= peer.size()) {
			host.merge(peer);
			host.markNodes();
			COMMUNITIES.remove(peer.id);
		    } else {
			peer.merge(host);
			peer.markNodes();
			COMMUNITIES.remove(host.id);
		    }

		    // calculate new value
//		    System.out.println("Communities after temp merge: " + COMMUNITIES.values().size());
		    currentValue = mapEquation();
		    System.out.println("Map equation value: " + currentValue);
		    if (currentValue < bestValue) {
			System.out.println("------------new best value = " + currentValue);
			// save new best option
			bestValue = currentValue;
			if (host.size() >= peer.size()) {
			    best = new CommunityPair(host.id, peer.id);
			} else {
			    best = new CommunityPair(peer.id, host.id);
			}
		    }

		    // revert merge
		    COMMUNITIES.remove(host.id);
		    COMMUNITIES.remove(peer.id);
		    COMMUNITIES.put(backup1.id, backup1);
		    COMMUNITIES.put(backup2.id, backup2);
		    backup1.markNodes();
		    backup2.markNodes();
//		    System.out.println("Communities after revert merge: " + COMMUNITIES.values().size());
		}
//		System.out.println("Communities size before2 =" + COMMUNITIES.values().size());
		if (best != null) {
		    System.out.println("Best value = " + bestValue);
		    host = COMMUNITIES.get(best.c1);
		    peer = COMMUNITIES.get(best.c2);
		    host.merge(peer);
		    COMMUNITIES.remove(peer.id);
		    host.markNodes();
		    done = false;
		}
//		System.out.println("Communities size after =" + COMMUNITIES.values().size());
	    }
	}

	for (Community community : COMMUNITIES.values()) {
	    community.markNodesAttributes();
	}

	graph.readUnlockAll();
	System.out.println("Finished clustering, iterations: " + iteration);
	Progress.finish(progressTicket);
    }

    @Override
    public String getReport() {

	//distribution of values
	Map<Integer, Integer> plot = new HashMap<Integer, Integer>();
//	for (Map.Entry<Integer, Integer> entry : NODE_COMMUNITIES.entrySet()) {
//	    plot.put(entry.getKey(), entry.getValue());
//	}

	//Distribution series
	XYSeries dSeries = ChartUtils.createXYSeries(plot, "SLPA communities graph");
	XYSeriesCollection dataset = new XYSeriesCollection();
	dataset.addSeries(dSeries);

	JFreeChart chart = ChartFactory.createScatterPlot(
		"Communities",
		"Community ID",
		"Members count",
		dataset,
		PlotOrientation.VERTICAL,
		true,
		false,
		false);
	chart.removeLegend();
	ChartUtils.decorateChart(chart);
	ChartUtils.scaleChart(chart, dSeries, false);
	String imageFile = ChartUtils.renderChart(chart, "partition-density.png");

	return "<HTML> <BODY> <h1> SLPA finished</h1> "
		+ "<hr>"
		+ "<br />"
		+ imageFile
		+ "<br />" + "<br />" + "Jierui Xie and Boleslaw K. Szymanski, "
		+ "<i>Towards Linear Time Overlapping Community Detection in Social Networks</i>"
		+ ", 2012 <br />"
		+ "</BODY> </HTML>";
    }

    @Override
    public boolean cancel() {
	this.cancelled = true;
	System.out.println("CANCEL infomap computation...");
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
	this.progressTicket = pt;
    }

    public double getTeleportChance() {
	return teleportChance;
    }

    public void setTeleportChance(double teleportChance) {
	this.teleportChance = teleportChance;
    }
//    protected Matrix transitionProbabilityMatrix() {
//	Matrix P = new Matrix(NODE_COUNT, NODE_COUNT);
//	Node Ni;
//	Node Nj;
//	// there is 0.15 chance to teleport to any node in the graph
//	double chanceByTeleport = teleportChance / NODE_COUNT;
//	double chanceByEdge;
//	int neighborCount;
//	Node[] nodes = graph.getNodes().toArray();
//	for (int i = 0; i < NODE_COUNT; i++) {
//	    Ni = nodes[i];
//	    neighborCount = graph.getNeighbors(Ni).toArray().length;
//	    chanceByEdge = (1 - teleportChance) / neighborCount;
//	    for (int j = 0; j < NODE_COUNT; j++) {
//		Nj = nodes[j];
//		P.getArray()[i][j] += chanceByTeleport;
//		if (i == j) {
//		    continue;
//		}
//		if (graph.isAdjacent(Ni, Nj)) {
//		    P.getArray()[i][j] += chanceByEdge;
//		}
//	    }
//	}
//	return P;
//    }
//
//    //
//    protected Matrix ergodicNodeVisitFrequencies() {
//	Matrix P = transitionProbabilityMatrix();
//	double[] initial = new double[NODE_COUNT];
//	for (int i = 0; i < initial.length; i++) {
//	    initial[i] = 1.0 / NODE_COUNT;
//	}
//	Matrix x = new Matrix(initial, 1);
//	Matrix tmp;
//	boolean converged = false;
//	while (!converged && !cancelled) {
////	    printMatrix(x);
//	    tmp = x.times(P);
////	    if (Arrays.equals(tmp.getArray()[0], x.getArray()[0])) {
//	    if (areSimilar(tmp.getArray()[0], x.getArray()[0], 0.01)) {
//		converged = true;
//		System.out.println("________________________________________________________________");
//		System.out.println("Converged...");
//		System.out.println("");
//	    }
//	    x = x.times(P);
//	}
//	return x;
//
//    }
//
//    private boolean areSimilar(double[] array1, double[] array2, double similarityRatio) {
//	double diff;
//	for (int i = 0; i < array1.length; i++) {
//	    diff = Math.abs(array1[i] - array2[i]);
//	    if ((diff / array1[i]) > similarityRatio) {
//		return false;
//	    }
//	    if ((diff / array2[i]) > similarityRatio) {
//		return false;
//	    }
//	}
//	return true;
//    }
//
//    public void printMatrix(Matrix m) {
//	System.out.println("");
//	System.out.println("Matrix " + m.getColumnDimension() + " x " + m.getRowDimension() + ":");
//	for (int i = 0; i < m.getRowDimension(); i++) {
//	    for (int j = 0; j < m.getColumnDimension(); j++) {
//		System.out.print(m.getArray()[i][j] + ", ");
//	    }
//	    System.out.println("");
//	}
//    }

    public class CommunityPair {

	String c1;
	String c2;

	public CommunityPair(String c1, String c2) {
	    this.c1 = c1;
	    this.c2 = c2;
	}

	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 89 * hash + (this.c1 != null ? this.c1.hashCode() : 0);
	    hash = 89 * hash + (this.c2 != null ? this.c2.hashCode() : 0);
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
	    final CommunityPair other = (CommunityPair) obj;
	    if (this.c1 != other.c1 && (this.c1 == null || !this.c1.equals(other.c1))) {
		return false;
	    }
	    if (this.c2 != other.c2 && (this.c2 == null || !this.c2.equals(other.c2))) {
		return false;
	    }
	    return true;
	}
    }

    public class Community {

	private static final double NOT_SET = -1;
	private final String id;
	private double frequency = NOT_SET;
	private double exitProbability = NOT_SET;
	private Set<String> members = new HashSet<>();

	public Community(String foundingMember) {
	    members.add(foundingMember);
	    id = foundingMember;
	}

	public Community(Community com) {
	    this.id = com.id;
	    this.frequency = com.frequency;
	    this.exitProbability = com.exitProbability;
	    for (String member : com.members) {
		this.members.add(member);
	    }
	}

	public void markNodes() {
	    for (String integer : members) {
		NODE_COMMUNITIES.put(integer, id);
	    }
	}

	public void markNodesAttributes() {
	    for (String i : members) {
		graph.getNode(i).setAttribute(COMMUNITY_ID, id);
	    }
	}

	public void merge(Community other) {
	    for (String otherMember : other.members) {
		members.add(otherMember);
	    }
	    frequency = NOT_SET;
	    exitProbability = NOT_SET;
	}

	public int size() {
	    return members.size();
	}

	public Set<String> getMembers() {
	    return members;
	}

	public String getId() {
	    return id;
	}

	public double getModuleFrequency() {
	    if (frequency != NOT_SET) {
		return frequency;
	    }
	    double communityFrequency = 0;
	    // traverse all nodes
	    for (String member : members) {
		communityFrequency += NODE_FREQUENCIES.get(member);
	    }

	    frequency = communityFrequency;
	    return frequency;

	}

	public double getExitProbability() {
	    if (exitProbability != NOT_SET) {
		return exitProbability;
	    }
	    double communityFrequency = 0;
	    double outsideWeight = 0;

	    // traverse all nodes
	    for (String member : members) {
		communityFrequency += NODE_FREQUENCIES.get(member);
		for (Node neighbor : graph.getNeighbors(graph.getNode(member))) {
		    if (!members.contains(neighbor.getId())) {
			outsideWeight += NODE_FREQUENCIES.get(member);
		    }
		}
	    }

	    frequency = communityFrequency;
	    // teleport
	    double tele = teleportChance * ((NODE_COUNT - members.size()) / (NODE_COUNT - 1)) * communityFrequency;
	    double guide = (1 - teleportChance) * outsideWeight;
	    exitProbability = tele + guide;
//	    System.out.println("ID: " + id + ", label: " + graph.getNode(id).getLabel() + " has exit probability: " + exitProbability);

	    return exitProbability;
	}
    }

    public Collection<Community> copyCommunities(Collection<Community> original) {
	Collection<Community> copy = new LinkedList<InfomapClusterer.Community>();
	for (Community c : original) {
	    copy.add(new Community(c));
	}
	return copy;
    }
}
