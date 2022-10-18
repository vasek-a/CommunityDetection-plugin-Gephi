package sk.smitala.gephi.communitydetection.markovalgo;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;

@ServiceProvider(service = StatisticsUI.class)
public class MarkovAlgoClustererUI implements StatisticsUI {

    private MarkovAlgoClustererPanel panel;

    private MarkovAlgoClusterer clusterer;


    @Override
    public JPanel getSettingsPanel() {
        panel = new MarkovAlgoClustererPanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistics) {
        clusterer = (MarkovAlgoClusterer) statistics;
        clusterer.setkMeansClusters(panel.getKMeansN());
    }

    @Override
    public void unsetup() {
        clusterer = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return MarkovAlgoClusterer.class;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "[Clustering] MarkovAlgo";
    }

    @Override
    public String getShortDescription() {
        return "Clustering algorithm based on absorbing markov chains";
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
