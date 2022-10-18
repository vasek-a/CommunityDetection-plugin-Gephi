/*
 * Your license here
 */
package sk.smitala.gephi.communitydetection.scan.heuristics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
//import org.gephi.data.attributes.api.AttributeModel;
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
 *
 * See http://wiki.gephi.org/index.php/HowTo_write_a_metric#Create_Statistics
 *
 * @author Your Name <your.name@your.company.com>
 */
public class ScanHeuristics implements Statistics, LongTask {

    // params
    private int k = 2;
    // algorithm
    private Graph graph;
    private HashMap<MyEdge, Double> BUFFER;
    private Double[] kSimilarities;
    // Long Task 
    private boolean cancel = false;
    private ProgressTicket progressTicket;

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

    private double getStructuralSimilarity(Node v, Node w) {

	//check buffer if there is already calculated similarity for these nodes
	Double s = BUFFER.get(new MyEdge((String)v.getId(), (String)w.getId()));
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
	structuralSimilarity = ((double) commonNeighbours) / (Math.sqrt(bothNeighbours));

	//put calculated similarity in buffer to prevent duplicate calculations
	BUFFER.put(new MyEdge((String)v.getId(), (String)w.getId()), new Double(structuralSimilarity));
	return structuralSimilarity;
    }

    private double kDistance(Node n) {

	LinkedList<Double> similarities = new LinkedList<Double>();
	similarities.add(new Double(1d));
	for (Node node : graph.getNeighbors(n)) {
	    similarities.add(new Double(getStructuralSimilarity(n, node)));
	}

	Double[] sorted = similarities.toArray(new Double[similarities.size()]);

	QuickSort qs = new QuickSort();
	qs.sort(sorted);


	int i = sorted.length - 1;
	double result = sorted[i--];
	int count = 1;
	while (i >= 0 && count++ < k) {
	    result = sorted[i--];
	}

	return result;
    }

    @Override
    public void execute(GraphModel graphModel) {
	graph = graphModel.getGraphVisible();
	graph.readLock();

	BUFFER = new HashMap<MyEdge, Double>(graph.getEdgeCount());

	//Your algorithm
	try {
	    Progress.start(progressTicket, graph.getNodeCount());

	    System.out.println("Calculating kSimilarities...");
	    kSimilarities = new Double[graph.getNodeCount()];
	    int i = 0;
	    for (Node n : graph.getNodes()) {
		Progress.progress(progressTicket);
		if (cancel) {
		    break;
		}
		kSimilarities[i++] = kDistance(n);

	    }
	    System.out.println("Sorting nodes...");
	    QuickSort qs = new QuickSort();
	    qs.sort(kSimilarities);

	    System.out.println("Finished ...");

	    graph.readUnlockAll();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	} finally {
	    //Unlock graph
	    graph.readUnlockAll();
	}
    }

    /**
     * -----------------------------------------------------------
     */
    @Override
    public String getReport() {
	//distribution of values
	Map<Integer, Double> dist = new HashMap<Integer, Double>();
	for (int i = 0; i < kSimilarities.length; i++) {
	    Double d = kSimilarities[i];
	    dist.put(i, d);
	}

	//Distribution series
	XYSeries dSeries = ChartUtils.createXYSeries(dist, "Clustering Coefficient");
	XYSeriesCollection dataset = new XYSeriesCollection();
	dataset.addSeries(dSeries);

	JFreeChart chart = ChartFactory.createScatterPlot(
		"k-nearest value sorted",
		"nodes",
		"k-nearest similarity",
		dataset,
		PlotOrientation.VERTICAL,
		true,
		false,
		false);
	chart.removeLegend();
	ChartUtils.decorateChart(chart);
	ChartUtils.scaleChart(chart, dSeries, false);
	String imageFile = ChartUtils.renderChart(chart, "clustering-coefficient.png");


	return "<HTML> <BODY> <h1> Heuristics for determining SCAN parameters report </h1> "
		+ "<hr>"
		+ "<br />" + "<h2> Heuristics params: </h2>"
		+ "k:  " + k + "<br />"
		+ "<br />" + "<h2> Results: </h2>"
		+ "<br />"
		+ imageFile
		+ "<br />" + "Find knee in chart, and set epsilon parameter according to k-nearest value. Use k as mu parameter."
		+ "<br />"
		+ "<br />" + "<h2> Algorithm: </h2>"
		+ "Xiaowei Xu, Nurcan Yuruk, Zhidan Feng, Thomas A. J. Schweiger, "
		+ "<i>SCAN: A Structural Clustering Algorithm for Networks</i>"
		+ ", 2007 <br />"
		+ "</BODY> </HTML>";
    }

    public void setK(int k) {
	this.k = k;
    }

    public int getK() {
	return k;
    }

    @Override
    public boolean cancel() {
	cancel = true;
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
	this.progressTicket = progressTicket;
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
	    if (this.sourceID != other.sourceID) {
		return false;
	    }
	    if (this.targetID != other.targetID) {
		return false;
	    }
	    return true;
	}
    }
}
