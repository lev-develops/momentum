package com.momentum.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.momentum.app.MomentumApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MomentumWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MomentumWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val container = (context.applicationContext as MomentumApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { container.widgetPrefsDataStore.clear(it) }
        }
    }
}
