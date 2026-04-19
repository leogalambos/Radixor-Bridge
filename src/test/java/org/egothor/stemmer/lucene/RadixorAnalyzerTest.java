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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Unit tests for {@link RadixorAnalyzer}.
 */
@Tag("unit")
@Tag("lucene")
@Tag("analysis")
@DisplayName("RadixorAnalyzer")
final class RadixorAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Constructor rejects null stemmer")
    void constructorRejectsNullStemmer() {
        assertThrows(NullPointerException.class, () -> new RadixorAnalyzer(null));
    }

    @Test
    @DisplayName("Analyzer stems tokens through the configured filter chain")
    void analyzerStemsTokensThroughConfiguredFilterChain() throws Exception {
        try (RadixorAnalyzer analyzer = new RadixorAnalyzer(term -> switch (term.toString()) {
            case "running" -> "run";
            case "houses" -> "house";
            default -> term;
        })) {
            assertArrayEquals(new String[] { "run", "house" }, TestSupport.analyze(analyzer, "running houses"));
        }
    }

    @Test
    @DisplayName("Analyzer returns empty result for empty input")
    void analyzerReturnsEmptyResultForEmptyInput() throws Exception {
        try (RadixorAnalyzer analyzer = new RadixorAnalyzer(term -> term)) {
            assertArrayEquals(new String[0], TestSupport.analyze(analyzer, ""));
        }
    }

    @Test
    @DisplayName("Default bundled language factory creates functioning analyzer")
    void defaultBundledLanguageFactoryCreatesFunctioningAnalyzer() throws Exception {
        try (RadixorAnalyzer analyzer = RadixorAnalyzer.forDefaultBundledLanguage()) {
            assertNotNull(TestSupport.analyze(analyzer, "running"));
        }
    }

    @Test
    @DisplayName("Bundled language factory rejects null language")
    void bundledLanguageFactoryRejectsNullLanguage() {
        assertThrows(NullPointerException.class, () -> RadixorAnalyzer.forBundledLanguage(null));
    }

    @Test
    @DisplayName("Bundled language factory creates functioning analyzer")
    void bundledLanguageFactoryCreatesFunctioningAnalyzer() throws Exception {
        try (RadixorAnalyzer analyzer = RadixorAnalyzer.forBundledLanguage(StemmerPatchTrieLoader.Language.US_UK)) {
            assertNotNull(TestSupport.analyze(analyzer, "running"));
        }
    }

    @Test
    @DisplayName("Binary factory rejects null path")
    void binaryFactoryRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> RadixorAnalyzer.fromBinary(null));
    }

    @Test
    @DisplayName("Binary factory creates functioning analyzer")
    void binaryFactoryCreatesFunctioningAnalyzer() throws Exception {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        final Path binaryPath = TestSupport.writeBinaryTrie(this.temporaryDirectory, "sample.radixor.gz", builder.build());

        try (RadixorAnalyzer analyzer = RadixorAnalyzer.fromBinary(binaryPath)) {
            assertArrayEquals(new String[] { "run" }, TestSupport.analyze(analyzer, "running"));
        }
    }
}
