/*
 * Your license here
 */
package sk.smitala.gephi.communitydetection.evaluation;

import javax.swing.JPanel;
//import org.gephi.data.attributes.api.AttributeController;
//import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsUI.class)
public class ClusteringEvaluationUI implements StatisticsUI {

    private ClusteringEvaluationPanel panel;
    private ClusteringEvaluation clusterer;

    @Override
    public JPanel getSettingsPanel() {
	panel = new ClusteringEvaluationPanel();
	return panel; //null if no panel exists
    }

    @Override
    public void setup(Statistics stat) {
	this.clusterer = (ClusteringEvaluation) stat;
    GraphModel gm = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
//	AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
	if (panel != null) {
	    panel.setAttributes(gm.getNodeTable().toArray());
	    panel.setDBI(clusterer.getComputeDBI());
	    panel.setSkipExternal(clusterer.isSkipExternal());
	}
    }

    @Override
    public void unsetup() {
	if (panel != null) {
	    clusterer.setAttributesToEvaluate(panel.getGroundTruthAttribute(), panel.getSelectedAttributes());
	    clusterer.setComputeDBI(panel.getDBI());
	    clusterer.setSkipExternal(panel.getSkipExternal());
	}

	panel = null;
	clusterer = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return ClusteringEvaluation.class;
    }

    @Override
    public String getValue() {
	//Returns the result value on the front-end. 
	//If your metric doesn't have a single result value, return null.
	return null;
    }

    @Override
    public String getDisplayName() {
	return "Clustering evaluation";
    }

    @Override
    public String getShortDescription() {
	return "Evaluate and compare quality of various clusterings on the same dataset";
    }

    @Override
    public String getCategory() {
	return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
    }

    @Override
    public int getPosition() {
	return 1000;
    }
}
