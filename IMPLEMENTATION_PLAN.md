# KissBinary — Implementation Plan

**Status: Initial implementation complete; release polish remains.**

Implementation changes must still be explicitly requested by a human.

## Phase 0: Documentation and Architecture (Complete)

**Goal**: Define what KissBinary is, what it does, and how it should be built — before writing any Java code.

**Deliverables**:
- Product specification
- API design
- Architecture documentation
- Error handling model
- Performance goals
- Binary format guide
- Release and publishing guides
- Security policy
- AI agent instructions

**Exit criteria**: Initial documentation reviewed.

**Status**: Complete.

## Phase 1: Core BinaryWriter and BinaryReader

**Goal**: Implement primitive read and write operations with explicit endianness.

**Scope**:
- `BinaryWriter` with `writeByte`, `writeShort`, `writeInt`, `writeLong`, `writeFloat`, `writeDouble`, `writeChar`, `writeBoolean`
- `BinaryReader` with corresponding read methods
- `Endianness` enum (BIG_ENDIAN, LITTLE_ENDIAN)
- `BinaryFormatException` with offset and context
- Output to `byte[]` and `OutputStream`
- Input from `byte[]` and `ByteBuffer`
- Bounds checking before reads
- EOF-safe behavior

**Tests**:
- Primitive roundtrip for all types
- Endianness roundtrip (LE and BE)
- Bounds checking
- EOF behavior
- Malformed input

**Do not**:
- Add array operations yet (Phase 2).
- Add header validation yet (Phase 3).
- Add mmap yet (Phase 4).

## Phase 2: Primitive Arrays

**Goal**: Add bulk read and write for primitive arrays.

**Scope**:
- `writeByteArray`, `writeShortArray`, `writeIntArray`, `writeLongArray`, `writeFloatArray`, `writeDoubleArray`, `writeCharArray`
- Corresponding read methods with explicit count
- Bulk `ByteBuffer` operations for arrays
- Count validation (non-negative, within bounds)

**Tests**:
- Array roundtrip for all types
- Partial array reads
- Zero-length arrays
- Array bounds errors
- Large arrays

## Phase 3: Headers, Magic, and Version Validation

**Goal**: Add header validation methods for common binary file patterns.

**Scope**:
- `validateMagic(byte[])` on `BinaryReader`
- `validateVersion(int)` on `BinaryReader`
- Count/size field conventions
- Optional `BinaryHeader` helper class

**Tests**:
- Valid magic/version passes
- Invalid magic throws with expected vs actual
- Invalid version throws with expected vs actual
- Truncated header throws with offset
- Multiple files with different headers

## Phase 4: MappedBinaryReader

**Goal**: Add memory-mapped read-only access.

**Scope**:
- `MappedBinaryReader.from(Path)` factory
- Same read API as `BinaryReader` but position-based
- `readInt(offset)`, `readLong(offset)`, etc.
- Array reads at offset
- Header validation on mmap
- Resource cleanup guidance (caller-managed or try-with-resources)

**Tests**:
- Mmap roundtrip (write file, mmap, read)
- Mmap header validation
- Mmap array reads
- Mmap bounds checking
- Mmap with large files
- Platform-specific behavior documentation

## Phase 5: Benchmarks

**Goal**: Measure and document performance.

**Scope**:
- JMH benchmark suite
- Primitive read/write throughput
- Array read/write throughput
- Endianness comparison
- File IO vs memory-mapped IO
- Comparison with `DataInputStream` / `ByteBuffer` baseline
- Capture results and document methodology

**Tests**:
- Benchmark results are reproducible
- No regression from baseline

## Phase 6: First Release Polish

**Goal**: Prepare for v0.1.0 release.

**Scope**:
- Complete Javadoc for all public API
- Review all documentation for accuracy
- Review all examples for correctness
- Finalize `pom.xml` with Maven Central metadata
- GPG signing configuration
- GitHub Actions CI workflow
- Update `CHANGELOG.md`
- Set version to `0.1.0`
- Tag `v0.1.0`

**Exit criteria**:
- All tests pass
- All docs reviewed
- All examples verified
- Benchmarks captured
- No production dependencies
- Maven Central metadata correct

## Dependencies Between Phases

```
Phase 0 ──> Phase 1 ──> Phase 2 ──> Phase 3 ──> Phase 4
                                                       │
                                          Phase 5 ────┘
                                                       │
                                          Phase 6 ────┘
```

Phase 5 and 6 depend on Phase 4 being complete. Phases 1–4 are sequential.

## Rules

1. Implementation changes require explicit human instruction.
2. Each phase must have passing tests before the next phase begins.
3. No phase may add production dependencies.
4. No phase may break Java 17 compatibility.
5. Documentation must be updated at each phase boundary.
6. `CHANGELOG.md` must be updated at each phase boundary.
7. `mvn -B verify` must pass at the end of each phase.
