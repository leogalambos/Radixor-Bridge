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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.Set;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RadixorFilter}.
 */
@Tag("unit")
@Tag("lucene")
@Tag("analysis")
@DisplayName("RadixorFilter")
class RadixorFilterTest extends BaseTokenStreamTestCase {

    @Test
    @DisplayName("Constructor rejects null input stream")
    void constructorRejectsNullInputStream() {
        assertThrows(NullPointerException.class, () -> new RadixorFilter(null, term -> term));
    }

    @Test
    @DisplayName("Constructor rejects null stemmer")
    void constructorRejectsNullStemmer() {
        assertThrows(NullPointerException.class,
                () -> new RadixorFilter(TestSupport.whitespaceTokenizer("token"), null));
    }

    @Test
    @DisplayName("Filter applies stemming to non-keyword tokens")
    void filterAppliesStemmingToNonKeywordTokens() throws Exception {
        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("running houses");
        final TokenStream stream = new RadixorFilter(tokenizer, term -> switch (term.toString()) {
            case "running" -> "run";
            case "houses" -> "house";
            default -> term;
        });

        assertTokenStreamContents(stream, new String[] { "run", "house" });
    }

    @Test
    @DisplayName("Filter preserves keyword tokens")
    void filterPreservesKeywordTokens() throws Exception {
        final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("keep change"));

        final TokenStream stream = new RadixorFilter(
                new SetKeywordMarkerFilter(tokenizer, new CharArraySet(Set.of("keep"), false)),
                term -> "x");

        assertTokenStreamContents(stream, new String[] { "keep", "x" });
    }

    @Test
    @DisplayName("Filter preserves tokens when stemmer returns null")
    void filterPreservesTokensWhenStemmerReturnsNull() throws Exception {
        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("alpha beta");
        final TokenStream stream = new RadixorFilter(tokenizer, term -> null);

        assertTokenStreamContents(stream, new String[] { "alpha", "beta" });
    }

    @Test
    @DisplayName("Filter preserves tokens when stemmer returns empty result")
    void filterPreservesTokensWhenStemmerReturnsEmptyResult() throws Exception {
        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("alpha beta");
        final TokenStream stream = new RadixorFilter(tokenizer, term -> "");

        assertTokenStreamContents(stream, new String[] { "alpha", "beta" });
    }

    @Test
    @DisplayName("Filter preserves tokens when stemmer returns equal content as StringBuilder")
    void filterPreservesTokensWhenStemmerReturnsEqualContent() throws Exception {
        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("mirror");
        final TokenStream stream = new RadixorFilter(tokenizer, term -> new StringBuilder(term.toString()));

        assertTokenStreamContents(stream, new String[] { "mirror" });
    }
}
