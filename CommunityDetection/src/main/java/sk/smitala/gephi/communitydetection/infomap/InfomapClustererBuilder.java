/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.infomap;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
//@ServiceProvider(service = StatisticsBuilder.class)
public class InfomapClustererBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "Infomap clustering";
    }

    @Override
    public Statistics getStatistics() {
	return new InfomapClusterer();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return InfomapClusterer.class;
    }
}
