package com.photocleaner.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.app.domain.model.AppMode
import com.photocleaner.app.domain.model.AppStats
import com.photocleaner.app.domain.model.DuplicateGroup
import com.photocleaner.app.domain.model.PhotoDecision
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.domain.model.ScanProgress
import com.photocleaner.app.domain.usecase.DuplicateFinder
import com.photocleaner.app.domain.usecase.GroupProtectionUseCase
import com.photocleaner.app.scan.MediaStoreScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val mode: AppMode = AppMode.SCAN_ONLY,
    val progress: ScanProgress = ScanProgress(),
    val isScanning: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val exactGroups: List<DuplicateGroup> = emptyList(),
    val similarGroups: List<DuplicateGroup> = emptyList(),
    val stats: AppStats = AppStats(),
    val errorMessage: String? = null,
    val statusMessage: String = ""
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = MediaStoreScanner(app)
    private val finder = DuplicateFinder()
    private val protection = GroupProtectionUseCase()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun setMode(mode: AppMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun startScan(computeHashes: Boolean = true) {
        if (_state.value.isScanning) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isScanning = true,
                    progress = ScanProgress(currentPhase = "starting"),
                    errorMessage = null,
                    statusMessage = "جاري الفحص…"
                )
            }
            try {
                scanner.scanImages(batchSize = 30, computeHashes = computeHashes).collect { result ->
                    _state.update {
                        it.copy(
                            photos = result.items,
                            progress = ScanProgress(
                                total = result.totalEstimate,
                                processed = result.processed,
                                currentPhase = result.phase
                            ),
                            statusMessage = "تم فحص ${result.processed} / ${result.totalEstimate}"
                        )
                    }
                }
                val photos = _state.value.photos
                val exact = finder.findExactDuplicates(photos)
                val exactHashes = exact.flatMap { g -> g.photos.mapNotNull { it.sha256 } }.toSet()
                val similar = finder.findSimilar(photos, maxHammingDistance = 8, excludeExactHashes = exactHashes)

                val stats = AppStats(
                    totalPhotos = photos.size,
                    totalSizeBytes = photos.sumOf { it.sizeBytes },
                    exactDuplicateGroups = exact.size,
                    exactDuplicatePhotos = exact.sumOf { it.photos.size },
                    similarGroups = similar.size,
                    similarPhotos = similar.sumOf { it.photos.size },
                    lastScanTimestamp = System.currentTimeMillis()
                )
                _state.update {
                    it.copy(
                        isScanning = false,
                        exactGroups = exact,
                        similarGroups = similar,
                        stats = stats,
                        statusMessage = "اكتمل الفحص",
                        progress = it.progress.copy(
                            currentPhase = "done",
                            processed = photos.size,
                            total = photos.size
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = e.message ?: "خطأ أثناء الفحص",
                        statusMessage = "توقف الفحص"
                    )
                }
            }
        }
    }

    fun pauseOrCancelScan() {
        scanJob?.cancel()
        scanJob = null
        _state.update {
            it.copy(
                isScanning = false,
                statusMessage = "تم إيقاف الفحص",
                progress = it.progress.copy(isCancelled = true)
            )
        }
    }

    fun changeDecision(groupId: String, photoId: String, decision: PhotoDecision, isExact: Boolean) {
        if (_state.value.mode != AppMode.CLEAN) return
        _state.update { s ->
            val list = if (isExact) s.exactGroups else s.similarGroups
            val updated = list.map { g ->
                if (g.id != groupId) g
                else protection.applyDecisionSafely(g, photoId, decision).getOrDefault(g)
            }
            if (isExact) s.copy(exactGroups = updated) else s.copy(similarGroups = updated)
        }
    }
}
