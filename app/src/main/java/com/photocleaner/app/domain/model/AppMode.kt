package com.photocleaner.app.domain.model

/**
 * Two strict modes:
 * - SCAN_ONLY (default): read, analyze, display. Never delete.
 * - CLEAN: user must explicitly enable; still requires review + copy-verify before any delete.
 */
enum class AppMode {
    SCAN_ONLY,
    CLEAN
}

enum class TrashStatus {
    COPIED_AND_VERIFIED,
    ORIGINAL_DELETED,
    RESTORE_PENDING,
    RESTORE_FAILED,
    ERROR
}
