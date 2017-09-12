package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class SortingTokenFilterFactory extends AbstractTokenFilterFactory {

    public static final String FILTER_NAME = "sort";

    public SortingTokenFilterFactory(IndexSettings indexSettings, Environment environment,
                                     String name, Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new SortingTokenFilter(tokenStream);
    }
}
