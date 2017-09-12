package org.elasticsearch.swoop;

import org.elasticsearch.index.analysis.CombineTokenFilterFactory;
import org.elasticsearch.index.analysis.ConcatenateTokenFilterFactory;
import org.elasticsearch.index.analysis.SortingTokenFilterFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Swoop's custom plugin for Elasticsearch
 */
public class SwoopPlugin extends Plugin implements AnalysisPlugin {

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters()
    {
        Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put(CombineTokenFilterFactory.FILTER_NAME, CombineTokenFilterFactory::new);
        extra.put(ConcatenateTokenFilterFactory.FILTER_NAME, ConcatenateTokenFilterFactory::new);
        extra.put(SortingTokenFilterFactory.FILTER_NAME, SortingTokenFilterFactory::new);
        return extra;
    }
}
