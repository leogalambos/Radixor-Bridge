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
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.ResourceLoader;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.ReductionMode;
import org.egothor.stemmer.StemmerPatchTrieLoader;

/**
 * Shared test helpers for the Radixor Lucene test suite.
 */
final class TestSupport {

    private TestSupport() {
        throw new AssertionError("No instances.");
    }

    static FrequencyTrie<String> trie(final String key, final String patchCommand) {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put(key, patchCommand);
        return builder.build();
    }

    static Tokenizer whitespaceTokenizer(final String text) {
        final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(text));
        return tokenizer;
    }

    static String[] analyze(final Analyzer analyzer, final String text) throws IOException {
        final List<String> terms = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream("field", text)) {
            final CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                terms.add(termAttribute.toString());
            }
            tokenStream.end();
        }
        return terms.toArray(String[]::new);
    }

    static Path writeBinaryTrie(final Path directory, final String resourceName, final FrequencyTrie<String> trie)
            throws IOException {
        final Path path = directory.resolve(resourceName);
        StemmerPatchTrieLoader.saveBinary(trie, path);
        return path;
    }

    static Path writeTextDictionary(final Path directory, final String resourceName, final String content)
            throws IOException {
        final Path path = directory.resolve(resourceName);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    static ResourceLoader fileSystemResourceLoader(final Path baseDirectory) {
        return new ResourceLoader() {
            @Override
            public InputStream openResource(final String resource) throws IOException {
                return Files.newInputStream(baseDirectory.resolve(resource));
            }

            @Override
            public <T> Class<? extends T> findClass(final String cname, final Class<T> expectedType) {
                try {
                    return Class.forName(cname).asSubclass(expectedType);
                } catch (ClassNotFoundException exception) {
                    throw new RuntimeException(exception);
                }
            }

            @Override
            public <T> T newInstance(final String cname, final Class<T> expectedType) {
                try {
                    return findClass(cname, expectedType).getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException exception) {
                    throw new RuntimeException(exception);
                }
            }
        };
    }
}
