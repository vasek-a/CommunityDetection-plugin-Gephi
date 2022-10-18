/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import org.gephi.graph.api.Column;
//import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
//import org.gephi.graph.api.NodeData;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import sk.smitala.gephi.communitydetection.evaluation.external.Fmeasure;
import sk.smitala.gephi.communitydetection.evaluation.external.NMI;
import sk.smitala.gephi.communitydetection.evaluation.external.RandIndex;
import sk.smitala.gephi.communitydetection.evaluation.internal.IntraEdgesRatio;
import sk.smitala.gephi.communitydetection.utils.Community;
import sk.smitala.gephi.communitydetection.utils.DijkstraShortestPathAlgorithm;
import sk.smitala.gephi.communitydetection.utils.GraphPartitionLoader;
import sk.smitala.gephi.communitydetection.utils.traverse.Traverse;
import sk.smitala.gephi.communitydetection.utils.traverse.TraverseListener;

/**
 * Link communities clustering
 *
 * @author smitalm
 *
 */
public class ClusteringEvaluation implements Statistics, LongTask {

    // constants
    // algorithm fields
    protected Graph graph;
    protected int NODE_COUNT;
    protected int EDGE_COUNT;
    // long task api fields
    protected boolean cancelled;
    protected ProgressTicket progressTicket;
    // params
    private Column groundTruth;
    private Column[] attributes;
    private boolean computeDBI = false;
    private boolean skipExternal = false;
    // results
    private NMI[] _nmi;
    private RandIndex[] _randIndex;
    private Fmeasure[] _fMeasure;
    private IntraEdgesRatio[] _intraEdgesRatio;
    private double[] _daviesBouldin;
    protected HashMap<NodePair, Double> distances;
    public boolean isSkipExternal() {
	return skipExternal;
    }

    public void setSkipExternal(boolean skipExternal) {
	this.skipExternal = skipExternal;
    }

    @Override
    public void execute(GraphModel gm) {

	long starttime = System.currentTimeMillis();
	long time;
	System.out.println("EVALUATION: starting...");
	// graph
	graph = gm.getGraphVisible();
	graph.readLock();
	NODE_COUNT = graph.getNodeCount();
	Progress.start(progressTicket, NODE_COUNT);
	_nmi = new NMI[attributes.length];
	_randIndex = new RandIndex[attributes.length];
	_fMeasure = new Fmeasure[attributes.length];
	_intraEdgesRatio = new IntraEdgesRatio[attributes.length];
	_daviesBouldin = new double[attributes.length];


	Traverse<Node> nodeTraverse = new Traverse<Node>();
	GraphPartitionLoader groundTruthLoader = new GraphPartitionLoader(groundTruth);
	GraphPartitionLoader[] partitionLoader = new GraphPartitionLoader[attributes.length];

	// after traverse we will have ground truth partition (collection of Communities)
	nodeTraverse.registerListener(groundTruthLoader);
	for (int i = 0; i < attributes.length; i++) {
	    //
	    _intraEdgesRatio[i] = new IntraEdgesRatio(graph, attributes[i]);
	    nodeTraverse.registerListener(_intraEdgesRatio[i]);
	    //
	    partitionLoader[i] = new GraphPartitionLoader(attributes[i]);
	    // after traverse, we will have partition for each attribute
	    nodeTraverse.registerListener(partitionLoader[i]);
	    //
	    if (skipExternal) {
		continue;
	    }
	    //
	    _randIndex[i] = new RandIndex(groundTruth, attributes[i]);
	    // after traverse, we will have RandIndex value calculated for each attribute
	    nodeTraverse.registerPairListener(_randIndex[i]);
	    //
	    _fMeasure[i] = new Fmeasure(groundTruth, attributes[i]);
	    // after traverse, we will have F measure value calculated for each attribute
	    nodeTraverse.registerPairListener(_fMeasure[i]);
	}

	// we will register one more listener to monitor traverse progress
	nodeTraverse.registerListener(new TraverseListener<Node>() {
	    @Override
	    public void onElementVisited(Node e) {
		Progress.progress(progressTicket);
	    }
	});
	// traverse all nodes. Registered listeners will calculate all values in single graph traverse.
	nodeTraverse.traverseArray(graph.getNodes().toArray(), false);

	Progress.switchToIndeterminate(progressTicket);
	// now we need to calculate NMI for each attribute
	if (!skipExternal) {

	    // we will only calculate ground truth entropy once
	    double groundTruthEntropy = 0;
	    double tmp;
	    for (Community c : groundTruthLoader.getPartition().values()) {
		tmp = ((double) c.size()) / NODE_COUNT;
		groundTruthEntropy += tmp * Math.log(tmp);
	    }


	    Traverse<Community> partitionTraverse = new Traverse<Community>();
	    for (int i = 0; i < attributes.length; i++) {
		_nmi[i] = new NMI(NODE_COUNT, groundTruthEntropy, partitionLoader[i].getPartition());
		// after partition traverse we will have nmi calculated for this attribute
		partitionTraverse.registerListener(_nmi[i]);
	    }

	    // traverse all communities. Registered listeners will calculate all values in single partition traverse.
	    partitionTraverse.traverseArray(groundTruthLoader.getPartition().values()
		    .toArray(new Community[0]), true);


	}
	/////////////////////////////
	//DBI
	///////////////////////////
	if (computeDBI) {
	    distances = new HashMap<NodePair, Double>(graph.getNodeCount());
	    System.out.println("Computing shortest paths for DBI....");
	    for (Node n : graph.getNodes()) {
		DijkstraShortestPathAlgorithm algo = new DijkstraShortestPathAlgorithm(graph, n);
		algo.compute();
		for (Node ndata : algo.getDistances().keySet()) {
		    distances.put(new NodePair((String)n.getId(), (String)ndata.getId()), algo.getDistances().get(ndata));
		}
	    }
	    System.out.println("DONE computing path");
	    for (int i = 0; i < attributes.length; i++) {
		double sum = 0;
		for (Community c1 : partitionLoader[i].getPartition().values()) {
		    double maxR = 0;
		    double sumR = 0;
		    int countR = 0;
		    for (Community c2 : partitionLoader[i].getPartition().values()) {
			if (c1.getId().equals(c2.getId())) {
			    continue;
			}

			double intraSimilarity1 = averagePathLengthBetweenMembers(c1);
			double intraSimilarity2 = averagePathLengthBetweenMembers(c2);
			double interSimilarity = averagePathLengthBetweenCommunities(c1, c2);
			System.out.println("Intrer similarity: " + interSimilarity);
			System.out.println("Intra similarity 1: " + intraSimilarity1);
			System.out.println("Intra similarity 2: " + intraSimilarity2);
			System.out.println("");
			double r = (intraSimilarity1 + intraSimilarity2) / interSimilarity;
			sumR += r;
			countR++;
			if (r > maxR) {
			    maxR = r;
			}
		    }
		    double avgR = sumR / countR;
//		sum += maxR;
		    sum += avgR;
		}
		_daviesBouldin[i] = sum / partitionLoader[i].getPartition().size();
	    }
	}
	System.out.println("DBI calculated");

	// output results to console
	for (int i = 0; i < attributes.length; i++) {
	    System.out.println("--------------------------" + attributes[i].getTitle() + "-----------------------");
	    System.out.println("Internal evaluation: ");
	    System.out.println(" IntraEdgesRatio: " + _intraEdgesRatio[i].getRatio());
	    if (computeDBI) {
		System.out.println(" Davies-Bouldin index: " + _daviesBouldin[i]);
	    }
	    if (skipExternal) {
		continue;
	    }
	    System.out.println("");
	    System.out.println("External evaluation: ");
	    System.out.println(" NMI: " + _nmi[i].calculate());
	    System.out.println(" Rand index: " + _randIndex[i].calculate());
	    System.out.println(" F measure: " + _fMeasure[i].calculate(2.0d));
	}

	graph.readUnlockAll();
	time = System.currentTimeMillis() - starttime;
	System.out.println("EVALUATION: finished... " + (time / 1000) + "sec");
	Progress.finish(progressTicket);

//                        algorithm = new DijkstraShortestPathAlgorithm(gc.getModel().getGraphVisible(), sourceNode);
    }

    @Override
    public String getReport() {
	System.out.println("EVALUATION: generating report... ");

	//Distribution series
	DefaultCategoryDataset datasetExternal = new DefaultCategoryDataset();
	DefaultCategoryDataset datasetInternal = new DefaultCategoryDataset();
	String evaluatedClusterings = "";
	for (int i = 0; i < attributes.length; i++) {
	    datasetInternal.addValue(_intraEdgesRatio[i].getRatio(), "intra/inter", attributes[i].getTitle());
	    evaluatedClusterings += attributes[i].getTitle() + "<br />";
	    if (skipExternal) {
		continue;
	    }
	    datasetExternal.addValue(_nmi[i].calculate(), "NMI", attributes[i].getTitle());
	    datasetExternal.addValue(_randIndex[i].calculate(), "Rand index", attributes[i].getTitle());
	    datasetExternal.addValue(_fMeasure[i].calculate(2.0d), "F measure", attributes[i].getTitle());
	}

	NumberFormat f = new DecimalFormat("#0.000");
	String externalTable = "<table style=\"margin-left: 40px;\" border=\"1\">";
	externalTable += "<tr><th>Algorithm</th><th width=\"50\">NMI</th><th width=\"50\">Rand index</th><th width=\"50\">F measure</th></tr>";
	if (!skipExternal) {
	    for (int i = 0; i < attributes.length; i++) {
		externalTable += "<tr><th>" + attributes[i].getTitle() + "</th><td>" + f.format(_nmi[i].calculate()) + "</td><td>" + f.format(_randIndex[i].calculate()) + "</td><td>" + f.format(_fMeasure[i].calculate(2.0d)) + "</td></tr>";
	    }
	}

	externalTable += "</table>";


	String internalTable = "<table style=\"margin-left: 40px;\" border=\"1\">";
	internalTable += "<tr><th>Algorithm</th><th width=\"50\">intra/inter</th>" + ((computeDBI) ? "<th width=\"50\">DBI</th>" : "") + "</tr>";
	for (int i = 0; i < attributes.length; i++) {
	    internalTable += "<tr><th>" + attributes[i].getTitle() + "</th><td>" + f.format(_intraEdgesRatio[i].getRatio()) + "</td>" + ((computeDBI) ? ("<td>" + f.format(_daviesBouldin[i]) + "</td>") : "") + "</tr>";
	}

	internalTable += "</table>";

	JFreeChart chartExternalEvaluation = ChartFactory.createBarChart(
		"",
		"Clustering",
		"Score",
		datasetExternal,
		PlotOrientation.VERTICAL,
		true,
		true,
		false);
	ChartUtils.scaleChart(chartExternalEvaluation.getCategoryPlot());
	String graphExternalEvaluation = ChartUtils.renderChart(chartExternalEvaluation, "clustering_evaluation_external.png");

	JFreeChart chartInternalEvaluation = ChartFactory.createBarChart(
		"",
		"Clustering",
		"Score",
		datasetInternal,
		PlotOrientation.VERTICAL,
		true,
		true,
		false);
	ChartUtils.scaleChart(chartInternalEvaluation.getCategoryPlot());
	String graphInternalEvaluation = ChartUtils.renderChart(chartInternalEvaluation, "clustering_evaluation_internal.png");
	return "<HTML> <BODY>"
		+ "<div style=\"padding: 10px;\">"
		+ "<h1>Clustering evaluation</h1> "
		+ "<hr>"
		+ "<h3>Evaluated algorithms:</h3>"
		+ evaluatedClusterings
		+ "<br />"
		+ ((!skipExternal) ? ("<h3>External evaluation results:</h3>"
		+ "<br /><b>Ground truth clustering (gold standard):</b>"
		+ "<br />" + groundTruth.getTitle()
		+ "<br />"
		+ "<br /><b>Computed measures:</b>"
		+ "<br />Normalized Mutual Information (NMI)"
		+ "<br />Rand index"
		+ "<br />F measure"
		+ "<br />"
		+ "<br />"
		+ externalTable
		+ "<br />"
		+ "<br />"
		+ graphExternalEvaluation) : "")
		+ "<h3>Internal evaluation results:</h3>"
		+ "<br /><b>Computed measures:</b>"
		+ "<br />Ratio intra community edges / inter community edges (intra/inter)"
		+ ((computeDBI) ? "<br />Davies-Bouldin index (DBI)" : "")
		+ "<br />"
		+ "<br />"
		+ internalTable
		+ "<br />"
		+ "<br />"
		+ graphInternalEvaluation
		+ "<br />"
		+ "</div>"
		+ "</BODY> </HTML>";
    }

    @Override
    public boolean cancel() {
	this.cancelled = true;
	System.out.println("EVALUATION: cancelled...");
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
	this.progressTicket = pt;
    }

    public void setAttributesToEvaluate(Column groundTruth, Column[] clusterings) {
	this.groundTruth = groundTruth;
	this.attributes = clusterings;
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

    public double averagePathLengthBetweenMembers(Community c) {
	if (c.size() <= 1) {
	    return 1;
	}
	String[] mem = c.getMembers().toArray(new String[0]);
	Double tmp;
	double pathSum = 0;
	int count = 0;
	for (int i = 0; i < mem.length; i++) {
	    for (int j = i + 1; j < mem.length; j++) {
		if (mem[i].equals(mem[j])) {
		    continue;
		}

		Node iNode = graph.getNode(mem[i]);
		Node jNode = graph.getNode(mem[j]);
		tmp = distances.get(new NodePair((String)iNode.getId(), (String)jNode.getId()));
//		System.out.println("distance between: " + iNode.getLabel() + ", " + jNode.getLabel() + " is: " + tmp);
		if (tmp == Double.POSITIVE_INFINITY || tmp == null) {
		    System.out.println("PATH DOES NOT EXIST!");
		    return Double.POSITIVE_INFINITY;
		}
		pathSum += tmp;
		count++;
	    }
	}
	return pathSum / count;
    }

    public double averagePathLengthBetweenCommunities(Community c1, Community c2) {

	String[] mem = c1.getMembers().toArray(new String[0]);
	String[] oth = c2.getMembers().toArray(new String[0]);
	Double tmp;
	double pathSum = 0;
	int count = 0;
	for (int i = 0; i < mem.length; i++) {
	    for (int j = 0; j < oth.length; j++) {
		Node iNode = graph.getNode(mem[i]);
		Node jNode = graph.getNode(oth[j]);
		tmp = distances.get(new NodePair((String)iNode.getId(), (String)jNode.getId()));
//		System.out.println("distance between: " + iNode.getLabel() + ", " + jNode.getLabel() + " is: " + tmp);
		if (tmp == Double.POSITIVE_INFINITY || tmp == null) {
		    System.out.println("PATH DOES NOT EXIST!");
		    return Double.POSITIVE_INFINITY;
		}
		pathSum += tmp;
		count++;
	    }
	}

	return pathSum / count;
    }

    public void setComputeDBI(boolean computeDBI) {
	this.computeDBI = computeDBI;
    }

    public boolean getComputeDBI() {
	return computeDBI;
    }
}
