/*
 * Copyright (C) 2026, Leo Galambos
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.egothor.stemmer.lucene;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * Applies Radixor stemming to Lucene token terms.
 *
 * <p>
 * Tokens marked with {@link KeywordAttribute} are preserved unchanged. If the
 * underlying Radixor stemmer returns {@code null}, an empty sequence, or
 * content equal to the original term, the original term is preserved as well.
 */
public final class RadixorFilter extends TokenFilter {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(RadixorFilter.class.getName());

    /**
     * Token term attribute.
     */
    private final CharTermAttribute termAttribute;

    /**
     * Keyword marker attribute.
     */
    private final KeywordAttribute keywordAttribute;

    /**
     * Radixor stemmer adapter.
     */
    private final RadixorLuceneStemmer stemmer;

    /**
     * Creates a new filter instance.
     *
     * @param input   upstream token stream
     * @param stemmer Radixor stemmer adapter
     */
    public RadixorFilter(final TokenStream input, final RadixorLuceneStemmer stemmer) {
        super(Objects.requireNonNull(input, "input"));
        this.stemmer = Objects.requireNonNull(stemmer, "stemmer");
        this.termAttribute = addAttribute(CharTermAttribute.class);
        this.keywordAttribute = addAttribute(KeywordAttribute.class);
    }

    /**
     * Advances the stream by one token and applies stemming when appropriate.
     *
     * @return {@code true} if a token is available, otherwise {@code false}
     * @throws IOException if the upstream token stream fails
     */
    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        if (keywordAttribute.isKeyword()) {
            return true;
        }

        final String original = this.termAttribute.toString();
        final CharSequence stemmed = this.stemmer.stem(original);
        if (stemmed == null) {
            return true;
        }
        if (stemmed.length() == 0) {
            LOGGER.log(Level.FINE, "Radixor returned an empty stem for token {0}; preserving original token.",
                    original);
            return true;
        }
        if (contentEquals(original, stemmed)) {
            return true;
        }

        this.termAttribute.setEmpty();
        this.termAttribute.append(stemmed);
        return true;
    }

    /**
     * Compares a {@link String} with an arbitrary {@link CharSequence} without
     * creating an intermediate {@link String} instance.
     *
     * @param left  left value
     * @param right right value
     * @return {@code true} if both values contain identical character content
     */
    private static boolean contentEquals(final String left, final CharSequence right) {
        if (left.length() != right.length()) {
            return false;
        }
        for (int index = 0; index < left.length(); index++) {
            if (left.charAt(index) != right.charAt(index)) {
                return false;
            }
        }
        return true;
    }
}
