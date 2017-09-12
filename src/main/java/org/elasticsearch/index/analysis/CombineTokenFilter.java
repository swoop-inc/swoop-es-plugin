package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A token filter that generate token combinations
 * eg: A,B,C -> A, A B, A C, A B C, B, B C, C
 *
 * It combine the 'current' token with following ones, not previous,
 * according to analysis order.
 *
 */
public class CombineTokenFilter extends TokenFilter {

    private static final String DEFAULT_TOKEN_SEPARATOR = " ";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final Combiner<Token> defaultCombiner = createCombiner();

    private String tokenSeparator = DEFAULT_TOKEN_SEPARATOR;
    private List<Token> allTokens = null;
    private Iterator<AttributeSource.State> iterator = null;
    private AttributeSource.State finalState;

    public CombineTokenFilter(TokenStream input) {
        this(input, DEFAULT_TOKEN_SEPARATOR);
    }

    public CombineTokenFilter(TokenStream input, String separator) {
        super(input);
        this.tokenSeparator = separator;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (allTokens == null) {
            // fill cache lazily
            allTokens = new LinkedList<>();
            fillTokens();
            combineTokens();
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

    private <T> List<T> generateCombinations(List<T> tokens, Combiner<T> combiner) {
        if (tokens == null || tokens.isEmpty()) return new ArrayList<>();

        List<T> result = new LinkedList<>();
        for (int i = 0; i < tokens.size(); i++) {
            T current = tokens.get(i);
            result.add(current);
            result.addAll(combineAll(current, tokens.subList(i+1, tokens.size()), combiner));
        }
        return result;
    }

    private <T> Collection<T> combineAll(T current, List<T> subList, Combiner<T> combiner) {
        List<T> result = new LinkedList<>();
        List<T> combinations = generateCombinations(subList, combiner);
        for (T combination : combinations) {
            result.add(combiner.combine(current, combination));
        }
        return result;
    }

    private Combiner<Token> createCombiner() {
        return (t1, t2) -> {
            Token tcomb = new Token();
            tcomb.posLen = t1.posLen + t2.posLen;
            tcomb.type = t1.type;
            char[] separator = tokenSeparator.toCharArray();
            tcomb.terms = new char[t1.terms.length + t2.terms.length + separator.length];
            System.arraycopy(t1.terms, 0, tcomb.terms, 0, t1.terms.length);
            System.arraycopy(separator, 0, tcomb.terms, t1.terms.length, separator.length);
            System.arraycopy(t2.terms, 0, tcomb.terms, t1.terms.length + separator.length, t2.terms.length);
            return tcomb;
        };
    }

    private void fillTokens() throws IOException {
        while (input.incrementToken()) {
            char[] termBuffer = new char[termAtt.length()];
            System.arraycopy(termAtt.buffer(), 0, termBuffer, 0, termAtt.length());
            Token token = new Token();
            token.terms = termBuffer;
            token.posLen = posLenAtt.getPositionLength();
            token.type = typeAtt.type();
            allTokens.add(token);
        }
        // capture final state
        input.end();
        finalState = captureState();
    }

    private void combineTokens() {
        allTokens = generateCombinations(allTokens, defaultCombiner);
    }

    private Iterator<AttributeSource.State> createIterator() {
        return new Iterator<AttributeSource.State>() {
            Iterator<Token> tupleIterator = allTokens.iterator();
            int pos = 0;

            @Override
            public boolean hasNext() {
                return tupleIterator.hasNext();
            }

            @Override
            public AttributeSource.State next() {
                Token state = tupleIterator.next();
                clearAttributes();

                termAtt.copyBuffer(state.terms, 0, state.terms.length);
                offsetAtt.setOffset(pos, pos + state.terms.length);
                posLenAtt.setPositionLength(state.posLen);
                typeAtt.setType(state.type);
                posIncAtt.setPositionIncrement(0);
                pos += state.terms.length + 1;
                return captureState();
            }

            @Override
            public void remove() {
                tupleIterator.remove();
            }
        };
    }

    public interface Combiner<T> {
        T combine(T t1, T t2);
    }

    private static class Token {
        public char[] terms;
        public String type;
        public int posLen;
        @Override
        public String toString() {
            return "Term: " + new String(terms);
        }
    }
}
