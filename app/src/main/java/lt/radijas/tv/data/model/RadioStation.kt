package lt.radijas.tv.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    @param:DrawableRes val iconResId: Int,
    val brandColor: Long,
    /**
     * True when the logo artwork is too dark to read on the brand-tinted tile and needs a light
     * plate behind it. Derived from the mean luminance of the asset's own opaque pixels, not from
     * the brand, so it must be re-derived if the artwork changes.
     */
    val logoOnLightPlate: Boolean = false
)
