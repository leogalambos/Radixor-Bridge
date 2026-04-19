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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.PatchCommandEncoder;
import org.egothor.stemmer.ReductionMode;
import org.egothor.stemmer.StemmerPatchTrieLoader;

/**
 * Concrete Lucene-facing stemmer backed by a Radixor patch-command trie.
 *
 * <p>
 * The trie stores patch commands as {@link String} values. For each token term,
 * the stemmer resolves the preferred patch command using
 * {@link FrequencyTrie#get(String)} and applies it to the original token
 * through {@link PatchCommandEncoder#apply(String, String)}.
 *
 * <p>
 * Unknown tokens, {@code null} patch commands, and identity results preserve
 * the original token.
 */
public final class RadixorPatchTrieStemmer implements RadixorLuceneStemmer {

    /**
     * Default bundled language used when no explicit source is specified.
     */
    public static final StemmerPatchTrieLoader.Language DEFAULT_LANGUAGE = StemmerPatchTrieLoader.Language.US_UK_PROFI;

    /**
     * Default reduction mode used when compiling textual dictionary resources.
     */
    public static final ReductionMode DEFAULT_REDUCTION_MODE = ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(RadixorPatchTrieStemmer.class.getName());

    /**
     * Runtime patch-command trie.
     */
    private final FrequencyTrie<String> trie;

    /**
     * Creates a new trie-backed stemmer.
     *
     * @param trie compiled patch-command trie
     */
    public RadixorPatchTrieStemmer(final FrequencyTrie<String> trie) {
        this.trie = Objects.requireNonNull(trie, "trie");
    }

    /**
     * Creates a stemmer from one of Radixor's bundled dictionaries.
     *
     * @param language bundled language dictionary to load
     * @return initialized stemmer
     * @throws NullPointerException if {@code language} is {@code null}
     * @throws IOException          if the bundled dictionary cannot be loaded or
     *                              compiled
     */
    public static RadixorPatchTrieStemmer forBundledLanguage(final StemmerPatchTrieLoader.Language language)
            throws IOException {
        Objects.requireNonNull(language, "language");
        return new RadixorPatchTrieStemmer(StemmerPatchTrieLoader.load(language, true, DEFAULT_REDUCTION_MODE));
    }

    /**
     * Creates a stemmer using the default bundled language configuration.
     *
     * @return initialized stemmer using {@link #DEFAULT_LANGUAGE}
     * @throws IOException if the bundled dictionary cannot be loaded or compiled
     */
    public static RadixorPatchTrieStemmer forDefaultBundledLanguage() throws IOException {
        return forBundledLanguage(DEFAULT_LANGUAGE);
    }

    /**
     * Creates a stemmer from a compressed binary patch-command trie file.
     *
     * @param path path to the compressed binary trie
     * @return initialized stemmer
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if the binary trie cannot be loaded
     */
    public static RadixorPatchTrieStemmer fromBinary(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return new RadixorPatchTrieStemmer(StemmerPatchTrieLoader.loadBinary(path));
    }

    /**
     * Resolves and applies a patch command for the supplied token term.
     *
     * @param term source token term
     * @return resulting stem, or the original term when no effective stemming
     *         result is available
     */
    @Override
    public CharSequence stem(final CharSequence term) {
        Objects.requireNonNull(term, "term");

        final String source = term.toString();
        final String patchCommand = this.trie.get(source);
        if (patchCommand == null) {
            return source;
        }

        final String stemmed = PatchCommandEncoder.apply(source, patchCommand);
        if (stemmed == null || stemmed.isEmpty()) {
            LOGGER.log(Level.FINE, "Patch application produced an empty result for token {0}.", source);
            return source;
        }

        return stemmed;
    }
}
