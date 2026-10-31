package com.photocleaner.app.domain.usecase

import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.domain.model.SimilarityLevel
import com.photocleaner.app.image.ImageHasher

class DuplicateFinder(
    private val protection: GroupProtectionUseCase = GroupProtectionUseCase()
) {

    fun findExactDuplicates(photos: List<PhotoItem>): List<DuplicateGroup> {
        val byHash = photos
            .filter { !it.sha256.isNullOrBlank() }
            .groupBy { it.sha256!! }
            .filter { it.value.size > 1 }

        return byHash.map { (_, list) ->
            val best = protection.selectBestPhotoToKeep(list)
            val withDecisions = list.map { p ->
                if (p.id == best.id) p.copy(decision = PhotoDecision.KEEP)
                else p.copy(decision = PhotoDecision.SUGGEST_DELETE)
            }
            val savings = withDecisions
                .filter { it.decision == PhotoDecision.SUGGEST_DELETE }
                .sumOf { it.sizeBytes }
            DuplicateGroup(
                type = SimilarityLevel.EXACT,
                photos = withDecisions,
                estimatedSavingsBytes = savings
            )
        }
    }

    fun findSimilar(
        photos: List<PhotoItem>,
        maxHammingDistance: Int = 8,
        excludeExactHashes: Set<String> = emptySet()
    ): List<DuplicateGroup> {
        val candidates = photos.filter {
            !it.perceptualHash.isNullOrBlank() &&
                    (it.sha256 == null || it.sha256 !in excludeExactHashes)
        }

        val used = mutableSetOf<String>()
        val groups = mutableListOf<DuplicateGroup>()

        for (i in candidates.indices) {
            val a = candidates[i]
            if (a.id in used) continue
            val groupPhotos = mutableListOf(a)
            used.add(a.id)

            for (j in i + 1 until candidates.size) {
                val b = candidates[j]
                if (b.id in used) continue
                val dist = ImageHasher.hammingDistance(a.perceptualHash!!, b.perceptualHash!!)
                if (dist <= maxHammingDistance) {
                    groupPhotos.add(b)
                    used.add(b.id)
                }
            }

            if (groupPhotos.size > 1) {
                val best = protection.selectBestPhotoToKeep(groupPhotos)
                val withDecisions = groupPhotos.map { p ->
                    if (p.id == best.id) p.copy(decision = PhotoDecision.KEEP)
                    else p.copy(decision = PhotoDecision.SUGGEST_DELETE)
                }
                val level = when {
                    maxHammingDistance <= 4 -> SimilarityLevel.VERY_HIGH
                    maxHammingDistance <= 10 -> SimilarityLevel.MEDIUM
                    else -> SimilarityLevel.LOW
                }
                val savings = withDecisions
                    .filter { it.decision == PhotoDecision.SUGGEST_DELETE }
                    .sumOf { it.sizeBytes }
                groups.add(
                    DuplicateGroup(
                        type = level,
                        photos = withDecisions,
                        estimatedSavingsBytes = savings
                    )
                )
            }
        }
        return groups
    }
}
