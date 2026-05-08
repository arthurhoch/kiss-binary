# AGENTS.md — Primary AI Agent Instructions

This is the primary instruction file for AI coding agents working on KissBinary.

## Mandatory Reading Order

Before making any changes to this project, read these files in order:

1. `CAVEMAN.md` — Compact summary
2. `AGENTS.md` — This file
3. `PRODUCT_SPEC.md` — Authoritative product specification
4. `IMPLEMENTATION_PLAN.md` — Phased implementation plan
5. `API_DESIGN.md` — Intended public API shape
6. `PERFORMANCE.md` — Performance goals and benchmark plan
7. `.github/architecture/index.md` — Architecture reading order

CAVEMAN.md is a summary only. If it conflicts with detailed docs, the detailed docs win.

## Project Purpose

KissBinary is a tiny, zero-dependency Java 17+ binary IO library focused on simple, predictable, high-performance reading and writing of primitive binary formats.

It reads and writes explicit binary data: primitives, primitive arrays, headers with magic/version validation, and memory-mapped files. It does not serialize Java objects.

## Philosophy

- **KISS**: Keep It Simple, Stupid. Small, understandable, explicit, and focused.
- **Zero dependencies**: No external libraries required.
- **Native JDK**: Built only with Java 17+ standard APIs.
- **Explicit binary formats**: Users define their own layout. The library reads and writes primitives.
- **Predictable performance**: No reflection, no object serialization, no hidden allocation, no framework overhead.
- **Rich errors**: Include file offset, expected vs actual values, and clear messages.
- **Safe defaults**: Bounds checking, safe EOF handling, no silent truncation.

## Non-Negotiable KISS Rules

1. Do one thing well: read and write primitive binary data.
2. Zero production dependencies. No exceptions.
3. Java 17+ compatibility. No preview features.
4. No Java object serialization. No `ObjectOutputStream`. No `Serializable`.
5. No reflection-based mapping.
6. No schema engine. No IDL. No code generation.
7. No framework. No annotation-driven configuration.
8. No abstraction layers beyond what is necessary for reading and writing primitives.
9. The public API must be memorable and obvious.
10. Default behavior must be safe: bounds-checked, EOF-safe, explicit endianness.

## Current Status

**Initial implementation complete.**

Java source code and tests now exist. Do not implement new code unless explicitly instructed by a human.

## v0.1.0 Scope

See `PRODUCT_SPEC.md` for the complete scope. Summary:

- `BinaryWriter` — write primitives and primitive arrays to byte streams
- `BinaryReader` — read primitives and primitive arrays from byte buffers
- `MappedBinaryReader` — memory-mapped read-only access
- Little-endian and big-endian support
- Header validation: magic bytes, version, counts
- Bounds checking and safe EOF handling
- `BinaryException` / `BinaryFormatException`
- Maven Central publishing readiness

## v0.1.0 Non-Goals

- No object serialization framework
- No schema engine or IDL
- No reflection-based mapping
- No annotation-driven configuration
- No checksum/CRC in v0.1.0 (future consideration)
- No compression support
- No encryption support
- No network IO
- No HTTP, JSON, or XML handling
- No Spring, Quarkus, or framework integrations
- No Lombok, annotation processing, or code generation

## Coding Rules

1. Java 17 source and target compatibility.
2. No production dependencies. Zero.
3. Prefer simple classes and records over complex hierarchies.
4. Prefer explicit names over clever abstractions.
5. Public API package: `io.github.arthurhoch.kissbinary` or a sub-package.
6. Internal implementation must be clearly separated from public API.
7. No Lombok, no annotation processing, no code generation.
8. No reflection-based magic.
9. Thread safety must be documented for all public classes.
10. All primitive read/write methods must accept an explicit endianness parameter or use a configured default.
11. Bounds checking must happen before reads, not after.
12. EOF must produce a clear `BinaryFormatException`, not a generic `IOException`.
13. Resources (`OutputStream`, `InputStream`, `ByteBuffer`, `FileChannel`) must be managed by the caller. The library does not close streams it did not open.

## Testing Rules

1. All tests must use JUnit 5.
2. Tests must not require network access.
3. Tests must be deterministic.
4. Every public method must have at least one test.
5. Test categories must cover: primitive roundtrip, endianness, malformed input, truncated files, array bounds, header validation, mmap where available.
6. Test failures must produce clear messages.
7. Tests must be updated alongside implementation changes.

## Documentation Rules

1. All public API changes must be reflected in documentation.
2. All public API changes must be reflected in examples.
3. Documentation must be updated when public behavior changes.
4. Every public API must have at least one copyable example.
5. README.md is the quick start.
6. `docs/` contains user-facing documentation.
7. `.github/architecture/` contains internal design rules.
8. Documentation must prioritize "can use without reading a manual."
9. All documentation must be in English.
10. Do not claim features work if they are not implemented.

## Maven Central Publishing Rules

1. Maven coordinates: `io.github.arthurhoch:kiss-binary`.
2. Publishing uses Sonatype Central Publisher Portal.
3. Source JAR and Javadoc JAR are required.
4. GPG signing is required.
5. All required metadata must be present in `pom.xml`.
6. Publishing only happens under the `release` Maven profile.
7. Publishing only happens via the release GitHub Actions workflow triggered by version tags.
8. CI must never require signing or publishing secrets.
9. Do not publish until implementation, tests, and documentation are complete.

## Release Rules

1. Semantic versioning: MAJOR.MINOR.PATCH.
2. Releases are triggered by tags matching `v*` (e.g., `v0.1.0`).
3. `CHANGELOG.md` must be updated before release.
4. The release workflow must run tests before publishing.
5. Do not release without updating documentation.

## AI Behavior Rules

1. Read `AGENTS.md`, `PRODUCT_SPEC.md`, `IMPLEMENTATION_PLAN.md`, `API_DESIGN.md`, and `PERFORMANCE.md` before making any changes.
2. Do not invent features outside v0.1.0 scope.
3. Do not add dependencies unless explicitly approved by a human.
4. Do not create a framework.
5. Do not over-engineer.
6. Do not implement code unless explicitly instructed.
7. Do not remove public API without documenting why.
8. Do not silently change behavior.
9. Always update docs and tests alongside code changes.
10. Prefer simple, explicit APIs.
11. Preserve Java 17 compatibility.
12. When in doubt, choose the simpler solution.

## Change Protocol

1. Read all required docs before starting.
2. Make the smallest correct change.
3. Add or update tests.
4. Run `mvn -B verify` for implementation changes.
5. Update documentation if public API changed.
6. Update `CHANGELOG.md` under `Unreleased`.
7. Verify no production dependencies were added.
8. Report what changed, what was verified, and what remains.

## Security Rules

1. Do not commit secrets, API keys, passwords, or credentials.
2. Do not trust file headers blindly. Validate magic, version, and sizes before allocating.
3. Do not allocate based on untrusted counts without bounds checking.
4. See `SECURITY.md` for full policy.

## Implementation Must Not

1. Add external dependencies without explicit justification and human approval.
2. Add framework patterns or integrations.
3. Break Java 17 compatibility.
4. Add object serialization, reflection-based mapping, or schema engines.
5. Make errors vague or unhelpful.
6. Skip tests for public behavior.
7. Skip documentation updates.
8. Implement code unless explicitly instructed by a human.
