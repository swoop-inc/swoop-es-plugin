package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class CombineTokenFilterFactory extends AbstractTokenFilterFactory
{
    public static final String FILTER_NAME = "combine";

    private final String separator;

    public CombineTokenFilterFactory(IndexSettings indexSettings, Environment environment,
                                     String name, Settings settings) {
        super(indexSettings, name, settings);
        this.separator = settings.get("separator", " ");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new CombineTokenFilter(tokenStream, separator);
    }
}
