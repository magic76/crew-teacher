package com.crewpocket.teacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReadingMaterialGenerator {
    private static final String TAG = "ReadingGen";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public interface GenerateCallback {
        void onSuccess(String generatedText);
        void onError(String error);
    }

    public static void generateAsync(final Context context, final String topic, final String level, final GenerateCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey.isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("請先設定 Gemini API Key"); }
            });
            return;
        }

        String targetLangCode = AppConfig.getTutorLanguage(context);
        String targetLangName = MainActivity.getLanguageLabel(targetLangCode);

        String topicDesc = "daily".equalsIgnoreCase(topic) ? "Daily life, hobbies, coffee shop, or morning routine" :
                ("travel".equalsIgnoreCase(topic) ? "Travel scenarios, airport, hotel, ordering food, or asking directions" :
                ("business".equalsIgnoreCase(topic) ? "Business workplace, presentations, project milestones, or AI technology" :
                ("phonetics".equalsIgnoreCase(topic) ? "Pronunciation, natural rhythm, vowel clarity, and challenging sounds" :
                (topic != null && !topic.isEmpty() ? topic : "Engaging conversational topic and cultural story"))));

        String levelDesc = "beginner".equalsIgnoreCase(level) ? "Beginner (Short simple sentences, A1-A2 level)" :
                ("advanced".equalsIgnoreCase(level) ? "Advanced (Rich vocabulary, natural native rhythm, C1 level)" : "Intermediate (Natural practical flow, B1-B2 level)");

        String prompt = "You are a native language teacher creating an oral reading and pronunciation practice passage for a student learning " + targetLangName + " (" + targetLangCode + ").\n"
                + "Topic: " + topicDesc + "\n"
                + "Difficulty Level: " + levelDesc + "\n\n"
                + "RULES:\n"
                + "1. Write an engaging, natural, 2-to-3 sentence passage in " + targetLangName + ".\n"
                + "2. It MUST be 100% written in the native script of " + targetLangName + " (e.g. Japanese kanji/hiragana for ja, Hangul for ko, Spanish for es, English for en, etc.).\n"
                + "3. Do NOT include romaji, pinyin, pronunciation guides, Chinese translations, quotes, bullet points, or markdown.\n"
                + "4. Output ONLY the raw " + targetLangName + " passage text directly.";

        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();

            HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, final IOException e) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() { callback.onError("生成失敗：" + e.getMessage()); }
                    });
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
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
                                        final String text = resParts.getJSONObject(0).optString("text", "").trim();
                                        if (!text.isEmpty()) {
                                            mainHandler.post(new Runnable() {
                                                @Override public void run() { callback.onSuccess(text); }
                                            });
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        mainHandler.post(new Runnable() {
                            @Override public void run() { callback.onError("AI 回應為空，請重試"); }
                        });
                    } catch (final Exception e) {
                        mainHandler.post(new Runnable() {
                            @Override public void run() { callback.onError("解析錯誤：" + e.getMessage()); }
                        });
                    }
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}
