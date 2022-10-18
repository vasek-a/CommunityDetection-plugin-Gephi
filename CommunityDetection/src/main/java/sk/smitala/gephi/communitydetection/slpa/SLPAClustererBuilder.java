/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.slpa;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class SLPAClustererBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "SLPA clustering";
    }

    @Override
    public Statistics getStatistics() {
	return new SLPAClusterer();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return SLPAClusterer.class;
    }
}
