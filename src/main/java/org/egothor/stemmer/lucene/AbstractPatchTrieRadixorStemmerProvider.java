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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.util.ResourceLoader;
import org.egothor.stemmer.FrequencyTrie;

/**
 * Base class for providers that create a {@link RadixorPatchTrieStemmer} from a
 * loaded compiled patch-command trie.
 */
public abstract class AbstractPatchTrieRadixorStemmerProvider implements RadixorStemmerProvider {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractPatchTrieRadixorStemmerProvider.class.getName());

    /**
     * Creates a new provider.
     */
    protected AbstractPatchTrieRadixorStemmerProvider() {
        super();
    }

    /**
     * Loads the compiled patch-command trie required by this provider.
     *
     * @param loader Lucene resource loader
     * @return loaded trie
     * @throws IOException if loading fails
     */
    protected abstract FrequencyTrie<String> loadTrie(ResourceLoader loader) throws IOException;

    /**
     * Creates a Lucene-facing trie-backed stemmer.
     *
     * @param loader Lucene resource loader
     * @return initialized trie-backed stemmer
     * @throws IOException if loading fails
     */
    @Override
    public final RadixorLuceneStemmer createStemmer(final ResourceLoader loader) throws IOException {
        Objects.requireNonNull(loader, "loader");

        final FrequencyTrie<String> trie = loadTrie(loader);

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "Initialized trie-backed Radixor Lucene stemmer provider {0}.",
                    getClass().getName());
        }

        return new RadixorPatchTrieStemmer(trie);
    }
}
