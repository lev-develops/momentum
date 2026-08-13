package com.momentum.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Every instance shows the same "all active habits" content, so unlike [MomentumWidgetReceiver]
 * there's no per-widget binding in DataStore to clean up on removal. */
class MomentumAllHabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentumAllHabitsWidget()
}
