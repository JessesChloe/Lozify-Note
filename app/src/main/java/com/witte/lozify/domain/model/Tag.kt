package com.witte.lozify.domain.model

import java.time.Instant

/**
 * Domain model representing a tag.
 *
 * ⚠️ PURE KOTLIN - NO ANDROID DEPENDENCIES
 * This is a business logic model, completely decoupled from Room/Android framework.
 *
 * As per ADR-004, color is handled at UI layer only.
 * All tags render with unified #4C88FF blue color in Compose UI.
 *
 * @property id Unique identifier (0 for unsaved tags)
 * @property name Tag name (without # prefix)
 * @property createdAt Creation timestamp
 * @property usageCount Number of notes currently using this tag
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val usageCount: Int = 0
) {
    /**
     * Get display name with # prefix (for UI rendering).
     */
    fun getDisplayName(): String = "#$name"

    /**
     * Check if this tag is actively used by any notes.
     */
    fun isUsed(): Boolean = usageCount > 0
}
