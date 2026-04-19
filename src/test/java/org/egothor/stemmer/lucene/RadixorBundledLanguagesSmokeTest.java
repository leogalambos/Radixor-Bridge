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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.egothor.stemmer.StemmerDictionaryParser;
import org.egothor.stemmer.StemmerPatchTrieLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Smoke tests covering all bundled Radixor language dictionaries available
 * through the Lucene bridge.
 *
 * <p>
 * The goal of this suite is not to exhaustively validate dictionary quality. It
 * verifies that every bundled language resource can be loaded through the
 * public bridge API and that at least one real dictionary entry from that
 * resource can be stemmed successfully.
 */
@DisplayName("Bundled Radixor languages")
@Tag("integration")
@Tag("lucene")
@Tag("languages")
final class RadixorBundledLanguagesSmokeTest {

    /**
     * Verifies that every bundled Radixor language can be loaded and used for at
     * least one real dictionary entry.
     *
     * @param language bundled language to verify
     * @throws IOException if loading fails unexpectedly
     */
    @ParameterizedTest(name = "Loads and stems with bundled language {0}")
    @EnumSource(StemmerPatchTrieLoader.Language.class)
    void loadsAndStemsWithEveryBundledLanguage(final StemmerPatchTrieLoader.Language language) throws IOException {
        final DictionarySample sample = loadFirstDictionarySample(language);
        final RadixorPatchTrieStemmer stemmer = RadixorPatchTrieStemmer.forBundledLanguage(language);

        final CharSequence stemmed = stemmer.stem(sample.token());

        assertNotNull(stemmed, () -> "Stemmer returned null for language " + language + '.');
        assertEquals(sample.expectedStem(), stemmed.toString(),
                () -> "Unexpected stemming result for language " + language + ", token '" + sample.token() + "'.");
    }

    /**
     * Loads one representative sample from the bundled dictionary of the supplied
     * language.
     *
     * <p>
     * The first logical entry is used. When that entry contains known variants, the
     * first variant is used as the source token and the canonical stem is used as
     * the expected result. When no variant is present, the stem itself is used as
     * both source and expected result.
     *
     * @param language bundled language
     * @return representative dictionary sample
     * @throws IOException if the resource cannot be opened or parsed
     */
    private static DictionarySample loadFirstDictionarySample(final StemmerPatchTrieLoader.Language language)
            throws IOException {
        Objects.requireNonNull(language, "language");

        final AtomicReference<DictionarySample> sampleReference = new AtomicReference<>();
        final String resourcePath = language.resourcePath();

        try (InputStream inputStream = openBundledResource(resourcePath);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StemmerDictionaryParser.parse(reader, resourcePath, (stem, variants, lineNumber) -> {
                if (sampleReference.get() == null) {
                    final String token = variants.length == 0 ? stem : variants[0];
                    sampleReference.set(new DictionarySample(token, stem));
                }
            });
        }

        final DictionarySample sample = sampleReference.get();
        assertTrue(sample != null,
                () -> "Bundled dictionary does not contain any logical entries for language " + language + '.');
        return sample;
    }

    /**
     * Opens one bundled dictionary resource from the classpath.
     *
     * @param resourcePath classpath resource path
     * @return opened stream
     * @throws IOException if the resource is missing
     */
    private static InputStream openBundledResource(final String resourcePath) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Bundled Radixor resource not found: " + resourcePath);
        }
        return inputStream;
    }

    /**
     * Representative dictionary sample used by the smoke test.
     *
     * @param token        source token to stem
     * @param expectedStem expected stemming result
     */
    private record DictionarySample(String token, String expectedStem) {

        /**
         * Creates a sample.
         *
         * @param token        source token to stem
         * @param expectedStem expected stemming result
         */
        private DictionarySample {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(expectedStem, "expectedStem");
        }
    }
}
