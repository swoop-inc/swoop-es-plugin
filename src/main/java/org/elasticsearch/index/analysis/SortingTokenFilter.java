package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.AttributeSource;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A token filter that sorts all its tokens
 */
public class SortingTokenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final Comparator<Tuple> tupleComparator = createTupleComparator();

    private List<Tuple> allTokens = null;
    private Iterator<AttributeSource.State> iterator = null;
    private AttributeSource.State finalState;

    public SortingTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (allTokens == null) {
            // get all tokens from the stream
            allTokens = new LinkedList<>();
            fillTokens();
            iterator = createIterator();
        }

        if (!iterator.hasNext()) {
            // the cache is exhausted, return false
            return false;
        }
        // Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
        restoreState(iterator.next());
        return true;
    }

    @Override
    public final void end() throws IOException {
        super.end();
        if (finalState != null) {
            restoreState(finalState);
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        allTokens = null;
        iterator = null;
        finalState = null;
    }

    private void fillTokens() throws IOException {
        while (input.incrementToken()) {
            char[] termBuffer = new char[termAtt.length()];
            System.arraycopy(termAtt.buffer(), 0, termBuffer, 0, termAtt.length());
            allTokens.add(new Tuple(termBuffer, captureState()));
        }
        // capture final state
        input.end();
        finalState = captureState();

        // sort cache tokens
        Collections.sort(allTokens, tupleComparator);
    }

    public static Comparator<char[]> createCharArrayComparator() {
        return (ca1, ca2) -> {
            int minLength = Math.min(ca1.length, ca2.length);
            for (int i = 0; i < minLength; i++) {
                int comparison = ca1[i] - ca2[i];
                if (comparison != 0)
                    return comparison;
            }
            return Integer.compare(ca1.length, ca2.length);
        };
    }

    private Comparator<Tuple> createTupleComparator() {
        return new Comparator<Tuple>() {
            private Comparator<char[]> charComparator = createCharArrayComparator();

            @Override
            public int compare(Tuple t1, Tuple t2) {
                return charComparator.compare(t1.term, t2.term);
            }
        };
    }

    private Iterator<State> createIterator() {
        return new Iterator<AttributeSource.State>() {
            Iterator<Tuple> tupleIterator = allTokens.iterator();
            int pos = 0;

            @Override
            public boolean hasNext() {
                return tupleIterator.hasNext();
            }

            @Override
            public State next() {
                AttributeSource.State state = tupleIterator.next().state;
                restoreState(state);
                int length = termAtt.length();
                offsetAtt.setOffset(pos, pos + length);
                pos += length + 1;
                return captureState();
            }

            @Override
            public void remove() {
                tupleIterator.remove();
            }
        };
    }

    private static class Tuple {
        public char[] term;
        public AttributeSource.State state;

        public Tuple(char[] term, State state) {
            super();
            this.term = term;
            this.state = state;
        }

        @Override
        public String toString() {
            return "Term: " + new String(term);
        }
    }
}
