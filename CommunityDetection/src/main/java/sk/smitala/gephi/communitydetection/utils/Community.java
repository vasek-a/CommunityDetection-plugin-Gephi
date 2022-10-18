/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.utils;

import org.gephi.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.gephi.graph.api.Graph;

import java.util.HashSet;
import java.util.Set;

/**
 * @author smitalm
 */
public class Community {

    final String id;
    HashSet<String> members = new HashSet<>();

    public Community(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Set<String> getMembers() {
        return members;
    }

    public Community(String id, String firstMember) {
        this.id = id;
        members.add(firstMember);
    }

    public int size() {
        return members.size();
    }

    public void addMember(String member) {
        members.add(member);
    }

    public boolean hasMember(String member) {
        return members.contains(member);
    }

    public int intersectionSize(Community c) {
        int intersectionSize = 0;
        for (String m : members) {
            if (c.hasMember(m)) {
                intersectionSize++;
            }
        }
        return intersectionSize;
    }

    public double averagePathLengthBetweenMembers(Graph graph) {
        String[] mem = members.toArray(new String[0]);
        double tmp;
        double pathSum = 0;
        int count = 0;
        DijkstraShortestPathAlgorithm algo;
        for (int i = 0; i < mem.length; i++) {
            algo = new DijkstraShortestPathAlgorithm(graph, graph.getNode(mem[i]));
            algo.compute();
            for (int j = i + 1; j < mem.length; j++) {
                tmp = algo.getDistances().get(graph.getNode(mem[j]));
                if (tmp == Double.POSITIVE_INFINITY) {
                    System.out.println("PATH DOES NOT EXIST!");
                    return Double.POSITIVE_INFINITY;
                }
                pathSum += tmp;
                count++;
            }
        }

        return pathSum / count;
    }

    public double averagePathLengthBetweenCommunities(Graph graph, Community other) {

        String[] mem = members.toArray(new String[0]);
        String[] oth = other.members.toArray(new String[0]);
        double tmp;
        double pathSum = 0;
        int count = 0;
        DijkstraShortestPathAlgorithm algo;
        for (int i = 0; i < mem.length; i++) {
            algo = new DijkstraShortestPathAlgorithm(graph, graph.getNode(mem[i]));
            algo.compute();
            for (int j = 0; j < oth.length; j++) {
                tmp = algo.getDistances().get(graph.getNode(oth[j]));
                if (tmp == Double.POSITIVE_INFINITY) {
                    return Double.POSITIVE_INFINITY;
                }
                pathSum += tmp;
                count++;
            }
        }

        return pathSum / count;
    }
}
