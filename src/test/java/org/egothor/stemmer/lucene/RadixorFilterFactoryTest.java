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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.util.ClasspathResourceLoader;
import org.apache.lucene.util.ResourceLoader;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.ReductionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link RadixorFilterFactory}.
 */
@DisplayName("RadixorFilterFactory")
@Tag("unit")
@Tag("lucene")
final class RadixorFilterFactoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Rejects unknown factory parameters")
    void rejectsUnknownFactoryParameters() {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        args.put("unexpected", "value");

        assertThrows(IllegalArgumentException.class, () -> new RadixorFilterFactory(args));
    }

    @Test
    @DisplayName("Default constructor requires subsequent configuration")
    void defaultConstructorRequiresSubsequentConfiguration() {
        final RadixorFilterFactory factory = new RadixorFilterFactory();
        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("alpha");
        assertThrows(IllegalStateException.class, () -> factory.create(tokenizer));
    }

    @Test
    @DisplayName("Map constructor accepts empty configuration and defaults to bundled language")
    void mapConstructorAcceptsEmptyConfiguration() throws Exception {
        final RadixorFilterFactory factory = new RadixorFilterFactory(new HashMap<>());
        factory.inform(new ClasspathResourceLoader(getClass()));

        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("running");
        final TokenStream stream = factory.create(tokenizer);
        assertNotNull(stream);
    }

    @Test
    @DisplayName("Inform rejects null resource loader")
    void informRejectsNullResourceLoader() {
        final RadixorFilterFactory factory = new RadixorFilterFactory(new HashMap<>());
        assertThrows(NullPointerException.class, () -> factory.inform(null));
    }

    @Test
    @DisplayName("Create rejects null token stream")
    void createRejectsNullTokenStream() throws Exception {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);
        factory.inform(new ClasspathResourceLoader(getClass()));

        assertThrows(NullPointerException.class, () -> factory.create(null));
    }

    @Test
    @DisplayName("Fails when create is called before inform")
    void failsWhenCreateIsCalledBeforeInform() {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("alpha");
        assertThrows(IllegalStateException.class, () -> factory.create(tokenizer));
    }

    @Test
    @DisplayName("Provider configuration creates functioning filter")
    void providerConfigurationCreatesFunctioningFilter() throws Exception {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        factory.inform(new ClasspathResourceLoader(getClass()));

        try (CustomAnalyzer analyzer = CustomAnalyzer.builder().withTokenizer("standard")
                .addTokenFilter("radixor", "provider", IdentityRadixorStemmerProvider.class.getName()).build()) {
            assertArrayEquals(new String[] { "Alpha", "Beta" }, TestSupport.analyze(analyzer, "Alpha Beta"));
        }
    }

    @Test
    @DisplayName("Text resource configuration compiles and stems")
    void textResourceConfigurationCompilesAndStems() throws Exception {
        TestSupport.writeTextDictionary(this.temporaryDirectory, "sample.dict",
                "# sample dictionary\nrun running runs\nhouse houses\n");

        final Map<String, String> args = new HashMap<>();
        args.put("resource", "sample.dict");
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);
        final ResourceLoader loader = TestSupport.fileSystemResourceLoader(this.temporaryDirectory);
        factory.inform(loader);

        try (CustomAnalyzer analyzer = CustomAnalyzer.builder(loader).withTokenizer("standard")
                .addTokenFilter("radixor", "resource", "sample.dict").build()) {
            assertArrayEquals(new String[] { "run", "house" }, TestSupport.analyze(analyzer, "running houses"));
        }
    }

    @Test
    @DisplayName("Binary resource configuration loads and stems")
    void binaryResourceConfigurationLoadsAndStems() throws Exception {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        TestSupport.writeBinaryTrie(this.temporaryDirectory, "sample.radixor.gz", builder.build());

        final Map<String, String> args = new HashMap<>();
        args.put("binaryResource", "sample.radixor.gz");
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);
        final ResourceLoader loader = TestSupport.fileSystemResourceLoader(this.temporaryDirectory);
        factory.inform(loader);

        final Tokenizer tokenizer = TestSupport.whitespaceTokenizer("running");
        final TokenStream stream = factory.create(tokenizer);
        assertNotNull(stream);
    }

    @Test
    @DisplayName("Binary path configuration loads and stems")
    void binaryPathConfigurationLoadsAndStems() throws Exception {
        final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS);
        builder.put("running", "Dd");
        final Path binaryPath = TestSupport.writeBinaryTrie(this.temporaryDirectory, "sample.radixor.gz",
                builder.build());

        try (CustomAnalyzer analyzer = CustomAnalyzer.builder().withTokenizer("standard")
                .addTokenFilter("radixor", "binaryPath", binaryPath.toString()).build()) {
            assertArrayEquals(new String[] { "run" }, TestSupport.analyze(analyzer, "running"));
        }
    }

    @Test
    @DisplayName("Explicit language configuration loads bundled stemmer")
    void explicitLanguageConfigurationLoadsBundledStemmer() throws Exception {
        try (CustomAnalyzer analyzer = CustomAnalyzer.builder().withTokenizer("standard")
                .addTokenFilter("radixor", "language", "US_UK").build()) {
            assertNotNull(TestSupport.analyze(analyzer, "running"));
        }
    }

    @Test
    @DisplayName("Rejects invalid language value")
    void rejectsInvalidLanguageValue() {
        final Map<String, String> args = new HashMap<>();
        args.put("language", "NOT_A_LANGUAGE");
        assertThrows(IllegalArgumentException.class, () -> new RadixorFilterFactory(args));
    }

    @Test
    @DisplayName("Rejects multiple explicit non-language sources")
    void rejectsMultipleExplicitNonLanguageSources() {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        args.put("binaryPath", "/tmp/sample.radixor.gz");
        assertThrows(IllegalArgumentException.class, () -> new RadixorFilterFactory(args));
    }

    @Test
    @DisplayName("Rejects language combined with provider")
    void rejectsLanguageCombinedWithProvider() {
        final Map<String, String> args = new HashMap<>();
        args.put("language", "US_UK");
        args.put("provider", IdentityRadixorStemmerProvider.class.getName());
        assertThrows(IllegalArgumentException.class, () -> new RadixorFilterFactory(args));
    }

    @Test
    @DisplayName("Inform fails when provider class does not exist")
    void informFailsWhenProviderClassDoesNotExist() {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", "org.egothor.stemmer.lucene.DoesNotExistProvider");
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        assertThrows(IOException.class, () -> factory.inform(new ClasspathResourceLoader(getClass())));
    }

    @Test
    @DisplayName("Inform fails when provider has no public no-arg constructor")
    void informFailsWhenProviderHasNoPublicNoArgConstructor() {
        final Map<String, String> args = new HashMap<>();
        args.put("provider", NoDefaultConstructorProvider.class.getName());
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        assertThrows(IOException.class, () -> factory.inform(new ClasspathResourceLoader(getClass())));
    }

    @Test
    @DisplayName("Inform fails when text resource is missing")
    void informFailsWhenTextResourceIsMissing() {
        final Map<String, String> args = new HashMap<>();
        args.put("resource", "missing.dict");
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        assertThrows(IOException.class,
                () -> factory.inform(TestSupport.fileSystemResourceLoader(this.temporaryDirectory)));
    }

    @Test
    @DisplayName("Inform fails when binary resource is missing")
    void informFailsWhenBinaryResourceIsMissing() {
        final Map<String, String> args = new HashMap<>();
        args.put("binaryResource", "missing.radixor.gz");
        final RadixorFilterFactory factory = new RadixorFilterFactory(args);

        assertThrows(IOException.class,
                () -> factory.inform(TestSupport.fileSystemResourceLoader(this.temporaryDirectory)));
    }

    @Test
    @DisplayName("Integrates through Lucene SPI with default configuration")
    void integratesThroughLuceneSpiWithDefaultConfiguration() throws IOException {
        try (CustomAnalyzer analyzer = CustomAnalyzer.builder().withTokenizer("standard").addTokenFilter("radixor")
                .build()) {
            assertNotNull(TestSupport.analyze(analyzer, "alpha beta"));
        }
    }

    /**
     * Provider without a public no-argument constructor.
     */
    public static final class NoDefaultConstructorProvider implements RadixorStemmerProvider {

        private NoDefaultConstructorProvider(final String ignored) {
        }

        @Override
        public RadixorLuceneStemmer createStemmer(final ResourceLoader loader) throws IOException {
            return term -> term;
        }
    }
}
