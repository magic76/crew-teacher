package com.crewpocket.teacher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LearningDataManager {
    private static final String PREFS_NAME = "crew_teacher_learning_data";
    private static final String KEY_SESSIONS = "session_history";
    private static final String KEY_STARRED = "starred_phrasebook";
    private static final String KEY_LAST_PRACTICE_DATE = "last_practice_date";
    private static final String KEY_STREAK_DAYS = "streak_days";
    private static final String KEY_TODAY_TURNS = "today_turns";
    private static final String KEY_TODAY_SECONDS = "today_seconds";
    private static final String KEY_DAILY_GOAL_TURNS = "daily_goal_turns";

    public static class SessionRecord {
        public String id;
        public long timestamp;
        public String dateString;
        public String tutorLang;
        public String scenario;
        public int durationSeconds;
        public int userTurns;
        public int aiTurns;
        public int overallScore;
        public int fluencyScore;
        public int vocabScore;
        public int grammarScore;
        public int phoneticScore;
        public String summary = "";
        public String strengths = "";
        public String recastsJson = "[]";
        public String takeawaysJson = "[]";
        public String cheer = "";
        public String fullTranscript = "";
    }

    public static class StarredItem {
        public String id;
        public long timestamp;
        public String originalText = "";
        public String translation = "";
        public String category = "phrase"; // "word", "phrase", "correction", "vocab"
        public String notes = "";
    }

    public static class StreakInfo {
        public int streakDays = 0;
        public int todayTurns = 0;
        public int todaySeconds = 0;
        public int todayPracticeSeconds = 0;
        public int dailyGoalTurns = 10;
        public boolean isGoalCompleted = false;
        public String formattedTodayTime = "0 分鐘";
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // ── Streak & Daily Progress ──

    public static int getDailyGoalMinutes(Context context) {
        if (context == null) return 15;
        return getPrefs(context).getInt("daily_goal_minutes", 15);
    }

    public static void setDailyGoalMinutes(Context context, int min) {
        if (context == null) return;
        getPrefs(context).edit().putInt("daily_goal_minutes", min).apply();
    }

    public static synchronized StreakInfo getStreakInfo(Context context) {
        SharedPreferences sp = getPrefs(context);
        String today = getTodayKey();
        String lastDate = sp.getString(KEY_LAST_PRACTICE_DATE, "");

        int streak = sp.getInt(KEY_STREAK_DAYS, 0);
        int todayTurns = 0;
        int todaySeconds = 0;

        if (today.equals(lastDate)) {
            todayTurns = sp.getInt(KEY_TODAY_TURNS, 0);
            todaySeconds = sp.getInt(KEY_TODAY_SECONDS, 0);
        } else {
            // Check if yesterday was practiced; if not, streak breaks (unless brand new)
            if (!lastDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    long diffMs = sdf.parse(today).getTime() - sdf.parse(lastDate).getTime();
                    long diffDays = diffMs / (1000 * 60 * 60 * 24);
                    if (diffDays > 1) {
                        streak = 0;
                        sp.edit().putInt(KEY_STREAK_DAYS, 0).apply();
                    }
                } catch (Exception ignored) {}
            }
        }

        StreakInfo info = new StreakInfo();
        info.streakDays = streak;
        info.todayTurns = todayTurns;
        info.todaySeconds = todaySeconds;
        info.todayPracticeSeconds = todaySeconds;
        info.dailyGoalTurns = sp.getInt(KEY_DAILY_GOAL_TURNS, 10);
        info.isGoalCompleted = todayTurns >= info.dailyGoalTurns;
        info.formattedTodayTime = (todaySeconds / 60) + " 分鐘";
        return info;
    }

    public static synchronized void recordPracticeActivity(Context context, int turnsAdded, int secondsAdded) {
        if (turnsAdded <= 0 && secondsAdded <= 0) return;
        SharedPreferences sp = getPrefs(context);
        String today = getTodayKey();
        String lastDate = sp.getString(KEY_LAST_PRACTICE_DATE, "");

        int streak = sp.getInt(KEY_STREAK_DAYS, 0);
        int todayTurns = 0;
        int todaySeconds = 0;

        if (today.equals(lastDate)) {
            todayTurns = sp.getInt(KEY_TODAY_TURNS, 0);
            todaySeconds = sp.getInt(KEY_TODAY_SECONDS, 0);
        } else {
            // New day practice!
            if (lastDate.isEmpty()) {
                streak = 1;
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    long diffDays = (sdf.parse(today).getTime() - sdf.parse(lastDate).getTime()) / (1000 * 60 * 60 * 24);
                    if (diffDays == 1) {
                        streak += 1;
                    } else if (diffDays > 1) {
                        streak = 1;
                    }
                } catch (Exception e) {
                    streak = 1;
                }
            }
        }

        todayTurns += turnsAdded;
        todaySeconds += secondsAdded;

        sp.edit()
                .putString(KEY_LAST_PRACTICE_DATE, today)
                .putInt(KEY_STREAK_DAYS, streak)
                .putInt(KEY_TODAY_TURNS, todayTurns)
                .putInt(KEY_TODAY_SECONDS, todaySeconds)
                .apply();
    }

    // ── Starred Phrasebook ──

    public static synchronized List<StarredItem> getStarredItems(Context context) {
        List<StarredItem> list = new ArrayList<StarredItem>();
        String raw = getPrefs(context).getString(KEY_STARRED, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                StarredItem item = new StarredItem();
                item.id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
                item.timestamp = obj.optLong("timestamp", System.currentTimeMillis());
                item.originalText = obj.optString("originalText", "");
                item.translation = obj.optString("translation", "");
                item.category = obj.optString("category", "phrase");
                item.notes = obj.optString("notes", "");
                list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized boolean isStarred(Context context, String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) return false;
        String clean = originalText.trim();
        List<StarredItem> items = getStarredItems(context);
        for (StarredItem item : items) {
            if (clean.equalsIgnoreCase(item.originalText.trim())) return true;
        }
        return false;
    }

    public static synchronized boolean addStarredItem(Context context, String originalText, String translation, String category, String notes) {
        if (originalText == null || originalText.trim().isEmpty()) return false;
        String clean = originalText.trim();
        List<StarredItem> items = getStarredItems(context);
        for (StarredItem it : items) {
            if (clean.equalsIgnoreCase(it.originalText.trim())) {
                if ((it.translation == null || it.translation.isEmpty()) && translation != null && !translation.isEmpty()) {
                    it.translation = translation.trim();
                    saveStarredItems(context, items);
                }
                return false; // Already present
            }
        }
        StarredItem item = new StarredItem();
        item.id = "star_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        item.timestamp = System.currentTimeMillis();
        item.originalText = clean;
        item.translation = translation != null ? translation.trim() : "";
        item.category = category != null ? category : "phrase";
        item.notes = notes != null ? notes.trim() : "";
        items.add(0, item);
        saveStarredItems(context, items);
        return true;
    }

    public static synchronized int addStarredItemsBatch(Context context, List<StarredItem> toAdd) {
        if (toAdd == null || toAdd.isEmpty()) return 0;
        List<StarredItem> items = getStarredItems(context);
        int added = 0;
        long now = System.currentTimeMillis();
        int seq = 0;
        for (StarredItem newItem : toAdd) {
            if (newItem == null || newItem.originalText == null || newItem.originalText.trim().isEmpty()) continue;
            String clean = newItem.originalText.trim();
            boolean exists = false;
            for (StarredItem it : items) {
                if (clean.equalsIgnoreCase(it.originalText.trim())) {
                    exists = true;
                    if ((it.translation == null || it.translation.isEmpty()) && newItem.translation != null && !newItem.translation.isEmpty()) {
                        it.translation = newItem.translation.trim();
                    }
                    break;
                }
            }
            if (!exists) {
                StarredItem item = new StarredItem();
                item.id = "star_" + (now + seq) + "_" + seq;
                item.timestamp = now + seq;
                item.originalText = clean;
                item.translation = newItem.translation != null ? newItem.translation.trim() : "";
                item.category = newItem.category != null ? newItem.category : "phrase";
                item.notes = newItem.notes != null ? newItem.notes.trim() : "";
                items.add(0, item);
                added++;
                seq++;
            }
        }
        if (added > 0 || seq > 0) {
            saveStarredItems(context, items);
        }
        return added;
    }

    public static synchronized boolean toggleStarItem(Context context, String originalText, String translation, String category, String notes) {
        if (originalText == null || originalText.trim().isEmpty()) return false;
        String clean = originalText.trim();
        List<StarredItem> items = getStarredItems(context);
        int foundIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (clean.equalsIgnoreCase(items.get(i).originalText.trim())) {
                foundIdx = i;
                break;
            }
        }

        if (foundIdx >= 0) {
            items.remove(foundIdx);
            saveStarredItems(context, items);
            return false; // unstarred
        } else {
            StarredItem item = new StarredItem();
            item.id = "star_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
            item.timestamp = System.currentTimeMillis();
            item.originalText = clean;
            item.translation = translation != null ? translation.trim() : "";
            item.category = category != null ? category : "phrase";
            item.notes = notes != null ? notes.trim() : "";
            items.add(0, item); // Newest on top
            saveStarredItems(context, items);
            return true; // starred
        }
    }

    public static synchronized void removeStarredItemById(Context context, String id) {
        if (id == null) return;
        List<StarredItem> items = getStarredItems(context);
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).id)) {
                items.remove(i);
                break;
            }
        }
        saveStarredItems(context, items);
    }

    private static void saveStarredItems(Context context, List<StarredItem> items) {
        try {
            JSONArray arr = new JSONArray();
            for (StarredItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("timestamp", item.timestamp);
                obj.put("originalText", item.originalText);
                obj.put("translation", item.translation);
                obj.put("category", item.category);
                obj.put("notes", item.notes);
                arr.put(obj);
            }
            getPrefs(context).edit().putString(KEY_STARRED, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ── Session History ──

    public static synchronized List<SessionRecord> getSessionHistory(Context context) {
        List<SessionRecord> list = new ArrayList<SessionRecord>();
        String raw = getPrefs(context).getString(KEY_SESSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                SessionRecord rec = new SessionRecord();
                rec.id = obj.optString("id", "");
                rec.timestamp = obj.optLong("timestamp", 0);
                rec.dateString = obj.optString("dateString", "");
                rec.tutorLang = obj.optString("tutorLang", "");
                rec.scenario = obj.optString("scenario", "");
                rec.durationSeconds = obj.optInt("durationSeconds", 0);
                rec.userTurns = obj.optInt("userTurns", 0);
                rec.aiTurns = obj.optInt("aiTurns", 0);
                rec.overallScore = obj.optInt("overallScore", 85);
                rec.fluencyScore = obj.optInt("fluencyScore", 85);
                rec.vocabScore = obj.optInt("vocabScore", 85);
                rec.grammarScore = obj.optInt("grammarScore", 85);
                rec.phoneticScore = obj.optInt("phoneticScore", 85);
                rec.summary = obj.optString("summary", "");
                rec.strengths = obj.optString("strengths", "");
                rec.recastsJson = obj.optString("recastsJson", "[]");
                rec.takeawaysJson = obj.optString("takeawaysJson", "[]");
                rec.cheer = obj.optString("cheer", "");
                rec.fullTranscript = obj.optString("fullTranscript", "");

                // Robust deduplication on load
                boolean isDuplicate = false;
                for (SessionRecord existing : list) {
                    if (!rec.id.isEmpty() && rec.id.equals(existing.id)) {
                        isDuplicate = true;
                        break;
                    }
                    if (!rec.fullTranscript.isEmpty() && rec.fullTranscript.equals(existing.fullTranscript)
                            && Math.abs(rec.timestamp - existing.timestamp) < 60000) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    list.add(rec);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void saveSessionRecord(Context context, SessionRecord record) {
        if (record == null) return;
        List<SessionRecord> list = getSessionHistory(context);
        int foundIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            SessionRecord existing = list.get(i);
            if (!record.id.isEmpty() && record.id.equals(existing.id)) {
                foundIdx = i;
                break;
            }
            if (!record.fullTranscript.isEmpty() && record.fullTranscript.equals(existing.fullTranscript)
                    && Math.abs(record.timestamp - existing.timestamp) < 60000) {
                foundIdx = i;
                break;
            }
        }

        if (foundIdx >= 0) {
            list.set(foundIdx, record); // update in-place
        } else {
            list.add(0, record); // newest first
        }

        if (list.size() > 50) {
            list = list.subList(0, 50); // keep last 50
        }
        try {
            JSONArray arr = new JSONArray();
            for (SessionRecord rec : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", rec.id);
                obj.put("timestamp", rec.timestamp);
                obj.put("dateString", rec.dateString);
                obj.put("tutorLang", rec.tutorLang);
                obj.put("scenario", rec.scenario);
                obj.put("durationSeconds", rec.durationSeconds);
                obj.put("userTurns", rec.userTurns);
                obj.put("aiTurns", rec.aiTurns);
                obj.put("overallScore", rec.overallScore);
                obj.put("fluencyScore", rec.fluencyScore);
                obj.put("vocabScore", rec.vocabScore);
                obj.put("grammarScore", rec.grammarScore);
                obj.put("phoneticScore", rec.phoneticScore);
                obj.put("summary", rec.summary);
                obj.put("strengths", rec.strengths);
                obj.put("recastsJson", rec.recastsJson);
                obj.put("takeawaysJson", rec.takeawaysJson);
                obj.put("cheer", rec.cheer);
                obj.put("fullTranscript", rec.fullTranscript);
                arr.put(obj);
            }
            getPrefs(context).edit().putString(KEY_SESSIONS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized void clearSessionHistory(Context context) {
        getPrefs(context).edit().putString(KEY_SESSIONS, "[]").apply();
    }
}
