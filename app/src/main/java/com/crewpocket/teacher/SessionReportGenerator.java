package com.crewpocket.teacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SessionReportGenerator {
    private static final String TAG = "SessionReportGen";

    private static final String[] CANDIDATE_MODELS = {
            "gemini-2.5-flash",
            "gemini-3.6-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build();

    public interface ReportCallback {
        void onReportReady(LearningDataManager.SessionRecord record);
    }

    public static void generateReportAsync(final Context context,
                                           final List<NativeLiveActivity.ChatTurn> turns,
                                           final int durationSeconds,
                                           final ReportCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String apiKey = AppConfig.getGeminiApiKey(context);
        final String tutorLang = AppConfig.getTutorLanguage(context);
        final String studentLang = AppConfig.getStudentLanguageDisplayName(context);
        final String scenario = AppConfig.getTutorPersona(context);

        int userCount = 0;
        int aiCount = 0;
        StringBuilder transcriptSb = new StringBuilder();
        for (NativeLiveActivity.ChatTurn turn : turns) {
            String sp = turn.spoken.toString().trim();
            if (sp.isEmpty()) continue;
            if ("user".equalsIgnoreCase(turn.role)) {
                userCount++;
                transcriptSb.append("Student: ").append(sp).append("\n");
            } else {
                aiCount++;
                transcriptSb.append("Tutor: ").append(sp).append("\n");
            }
        }

        final int finalUserCount = userCount;
        final int finalAiCount = aiCount;
        final String fullTranscript = transcriptSb.toString().trim();

        // Create base record
        final LearningDataManager.SessionRecord record = new LearningDataManager.SessionRecord();
        record.id = "ses_" + System.currentTimeMillis();
        record.timestamp = System.currentTimeMillis();
        record.dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        record.tutorLang = tutorLang;
        record.scenario = scenario;
        record.durationSeconds = durationSeconds;
        record.userTurns = finalUserCount;
        record.aiTurns = finalAiCount;
        record.fullTranscript = fullTranscript;

        if (apiKey.isEmpty() || fullTranscript.isEmpty() || finalUserCount < 1) {
            // Heuristic default report
            populateDefaultScores(record, studentLang);
            LearningDataManager.saveSessionRecord(context, record);
            LearningDataManager.recordPracticeActivity(context, finalUserCount, durationSeconds);
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onReportReady(record); }
            });
            return;
        }

        final String prompt = "You are a professional oral language coach evaluating a completed tutoring session.\n"
                + "Target Language: " + tutorLang + "\n"
                + "Student Native Language: " + studentLang + "\n"
                + "Scenario: " + scenario + "\n\n"
                + "Session Transcript:\n"
                + fullTranscript + "\n\n"
                + "Evaluate the student's speaking performance and output a valid JSON object with EXACTLY these keys:\n"
                + "{\n"
                + "  \"overall_score\": (integer 60-98 based on coherence and effort),\n"
                + "  \"fluency_score\": (integer 60-98),\n"
                + "  \"vocab_score\": (integer 60-98),\n"
                + "  \"grammar_score\": (integer 60-98),\n"
                + "  \"phonetic_score\": (integer 60-98),\n"
                + "  \"summary\": \"(1-2 sentence overall review in " + studentLang + ")\",\n"
                + "  \"strengths\": \"(1-2 sentence highlight of what the student did well in " + studentLang + ")\",\n"
                + "  \"recasts\": [\n"
                + "    {\n"
                + "      \"original\": \"(a student utterance that had minor grammar/vocabulary flaws, or a good attempt)\",\n"
                + "      \"corrected\": \"(authentic native phrasing in " + tutorLang + ")\",\n"
                + "      \"explanation\": \"(brief explanation in " + studentLang + ")\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"takeaways\": [\n"
                + "    {\"phrase\": \"(practical phrase 1 in " + tutorLang + ")\", \"translation\": \"(translation in " + studentLang + ")\"},\n"
                + "    {\"phrase\": \"(practical phrase 2 in " + tutorLang + ")\", \"translation\": \"(translation in " + studentLang + ")\"}\n"
                + "  ],\n"
                + "  \"cheer\": \"(warm, enthusiastic encouragement in " + studentLang + ")\"\n"
                + "}\n"
                + "Output strictly ONLY the raw JSON object.";

        tryEvaluateAt(0, apiKey.trim(), prompt, record, context, mainHandler, callback);
    }

    private static void tryEvaluateAt(final int modelIdx,
                                      final String apiKey,
                                      final String prompt,
                                      final LearningDataManager.SessionRecord record,
                                      final Context context,
                                      final Handler mainHandler,
                                      final ReportCallback callback) {
        if (modelIdx >= CANDIDATE_MODELS.length) {
            populateDefaultScores(record, AppConfig.getStudentLanguageDisplayName(context));
            LearningDataManager.saveSessionRecord(context, record);
            LearningDataManager.recordPracticeActivity(context, record.userTurns, record.durationSeconds);
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onReportReady(record); }
            });
            return;
        }

        final String model = CANDIDATE_MODELS[modelIdx];
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.3);
            genConfig.put("maxOutputTokens", 1200);
            root.put("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();

            HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {
                    Log.w(TAG, "Report model " + model + " failed: " + e.getMessage());
                    tryEvaluateAt(modelIdx + 1, apiKey, prompt, record, context, mainHandler, callback);
                }

                @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String resStr = response.body().string();
                            JSONObject json = new JSONObject(resStr);
                            JSONArray candidates = json.optJSONArray("candidates");
                            if (candidates != null && candidates.length() > 0) {
                                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                                if (content != null) {
                                    JSONArray resParts = content.optJSONArray("parts");
                                    if (resParts != null && resParts.length() > 0) {
                                        String raw = resParts.getJSONObject(0).optString("text", "").trim();
                                        if (raw.startsWith("```")) {
                                            raw = raw.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
                                        }
                                        JSONObject obj = new JSONObject(raw);
                                        record.overallScore = obj.optInt("overall_score", 88);
                                        record.fluencyScore = obj.optInt("fluency_score", 85);
                                        record.vocabScore = obj.optInt("vocab_score", 86);
                                        record.grammarScore = obj.optInt("grammar_score", 87);
                                        record.phoneticScore = obj.optInt("phonetic_score", 89);
                                        record.summary = obj.optString("summary", "");
                                        record.strengths = obj.optString("strengths", "");
                                        record.recastsJson = obj.optJSONArray("recasts") != null ? obj.optJSONArray("recasts").toString() : "[]";
                                        record.takeawaysJson = obj.optJSONArray("takeaways") != null ? obj.optJSONArray("takeaways").toString() : "[]";
                                        record.cheer = obj.optString("cheer", "");

                                        LearningDataManager.saveSessionRecord(context, record);
                                        LearningDataManager.recordPracticeActivity(context, record.userTurns, record.durationSeconds);
                                        mainHandler.post(new Runnable() {
                                            @Override public void run() { callback.onReportReady(record); }
                                        });
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Report parse error for " + model + ": " + e.getMessage());
                    }
                    tryEvaluateAt(modelIdx + 1, apiKey, prompt, record, context, mainHandler, callback);
                }
            });
        } catch (Exception e) {
            tryEvaluateAt(modelIdx + 1, apiKey, prompt, record, context, mainHandler, callback);
        }
    }

    private static void populateDefaultScores(LearningDataManager.SessionRecord record, String studentLang) {
        record.overallScore = 85;
        record.fluencyScore = 82;
        record.vocabScore = 86;
        record.grammarScore = 84;
        record.phoneticScore = 88;
        record.summary = "本次練習積極開口，與 AI 導師完成了 " + record.userTurns + " 輪對話！";
        record.strengths = "勇於表達觀點，發音清晰，持續保持練習必能快速突破！";
        record.cheer = "太棒了！堅持每天開口練習，自信心與流暢度正在穩步提升！";
    }
}
