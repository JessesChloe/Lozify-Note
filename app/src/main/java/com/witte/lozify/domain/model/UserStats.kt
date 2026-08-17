package com.witte.lozify.domain.model

/**
 * Domain model representing user statistics and achievement milestones.
 *
 * @param notesCount Total count of active notes
 * @param tagsCount Total count of tags
 * @param daysCount Consecutive or total days since first note creation
 */
data class UserStats(
    val notesCount: Int = 0,
    val tagsCount: Int = 0,
    val daysCount: Int = 1
)
