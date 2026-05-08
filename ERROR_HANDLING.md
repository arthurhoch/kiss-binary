# KissBinary — Error Handling

**Status: Initial implementation complete.**

## Error Model

KissBinary uses unchecked exceptions. Binary IO errors are typically unrecoverable and should not require `try/catch` boilerplate for normal control flow.

All exceptions inherit from `BinaryException`.

## Exception Hierarchy

```
RuntimeException
  └── BinaryException          — base exception for all library errors
        └── BinaryFormatException  — data-specific errors
```

### BinaryException

Base exception for all KissBinary errors. Used for:

- Invalid arguments passed to API methods.
- Unsupported operations.
- Configuration errors.

### BinaryFormatException

Exception for malformed, truncated, or unexpected binary data. Used for:

- EOF / truncated files.
- Bounds violations.
- Magic byte mismatches.
- Version mismatches.
- Corrupt or unexpected data.

## Error Messages

All error messages must be human-readable and include relevant context.

### EOF / Truncated File Errors

```
Unexpected EOF at offset 42: required 4 bytes for int, but 1 byte remaining
```

Must include:
- Current offset.
- Number of bytes required.
- Number of bytes remaining.

### Magic Byte Mismatch

```
Invalid magic at offset 0: expected [0x4B, 0x42], actual [0x4A, 0x53]
```

Must include:
- Offset where magic was read.
- Expected magic bytes (hex).
- Actual magic bytes (hex).

### Version Mismatch

```
Invalid version at offset 2: expected 1, actual 3
```

Must include:
- Offset where version was read.
- Expected version.
- Actual version.

### Bounds Errors

```
Read past end of buffer at offset 56: required 8 bytes for long, but 4 bytes remaining
```

Must include:
- Current offset.
- Number of bytes required for the operation.
- Number of bytes remaining.

### Invalid Argument Errors

```
Array count must be non-negative: got -1
```

```
Array count exceeds remaining bytes: count=1000, available=50
```

Must include:
- The invalid value.
- What was expected.

### Unsupported Format Errors

```
Unsupported boolean value at offset 10: expected 0 or 1, got 5
```

## Design Rules

1. **Include file offset** where the error was detected, when available.
2. **Include expected vs actual** for validation errors (magic, version, count).
3. **Never swallow parse corruption.** If the data is wrong, fail loudly.
4. **Never throw vague `RuntimeException`.** Always use `BinaryException` or `BinaryFormatException`.
5. **Never silently truncate.** If there are not enough bytes, throw.
6. **Never hide IO failure context.** Public APIs use unchecked `BinaryException`; methods that write to an `OutputStream` or open a mapped file wrap the `IOException` with a clear operation-specific message and preserve it as the cause.
7. **Keep error messages simple.** No stack trace walking, no nested exception chains for normal binary format errors.

## Error Examples by Category

| Category | Exception | Typical Message |
|----------|-----------|-----------------|
| EOF while reading int | `BinaryFormatException` | `Unexpected EOF at offset 42: required 4 bytes for int, but 1 byte remaining` |
| Magic mismatch | `BinaryFormatException` | `Invalid magic at offset 0: expected [0x4B, 0x42], actual [0x4A, 0x53]` |
| Version mismatch | `BinaryFormatException` | `Invalid version at offset 2: expected 1, actual 3` |
| Negative array count | `BinaryFormatException` | `Array count must be non-negative: got -1` |
| Array exceeds buffer | `BinaryFormatException` | `Array count exceeds remaining: count=1000, available=50` |
| Invalid boolean byte | `BinaryFormatException` | `Invalid boolean at offset 10: expected 0 or 1, got 5` |
| Null argument | `NullPointerException` | `Byte array must not be null` |
| Output stream write failure | `BinaryException` | `Write failed` |
| Offset out of range (mmap) | `BinaryFormatException` | `At offset 1000: Offset out of range: offset=1000, required=4, fileSize=500` |

## How Errors Should Remain Useful

1. **Copy-paste friendly**: Error messages should be searchable.
2. **Actionable**: The message should tell the user what to check.
3. **Consistent**: Same format for the same category of error.
4. **No unnecessary nesting**: One exception, one clear message.
5. **Context-rich but concise**: Include what went wrong, where, and what was expected.
