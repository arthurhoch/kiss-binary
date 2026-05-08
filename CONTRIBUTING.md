# Contributing

## Before You Start

Read:

1. `AGENTS.md`
2. `PRODUCT_SPEC.md`
3. `IMPLEMENTATION_PLAN.md`
4. `API_DESIGN.md`
5. `.github/architecture/index.md`

## Build

```bash
mvn -B verify
```

Run this before claiming work is complete. (Build will work once implementation exists.)

## Rules

- Keep zero production dependencies.
- Do not add frameworks.
- Preserve Java 17 compatibility.
- No object serialization, reflection-based mapping, or schema engines.
- Add or update tests for behavior changes.
- Update docs for public behavior changes.
- Update examples for public API changes.
- Update `CHANGELOG.md` under `Unreleased`.
- Do not commit `target/`, IDE files, logs, `.DS_Store`, local env files, or generated build output.
- Follow `AGENTS.md` and the architecture docs.

## Dependency Changes

Any production dependency requires:

1. justification in `CHANGELOG.md`;
2. an update to `MAVEN_CENTRAL.md` and `PRODUCT_SPEC.md`;
3. an update to docs if public behavior changes;
4. tests proving the dependency is necessary.

The default answer should be no.

## Performance Claims

- Benchmark with JMH before making performance claims.
- Include raw results, environment details, and methodology.
- Do not claim benchmarks exist if they have not been run.
- See `PERFORMANCE.md` for benchmark plan.

## Documentation

- Documentation is required for all public API changes.
- Examples are required for all public API changes.
- Keep examples consistent across `EXAMPLES.md`, `README.md`, and `docs/`.
- Use English for all documentation.

## Style

- Simple, readable code over clever code.
- Every exception must be informative with context.
- Prefer explicit names over clever abstractions.
- Prefer small classes with clear responsibilities.
- When in doubt, choose the simpler solution.
