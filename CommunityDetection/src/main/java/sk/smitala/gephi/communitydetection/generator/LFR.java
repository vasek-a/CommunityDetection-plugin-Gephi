/*
 Copyright 2008-2010 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2011 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package sk.smitala.gephi.communitydetection.generator;

import com.google.common.collect.TreeMultiset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import nl.peterbloem.powerlaws.Discrete;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.gephi.graph.api.Column;
//import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.graph.api.Origin;
//import org.gephi.data.attributes.api.AttributeType;
import org.gephi.io.generator.spi.Generator;
import org.gephi.io.generator.spi.GeneratorUI;
import org.gephi.io.importer.api.*;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = Generator.class)
public class LFR implements Generator, LongTask {

    // params
    protected int numberOfNodes = 1000;
    protected int averageDegree = 20;
    protected int minimumDegree = 3;
    protected double nodeDegreeExponent = 2;
    protected double communitySizeExponent = 2;
    protected double mixingParameter = 0.2;
    // Progress 
    protected ProgressTicket progress;
    protected boolean cancel = false;
    // 
    private Community[] community;
    private int[] nodeCommunities;
    private LinkedList<EdgeDraft> edges;
    private NodeDraft[] nodes;

    protected int[] generateDegrees() {

	RandomDataGenerator r = new RandomDataGenerator();
	TreeMultiset<Integer> degrees = TreeMultiset.create();
	long starttime = System.currentTimeMillis();

//	int[] buffer = new int[numberOfNodes * 10];
//	for (int i = 0; i < buffer.length; i++) {
//	    buffer[i] = r.nextZipf(numberOfNodes - 1, nodeDegreeExponent);
//	}
	Integer[] buffer = new Discrete(minimumDegree, nodeDegreeExponent).generate(numberOfNodes * 10).toArray(new Integer[numberOfNodes * 10]);
	long time = System.currentTimeMillis() - starttime;
	starttime = System.currentTimeMillis();
	System.out.println("Power-law buffer generated in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");
	Arrays.sort(buffer);
	System.out.println("BUFFER: " + buffer);
	time = System.currentTimeMillis() - starttime;
	starttime = System.currentTimeMillis();
	System.out.println("Power-law buffer sorted in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");

	double sum = 0;
	for (int i = buffer.length - 1; i >= 0; i--) {
	    degrees.add(buffer[i]);
	    sum += buffer[i];
	    if (degrees.size() == numberOfNodes) {
		break;
	    }
	}
	time = System.currentTimeMillis() - starttime;
	starttime = System.currentTimeMillis();
	System.out.println("Degrees picked in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");

	double avg = sum / numberOfNodes;
	double avgDelta;
	double max;
	for (int i = buffer.length - numberOfNodes - 1; i >= 0; i--) {
	    max = degrees.lastEntry().getElement();
	    avgDelta = (buffer[i] - max) / numberOfNodes;
	    if ((avg + avgDelta) > averageDegree) {
		degrees.remove(degrees.lastEntry().getElement());
		degrees.add(buffer[i]);
		avg += avgDelta;
	    } else {
		break;
	    }
	}
	time = System.currentTimeMillis() - starttime;
	System.out.println("Degrees modified in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");
	int[] finalDegrees = new int[degrees.size()];
	int i = 0;
	for (int a : degrees) {
	    finalDegrees[i++] = a;
	}
	return finalDegrees;
    }

    protected int[] generateSizes(int minMin, int minMax) {
	System.out.println("Generating sizes: " + minMin + "," + minMax);
	RandomDataGenerator r = new RandomDataGenerator();
	LinkedList<Integer> sizes = new LinkedList<Integer>();
	int tmp;
	int total = numberOfNodes;
	boolean done = false;
	while (!done) {
	    tmp = r.nextZipf(total, communitySizeExponent);
	    if (tmp <= minMin) {
		continue;
	    }
	    if (tmp > total) {
		continue;
	    }
	    total -= tmp;
	    if (total <= minMin) {
		sizes.add(tmp + total);
		done = true;
	    } else {
		sizes.add(tmp);
	    }
	}
	int[] sizesArray = new int[sizes.size()];
	for (int i = 0; i < sizesArray.length; i++) {
	    sizesArray[i] = sizes.get(i);
	}
	return sizesArray;
    }

    protected void generateCommunities(int[] sizes) {
	community = new Community[sizes.length];
	for (int i = 0; i < community.length; i++) {
	    community[i] = new Community(i, sizes[i]);
	}
	nodeCommunities = new int[numberOfNodes];
	for (int i = 0; i < nodeCommunities.length; i++) {
	    nodeCommunities[i] = -1;
	}
	Random r = new Random();
	boolean done = false;
	while (!done) {
	    done = true;
	    for (int i = 0; i < numberOfNodes; i++) {
		if (nodeCommunities[i] == -1) {
		    done = false;
		    int c = r.nextInt(community.length);
		    if (community[c].isComplete()) {
			nodeCommunities[community[c].removeRandomMember()] = -1;
		    }
		    community[c].addMember(i);
		    nodeCommunities[i] = c;
		}
	    }
	}
    }

    protected void generateOutsideEdges(int[] degrees, ContainerLoader container) {

	Quicksort qs = new Quicksort();
	int[] nodeIndex = new int[nodes.length];
	for (int i = 0; i < nodeIndex.length; i++) {
	    nodeIndex[i] = i;
	}


	int[] backup;
	int max;
	boolean done = false;
	while (!done) {

	    qs.sort(degrees, nodeIndex);
	    max = degrees.length - 1;
	    int d = 1;
	    int localIndex = max - 1;
	    while (d <= degrees[max] && localIndex >= 0) {
		if (community[nodeCommunities[nodeIndex[max]]].equals(community[nodeCommunities[nodeIndex[localIndex]]])) {
		    localIndex--;
		    continue;
		}
		EdgeDraft e = container.factory().newEdgeDraft();
		e.setSource(nodes[nodeIndex[max]]);
		e.setTarget(nodes[nodeIndex[localIndex]]);
		e.setDirection(EdgeDirection.UNDIRECTED);
//		e.setType(EdgeDraft.EdgeType.UNDIRECTED);
		edges.add(e);
		degrees[localIndex]--;
		d++;
		localIndex--;
	    }

	    backup = new int[degrees.length - 1];
	    System.arraycopy(degrees, 0, backup, 0, backup.length);
	    degrees = backup;
	    done = true;
	    for (int i = 0; i < degrees.length; i++) {
		if (degrees[i] > 0) {
		    done = false;
		}
	    }
	}
    }

    protected void generateInsideEdges(NodeDraft[] nodes, int[] degrees, ContainerLoader container) {

	Quicksort qs = new Quicksort();
	int[] nodeIndex = new int[nodes.length];
	for (int i = 0; i < nodeIndex.length; i++) {
	    nodeIndex[i] = i;
	}
	int[] backup;
	int max;
	boolean done = false;
	while (!done) {

	    qs.sort(degrees, nodeIndex);
	    max = degrees.length - 1;
	    for (int i = max - 1; i >= (((max - 1 - degrees[max]) >= 0) ? (max - 1 - degrees[max]) : 0); i--) {
		EdgeDraft e = container.factory().newEdgeDraft();
		e.setSource(nodes[nodeIndex[max]]);
		e.setTarget(nodes[nodeIndex[i]]);
		e.setDirection(EdgeDirection.UNDIRECTED);
//		e.setType(EdgeDraft.EdgeType.UNDIRECTED);
		edges.add(e);
		degrees[i]--;
	    }

	    backup = new int[degrees.length - 1];
	    System.arraycopy(degrees, 0, backup, 0, backup.length);
	    degrees = backup;
	    done = true;
	    for (int i = 0; i < degrees.length; i++) {
		if (degrees[i] > 0) {
		    done = false;
		}
	    }
	}
    }

    private void printArray(int[] arr) {
	System.out.println("Array length " + arr.length);
	for (int i = 0; i < arr.length; i++) {
	    System.out.print(arr[i] + ", ");
	}
	System.out.println("");
    }

    //Find maximum (largest) value in array using loop
    public static int getMaxValue(int[] numbers) {
	int maxValue = numbers[0];
	for (int i = 1; i < numbers.length; i++) {
	    if (numbers[i] > maxValue) {
		maxValue = numbers[i];
	    }
	}
	return maxValue;
    }

    //Find minimum (lowest) value in array using loop
    public static int getMinValue(int[] numbers) {
	int minValue = numbers[0];
	for (int i = 1; i < numbers.length; i++) {
	    if (numbers[i] < minValue) {
		minValue = numbers[i];
	    }
	}
	return minValue;
    }

    @Override
    public void generate(ContainerLoader container) {
	System.out.println("LFR: starting...");
	long starttime = System.currentTimeMillis();
	long time;
	Progress.start(progress);
	nodes = new NodeDraft[numberOfNodes];
	edges = new LinkedList<EdgeDraft>();
	int[] degrees = generateDegrees();
	time = System.currentTimeMillis() - starttime;
	System.out.println("Degrees generated in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");
	time = System.currentTimeMillis();
	int minDegree = getMinValue(degrees);
	int maxDegree = getMaxValue(degrees);
	int[] degreesInternal = new int[numberOfNodes];
	int[] degreesExternal = new int[numberOfNodes];
	int[] sizes = generateSizes(minDegree, maxDegree);
	time = System.currentTimeMillis() - time;
	System.out.println("Community sizes generated in " + (time / 1000) + "," + (int) (time % 1000d) + "sec");
	System.out.println("Number of communities: " + sizes.length);
	//
	for (int i = 0; i < numberOfNodes; i++) {
	    nodes[i] = container.factory().newNodeDraft();
	    container.addNode(nodes[i]);
	}
	for (int i = 0; i < numberOfNodes; i++) {
	    degreesInternal[i] = (int) Math.round((1 - mixingParameter) * degrees[i]);
	}
	for (int i = 0; i < numberOfNodes; i++) {
	    degreesExternal[i] = degrees[i] - degreesInternal[i];
	}
	//
	generateCommunities(sizes);
//	Column col = container.getAttributeModel().getNodeTable().addColumn("LFR community", AttributeType.INT, AttributeOrigin.DATA);
	ColumnDraft col = container.addNodeColumn("LFR community", Integer.class, false);
	for (int i = 0; i < nodeCommunities.length; i++) {
	    nodes[i].setValue("LFR community", nodeCommunities[i]);
	}
	System.out.println("Communities generated...");
	for (int i = 0; i < community.length; i++) {
	    generateInsideEdges(community[i].getNodeArray(), community[i].getDegreeArray(degreesInternal), container);

	}
	generateOutsideEdges(degreesExternal, container);
	System.out.println("Edges generated...");

	for (EdgeDraft edgeDraft : edges) {
	    container.addEdge(edgeDraft);
	}

	Progress.finish(progress);
	progress = null;
	time = System.currentTimeMillis() - starttime;
	System.out.println("LFR: finished in " + (time / 1000) + "sec");
	this.community = null;
	this.edges = null;
	this.nodeCommunities = null;
	this.nodes = null;
    }

    @Override
    public String getName() {
	return "LFR";
    }

    @Override
    public GeneratorUI getUI() {
	return new LFRUI();
    }

    public void setNumberOfNodes(int numberOfNodes) {
	this.numberOfNodes = numberOfNodes;
    }

    public int getNumberOfNodes() {
	return numberOfNodes;
    }

    public void setMixingParameter(double mixingParameter) {
	this.mixingParameter = mixingParameter;
    }

    public double getMixingParameter() {
	return mixingParameter;
    }

    public void setAverageDegree(int averageDegree) {
	this.averageDegree = averageDegree;
    }

    public int getAverageDegree() {
	return averageDegree;
    }

    public void setNodeDegreeExponent(double nodeDegreeExponent) {
	this.nodeDegreeExponent = nodeDegreeExponent;
    }

    public double getNodeDegreeExponent() {
	return nodeDegreeExponent;
    }

    public void setCommunitySizeExponent(double communitySizeExponent) {
	this.communitySizeExponent = communitySizeExponent;
    }

    public double getCommunitySizeExponent() {
	return communitySizeExponent;
    }

    @Override
    public boolean cancel() {
	cancel = true;
	return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
	this.progress = progressTicket;
    }

    public int getMinimumDegree() {
	return minimumDegree;
    }

    public void setMinimumDegree(int minimumDegree) {
	this.minimumDegree = minimumDegree;
    }

    public class Quicksort {

	private int[] nodes;
	private int[] numbers;
	private int number;

	public void sort(int[] values, int[] nodes) {
	    // Check for empty or null array
	    if (values == null || values.length == 0) {
		return;
	    }
	    this.numbers = values;
	    this.nodes = nodes;
	    number = values.length;
	    quicksort(0, number - 1);
	}

	private void quicksort(int low, int high) {
	    int i = low, j = high;
	    // Get the pivot element from the middle of the list
	    int pivot = numbers[low + (high - low) / 2];

	    // Divide into two lists
	    while (i <= j) {
		// If the current value from the left list is smaller then the pivot
		// element then get the next element from the left list
		while (numbers[i] < pivot) {
		    i++;
		}
		// If the current value from the right list is larger then the pivot
		// element then get the next element from the right list
		while (numbers[j] > pivot) {
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
	    int temp = numbers[i];
	    numbers[i] = numbers[j];
	    numbers[j] = temp;

	    if (nodes == null) {
		return;
	    }

	    int temp2 = nodes[i];
	    nodes[i] = nodes[j];
	    nodes[j] = temp2;
	}
    }

    public class Community {

	private int id;
	final int maxSize;
	ArrayList<Integer> members = new ArrayList<Integer>();

	public Community(int id, int maxSize) {
	    this.id = id;
	    this.maxSize = maxSize;
	}

	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 89 * hash + this.id;
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
	    final Community other = (Community) obj;
	    if (this.id != other.id) {
		return false;
	    }
	    return true;
	}

	public int size() {
	    return members.size();
	}

	public boolean isComplete() {
	    return members.size() >= maxSize;
	}

	public boolean addMember(Integer member) {
	    if (members.size() < maxSize) {
		if (members.contains(member)) {
		    return false;
		} else {
		    members.add(member);
		    return true;
		}
	    }
	    return false;
	}

	public Integer removeRandomMember() {
	    Random r = new Random();
	    return members.remove(r.nextInt(members.size()));
	}

	public NodeDraft[] getNodeArray() {
	    NodeDraft[] nodeArray = new NodeDraft[members.size()];
	    for (int i = 0; i < nodeArray.length; i++) {
		nodeArray[i] = nodes[members.get(i)];
	    }
	    return nodeArray;
	}

	public int[] getRealIndexArray() {
	    int[] indexArray = new int[members.size()];
	    for (int i = 0; i < indexArray.length; i++) {
		indexArray[i] = members.get(i);
	    }
	    return indexArray;
	}

	public int[] getDegreeArray(int[] degrees) {
	    int[] degreeArray = new int[members.size()];
	    for (int i = 0; i < degreeArray.length; i++) {
		degreeArray[i] = degrees[members.get(i)];
	    }
	    return degreeArray;
	}

	public boolean hasMember(Integer member) {
	    return members.contains(member);
	}

	public int intersectionSize(Community c) {
	    int intersectionSize = 0;
	    for (Integer m : members) {
		if (c.hasMember(m)) {
		    intersectionSize++;
		}
	    }
	    return intersectionSize;
	}
    }
}
