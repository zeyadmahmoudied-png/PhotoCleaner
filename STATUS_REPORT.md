# STATUS REPORT — Photo Cleaner (Safety Phase)

**Date:** 2026-10-31  
**Repo:** https://github.com/zeyadmahmoudied-png/PhotoCleaner

## Completed in this phase

### Safety architecture
- `AppMode.SCAN_ONLY` (default) vs `AppMode.CLEAN` (explicit opt-in)
- No automatic deletion during scan or similarity analysis
- `GroupProtectionUseCase` still prevents deleting the last copy (unit-tested)
- `SafeDeleteUseCase` blocks execution unless mode is CLEAN + user confirmed + groups valid

### MediaStore scanner (read-only)
- `MediaStoreScanner` lists images via MediaStore
- Progressive Flow with batch progress
- Optional SHA-256 + dHash computation
- Cancellable; does not write or delete any media

### Permanent trash (core path)
- `TrashRepository.moveToTrash`:
  1. Copy file into `filesDir/permanent_trash/`
  2. Verify size
  3. Verify SHA-256 against original hash when available
  4. Persist metadata in Room
  5. Only then attempt MediaStore delete of original
- If copy/verify fails → original is left untouched
- `restore()` re-inserts into MediaStore and removes trash copy only after success
- `deletePermanently()` is explicit and manual only

### UI
- New Scan tab with mode indicator, progress, start/stop
- Explicit switch to CLEAN mode (never automatic)

### Tests
- Existing GroupProtection tests
- New SafeDeleteUseCase planning tests

## Still incomplete

- Side-by-side zoom comparison UI
- Video scanning
- Blur/screenshot detectors
- SAF export of trash
- Full backup/restore
- Device instrumented tests
- Production signing

## Safety guarantees in code

1. Default mode = SCAN_ONLY → no delete path runs.
2. Trash copy is verified before original delete is attempted.
3. Domain rules refuse groups with zero KEEP.
4. No automatic emptying of trash.
5. No automatic permanent delete.

## Residual risks

- MediaStore delete on Android 10+ may need user confirmation; original may remain if permission denied (copy stays safe).
- Uninstalling the app deletes app-private trash — export backups first (export UI not finished).
