package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class ConcatenateTokenFilterFactory extends AbstractTokenFilterFactory {

    public static final String FILTER_NAME = "concat";

    private final String separator;

    public ConcatenateTokenFilterFactory(IndexSettings indexSettings, Environment environment,
                                         String name, Settings settings) {
        super(indexSettings, name, settings);
        this.separator = settings.get("separator", " ");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new ConcatenateTokenFilter(tokenStream, separator);
    }
}
