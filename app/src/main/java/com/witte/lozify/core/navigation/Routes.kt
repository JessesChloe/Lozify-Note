package com.witte.lozify.core.navigation

/**
 * Navigation routes for Lozify app.
 *
 * Stage 12: Renamed ARCHIVE to TRASH, added TAG_EDIT route.
 */
object Routes {
    const val HOME = "home"
    const val TRASH = "trash"
    const val HELP = "help"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val WEBDAV_SYNC = "webdav_sync"
    const val PRO = "pro"
    const val DAILY_REVIEW = "daily_review"
    const val TAG_EDIT = "tag_edit/{tagId}"

    fun tagEdit(tagId: Long): String = "tag_edit/$tagId"
}
