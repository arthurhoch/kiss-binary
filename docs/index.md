---
layout: default
title: KissBinary
---

<section class="hero">
  <div>
    <p class="eyebrow">KISS Java Libraries</p>
    <h1>KissBinary</h1>
    <p class="lead">Tiny zero-dependency Java 17+ binary IO for explicit primitive formats, memory-mapped reads, header validation, and predictable format errors.</p>
    <div class="meta-row">
      <span class="tag">Latest stable: 0.1.0</span>
      <span class="tag">Java 17+</span>
      <span class="tag">Apache-2.0</span>
    </div>
    <div class="actions">
      <a class="button" href="getting-started.html">Getting Started</a>
      <a class="button secondary" href="api-overview.html">API Overview</a>
      <a class="button secondary" href="https://github.com/arthurhoch/kiss-binary">GitHub</a>
    </div>
  </div>
  <div class="panel">
    <p class="panel-title">Maven</p>
<pre><code>&lt;dependency&gt;
  &lt;groupId&gt;io.github.arthurhoch&lt;/groupId&gt;
  &lt;artifactId&gt;kiss-binary&lt;/artifactId&gt;
  &lt;version&gt;0.1.0&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
  </div>
</section>

<section class="section two-column">
  <div>
    <h2>Small Surface</h2>
    <p>KissBinary reads and writes explicit binary data without object serialization, schema compilers, IDL tooling, reflection mapping, or runtime dependencies.</p>
  </div>
  <div class="panel">
    <p class="panel-title">Quick Example</p>
<pre><code>BinaryWriter writer = BinaryWriter.create();
writer.writeMagic("KB");
writer.writeVersion(1);
writer.writeInt(42);

BinaryReader reader = BinaryReader.from(writer.toByteArray());
reader.expectMagic("KB");
reader.expectVersion(1);</code></pre>
  </div>
</section>

<section class="section">
  <h2>KISS Principles</h2>
  <div class="feature-grid">
    <article class="feature">
      <h3>Explicit Format</h3>
      <p>Magic bytes, versions, endianness, primitive values, and arrays stay under caller control.</p>
    </article>
    <article class="feature">
      <h3>Bounds Checked</h3>
      <p>Malformed input fails with format exceptions instead of silent truncation or hidden defaults.</p>
    </article>
    <article class="feature">
      <h3>Measured Paths</h3>
      <p>JMH benchmarks cover scalar IO, arrays, memory-mapped reads, and Rinha-shaped access patterns.</p>
    </article>
  </div>
</section>

<section class="section">
  <h2>Documentation</h2>
  <div class="doc-grid">
    <a href="getting-started.html">Getting Started<span>Install and write the first binary payload.</span></a>
    <a href="api-overview.html">API Overview<span>Reader, writer, mapped reader, and exceptions.</span></a>
    <a href="skills/index.html">AI Skills<span>Versioned Markdown skill files for AI-assisted usage.</span></a>
    <a href="examples.html">Examples<span>Copyable binary IO examples.</span></a>
    <a href="binary-format-design.html">Binary Format Design<span>Format rules and safety expectations.</span></a>
    <a href="performance-notes.html">Performance Notes<span>Measured behavior and benchmark guidance.</span></a>
    <a href="rinha-dataset-benchmark.html">Rinha Dataset Benchmark<span>Dataset benchmark notes.</span></a>
    <a href="security.html">Security<span>Input validation and dependency policy.</span></a>
    <a href="security-hardening.html">Security Hardening<span>Repository hardening and local quality commands.</span></a>
    <a href="code-cleanup.html">Safe Code Cleanup<span>Deletion policy and quality gates.</span></a>
    <a href="testing-report.html">Testing Report<span>Current verification state.</span></a>
    <a href="release.html">Release<span>Release process and Maven Central flow.</span></a>
    <a href="maven-central.html">Maven Central<span>Publishing and coordinates.</span></a>
  </div>
</section>

<section class="section">
  <h2>Related Projects</h2>
  <div class="related-grid">
    <a href="https://github.com/arthurhoch/kiss-json">kiss-json<span>Field-based JSON serialization and deserialization.</span></a>
    <a href="https://github.com/arthurhoch/kiss-requests">kiss-requests<span>Simple HTTP client built on Java HttpClient.</span></a>
    <a href="https://github.com/arthurhoch/kiss-server">kiss-server<span>Small HTTP/1.1 server for simple REST-style applications.</span></a>
    <a href="https://github.com/arthurhoch/kiss-config">kiss-config<span>Configuration from properties, .env, system properties, and environment variables.</span></a>
    <a href="https://github.com/arthurhoch/kiss-binary">kiss-binary<span>Explicit binary IO for primitive binary formats.</span></a>
  </div>
</section>
