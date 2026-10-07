package com.pocketbank.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class CalendarWidget extends AppWidgetProvider {

    private static final String SUPABASE_URL = "https://ntaaxhayfraqcpnbreme.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_3B1-IKzYNazYswN91dtHsg_16Mb6lKY";
    private static final String PREFS_NAME = "PocketBankWidget";

    static class CalEvent {
        String date, title, color, endDate, repeatType;
        CalEvent(String date, String title, String color, String endDate, String repeatType) {
            this.date = date; this.title = title; this.color = color;
            this.endDate = endDate; this.repeatType = repeatType;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            final int widgetId = id;
            new Thread(() -> updateWidget(context, appWidgetManager, widgetId)).start();
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String groupId = prefs.getString("current_group_id", null);
        String token = prefs.getString("supabase_access_token", null);

        List<CalEvent> events = (groupId != null && token != null) ? fetchEvents(groupId, token) : new ArrayList<>();

        float density = context.getResources().getDisplayMetrics().density;
        android.os.Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
        int maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 360);
        int maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 360);
        int width = Math.max((int)(maxW * density), 300);
        int height = Math.max((int)(maxH * density), 300);

        Bitmap bitmap = drawCalendar(events, width, height, density, groupId == null);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.calendar_widget);
        views.setImageViewBitmap(R.id.calendarImage, bitmap);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.calendarImage, pi);

        appWidgetManager.updateAppWidget(widgetId, views);
    }

    private List<CalEvent> fetchEvents(String groupId, String token) {
        List<CalEvent> list = new ArrayList<>();
        try {
            String urlStr = SUPABASE_URL + "/rest/v1/calendar_events?group_id=eq." + groupId +
                    "&select=event_date,title,color,end_date,repeat_type";
            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            conn.disconnect();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String date = obj.getString("event_date");
                String endDate = obj.isNull("end_date") ? date : obj.getString("end_date");
                list.add(new CalEvent(
                    date,
                    obj.optString("title", ""),
                    obj.optString("color", "#1A73E8"),
                    endDate,
                    obj.optString("repeat_type", "none")
                ));
            }
        } catch (Exception e) { /* 네트워크 오류 무시 */ }
        return list;
    }

    private Bitmap drawCalendar(List<CalEvent> events, int width, int height, float density, boolean notLoggedIn) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 20 * density, 20 * density, bgPaint);

        Paint headerBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerBg.setColor(Color.parseColor("#1A73E8"));
        canvas.drawRoundRect(new RectF(0, 0, width, 40 * density), 20 * density, 20 * density, headerBg);
        canvas.drawRect(new RectF(0, 20 * density, width, 40 * density), headerBg);

        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        String[] monthNames = {"1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월"};

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(15 * density);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PocketBank  " + year + "년 " + monthNames[month - 1], width / 2f, 27 * density, headerPaint);

        if (notLoggedIn) {
            Paint msgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            msgPaint.setColor(Color.parseColor("#999999"));
            msgPaint.setTextSize(13 * density);
            msgPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("앱에서 그룹을 선택해주세요", width / 2f, height / 2f, msgPaint);
            return bitmap;
        }

        String[] dayHeaders = {"일","월","화","수","목","금","토"};
        float cellW = (width - 16 * density) / 7f;
        float startX = 8 * density + cellW / 2f;

        Paint dayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayLabelPaint.setTextSize(11 * density);
        dayLabelPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            if (i == 0) dayLabelPaint.setColor(Color.parseColor("#D32F2F"));
            else if (i == 6) dayLabelPaint.setColor(Color.parseColor("#1976D2"));
            else dayLabelPaint.setColor(Color.parseColor("#555555"));
            canvas.drawText(dayHeaders[i], startX + i * cellW, 52 * density, dayLabelPaint);
        }

        Paint divPaint = new Paint();
        divPaint.setColor(Color.parseColor("#E0E0E0"));
        divPaint.setStrokeWidth(1);
        canvas.drawLine(8 * density, 56 * density, width - 8 * density, 56 * density, divPaint);

        Calendar firstDay = Calendar.getInstance();
        firstDay.set(year, month - 1, 1);
        int firstDow = firstDay.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (int) Math.ceil((firstDow + daysInMonth) / 7.0);
        if (rows < 5) rows = 5;

        float gridStartY = 60 * density;
        float cellH = (height - gridStartY - 4 * density) / rows;

        // 이벤트 날짜 맵
        Map<Integer, List<String>> eventsByDay = new HashMap<>();
        for (CalEvent ev : events) {
            for (int d = 1; d <= daysInMonth; d++) {
                String dayStr = String.format("%04d-%02d-%02d", year, month, d);
                boolean matched;
                if ("monthly".equals(ev.repeatType)) {
                    int sd = Integer.parseInt(ev.date.substring(8));
                    int ed = Integer.parseInt(ev.endDate.substring(8));
                    matched = d >= sd && d <= ed;
                } else {
                    matched = dayStr.compareTo(ev.date) >= 0 && dayStr.compareTo(ev.endDate) <= 0;
                }
                if (matched) {
                    if (!eventsByDay.containsKey(d)) eventsByDay.put(d, new ArrayList<>());
                    eventsByDay.get(d).add(ev.color);
                }
            }
        }

        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setTextAlign(Paint.Align.CENTER);
        numPaint.setTextSize(13 * density);

        Paint todayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayBgPaint.setColor(Color.parseColor("#1A73E8"));

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        for (int day = 1; day <= daysInMonth; day++) {
            int pos = day + firstDow - 1;
            int col = pos % 7;
            int row = pos / 7;
            float cx = startX + col * cellW;
            float cy = gridStartY + row * cellH + cellH * 0.38f;

            if (day == today) {
                canvas.drawCircle(cx, cy - 5 * density, 13 * density, todayBgPaint);
                numPaint.setColor(Color.WHITE);
                numPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            } else {
                if (col == 0) numPaint.setColor(Color.parseColor("#D32F2F"));
                else if (col == 6) numPaint.setColor(Color.parseColor("#1976D2"));
                else numPaint.setColor(Color.parseColor("#333333"));
                numPaint.setTypeface(Typeface.DEFAULT);
            }
            canvas.drawText(String.valueOf(day), cx, cy, numPaint);

            List<String> colors = eventsByDay.get(day);
            if (colors != null && !colors.isEmpty()) {
                float dotR = 3 * density;
                float dotY = cy + 9 * density;
                int maxDots = Math.min(colors.size(), 3);
                float spacing = dotR * 2.5f;
                float totalW = (maxDots - 1) * spacing;
                for (int di = 0; di < maxDots; di++) {
                    try { dotPaint.setColor(Color.parseColor(colors.get(di))); }
                    catch (Exception e) { dotPaint.setColor(Color.parseColor("#1A73E8")); }
                    canvas.drawCircle(cx - totalW / 2 + di * spacing, dotY, dotR, dotPaint);
                }
            }
        }

        return bitmap;
    }
}
