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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.PatchCommandEncoder;
import org.egothor.stemmer.StemmerDictionaryParser;
import org.egothor.stemmer.StemmerPatchTrieLoader;

/**
 * Lucene {@link TokenFilterFactory} for {@link RadixorFilter}.
 *
 * <p>
 * The factory is intentionally ergonomic for common deployments. Most users do
 * not need to implement a custom {@link RadixorStemmerProvider}; they can
 * configure the filter directly with one of the supported source parameters:
 *
 * <ul>
 * <li>{@code language} for a bundled Radixor dictionary,</li>
 * <li>{@code resource} for a textual classpath dictionary resource,</li>
 * <li>{@code binaryResource} for a compressed binary classpath trie,</li>
 * <li>{@code binaryPath} for a compressed binary filesystem trie,</li>
 * <li>{@code provider} for an advanced custom provider implementation.</li>
 * </ul>
 *
 * <p>
 * If no explicit source is configured, the factory falls back to
 * {@link StemmerPatchTrieLoader.Language#US_UK_PROFI}.
 */
public final class RadixorFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

    /**
     * SPI name of this filter.
     */
    public static final String NAME = "radixor";

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(RadixorFilterFactory.class.getName());

    /**
     * Advanced override provider class name.
     */
    private final String providerClassName;

    /**
     * Bundled language dictionary identifier.
     */
    private final StemmerPatchTrieLoader.Language language;

    /**
     * Textual dictionary classpath resource path.
     */
    private final String resourcePath;

    /**
     * Binary trie classpath resource path.
     */
    private final String binaryResourcePath;

    /**
     * Binary trie filesystem path.
     */
    private final String binaryFilePath;

    /**
     * Initialized stemmer instance.
     */
    private volatile RadixorLuceneStemmer stemmer; // NOPMD

    /**
     * Public no-argument constructor required for Lucene SPI discovery.
     */
    public RadixorFilterFactory() {
        super(Collections.emptyMap());
        this.providerClassName = null;
        this.language = RadixorPatchTrieStemmer.DEFAULT_LANGUAGE;
        this.resourcePath = null;
        this.binaryResourcePath = null;
        this.binaryFilePath = null;
    }

    /**
     * Creates the factory from Lucene analysis arguments.
     *
     * @param args factory arguments
     */
    public RadixorFilterFactory(final Map<String, String> args) {
        super(args);
        this.providerClassName = get(args, "provider");
        this.resourcePath = get(args, "resource");
        this.binaryResourcePath = get(args, "binaryResource");
        this.binaryFilePath = get(args, "binaryPath");
        this.language = parseLanguage(get(args, "language"));
        validateSourceSelection();
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    /**
     * Initializes the factory with Lucene's resource loader.
     *
     * @param loader Lucene resource loader
     * @throws IOException if stemmer initialization fails
     */
    @Override
    public void inform(final ResourceLoader loader) throws IOException {
        ResourceLoader resourceLoader = Objects.requireNonNull(loader, "loader");
        initializeStemmerIfNeeded(resourceLoader);
    }

    /**
     * Creates the filter instance.
     *
     * @param input upstream token stream
     * @return configured Radixor filter
     */
    @Override
    public TokenStream create(final TokenStream input) {
        Objects.requireNonNull(input, "input");
        final RadixorLuceneStemmer localStemmer = this.stemmer;
        if (localStemmer == null) {
            throw new IllegalStateException(
                    "RadixorFilterFactory was not initialized. Lucene did not call inform(ResourceLoader).");
        }
        return new RadixorFilter(input, localStemmer);
    }

    /**
     * Initializes the configured stemmer once.
     *
     * @param loader resource loader to use
     * @throws IOException if initialization fails
     */
    private synchronized void initializeStemmerIfNeeded(final ResourceLoader loader) throws IOException { // NOPMD
        if (this.stemmer != null) {
            return;
        }

        this.stemmer = createStemmer(loader);

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO,
                    "Initialized Radixor Lucene filter factory. provider={0}, language={1}, resource={2}, binaryResource={3}, binaryPath={4}.",
                    new Object[] { this.providerClassName, this.language, this.resourcePath, this.binaryResourcePath,
                            this.binaryFilePath });
        }
    }

    /**
     * Creates the configured stemmer.
     *
     * @param loader Lucene resource loader
     * @return initialized stemmer
     * @throws IOException if initialization fails
     */
    private RadixorLuceneStemmer createStemmer(final ResourceLoader loader) throws IOException {
        if (this.providerClassName != null) {
            return instantiateProvider(loader, this.providerClassName).createStemmer(loader);
        }
        if (this.binaryFilePath != null) {
            return RadixorPatchTrieStemmer.fromBinary(Path.of(this.binaryFilePath));
        }
        if (this.binaryResourcePath != null) {
            try (InputStream inputStream = loader.openResource(this.binaryResourcePath)) {
                return new RadixorPatchTrieStemmer(StemmerPatchTrieLoader.loadBinary(inputStream));
            }
        }
        if (this.resourcePath != null) {
            return new RadixorPatchTrieStemmer(loadTextDictionaryResource(loader, this.resourcePath));
        }
        return RadixorPatchTrieStemmer.forBundledLanguage(this.language);
    }

    /**
     * Loads and compiles a textual dictionary resource through Lucene's resource
     * loader.
     *
     * @param loader Lucene resource loader
     * @param path   textual resource path
     * @return compiled patch-command trie
     * @throws IOException if loading or parsing fails
     */
    private static FrequencyTrie<String> loadTextDictionaryResource(final ResourceLoader loader, final String path)
            throws IOException {
        try (InputStream inputStream = loader.openResource(path);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            final FrequencyTrie.Builder<String> builder = new FrequencyTrie.Builder<>(String[]::new,
                    RadixorPatchTrieStemmer.DEFAULT_REDUCTION_MODE);
            final PatchCommandEncoder encoder = new PatchCommandEncoder();

            StemmerDictionaryParser.parse(reader, path, (stem, variants, lineNumber) -> {
                builder.put(stem, encoder.encode(stem, stem));
                for (String variant : variants) {
                    if (!variant.equals(stem)) {
                        builder.put(variant, encoder.encode(variant, stem));
                    }
                }
            });

            return builder.build();
        }
    }

    /**
     * Parses an optional bundled language argument.
     *
     * @param value raw argument value
     * @return configured language, or the default language if the argument is
     *         absent
     */
    private static StemmerPatchTrieLoader.Language parseLanguage(final String value) {
        if (value == null || value.isBlank()) {
            return RadixorPatchTrieStemmer.DEFAULT_LANGUAGE;
        }
        return StemmerPatchTrieLoader.Language.valueOf(value.trim());
    }

    /**
     * Validates that at most one stemmer source selection strategy is configured.
     */
    private void validateSourceSelection() {
        int explicitSourceCount = 0;
        if (this.providerClassName != null) {
            explicitSourceCount++;
        }
        if (this.resourcePath != null) {
            explicitSourceCount++;
        }
        if (this.binaryResourcePath != null) {
            explicitSourceCount++;
        }
        if (this.binaryFilePath != null) {
            explicitSourceCount++;
        }
        if (explicitSourceCount > 1) { // NOPMD
            throw new IllegalArgumentException(
                    "Only one of provider, resource, binaryResource, or binaryPath may be specified.");
        }
        if (this.language != RadixorPatchTrieStemmer.DEFAULT_LANGUAGE && explicitSourceCount > 0) {
            throw new IllegalArgumentException(
                    "language cannot be combined with provider, resource, binaryResource, or binaryPath.");
        }
    }

    /**
     * Instantiates the configured provider through Lucene's resource loader.
     *
     * @param loader    Lucene resource loader
     * @param className provider class name
     * @return provider instance
     * @throws IOException if provider loading or instantiation fails
     */
    private static RadixorStemmerProvider instantiateProvider(final ResourceLoader loader, final String className)
            throws IOException {
        final Class<? extends RadixorStemmerProvider> providerClass;
        try {
            providerClass = loader.findClass(className, RadixorStemmerProvider.class);
        } catch (RuntimeException exception) { // NOPMD
            throw new IOException("Unable to load Radixor stemmer provider class: " + className, exception);
        }

        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IOException("Unable to instantiate Radixor stemmer provider: " + className, exception);
        }
    }
}
