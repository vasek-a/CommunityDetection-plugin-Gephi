/*
 * Your license here
 */
package sk.smitala.gephi.communitydetection.scan;

import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsUI.class)
public class ScanClustererUI implements StatisticsUI {
    
    private ScanClustererPanel panel;
    private ScanClusterer scanClusterer;
    
    @Override
    public JPanel getSettingsPanel() {
	panel = new ScanClustererPanel();
	return panel; //null if no panel exists
    }
    
    @Override
    public void setup(Statistics stat) {
	this.scanClusterer = (ScanClusterer) stat;
	if (panel != null) {
	    panel.setEpsilon(scanClusterer.getEpsilon());
	    panel.setMu(scanClusterer.getMu());
	    panel.setGroupHubs(scanClusterer.isGroupHubs());
	    panel.setGroupOutliers(scanClusterer.isGroupOutliers());
	    panel.setExperimentalImprovement(scanClusterer.isExperimentalImprovement());
	}
    }
    
    @Override
    public void unsetup() {
	if (panel != null) {
	    scanClusterer.setEpsilon(panel.getEpsilon());
	    scanClusterer.setMu(panel.getMu());
	    scanClusterer.setGroupHubs(panel.getGroupHubs());
	    scanClusterer.setGroupOutliers(panel.getGroupOutliers());
	    scanClusterer.setExperimentalImprovement(panel.getExperimentalImprovement());
	}
	
	panel = null;
	scanClusterer = null;
    }
    
    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return ScanClusterer.class;
    }
    
    @Override
    public String getValue() {
	//Returns the result value on the front-end. 
	//If your metric doesn't have a single result value, return null.
	return null;
    }
    
    @Override
    public String getDisplayName() {
	return "[Clustering] SCAN";
    }
    
    @Override
    public String getShortDescription() {
	return "SCAN clustering algorithm";
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
