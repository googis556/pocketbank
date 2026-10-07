package com.pocketbank.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import java.util.Calendar
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class CalendarWidget : AppWidgetProvider() {

    companion object {
        private const val SUPABASE_URL = "https://ntaaxhayfraqcpnbreme.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_3B1-IKzYNazYswN91dtHsg_16Mb6lKY"
        private const val PREFS_NAME = "PocketBankWidget"
    }

    data class CalEvent(val date: String, val title: String, val color: String, val endDate: String, val repeatType: String)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            CoroutineScope(Dispatchers.IO).launch {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    private suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val groupId = prefs.getString("current_group_id", null)
        val token = prefs.getString("supabase_access_token", null)

        val events = if (groupId != null && token != null) fetchEvents(groupId, token) else emptyList()

        val density = context.resources.displayMetrics.density
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 360)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 360)
        val width = (maxW * density).toInt().coerceAtLeast(300)
        val height = (maxH * density).toInt().coerceAtLeast(300)

        val bitmap = drawCalendar(events, width, height, density, groupId == null)

        val views = RemoteViews(context.packageName, R.layout.calendar_widget)
        views.setImageViewBitmap(R.id.calendarImage, bitmap)

        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.calendarImage, pi)

        withContext(Dispatchers.Main) {
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun fetchEvents(groupId: String, token: String): List<CalEvent> {
        return try {
            val urlStr = "$SUPABASE_URL/rest/v1/calendar_events?group_id=eq.$groupId&select=event_date,title,color,end_date,repeat_type"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpsURLConnection
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = JSONArray(response)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CalEvent(
                    date = obj.getString("event_date"),
                    title = obj.optString("title", ""),
                    color = obj.optString("color", "#1A73E8"),
                    endDate = if (obj.isNull("end_date")) obj.getString("event_date") else obj.getString("end_date"),
                    repeatType = obj.optString("repeat_type", "none")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun drawCalendar(events: List<CalEvent>, width: Int, height: Int, density: Float, notLoggedIn: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 20f * density, 20f * density, bgPaint)

        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        // Header background strip
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A73E8") }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), 40f * density), 20f * density, 20f * density, headerBg)
        canvas.drawRect(RectF(0f, 20f * density, width.toFloat(), 40f * density), headerBg)

        // Header text
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val monthNames = arrayOf("1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월")
        canvas.drawText("PocketBank  ${year}년 ${monthNames[month-1]}", width / 2f, 27f * density, headerPaint)

        if (notLoggedIn) {
            val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#999999")
                textSize = 13f * density
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("앱에서 그룹을 선택해주세요", width / 2f, height / 2f, msgPaint)
            return bitmap
        }

        // Day headers
        val dayHeaders = arrayOf("일","월","화","수","목","금","토")
        val cellW = (width - 16f * density) / 7f
        val startX = 8f * density + cellW / 2f
        val dayHeaderY = 52f * density

        val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f * density
            textAlign = Paint.Align.CENTER
        }
        for (i in 0..6) {
            dayLabelPaint.color = when (i) {
                0 -> Color.parseColor("#D32F2F")
                6 -> Color.parseColor("#1976D2")
                else -> Color.parseColor("#555555")
            }
            canvas.drawText(dayHeaders[i], startX + i * cellW, dayHeaderY, dayLabelPaint)
        }

        // Divider
        val divPaint = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 1f }
        canvas.drawLine(8f * density, 56f * density, width - 8f * density, 56f * density, divPaint)

        // First day of month
        val firstDay = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val firstDow = firstDay.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = Math.ceil((firstDow + daysInMonth) / 7.0).toInt().coerceAtLeast(5)

        val gridStartY = 60f * density
        val cellH = (height - gridStartY - 4f * density) / rows

        // Build event map
        val monthStr = String.format(Locale.US, "%04d-%02d", year, month)
        val eventsByDay = mutableMapOf<Int, MutableList<String>>()
        for (ev in events) {
            for (d in 1..daysInMonth) {
                val dayStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, d)
                val matched = if (ev.repeatType == "monthly") {
                    val startDay = ev.date.takeLast(2).toIntOrNull() ?: 1
                    val endDay = ev.endDate.takeLast(2).toIntOrNull() ?: startDay
                    d in startDay..endDay
                } else {
                    dayStr >= ev.date && dayStr <= ev.endDate
                }
                if (matched) eventsByDay.getOrPut(d) { mutableListOf() }.add(ev.color)
            }
        }

        // Draw day cells
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 13f * density
        }
        val todayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A73E8") }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (day in 1..daysInMonth) {
            val pos = day + firstDow - 1
            val col = pos % 7
            val row = pos / 7
            val cx = startX + col * cellW
            val cy = gridStartY + row * cellH + cellH * 0.38f

            val isToday = day == today
            if (isToday) {
                canvas.drawCircle(cx, cy - 5f * density, 13f * density, todayBgPaint)
                numPaint.color = Color.WHITE
                numPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                numPaint.color = when (col) {
                    0 -> Color.parseColor("#D32F2F")
                    6 -> Color.parseColor("#1976D2")
                    else -> Color.parseColor("#333333")
                }
                numPaint.typeface = Typeface.DEFAULT
            }
            canvas.drawText(day.toString(), cx, cy, numPaint)

            val colors = eventsByDay[day]
            if (!colors.isNullOrEmpty()) {
                val dotR = 3f * density
                val maxDots = minOf(colors.size, 3)
                val spacing = dotR * 2.5f
                val totalW = (maxDots - 1) * spacing
                val dotY = cy + 9f * density
                for (di in 0 until maxDots) {
                    try { dotPaint.color = Color.parseColor(colors[di]) } catch (e: Exception) { dotPaint.color = Color.parseColor("#1A73E8") }
                    canvas.drawCircle(cx - totalW / 2 + di * spacing, dotY, dotR, dotPaint)
                }
            }
        }

        return bitmap
    }
}
