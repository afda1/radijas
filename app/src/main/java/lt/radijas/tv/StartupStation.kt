package lt.radijas.tv

import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository

object StartupStation {
    const val DEFAULT_ID = "lietus"

    fun resolve(repository: RadioRepository): RadioStation =
        repository.stations.firstOrNull { it.id == repository.lastStationId() }
            ?: repository.stations.first { it.id == DEFAULT_ID }
}
