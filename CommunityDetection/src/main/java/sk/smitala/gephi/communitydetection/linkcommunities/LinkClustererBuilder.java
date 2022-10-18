/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.linkcommunities;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class LinkClustererBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "Link communities";
    }

    @Override
    public Statistics getStatistics() {
	return new LinkClusterer();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return LinkClusterer.class;
    }
}
