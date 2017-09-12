package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;

/**
 * A token filter that concatenates all the tokens into a single big token, with 'spacer' added to join them
 *
 */
public class ConcatenateTokenFilter extends TokenFilter {

    private static final String DEFAULT_SEPARATOR = " ";

    private final StringBuilder sb = new StringBuilder();
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private boolean consumedAll; // true if we already consumed

    private final String separator;

    public ConcatenateTokenFilter(TokenStream in) {
        this(in, DEFAULT_SEPARATOR);
    }

    public ConcatenateTokenFilter(TokenStream in, String separator) {
        super(in);
        this.separator = separator;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (consumedAll) {
            return false;
        }
        consumedAll = true;

        int startOffset = 0;
        int endOffset = 0;

        boolean foundTokens = false; // true if we actually consumed any tokens
        while (input.incrementToken()) {
            if (!foundTokens) {
                startOffset = offsetAtt.startOffset();
                foundTokens = true;
            }
            sb.append(termAtt);
            sb.append(separator);
            endOffset = offsetAtt.endOffset();
        }

        if (foundTokens) {
            sb.setLength(sb.length() - separator.length());
            clearAttributes();
            termAtt.setEmpty().append(sb);
            offsetAtt.setOffset(startOffset, endOffset);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        sb.setLength(0);
        consumedAll = false;
    }
}
