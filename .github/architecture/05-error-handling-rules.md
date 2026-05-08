# 05 — Error Handling Rules

This document defines the rules for error messages and exception handling in KissBinary.

## Exception Hierarchy

```
RuntimeException
  └── BinaryException           — invalid arguments, unsupported operations
        └── BinaryFormatException  — malformed data, EOF, bounds, validation
```

All exceptions are unchecked. Binary IO errors are typically unrecoverable.

## Include File Offset

When the error occurs during a read operation, include the current offset:

```
Unexpected EOF at offset 42: required 4 bytes for int, but 1 byte remaining
```

```
Invalid magic at offset 0: expected [0x4B, 0x42], actual [0x4A, 0x53]
```

This makes errors immediately actionable — the user knows exactly where the problem is.

## Include Expected vs Actual

For validation errors, always include both values:

- Magic mismatch: expected bytes (hex) vs actual bytes (hex).
- Version mismatch: expected version vs actual version.
- Boolean byte: expected 0 or 1 vs actual byte value.

```
Invalid version at offset 2: expected 1, actual 3
```

## Never Swallow Parse Corruption

If data is unexpected, fail immediately and loudly:

- Do not skip bytes and continue reading.
- Do not substitute default values.
- Do not wrap corruption in a generic exception.
- Do not log and continue.

## Never Throw Vague RuntimeException

- Always use `BinaryException` or `BinaryFormatException`.
- Always include context in the message.
- Never throw `RuntimeException` with a generic message like "read failed".

## Error Message Format

Use a consistent format:

```
<error description> at offset <N>: <details>
```

Examples:

```
Unexpected EOF at offset 42: required 4 bytes for int, but 1 byte remaining
Array count exceeds remaining bytes at offset 7: count=1000, available=50
Invalid boolean at offset 10: expected 0 or 1, got 5
```

For non-offset errors (e.g., invalid arguments):

```
Array count must be non-negative: got -1
Byte array must not be null
```

## Specific Error Categories

| Category | Required Context |
|----------|-----------------|
| EOF / truncated | Offset, bytes required, bytes remaining |
| Magic mismatch | Offset, expected bytes (hex), actual bytes (hex) |
| Version mismatch | Offset, expected version, actual version |
| Bounds violation | Offset, bytes required, bytes remaining |
| Invalid boolean | Offset, actual byte value |
| Negative count | Offset, actual count value |
| Count exceeds remaining | Offset, count, bytes available |
| Null argument | Argument name |
| Invalid argument | Argument name, actual value, expected constraint |
| Closed reader | None (state error) |
| Offset out of range (mmap) | Requested offset, file size |

## Keep Errors Simple

1. One exception per error. No nested exception chains for format errors.
2. Human-readable messages. No cryptic codes.
3. Copy-paste friendly. Users should be able to search for the message.
4. No stack trace walking in error messages.
5. No internal class names in error messages.
