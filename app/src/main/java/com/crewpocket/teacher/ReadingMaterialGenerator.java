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
    private static final String[] CANDIDATE_MODELS = {
            "gemini-3.6-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };

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
        if (apiKey == null || apiKey.trim().isEmpty()) {
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

        final String prompt = "You are a native language teacher creating an oral reading and pronunciation practice passage for a student learning " + targetLangName + " (" + targetLangCode + ").\n"
                + "Topic: " + topicDesc + "\n"
                + "Difficulty Level: " + levelDesc + "\n\n"
                + "RULES:\n"
                + "1. Write an engaging, natural, 2-to-3 sentence passage in " + targetLangName + ".\n"
                + "2. It MUST be 100% written in the native script of " + targetLangName + " (e.g. Japanese kanji/hiragana for ja, Hangul for ko, Spanish for es, English for en, etc.).\n"
                + "3. Do NOT include romaji, pinyin, pronunciation guides, translations, quotes, bullet points, or markdown.\n"
                + "4. Output ONLY the raw " + targetLangName + " passage text directly.";

        tryModelAt(0, apiKey.trim(), prompt, mainHandler, callback);
    }

    private static void tryModelAt(final int modelIndex, final String apiKey, final String prompt, final Handler mainHandler, final GenerateCallback callback) {
        if (modelIndex >= CANDIDATE_MODELS.length) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("所有 AI 模型請求均未成功，請檢查 API Key 或網路連線"); }
            });
            return;
        }

        final String model = CANDIDATE_MODELS[modelIndex];
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 400);
            root.put("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();

            HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, final IOException e) {
                    Log.w(TAG, "Model " + model + " network failure: " + e.getMessage());
                    // Try next candidate model
                    tryModelAt(modelIndex + 1, apiKey, prompt, mainHandler, callback);
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    String resStr = "";
                    try {
                        if (response.body() != null) {
                            resStr = response.body().string();
                        }
                        if (response.isSuccessful() && !resStr.isEmpty()) {
                            JSONObject json = new JSONObject(resStr);
                            JSONArray candidates = json.optJSONArray("candidates");
                            if (candidates != null && candidates.length() > 0) {
                                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                                if (content != null) {
                                    JSONArray resParts = content.optJSONArray("parts");
                                    if (resParts != null && resParts.length() > 0) {
                                        String text = resParts.getJSONObject(0).optString("text", "").trim();
                                        // Strip optional surrounding quotes or markdown code block fences
                                        if (text.startsWith("```") && text.endsWith("```")) {
                                            text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "").trim();
                                        }
                                        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
                                            text = text.substring(1, text.length() - 1).trim();
                                        }
                                        if (!text.isEmpty()) {
                                            final String finalText = text;
                                            mainHandler.post(new Runnable() {
                                                @Override public void run() { callback.onSuccess(finalText); }
                                            });
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        Log.w(TAG, "Model " + model + " returned HTTP " + response.code() + ": " + resStr);
                    } catch (Exception e) {
                        Log.w(TAG, "Model " + model + " parse exception: " + e.getMessage());
                    }

                    // Fallback to next model
                    tryModelAt(modelIndex + 1, apiKey, prompt, mainHandler, callback);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "tryModelAt error: " + e.getMessage(), e);
            tryModelAt(modelIndex + 1, apiKey, prompt, mainHandler, callback);
        }
    }
}
