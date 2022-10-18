/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.scan;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class ScanClustererBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "SCAN clustering";
    }

    @Override
    public Statistics getStatistics() {
	return new ScanClusterer();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return ScanClusterer.class;
    }
}
