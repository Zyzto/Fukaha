package app.fukaha.android

import app.fukaha.EmbedHealthChecker
import app.fukaha.EmbedHealthPolicy
import app.fukaha.EmbedHealthSnapshot
import app.fukaha.EmbedHealthStore
import app.fukaha.PlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs gentle embedder probes on demand and when due (every 6h while foregrounded).
 */
class EmbedHealthController(
    private val store: EmbedHealthStore,
    private val scope: CoroutineScope,
    private val checker: EmbedHealthChecker = EmbedHealthChecker.create(),
) {
    private var job: Job? = null

    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress.asStateFlow()

    fun observeSnapshot() = store.observe()

    suspend fun snapshot(): EmbedHealthSnapshot = store.get()

    fun startAutoIfDue() {
        scope.launch {
            val snap = store.get()
            if (EmbedHealthPolicy.isDue(snap.checkedAtEpochMs)) {
                refresh()
            }
        }
    }

    fun refresh() {
        if (job?.isActive == true) return
        job = scope.launch {
            _inProgress.value = true
            try {
                val results = checker.refresh()
                store.save(results, PlatformClock.epochMillis())
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _inProgress.value = false
    }
}
