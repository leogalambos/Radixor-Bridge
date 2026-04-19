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
import java.nio.file.Path;
import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.egothor.stemmer.StemmerPatchTrieLoader;

/**
 * Simple analyzer composed of Lucene's {@link StandardTokenizer} followed by
 * {@link RadixorFilter}.
 */
public final class RadixorAnalyzer extends Analyzer {

    /**
     * Radixor stemmer adapter.
     */
    private final RadixorLuceneStemmer stemmer;

    /**
     * Creates a new analyzer instance.
     *
     * @param stemmer Radixor stemmer adapter
     */
    public RadixorAnalyzer(final RadixorLuceneStemmer stemmer) {
        super();

        this.stemmer = Objects.requireNonNull(stemmer, "stemmer");
    }

    /**
     * Creates an analyzer using the default bundled Radixor language configuration.
     *
     * @return initialized analyzer
     * @throws IOException if the bundled dictionary cannot be loaded
     */
    public static RadixorAnalyzer forDefaultBundledLanguage() throws IOException {
        return new RadixorAnalyzer(RadixorPatchTrieStemmer.forDefaultBundledLanguage());
    }

    /**
     * Creates an analyzer for a specific bundled Radixor language.
     *
     * @param language bundled language dictionary
     * @return initialized analyzer
     * @throws NullPointerException if {@code language} is {@code null}
     * @throws IOException          if the bundled dictionary cannot be loaded
     */
    public static RadixorAnalyzer forBundledLanguage(final StemmerPatchTrieLoader.Language language)
            throws IOException {
        return new RadixorAnalyzer(RadixorPatchTrieStemmer.forBundledLanguage(language));
    }

    /**
     * Creates an analyzer from a compressed binary Radixor trie file.
     *
     * @param path path to the compressed binary trie
     * @return initialized analyzer
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if the binary trie cannot be loaded
     */
    public static RadixorAnalyzer fromBinary(final Path path) throws IOException {
        return new RadixorAnalyzer(RadixorPatchTrieStemmer.fromBinary(path));
    }

    /**
     * Creates the analyzer pipeline.
     *
     * @param fieldName field name being analyzed
     * @return tokenizer and filter chain
     */
    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final StandardTokenizer tokenizer = new StandardTokenizer();
        final TokenStream filter = new RadixorFilter(tokenizer, this.stemmer);
        return new TokenStreamComponents(tokenizer, filter);
    }
}
