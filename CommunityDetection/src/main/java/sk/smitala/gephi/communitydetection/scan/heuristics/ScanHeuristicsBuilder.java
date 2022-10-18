/*
 * Your license here
 */
package sk.smitala.gephi.communitydetection.scan.heuristics;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * See
 * http://wiki.gephi.org/index.php/HowTo_write_a_metric#Create_StatisticsBuilder
 *
 * @author Your Name <your.name@your.company.com>
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class ScanHeuristicsBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "SCAN heuristics";
    }

    @Override
    public Statistics getStatistics() {
	return new ScanHeuristics();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return ScanHeuristics.class;
    }
}
