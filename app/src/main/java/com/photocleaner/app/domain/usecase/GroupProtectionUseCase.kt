package com.photocleaner.app.domain.usecase

import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem

class GroupProtectionUseCase {

    fun wouldLeaveGroupEmpty(group: DuplicateGroup, photoId: String, newDecision: PhotoDecision): Boolean {
        if (newDecision == PhotoDecision.KEEP || newDecision == PhotoDecision.UNDECIDED) {
            return false
        }
        val remainingKeep = group.photos.count {
            it.id != photoId && it.decision == PhotoDecision.KEEP
        }
        return remainingKeep == 0
    }

    fun applyDecisionSafely(
        group: DuplicateGroup,
        photoId: String,
        newDecision: PhotoDecision
    ): Result<DuplicateGroup> {
        if (wouldLeaveGroupEmpty(group, photoId, newDecision)) {
            return Result.failure(
                IllegalStateException("Cannot delete the last remaining copy in the group")
            )
        }
        return Result.success(group.withDecision(photoId, newDecision))
    }

    fun markSuggestedDeletesWhileKeepingOne(group: DuplicateGroup): DuplicateGroup {
        if (group.photos.isEmpty()) return group
        val keepPhoto = group.photos.firstOrNull { it.decision == PhotoDecision.KEEP }
            ?: group.photos.maxByOrNull { it.width * it.height.toLong() }
            ?: group.photos.first()

        val updated = group.photos.map { photo ->
            if (photo.id == keepPhoto.id) {
                photo.copy(decision = PhotoDecision.KEEP)
            } else {
                photo.copy(decision = PhotoDecision.SUGGEST_DELETE)
            }
        }
        return group.copy(photos = updated)
    }

    fun validateBeforeDeletion(groups: List<DuplicateGroup>): ValidationResult {
        val invalidGroups = groups.filter { !it.hasAtLeastOneKeep() }
        return if (invalidGroups.isEmpty()) {
            ValidationResult.Ok
        } else {
            ValidationResult.Invalid(
                "Some groups have no photo marked as KEEP. Groups: ${invalidGroups.map { it.id }}"
            )
        }
    }

    fun selectBestPhotoToKeep(photos: List<PhotoItem>): PhotoItem {
        require(photos.isNotEmpty()) { "Cannot select best photo from empty list" }
        return photos.maxWithOrNull(
            compareBy<PhotoItem> { it.width * it.height.toLong() }
                .thenByDescending { it.sizeBytes }
                .thenByDescending { it.dateTaken ?: 0L }
        ) ?: photos.first()
    }

    sealed class ValidationResult {
        data object Ok : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
