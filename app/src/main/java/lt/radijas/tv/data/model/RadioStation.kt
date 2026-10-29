package lt.radijas.tv.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    @param:DrawableRes val iconResId: Int,
    val brandColor: Long
)
