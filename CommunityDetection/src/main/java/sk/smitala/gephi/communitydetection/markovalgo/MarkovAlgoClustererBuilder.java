package sk.smitala.gephi.communitydetection.markovalgo;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = StatisticsBuilder.class)
public class MarkovAlgoClustererBuilder implements StatisticsBuilder {
    @Override
    public String getName() {
        return "MarkovAlgo clustering";
    }

    @Override
    public Statistics getStatistics() {
        return new MarkovAlgoClusterer();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return MarkovAlgoClusterer.class;
    }
}
