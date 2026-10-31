# TEST_REPORT — Photo Cleaner

**Date:** 2026-10-31

## Unit tests present

`app/src/test/java/com/photocleaner/app/GroupProtectionUseCaseTest.kt`

| Test | Purpose | Expected |
|------|---------|----------|
| `cannotMarkLastKeepAsDelete` | Reject deleting the sole KEEP photo | Failure / wouldLeave = true |
| `canChangeKeepWhenAnotherExists` | Allow reassignment when ≥2 KEEP | Success, keepCount becomes 1 |
| `markAllSuggestedStillKeepsOne` | “Select all delete” still leaves one KEEP | keepCount = 1 |
| `selectBestPrefersHigherResolution` | Best-photo heuristic | Highest resolution wins |
| `validateRejectsGroupsWithoutKeep` | Pre-delete validation | Invalid result |

These tests exercise the domain rules that prevent data loss. They do **not** require an Android device.

## Tests not yet written / not executed on device

- End-to-end scan of a folder with known duplicates
- Actual move to permanent trash + restore round-trip
- Permission denial / storage full / unreadable file handling
- Performance / memory with 4k–20k images
- UI instrumentation (Compose)

## How to run

```bash
./gradlew test
```

CI also runs `./gradlew test` (non-blocking on failure for the first iterations).

## Conclusion

Core safety logic is covered by unit tests. Full product validation requires a real device or emulator with a photo library — that step is still pending.
