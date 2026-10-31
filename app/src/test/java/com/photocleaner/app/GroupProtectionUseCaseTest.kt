package com.photocleaner.app

import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.domain.model.SimilarityLevel
import com.photocleaner.app.domain.usecase.GroupProtectionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupProtectionUseCaseTest {

    private val useCase = GroupProtectionUseCase()

    private fun photo(id: String, decision: PhotoDecision = PhotoDecision.UNDECIDED, w: Int = 1000, h: Int = 1000) =
        PhotoItem(
            id = id,
            uri = "content://media/$id",
            displayName = "$id.jpg",
            sizeBytes = 1_000_000L,
            width = w,
            height = h,
            decision = decision
        )

    @Test
    fun cannotMarkLastKeepAsDelete() {
        val group = DuplicateGroup(
            type = SimilarityLevel.EXACT,
            photos = listOf(
                photo("1", PhotoDecision.KEEP),
                photo("2", PhotoDecision.SUGGEST_DELETE),
                photo("3", PhotoDecision.SUGGEST_DELETE)
            )
        )
        assertTrue(useCase.wouldLeaveGroupEmpty(group, "1", PhotoDecision.SUGGEST_DELETE))
        val result = useCase.applyDecisionSafely(group, "1", PhotoDecision.SUGGEST_DELETE)
        assertTrue(result.isFailure)
    }

    @Test
    fun canChangeKeepWhenAnotherExists() {
        val group = DuplicateGroup(
            type = SimilarityLevel.EXACT,
            photos = listOf(
                photo("1", PhotoDecision.KEEP),
                photo("2", PhotoDecision.KEEP),
                photo("3", PhotoDecision.SUGGEST_DELETE)
            )
        )
        assertFalse(useCase.wouldLeaveGroupEmpty(group, "1", PhotoDecision.SUGGEST_DELETE))
        val result = useCase.applyDecisionSafely(group, "1", PhotoDecision.SUGGEST_DELETE)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.keepCount)
    }

    @Test
    fun markAllSuggestedStillKeepsOne() {
        val group = DuplicateGroup(
            type = SimilarityLevel.EXACT,
            photos = listOf(photo("1"), photo("2"), photo("3"))
        )
        val result = useCase.markSuggestedDeletesWhileKeepingOne(group)
        assertEquals(1, result.keepCount)
        assertEquals(2, result.deleteCount)
        assertTrue(result.hasAtLeastOneKeep())
    }

    @Test
    fun selectBestPrefersHigherResolution() {
        val photos = listOf(
            photo("small", w = 800, h = 600),
            photo("large", w = 4000, h = 3000),
            photo("medium", w = 1920, h = 1080)
        )
        val best = useCase.selectBestPhotoToKeep(photos)
        assertEquals("large", best.id)
    }

    @Test
    fun validateRejectsGroupsWithoutKeep() {
        val bad = DuplicateGroup(
            type = SimilarityLevel.EXACT,
            photos = listOf(
                photo("1", PhotoDecision.SUGGEST_DELETE),
                photo("2", PhotoDecision.SUGGEST_DELETE)
            )
        )
        val result = useCase.validateBeforeDeletion(listOf(bad))
        assertTrue(result is GroupProtectionUseCase.ValidationResult.Invalid)
    }
}
