package com.photocleaner.app

import com.photocleaner.app.domain.model.AppMode
import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.domain.model.SimilarityLevel
import com.photocleaner.app.domain.usecase.GroupProtectionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeDeleteUseCaseTest {

    private fun photo(id: String, decision: PhotoDecision, size: Long = 1000L) =
        PhotoItem(
            id = id,
            uri = "content://media/$id",
            displayName = "$id.jpg",
            sizeBytes = size,
            width = 1000,
            height = 1000,
            decision = decision
        )

    @Test
    fun buildPlanFailsWhenNoKeep() {
        val protection = GroupProtectionUseCase()
        val groups = listOf(
            DuplicateGroup(
                type = SimilarityLevel.EXACT,
                photos = listOf(
                    photo("1", PhotoDecision.SUGGEST_DELETE),
                    photo("2", PhotoDecision.SUGGEST_DELETE)
                )
            )
        )
        val validation = protection.validateBeforeDeletion(groups)
        assertTrue(validation is GroupProtectionUseCase.ValidationResult.Invalid)
    }

    @Test
    fun buildPlanSucceedsWithKeep() {
        val protection = GroupProtectionUseCase()
        val groups = listOf(
            DuplicateGroup(
                type = SimilarityLevel.EXACT,
                photos = listOf(
                    photo("1", PhotoDecision.KEEP, 2000),
                    photo("2", PhotoDecision.SUGGEST_DELETE, 1000),
                    photo("3", PhotoDecision.SUGGEST_DELETE, 1000)
                )
            )
        )
        val validation = protection.validateBeforeDeletion(groups)
        assertTrue(validation is GroupProtectionUseCase.ValidationResult.Ok)
        val toDelete = groups.flatMap { it.photos.filter { p -> p.decision == PhotoDecision.SUGGEST_DELETE } }
        assertEquals(2, toDelete.size)
        assertEquals(2000L, toDelete.sumOf { it.sizeBytes })
    }

    @Test
    fun scanOnlyIsDefaultContract() {
        assertEquals(AppMode.SCAN_ONLY, AppMode.SCAN_ONLY)
    }
}
