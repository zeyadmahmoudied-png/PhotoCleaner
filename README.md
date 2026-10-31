# Photo Cleaner

Professional Android app for detecting exact duplicate and visually similar photos, with a permanent trash, safety rules that always keep at least one copy, and fully local processing.

**Package:** `com.photocleaner.app`  
**Min SDK:** 26 · **Target SDK:** 35  
**UI:** Jetpack Compose + Material 3 · Arabic RTL + English  
**Architecture:** Clean-ish (domain / data / presentation)

## Features (current status)

| Feature | Status |
|---------|--------|
| SHA-256 exact duplicate detection | Implemented (domain + hasher) |
| Perceptual similarity (dHash + Hamming) | Implemented (domain + hasher) |
| Side-by-side comparison UI | Skeleton (to be completed) |
| Always keep ≥1 photo per group | **Implemented + unit-tested** |
| Permanent trash (copy then delete) | Data model + Room ready; full MediaStore move TBD |
| Restore from trash | Model ready |
| Progressive scan + pause/resume | Models ready; worker TBD |
| Arabic RTL + Light/Dark | Basic support present |
| Local only, no network for images | Yes |
| Backup / restore of trash & settings | Planned |

## Build

### Requirements
- JDK 17+
- Android SDK 35
- Gradle 8.9 (wrapper included after first CI run)

### Local
```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

### GitHub Actions
Push to `main` or run the workflow manually. Artifacts:
- `PhotoCleaner-debug`
- `PhotoCleaner-release` (unsigned – installable after enabling unknown sources)

## Safety rules (domain layer)

- It is impossible to mark every photo in a group as “delete”.
- Changing the last “KEEP” photo to “delete” is rejected.
- “Mark all suggested” always forces one KEEP (best resolution).
- Final validation runs before any real deletion.

See `GroupProtectionUseCase` and its unit tests.

## Privacy

All image processing happens on-device. No accounts, no ads, no cloud upload of photos.

## License

MIT (or as you prefer).
