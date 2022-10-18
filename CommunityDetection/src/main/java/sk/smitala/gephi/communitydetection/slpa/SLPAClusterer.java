/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.slpa;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
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
public class SLPAClusterer implements Statistics, LongTask {

    public static final String COMMUNITY_ID = "SLPA community";
    public static final String COMMUNITY_ID_SECONDARY = "SLPA secondary";
    public static int ITERATIONS_DEFAULT = 20;
    public static double THRESHOLD_DEFAULT = 0.1;
    protected int iterations = ITERATIONS_DEFAULT;
    protected double threshold = THRESHOLD_DEFAULT;
    // algorithm fields
    protected Graph graph;
    protected int NODE_COUNT;
    protected int EDGE_COUNT;
    // long task api fields
    protected boolean cancelled;
    protected ProgressTicket progressTicket;
    // node memory
    protected HashMap<String, HashMultiset<String>> MEMORY;
    protected HashMultiset<String> COMMUNITIES;

    @Override
    public boolean cancel() {
	this.cancelled = true;
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
	this.progressTicket = pt;
    }

    protected String listenerRule(Node listener, Multiset<String> labels) {
	String label = null;
	int max = 0;
	for (Entry<String> entry : labels.entrySet()) {
	    if (entry.getCount() > max) {
		max = entry.getCount();
		label = entry.getElement();
	    }
	}
	return label;
    }

    protected String speakerRule(Node speaker) {
	// put memory to array
	String[] speakerMemory = MEMORY.get((String)speaker.getId()).toArray(new String[MEMORY.get((String)speaker.getId()).size()]);
	Random random = new Random();
	// selecting random label from array ensures probability proportional to number of occurences
	return speakerMemory[random.nextInt(speakerMemory.length)];
    }

    protected void removeLabels(Node node) {
	Multiset<String> nodeMemory = MEMORY.get((String)node.getId());
	int size = nodeMemory.size();
	ArrayList<String> markedForRemoval = new ArrayList<String>();
	SortedMap<Integer, String> sorted = new TreeMap<Integer, String>();

	for (Entry<String> entry : nodeMemory.entrySet()) {
	    sorted.put(entry.getCount(), entry.getElement());
	}

	for (Map.Entry<Integer, String> entry : sorted.entrySet()) {
	    double probability = ((double) entry.getKey()) / ((double) size);
	    if (probability < threshold) {
		markedForRemoval.add(entry.getValue());
		size -= entry.getKey();
	    }
	}

	for (String marked : markedForRemoval) {
	    nodeMemory.setCount(marked, 0);
	}
    }

    protected void setNodeAttribute(Node node) {
	Multiset<String> nodeMemory = MEMORY.get((String)node.getId());
	int max = 0;
	int secondMax = 0;
	String maxLabel = "";
	String secondMaxLabel = "";
	for (Entry<String> entry : nodeMemory.entrySet()) {
	    if (entry.getCount() > max) {
		secondMax = max;
		secondMaxLabel = maxLabel;
		max = entry.getCount();
		maxLabel = entry.getElement();
	    } else if (entry.getCount() > secondMax) {
		secondMax = entry.getCount();
		secondMaxLabel = entry.getElement();
	    }
	}
	node.setAttribute(COMMUNITY_ID, maxLabel);
	if (secondMax > 0) {
	    node.setAttribute(COMMUNITY_ID_SECONDARY, secondMaxLabel);
	}
	COMMUNITIES.add(maxLabel);
    }

    @Override
    public void execute(GraphModel gm) {
	// graph
	graph = gm.getGraphVisible();
	graph.readLock();
//	if (!am.getNodeTable().hasColumn(COMMUNITY_ID)) {
//		am.getNodeTable().addColumn(COMMUNITY_ID, COMMUNITY_ID, AttributeType.STRING, AttributeOrigin.COMPUTED, "");
//	}
	// add column to datamodel
	if (!gm.getNodeTable().hasColumn(COMMUNITY_ID)) {
	    gm.getNodeTable().addColumn(COMMUNITY_ID, String.class);
	}
	if (!gm.getNodeTable().hasColumn(COMMUNITY_ID_SECONDARY)) {
	    gm.getNodeTable().addColumn(COMMUNITY_ID_SECONDARY, String.class);
	}

	EDGE_COUNT = graph.getEdgeCount();
	NODE_COUNT = graph.getNodeCount();

	Progress.start(progressTicket, NODE_COUNT + iterations * NODE_COUNT + NODE_COUNT);

	System.out.println("Started SLPA clustering with params:");
	System.out.println("Iterations = " + iterations);
	System.out.println("Threshold = " + threshold);
	System.out.println("");
	System.out.println("STAGE 1 : initialization...");
	// init memory of all nodes to unique label
	MEMORY = new HashMap<String, HashMultiset<String>>(NODE_COUNT);
	for (Node node : graph.getNodes()) {
	    HashMultiset<String> memory = HashMultiset.create();
	    memory.add((String)node.getId());
	    MEMORY.put((String)node.getId(), memory);
	    Progress.progress(progressTicket);
	}

	System.out.println("STAGE 2 : evolution...");
	List<Node> nodes = Arrays.asList(graph.getNodes().toArray());
	for (int i = 0; i < iterations; i++) {
	    Collections.shuffle(nodes);
	    for (Node listener : nodes) {
		Multiset<String> labels = HashMultiset.create();
		for (Node speaker : graph.getNeighbors(listener)) {
		    labels.add(speakerRule(speaker));
		}
			String label = listenerRule(listener, labels);
		if (label != null) {
		    MEMORY.get((String)listener.getId()).add(label);
		}
		Progress.progress(progressTicket);
	    }
	}

	System.out.println("STAGE 3 : post-processing...");
	for (Node node : graph.getNodes()) {
	    removeLabels(node);
	    Progress.progress(progressTicket);
	}

	System.out.println("SLPA finished...");
	System.out.println("Setting nodes attributes...");
	COMMUNITIES = HashMultiset.create();
	for (Node node : graph.getNodes()) {
	    setNodeAttribute(node);
	}

	System.out.println("Finished clustering...");
	graph.readUnlockAll();
    }

    @Override
    public String getReport() {

	//distribution of values
	Map<String, Integer> plot = new HashMap<String, Integer>();
	for (Entry<String> entry : COMMUNITIES.entrySet()) {
	    plot.put(entry.getElement(), entry.getCount());
	}

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

    public int getIterations() {
	return iterations;
    }

    public void setIterations(int iterations) {
	this.iterations = iterations;
    }

    public double getThreshold() {
	return threshold;
    }

    public void setThreshold(double threshold) {
	this.threshold = threshold;
    }
}
