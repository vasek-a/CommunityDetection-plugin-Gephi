package sk.smitala.gephi.communitydetection.markovalgo;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.util.*;

public class MarkovAlgoClusterer implements Statistics, LongTask {
    private ProgressTicket progressTicket;
    private boolean cancelled = false;
    private Graph graph;

    private static final String CLUSTER_LABEL = "MarkovAlgo Cluster";

    private static final int N_POW_APPROX = 10_000;
    private int kMeansClusters = 2;

    private RealMatrix qMatrix;
    private RealMatrix rMatrix;
    private RealMatrix nMatrix;
    private RealMatrix bMatrix;

    @Override
    public void execute(GraphModel graphModel) {
        graph = graphModel.getGraphVisible();
        graph.readLock();

        Progress.start(progressTicket, graph.getNodeCount() * 6);

        if (!graphModel.getNodeTable().hasColumn(CLUSTER_LABEL)) {
            graphModel.getNodeTable().addColumn(CLUSTER_LABEL, String.class);
        }

        // Loading matrix Q with graph adjacency matrix.
        // Ignoring isolated nodes. Uniting nodes with identical vector.

        int nodeCount = 0;
        List<Node> nodeList = new ArrayList<>();
        Map<Set<NeighbourConnection>, Node> vectorMap = new HashMap<>();
        Map<Node, Node> unitedNodes = new HashMap<>();

        // Filtering nodes of the graph.
        for (Node node : graph.getNodes()) {
            if (cancelled) break;
            Progress.progress(progressTicket);
            Set<Node> neighbours = graph.getNeighbors(node).toSet();
            // Node is isolated, marking as such and skipping.
            if (neighbours.isEmpty()) {
                assign(node, "Outlier");
                continue;
            }
            // Creating set of neighbour connections to check for node uniqueness.
            Set<NeighbourConnection> vector = new HashSet<>();
            for (Node neighbour : neighbours) {
                double weight = 0;
                // Directed edge
                for (Edge edge : graph.getEdges(node, neighbour)) {
                    weight += edge.getWeight();
                }
                // Undirected edge
                for (Edge edge : graph.getEdges(neighbour, node)) {
                    if (!edge.isDirected()) {
                        weight += edge.getWeight();
                    }
                }
                vector.add(new NeighbourConnection(neighbour, weight));
            }
            // Identical vector was already found, uniting.
            if (vectorMap.containsKey(vector)) {
                unitedNodes.put(node, vectorMap.get(vector));
            } else {
                // Found node with unique vector, noting down.
                vectorMap.put(vector, node);
                nodeList.add(node);
                nodeCount++;
            }
        }

        qMatrix = new Array2DRowRealMatrix(nodeCount, nodeCount);
        Node[] nodes = nodeList.toArray(new Node[0]);

        for (int a = 0; a < nodeCount; a++) {
            for (int b = 0; b < nodeCount; b++) {
                for (Edge e : graph.getEdges(nodes[a], nodes[b])) {
                    qMatrix.setEntry(a, b, e.getWeight() + qMatrix.getEntry(a, b));
                }
            }
        }

        // Making matrix Q stochastic.
        normalize(qMatrix);
        Progress.progress(progressTicket, graph.getNodeCount());

        // Calculating matrix N.
        try {
            nMatrix = MatrixUtils.inverse(
                    MatrixUtils.createRealIdentityMatrix(nodeCount).subtract(qMatrix));
        } catch (SingularMatrixException e) {
            // If inversion fails (nMatrix is singular), result is approximated as
            // sum of Q^k for k in <0; nPowApprox).

            // (This could be optimized to run in logarithmic number of steps.)
            nMatrix = qMatrix.power(0);
            for (int i = 1; i < N_POW_APPROX; i++) {
                if (cancelled) break;
                nMatrix = nMatrix.add(qMatrix.power(i));
            }
        }
        Progress.progress(progressTicket, graph.getNodeCount());

        normalize(nMatrix);
        Progress.progress(progressTicket, graph.getNodeCount());

        kMeans(kMeansClusters);
        Progress.progress(progressTicket, graph.getNodeCount());

        // Updates nodes' cluster assignment in graph based
        // on the result of k-means.

        // This step can be safely removed (as it has only visual effect).
        for (int n = 0; n < nodeCount; n++) {
            double absChance = 0;
            for (int c = 0; c < rMatrix.getColumnDimension(); c++) {
                if (rMatrix.getEntry(n, c) > absChance) {
                    absChance = rMatrix.getEntry(n, c);
                    assign(nodes[n], c);
                }
            }
        }

        // Expectation-maximization
        double lastEval = -1;
        double currEval = 0;

        while (lastEval < currEval) {
            if (cancelled) break;
            lastEval = currEval;

            bMatrix = nMatrix.multiply(rMatrix);
            RealMatrix vMatrix = bMatrix.transpose();

            normalize(vMatrix);

            RealMatrix vnMatrix = vMatrix.multiply(nMatrix);

            for (int j = 0; j < nodeCount; j++) {
                // Searching for argmax of row j in transposed vnMatrix.
                int max = -1;
                for (int i = 0; i < vnMatrix.getRowDimension(); i++) {
                    if (max == -1 || vnMatrix.getEntry(i, j) > vnMatrix.getEntry(max, j)) {
                        max = i;
                    }
                }

                // Updating rMatrix - maximization step.
                for (int i = 0; i < rMatrix.getColumnDimension(); i++) {
                    rMatrix.setEntry(j, i, 0);
                }
                rMatrix.setEntry(j, max, 1);
                assign(nodes[j], max);
            }

            currEval = vMatrix.multiply(bMatrix).getTrace();
        }
        Progress.progress(progressTicket, graph.getNodeCount());

        // Assigning nodes to calculated clusters.

        // Modifying for fuzzy clustering should read other columns of
        // row vector in bMatrix. Currently, only the largest value is used.
        for (int n = 0; n < nodeCount; n++) {
            int max = 0;
            for (int c = 0; c < rMatrix.getColumnDimension(); c++) {
                if (bMatrix.getEntry(n, c) > bMatrix.getEntry(n, max)) {
                    max = c;
                }
            }
            assign(nodes[n], max);
        }

        // Assigning united nodes to the same cluster.
        for (var entry : unitedNodes.entrySet()) {
            assignSame(entry.getKey(), entry.getValue());
        }

        if (cancelled) {
            graph.removeAttribute(CLUSTER_LABEL);
        }

        graph.readUnlockAll();
    }

    private void kMeans(int clusters) {
        if (clusters < 1) throw new IllegalArgumentException("Number of expected clusters can't be less than 1.");

        Random rand = new Random();
        // k-means++ initialization
        int[] seeds = new int[clusters];
        seeds[0] = rand.nextInt(clusters);

        double[] distances = new double[nMatrix.getRowDimension()];
        double distSum;

        for (int nextClCen = 1; nextClCen < clusters; nextClCen++) {
            if (cancelled) break;
            distSum = 0;
            for (int node = 0; node < nMatrix.getRowDimension(); node++) {
                double dist = Double.MAX_VALUE;
                for (int seed = 0; seed < nextClCen; seed++) {
                    double d = kMeansDistance(seeds[seed], node);
                    if (d < dist) {
                        dist = d;
                    }
                }
                distances[node] = dist * dist;
                distSum += dist * dist;
            }
            // Lottery for next cluster seed, each node has chance
            // proportional to its distance from cluster seed squared.
            double choice = rand.nextDouble() * distSum;
            double oldChoice = choice;
            int i = 0;
            while (distances[i] == 0 || choice > distances[i]) {
                if (cancelled) break;
                choice -= distances[i];
                i++;
                if (i >= distances.length) {
                    i = 0;
                    // If there is less than not enough different nodes,
                    // selecting first node as cluster center on second
                    // inactive pass through choice loop.
                    if (oldChoice == choice){
                        break;
                    }
                    oldChoice = choice;
                }
            }
            seeds[nextClCen] = i;
        }

        // Initializing cluster centers.
        double[][] clusterCenters = new double[clusters][nMatrix.getRowDimension()];
        for (int i = 0; i < clusters; i++) {
            for (int j = 0; j < nMatrix.getColumnDimension(); j++) {
                clusterCenters[i][j] = nMatrix.getEntry(seeds[i], j);
            }
        }

        // Initializing node - cluster assignment.
        int[] nodeClusters = new int[nMatrix.getRowDimension()];

        // Repeating reassignment of every node to the closest cluster's center
        // and recalculating new cluster center.
        int changes = -1;
        while (changes != 0) {
            if (cancelled) break;
            changes = 0;
            // Reassignment
            for (int node = 0; node < nMatrix.getRowDimension(); node++) {
                int closest = nodeClusters[node];
                double clDist = kMeansDistance(nMatrix.getRow(node), clusterCenters[closest]);
                for (int clCent = 0; clCent < clusters; clCent++) {
                    double d = kMeansDistance(nMatrix.getRow(node), clusterCenters[clCent]);
                    if (d < clDist) {
                        clDist = d;
                        closest = clCent;
                    }
                }
                if (closest != nodeClusters[node]) {
                    changes++;
                    nodeClusters[node] = closest;
                }
            }

            // Recalculation
            int[] clusterSizes = new int[clusters];
            for (int i = 0; i < clusters; i++) {
                for (int j = 0; j < nMatrix.getColumnDimension(); j++) {
                    clusterCenters[i][j] = 0;
                }
            }

            // Summing all dimensions for nodes to their clusters.
            for (int node = 0; node < nMatrix.getRowDimension(); node++) {
                int cluster = nodeClusters[node];
                clusterSizes[cluster]++;
                for (int dim = 0; dim < nMatrix.getColumnDimension(); dim++) {
                    clusterCenters[cluster][dim] += nMatrix.getEntry(node, dim);
                }
            }

            // Calculating cluster center as position at average for each dimension.
            for (int cl = 0; cl < clusters; cl++) {
                if (clusterSizes[cl] == 0) {
                    continue;
                }
                for (int dim = 0; dim < nMatrix.getColumnDimension(); dim++) {
                    clusterCenters[cl][dim] /= clusterSizes[cl];
                }
            }

        }

        // Creating R matrix based on current cluster membership.
        rMatrix = new Array2DRowRealMatrix(nMatrix.getRowDimension(), clusters);
        for (int node = 0; node < nMatrix.getRowDimension(); node++) {
            rMatrix.setEntry(node, nodeClusters[node], 1);
        }
    }

    private double kMeansDistance(int a, int b) {
        return kMeansDistance(nMatrix.getRow(a), nMatrix.getRow(b));
    }

    private double kMeansDistance(double[] a, double[] b) {
        double dist = 0;
        for (int i = 0; i < a.length; i++) {
            dist += Math.pow(b[i] - a[i], 2);
        }
        dist = Math.pow(dist, 0.5);

        return Math.abs(dist / N_POW_APPROX);
    }

    private void normalize(RealMatrix m) {
        for (int row = 0; row < m.getRowDimension(); row++) {
            if (cancelled) break;
            double rowSum = 0;
            for (int col = 0; col < m.getColumnDimension(); col++) {
                rowSum += m.getEntry(row, col);
            }
            if (rowSum > 0) {
                for (int col = 0; col < m.getColumnDimension(); col++) {
                    m.multiplyEntry(row, col, (1 / rowSum));
                }
            }
        }
    }

    private void assignSame(Node assigned, Node model) {
        assigned.setAttribute(CLUSTER_LABEL, model.getAttribute(CLUSTER_LABEL));
    }

    private void assign(Node n, Object id) {
        n.setAttribute(CLUSTER_LABEL, "Cluster: " + id);
    }

    public void setkMeansClusters(int kMeansN) {
        this.kMeansClusters = kMeansN;
    }

    @Override
    public String getReport() {
//        return "<HTML> <BODY> " +
//                "done" +
//                "</BODY> </HTML>";
        return null;
    }

    @Override
    public boolean cancel() {
        this.cancelled = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    // Record type of class for storing outbound connections
    // as destination Node with weight of the edge.
    private static class NeighbourConnection {
        Node dstNode;
        double weight;

        NeighbourConnection(Node dstNode, double weight) {
            this.dstNode = dstNode;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "--(" + weight + ")->" + dstNode.getId();
        }
    }
}
