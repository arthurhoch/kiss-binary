# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Planning (not yet released) |

KissBinary has not been published to Maven Central yet. Security support applies to released versions once artifacts are available.

## Reporting a Vulnerability

Use GitHub Security Advisories when possible. Do not open a public issue for an undisclosed vulnerability.

Include:

- affected version or commit;
- reproduction steps;
- expected and actual behavior;
- impact and suggested mitigation if known.

## Zero Production Dependency Policy

KissBinary has zero production dependencies. Test dependencies (JUnit 5) and build plugins do not ship in the published artifact.

This means:

- The attack surface from transitive dependencies is zero.
- No third-party library vulnerabilities can affect production users.
- The only code running in production is KissBinary code and the Java standard library.

This policy must be maintained for all releases. Adding any production dependency requires explicit approval and a security review.

## Binary Parsing Security

Binary parsing introduces specific security concerns that KissBinary must address:

### Malformed Input

All binary input must be treated as untrusted. The library must validate:

- Magic bytes before processing file content.
- Version numbers before interpreting format details.
- Sizes, counts, and offsets before allocating memory or arrays.
- Remaining bytes before reading primitives or arrays.

### Size and Count Validation

- Reject negative counts or sizes.
- Reject counts that exceed remaining bytes.
- Cap maximum array sizes to prevent denial-of-service via oversized allocation.
- Do not allocate based on untrusted header values without bounds checking.

### Denial-of-Service Considerations

- Memory-mapped files must not grow unbounded.
- Array reads must validate count against available data before allocation.
- File header validation must happen before any large allocation.
- Bounds checking must occur before reads, not after.

### No Unsafe Trust

- Do not trust `Content-Length` or declared sizes without validation.
- Do not skip magic/version validation for "trusted" files.
- Do not silently truncate on short reads.
- Do not swallow parse corruption with vague exceptions.

## Security Model

KissBinary avoids common deserialization risks by design:

- No Java object serialization (`ObjectOutputStream`, `Serializable`).
- No dynamic class loading from binary data.
- No reflection-based mapping.
- No polymorphic type metadata.
- No network access.
- No framework integration.
- No schema engine that could introduce code generation risks.

## Security Scanning

Normal build (once implementation exists):

```bash
mvn -B verify
```

Security scans (separate profile):

```bash
mvn -Psecurity verify
```

CodeQL and Dependabot will run in GitHub Actions once CI is configured.

## No Secrets

- Do not commit secrets, API keys, passwords, or credentials.
- Maven Central publication secrets are stored in GitHub repository secrets only.
- GPG private keys are stored in GitHub repository secrets only.
- The normal build must never require secrets.

## Reporting

Report security issues via GitHub Security Advisories or by contacting the maintainer.
