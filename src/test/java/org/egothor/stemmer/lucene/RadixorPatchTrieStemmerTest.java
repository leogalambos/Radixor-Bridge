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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.ReductionMode;
import org.egothor.stemmer.StemmerPatchTrieLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link RadixorPatchTrieStemmer}.
 */
@Tag("unit")
@Tag("lucene")
@Tag("radixor")
@DisplayName("RadixorPatchTrieStemmer")
final class RadixorPatchTrieStemmerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Constructor rejects null trie")
    void constructorRejectsNullTrie() {
        assertThrows(NullPointerException.class, () -> new RadixorPatchTrieStemmer(null));
    }

    @Test
    @DisplayName("Applies patch commands from the trie")
    void appliesPatchCommandsFromTheTrie() {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        builder.put("stemmers", "Da");

        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(builder.build());

        assertEquals("run", stemmer.stem("running").toString());
        assertEquals("stemmer", stemmer.stem("stemmers").toString());
    }

    @Test
    @DisplayName("Preserves unknown tokens")
    void preservesUnknownTokens() {
        final FrequencyTrie<String> trie = new FrequencyTrie.Builder<String>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS).build();
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(trie);

        assertEquals("unknown", stemmer.stem("unknown").toString());
    }

    @Test
    @DisplayName("Preserves original token when patch command is malformed")
    void preservesOriginalTokenWhenPatchCommandIsMalformed() {
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(TestSupport.trie("token", "D`"));

        assertEquals("token", stemmer.stem("token").toString());
    }

    @Test
    @DisplayName("Preserves original token when patch command produces empty result")
    void preservesOriginalTokenWhenPatchProducesEmptyResult() {
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(TestSupport.trie("token", "De"));

        assertEquals("token", stemmer.stem("token").toString());
    }

    @Test
    @DisplayName("Rejects null input term")
    void rejectsNullInputTerm() {
        final FrequencyTrie<String> trie = new FrequencyTrie.Builder<String>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS).build();
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(trie);

        assertThrows(NullPointerException.class, () -> stemmer.stem(null));
    }

    @Test
    @DisplayName("Preserves identity transformations")
    void preservesIdentityTransformations() {
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(TestSupport.trie("term", "Na"));
        final CharSequence result = stemmer.stem("term");

        assertSame("term", result);
    }

    @Test
    @DisplayName("Accepts arbitrary CharSequence input")
    void acceptsArbitraryCharSequenceInput() {
        final RadixorPatchTrieStemmer stemmer = new RadixorPatchTrieStemmer(TestSupport.trie("running", "Dd"));

        final CharSequence result = stemmer.stem(new StringBuilder("running"));

        assertNotNull(result);
        assertEquals("run", result.toString());
    }

    @Test
    @DisplayName("Default bundled language factory creates functioning stemmer")
    void defaultBundledLanguageFactoryCreatesFunctioningStemmer() throws Exception {
        final RadixorPatchTrieStemmer stemmer = RadixorPatchTrieStemmer.forDefaultBundledLanguage();
        assertNotNull(stemmer.stem("running"));
    }

    @Test
    @DisplayName("Bundled language factory rejects null language")
    void bundledLanguageFactoryRejectsNullLanguage() {
        assertThrows(NullPointerException.class, () -> RadixorPatchTrieStemmer.forBundledLanguage(null));
    }

    @Test
    @DisplayName("Bundled language factory creates functioning stemmer")
    void bundledLanguageFactoryCreatesFunctioningStemmer() throws Exception {
        final RadixorPatchTrieStemmer stemmer = RadixorPatchTrieStemmer.forBundledLanguage(StemmerPatchTrieLoader.Language.US_UK);
        assertNotNull(stemmer.stem("running"));
    }

    @Test
    @DisplayName("Binary factory rejects null path")
    void binaryFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> RadixorPatchTrieStemmer.fromBinary(null));
    }

    @Test
    @DisplayName("Binary factory creates functioning stemmer")
    void binaryFactoryCreatesFunctioningStemmer() throws Exception {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        final Path binaryPath = TestSupport.writeBinaryTrie(this.temporaryDirectory, "sample.radixor.gz", builder.build());

        final RadixorPatchTrieStemmer stemmer = RadixorPatchTrieStemmer.fromBinary(binaryPath);
        assertEquals("run", stemmer.stem("running").toString());
    }
}
