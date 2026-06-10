package com.worldbarometer.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Most między systemem Androida (AppWidgetProvider) a widgetem Glance.
 * Zadeklarowany w AndroidManifest.xml.
 */
class BarometerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BarometerWidget()
}
