# Security

KissBinary treats binary input as untrusted. The core security model is explicit validation before reads or allocation.

## Input Safety

- Validate magic bytes and versions before processing file content.
- Check remaining bytes before every read.
- Reject negative counts.
- Validate array counts against available data before allocation.
- Throw `BinaryFormatException` for malformed binary data instead of silently truncating or substituting defaults.

## Dependency Policy

The production artifact has zero external dependencies. JUnit, JMH, build plugins, and release plugins are test, benchmark, or build-time tooling only.

## Reporting

Report vulnerabilities privately through GitHub Security Advisories when possible. Do not open a public issue for an undisclosed vulnerability.

See the repository [security policy](https://github.com/arthurhoch/kiss-binary/blob/main/SECURITY.md).
