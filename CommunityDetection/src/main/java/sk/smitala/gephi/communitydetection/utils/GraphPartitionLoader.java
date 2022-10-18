/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.utils;

import java.util.HashMap;
import java.util.Map;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Node;
import sk.smitala.gephi.communitydetection.utils.traverse.TraverseListener;

/**
 *
 * @author smitalm
 */
public class GraphPartitionLoader implements TraverseListener<Node> {

    Column attribute;
    HashMap<String, Community> communities = new HashMap<>();

    public GraphPartitionLoader(Column attribute) {
	this.attribute = attribute;
    }

    @Override
    public void onElementVisited(Node node) {
	Object obj = node.getAttribute(attribute);
	String value = obj + "";

	if (communities.containsKey(value)) {
	    communities.get(value).addMember((String) node.getId());
	} else {
	    communities.put(value, new Community(value, (String)node.getId()));
	}
    }

    public Map<String, Community> getPartition() {
	return communities;
    }
}
