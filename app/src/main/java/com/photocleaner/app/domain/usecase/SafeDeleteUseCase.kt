package com.photocleaner.app.domain.usecase

import com.photocleaner.app.domain.model.AppMode
import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.trash.TrashRepository

class SafeDeleteUseCase(
    private val protection: GroupProtectionUseCase = GroupProtectionUseCase(),
    private val trash: TrashRepository
) {

    data class DeletePlan(
        val toDelete: List<PhotoItem>,
        val kept: List<PhotoItem>,
        val totalBytes: Long
    )

    data class DeleteReport(
        val succeeded: List<PhotoItem>,
        val failed: List<Pair<PhotoItem, String>>,
        val blockedByMode: Boolean = false,
        val blockedByProtection: Boolean = false
    )

    fun buildPlan(groups: List<DuplicateGroup>): Result<DeletePlan> {
        val validation = protection.validateBeforeDeletion(groups)
        if (validation is GroupProtectionUseCase.ValidationResult.Invalid) {
            return Result.failure(IllegalStateException(validation.reason))
        }
        val toDelete = groups.flatMap { g ->
            g.photos.filter { it.decision == PhotoDecision.SUGGEST_DELETE }
        }
        val kept = groups.flatMap { g ->
            g.photos.filter { it.decision == PhotoDecision.KEEP }
        }
        return Result.success(
            DeletePlan(
                toDelete = toDelete,
                kept = kept,
                totalBytes = toDelete.sumOf { it.sizeBytes }
            )
        )
    }

    suspend fun execute(
        mode: AppMode,
        groups: List<DuplicateGroup>,
        userConfirmed: Boolean
    ): DeleteReport {
        if (mode != AppMode.CLEAN) {
            return DeleteReport(emptyList(), emptyList(), blockedByMode = true)
        }
        if (!userConfirmed) {
            return DeleteReport(emptyList(), emptyList())
        }

        val planResult = buildPlan(groups)
        if (planResult.isFailure) {
            return DeleteReport(emptyList(), emptyList(), blockedByProtection = true)
        }
        val plan = planResult.getOrThrow()

        val succeeded = mutableListOf<PhotoItem>()
        val failed = mutableListOf<Pair<PhotoItem, String>>()

        for (photo in plan.toDelete) {
            val result = trash.moveToTrash(photo)
            if (result.isSuccess) {
                succeeded.add(photo)
            } else {
                failed.add(photo to (result.exceptionOrNull()?.message ?: "unknown error"))
            }
        }

        return DeleteReport(succeeded, failed)
    }
}
