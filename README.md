<img src="Radixor-Lucene.jpg" width="100%" align="left" alt="Radixor logo" />

[![Quality gates](https://github.com/leogalambos/radixor-bridge/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/leogalambos/radixor-bridge/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.egothor/radixor-bridge)](https://central.sonatype.com/artifact/org.egothor/radixor-bridge)
[![License](https://img.shields.io/github/license/leogalambos/radixor%2dbridge)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-brightgreen)](#)
[![Lucene](https://img.shields.io/badge/Lucene-10.4.0-blue)](#)

*Apache Lucene integration module for Radixor stemming.*

**Radixor Bridge** is a dedicated bridge module that adapts **Radixor** to **Apache Lucene 10**.
It builds on the Radixor runtime model of compiled patch-command tries and applies those transformations
inside Lucene token streams through a focused integration layer rather than by introducing Lucene-specific concerns into the core project.

It is particularly well suited to systems that need stemming which is:

- fast in Lucene analysis pipelines,
- operationally simple to package and configure,
- explicit about model loading,
- deterministic in token transformation behavior,
- aligned with Radixor’s compiled trie runtime rather than a second stemming implementation.

## Table of Contents

- [Why Radixor Bridge](#why-radixor-lucene)
- [Architecture](#architecture)
- [Key features](#key-features)
- [Coordinates](#coordinates)
- [Usage](#usage)
- [Configuration model](#configuration-model)
- [Provider model](#provider-model)
- [Documentation](#documentation)
- [Project philosophy](#project-philosophy)
- [License](#license)

## Why Radixor in Lucene

The core Radixor project should remain focused on stemming itself: dictionary loading, patch-command encoding and application, compiled trie persistence, and deterministic runtime lookup.

The bridge therefore exists as a separate artifact so that:

- `org.egothor:radixor` remains free of Lucene as an incoming dependency,
- Lucene-facing code can evolve on the cadence of Lucene integration needs,
- downstream search systems can depend on a clean adapter module,
- the stemming runtime remains shared rather than reimplemented.

That separation mirrors the general Radixor philosophy of keeping the practical runtime path small, explicit, and operationally predictable.

## Architecture

The integration is intentionally narrow.

- `RadixorFilter` applies stemming to `CharTermAttribute` values.
- `RadixorFilterFactory` exposes the Lucene SPI integration point.
- `RadixorPatchTrieStemmer` performs the actual runtime operation by looking up a patch command in `FrequencyTrie<String>` and applying it through `PatchCommandEncoder.apply(...)`.
- factory configuration can load bundled dictionaries, textual classpath dictionaries, compressed binary classpath resources, compressed binary filesystem artifacts, or an advanced custom provider.

Tokens marked with `KeywordAttribute` are preserved unchanged. Missing keys, malformed patches, empty patch results, or identity transforms preserve the original token rather than introducing surprising behavior into analysis chains.

## Key features

- Dedicated Apache Lucene 10 adapter module
- `TokenFilter`, `TokenFilterFactory`, and `Analyzer` support
- Lucene SPI / `CustomAnalyzer` integration
- Runtime stemming through Radixor patch-command tries
- Ergonomic multi-language configuration through a simple `language` parameter
- Direct activation from textual classpath dictionaries and binary Radixor artifacts
- Advanced custom provider support for controlled deployments
- Keyword token preservation through `KeywordAttribute`
- Deterministic fallback behavior for unknown tokens and identity patches
- Professional JUnit 5 coverage for filter and factory behavior

## Coordinates

```gradle
dependencies {
    implementation("org.egothor:radixor:2.0.0")
    implementation("org.egothor:radixor-bridge:2.0.0")
}
```

## Usage

### Direct analyzer wiring

```java
import org.apache.lucene.analysis.Analyzer;
import org.egothor.stemmer.lucene.RadixorAnalyzer;
import org.egothor.stemmer.lucene.RadixorPatchTrieStemmer;

Analyzer analyzer = new RadixorAnalyzer(
        RadixorPatchTrieStemmer.forDefaultBundledLanguage());
```

### Lucene SPI / `CustomAnalyzer` with the default language

When no explicit source is configured, the filter uses Radixor's bundled `US_UK_PROFI` language model.

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("radixor")
        .build();
```

### Lucene SPI / `CustomAnalyzer` with an explicit bundled language

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("radixor", "language", "DE_DE")
        .build();
```

### Lucene SPI / `CustomAnalyzer` with a textual classpath dictionary resource

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("radixor", "resource", "custom/de_de/stemmer")
        .build();
```

### Lucene SPI / `CustomAnalyzer` with a compressed binary classpath resource

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("radixor", "binaryResource", "custom/de_de.radixor.gz")
        .build();
```

### Lucene SPI / `CustomAnalyzer` with a compressed binary filesystem artifact

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter("radixor", "binaryPath", "/opt/radixor/de_de.radixor.gz")
        .build();
```

### Advanced custom provider override

```java
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.egothor.stemmer.lucene.EnglishBundledRadixorStemmerProvider;

Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer("standard")
        .addTokenFilter(
                "radixor",
                "provider", EnglishBundledRadixorStemmerProvider.class.getName())
        .build();
```

## Configuration model

The filter supports the following mutually exclusive activation modes:

- `language`  
  Loads one of the bundled `StemmerPatchTrieLoader.Language` values such as `US_UK_PROFI`, `DE_DE`, or `FR_FR`.

- `resource`  
  Loads and compiles a textual Radixor dictionary from a classpath resource made available to Lucene's `ResourceLoader`.

- `binaryResource`  
  Loads a precompiled compressed binary trie from a classpath resource.

- `binaryPath`  
  Loads a precompiled compressed binary trie from the filesystem.

- `provider`  
  Advanced override for a custom `RadixorStemmerProvider` implementation.

The precedence model is intentionally simple: normal deployments should configure exactly one explicit source, or none at all. If none is provided, `US_UK_PROFI` is used by default.

## Provider model

The project still supports provider classes for cases where a deployment wants stronger encapsulation around resource choice or custom bootstrap logic.

The repository includes:

- `IdentityRadixorStemmerProvider` for smoke testing and diagnostics,
- `EnglishBundledRadixorStemmerProvider` as a simple convenience provider for the bundled `US_UK_PROFI` dictionary,
- abstract support classes for custom bundled-language and binary-resource providers.

For ordinary Lucene configuration, however, a custom provider class is no longer required.

## Documentation

The repository keeps the front page concise and places the important operational details directly in code and JavaDoc.

### Main entry points

- `RadixorFilter`
  Lucene token filter applying Radixor stemming.

- `RadixorFilterFactory`
  SPI factory used by Lucene `CustomAnalyzer` and other analysis wiring.

- `RadixorPatchTrieStemmer`
  Concrete runtime adapter between `FrequencyTrie<String>` patch data and Lucene token terms.

- `RadixorAnalyzer`
  Minimal direct analyzer adapter for programmatic use.

## Project philosophy

Radixor Bridge does not try to turn the Lucene module into a second stemming engine.

It keeps the valuable split:

- Radixor handles stemming data, compilation, persistence, and patch semantics.
- Radixor Bridge handles Lucene integration and token-stream behavior.

That way the bridge stays small, understandable, and operationally robust while still being practical for multi-language deployments.

## License

Radixor Bridge uses the same BSD-3-Clause style license as the upstream Radixor project.
