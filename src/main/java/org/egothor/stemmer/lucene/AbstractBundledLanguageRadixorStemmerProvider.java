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

import org.apache.lucene.util.ResourceLoader;
import org.egothor.stemmer.FrequencyTrie;
import org.egothor.stemmer.ReductionMode;
import org.egothor.stemmer.StemmerPatchTrieLoader;

/**
 * Base class for providers loading one of Radixor's bundled dictionary
 * resources.
 *
 * <p>
 * The provider compiles the bundled dictionary to a runtime patch-command trie
 * on initialization.
 */
public abstract class AbstractBundledLanguageRadixorStemmerProvider extends AbstractPatchTrieRadixorStemmerProvider {

    /**
     * Creates a new provider.
     */
    protected AbstractBundledLanguageRadixorStemmerProvider() {
        super();
    }

    /**
     * Returns the bundled language dictionary to load.
     *
     * @return bundled language dictionary
     */
    protected abstract StemmerPatchTrieLoader.Language language();

    /**
     * Returns whether canonical stem forms should also be stored under the no-op
     * patch command.
     *
     * @return {@code true} when original forms should be stored as explicit no-op
     *         mappings
     */
    protected boolean storeOriginal() {
        return true;
    }

    /**
     * Returns the reduction mode used when compiling the bundled dictionary.
     *
     * @return reduction mode
     */
    protected ReductionMode reductionMode() {
        return ReductionMode.MERGE_SUBTREES_WITH_EQUIVALENT_RANKED_GET_ALL_RESULTS;
    }

    /**
     * Loads the configured bundled dictionary.
     *
     * @param loader Lucene resource loader
     * @return compiled patch-command trie
     * @throws IOException if loading fails
     */
    @Override
    protected final FrequencyTrie<String> loadTrie(final ResourceLoader loader) throws IOException {
        Objects.requireNonNull(loader, "loader");
        return StemmerPatchTrieLoader.load(language(), storeOriginal(), reductionMode());
    }
}
