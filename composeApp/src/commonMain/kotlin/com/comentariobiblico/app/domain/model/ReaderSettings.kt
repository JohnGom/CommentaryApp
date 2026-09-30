package com.comentariobiblico.app.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK, SEPIA }

enum class LineHeight(val multiplier: Float) {
    COMPACT(1.4f),
    NORMAL(1.6f),
    RELAXED(1.85f),
}

data class ReaderSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Multiplicador sobre la tipografía del lector. */
    val fontScale: Float = 1.0f,
    val lineHeight: LineHeight = LineHeight.NORMAL,
    val serif: Boolean = true,
    val keepScreenOn: Boolean = false,
) {
    companion object {
        /** Cinco pasos discretos: un slider continuo invita a dejarlo torcido. */
        val FONT_SCALES = listOf(0.875f, 1.0f, 1.15f, 1.3f, 1.5f)
    }
}
