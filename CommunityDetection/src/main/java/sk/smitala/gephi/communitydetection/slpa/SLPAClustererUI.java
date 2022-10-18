/*
 * Your license here
 */
package sk.smitala.gephi.communitydetection.slpa;

import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsUI.class)
public class SLPAClustererUI implements StatisticsUI {

    private SLPAClustererPanel panel;
    private SLPAClusterer clusterer;

    @Override
    public JPanel getSettingsPanel() {
	panel = new SLPAClustererPanel();
	return panel; //null if no panel exists
    }

    @Override
    public void setup(Statistics stat) {
	this.clusterer = (SLPAClusterer) stat;
	if (panel != null) {
	    panel.setIterations(clusterer.getIterations());
	    panel.setThreshold(clusterer.getThreshold());
	}
    }

    @Override
    public void unsetup() {
	if (panel != null) {
	    clusterer.setIterations(panel.getIterations());
	    clusterer.setThreshold(panel.getThreshold());
	}

	panel = null;
	clusterer = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return SLPAClusterer.class;
    }

    @Override
    public String getValue() {
	//Returns the result value on the front-end. 
	//If your metric doesn't have a single result value, return null.
	return null;
    }

    @Override
    public String getDisplayName() {
	return "[Clustering] SLPA community detection";
    }

    @Override
    public String getShortDescription() {
	return "Speaker-listener Label Propagation Algorithm";
    }

    @Override
    public String getCategory() {
	//The category is just where you want your metric to be displayed: NODE, EDGE or NETWORK.
	//Choose between:
	//- StatisticsUI.CATEGORY_NODE_OVERVIEW
	//- StatisticsUI.CATEGORY_EDGE_OVERVIEW
	//- StatisticsUI.CATEGORY_NETWORK_OVERVIEW
	return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
    }

    @Override
    public int getPosition() {
	//The position control the order the metric front-end are displayed. 
	//Returns a value between 1 and 1000, that indicates the position. 
	//Less means upper.
	return 1000;
    }
}
