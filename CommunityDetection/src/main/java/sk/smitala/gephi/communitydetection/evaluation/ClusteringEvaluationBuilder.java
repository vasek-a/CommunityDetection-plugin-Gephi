/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.evaluation;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author smitalm
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class ClusteringEvaluationBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
	return "Clustering evaluation";
    }

    @Override
    public Statistics getStatistics() {
	return new ClusteringEvaluation();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
	return ClusteringEvaluation.class;
    }
}
