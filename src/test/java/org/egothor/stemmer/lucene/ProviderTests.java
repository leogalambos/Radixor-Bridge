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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.util.ClasspathResourceLoader;
import org.apache.lucene.util.ResourceLoader;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.ReductionMode;
import org.egothor.stemmer.StemmerPatchTrieLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for provider implementations and provider base classes.
 */
@Tag("unit")
@Tag("lucene")
@Tag("provider")
@DisplayName("Radixor stemmer providers")
final class ProviderTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Identity provider preserves original token")
    void identityProviderPreservesOriginalToken() throws Exception {
        final IdentityRadixorStemmerProvider provider = new IdentityRadixorStemmerProvider();
        final RadixorLuceneStemmer stemmer = provider.createStemmer(new ClasspathResourceLoader(getClass()));

        assertEquals("token", stemmer.stem("token").toString());
    }

    @Test
    @DisplayName("Abstract trie provider rejects null loader")
    void abstractTrieProviderRejectsNullLoader() {
        final AbstractPatchTrieRadixorStemmerProvider provider = new AbstractPatchTrieRadixorStemmerProvider() {
            @Override
            protected FrequencyTrie<String> loadTrie(final ResourceLoader loader) {
                return TestSupport.trie("running", "Dd");
            }
        };

        assertThrows(NullPointerException.class, () -> provider.createStemmer(null));
    }

    @Test
    @DisplayName("Abstract trie provider creates trie backed stemmer")
    void abstractTrieProviderCreatesTrieBackedStemmer() throws Exception {
        final AbstractPatchTrieRadixorStemmerProvider provider = new AbstractPatchTrieRadixorStemmerProvider() {
            @Override
            protected FrequencyTrie<String> loadTrie(final ResourceLoader loader) {
                return TestSupport.trie("running", "Dd");
            }
        };

        final RadixorLuceneStemmer stemmer = provider.createStemmer(new ClasspathResourceLoader(getClass()));
        assertEquals("run", stemmer.stem("running").toString());
    }

    @Test
    @DisplayName("Abstract trie provider propagates loader failure")
    void abstractTrieProviderPropagatesLoaderFailure() {
        final AbstractPatchTrieRadixorStemmerProvider provider = new AbstractPatchTrieRadixorStemmerProvider() {
            @Override
            protected FrequencyTrie<String> loadTrie(final ResourceLoader loader) throws IOException {
                throw new IOException("boom");
            }
        };

        assertThrows(IOException.class, () -> provider.createStemmer(new ClasspathResourceLoader(getClass())));
    }

    @Test
    @DisplayName("Abstract trie provider rejects null trie returned by subclass")
    void abstractTrieProviderRejectsNullTrieReturnedBySubclass() {
        final AbstractPatchTrieRadixorStemmerProvider provider = new AbstractPatchTrieRadixorStemmerProvider() {
            @Override
            protected FrequencyTrie<String> loadTrie(final ResourceLoader loader) {
                return null;
            }
        };

        assertThrows(NullPointerException.class, () -> provider.createStemmer(new ClasspathResourceLoader(getClass())));
    }

    @Test
    @DisplayName("Bundled language provider creates functioning stemmer")
    void bundledLanguageProviderCreatesFunctioningStemmer() throws Exception {
        final RadixorLuceneStemmer stemmer = new EnglishBundledRadixorStemmerProvider()
                .createStemmer(new ClasspathResourceLoader(getClass()));

        assertNotNull(stemmer.stem("running"));
    }

    @Test
    @DisplayName("Binary resource provider loads trie through resource loader")
    void binaryResourceProviderLoadsTrieThroughResourceLoader() throws Exception {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        TestSupport.writeBinaryTrie(this.temporaryDirectory, "sample.radixor.gz", builder.build());

        final AbstractBinaryResourceRadixorStemmerProvider provider = new AbstractBinaryResourceRadixorStemmerProvider() {
            @Override
            protected String resourcePath() {
                return "sample.radixor.gz";
            }
        };

        final RadixorLuceneStemmer stemmer = provider
                .createStemmer(TestSupport.fileSystemResourceLoader(this.temporaryDirectory));

        assertEquals("run", stemmer.stem("running").toString());
    }

    @Test
    @DisplayName("Binary resource provider rejects null loader")
    void binaryResourceProviderRejectsNullLoader() {
        final AbstractBinaryResourceRadixorStemmerProvider provider = new AbstractBinaryResourceRadixorStemmerProvider() {
            @Override
            protected String resourcePath() {
                return "ignored.radixor.gz";
            }
        };

        assertThrows(NullPointerException.class, () -> provider.createStemmer(null));
    }

    @Test
    @DisplayName("Binary resource provider propagates missing resource failure")
    void binaryResourceProviderPropagatesMissingResourceFailure() {
        final AbstractBinaryResourceRadixorStemmerProvider provider = new AbstractBinaryResourceRadixorStemmerProvider() {
            @Override
            protected String resourcePath() {
                return "missing.radixor.gz";
            }
        };

        assertThrows(IOException.class,
                () -> provider.createStemmer(TestSupport.fileSystemResourceLoader(this.temporaryDirectory)));
    }

    @Test
    @DisplayName("Bundled language provider can override language and options")
    void bundledLanguageProviderCanOverrideLanguageAndOptions() throws Exception {
        final AbstractBundledLanguageRadixorStemmerProvider provider = new AbstractBundledLanguageRadixorStemmerProvider() {
            @Override
            protected StemmerPatchTrieLoader.Language language() {
                return StemmerPatchTrieLoader.Language.US_UK;
            }

            @Override
            protected boolean storeOriginal() {
                return false;
            }
        };

        final RadixorLuceneStemmer stemmer = provider.createStemmer(new ClasspathResourceLoader(getClass()));
        assertNotNull(stemmer.stem("running"));
    }
}
