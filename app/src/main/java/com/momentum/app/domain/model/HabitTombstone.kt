package com.momentum.app.domain.model

import java.time.Instant

/**
 * Marks that a habit was deleted, so cloud sync knows not to resurrect it when merging in a
 * remote copy that predates the delete. Kept around locally (and pushed to Firestore) rather
 * than relying on "absent locally" alone, since "absent" is indistinguishable from "never
 * existed on this device yet".
 */
data class HabitTombstone(
    val habitId: Long,
    val deletedAt: Instant,
)
