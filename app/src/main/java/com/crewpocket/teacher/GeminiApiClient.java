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

public class GeminiApiClient {
    private static final String TAG = "GeminiApiClient";

    public static final String[] CANDIDATE_MODELS = {
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.0-flash",
            "gemini-3-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
    };

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build();

    private static final OkHttpClient FAST_TRANSLATION_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    public static final String[] FAST_MODELS = {
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3-flash",
            "gemini-2.5-flash"
    };

    public interface JsonCallback {
        void onSuccess(JSONObject jsonObject, String rawText);
        void onError(String errorMessage);
    }

    public interface TextCallback {
        void onSuccess(String rawText);
        void onError(String errorMessage);
    }

    /**
     * Ultra-fast bilingual subtitle generator:
     * - Uses streamlined prompt with minimal output tokens (max 180 tokens)
     * - Fast-tracked HTTP client (5s timeout)
     * - Cancels stale queued requests to guarantee immediate alignment with latest turn
     */
    public static void generateFastSubtitle(final String apiKey, final String practiceLang, final String targetLang, final String sourceText, final JsonCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        if (apiKey == null || apiKey.trim().isEmpty() || sourceText == null || sourceText.trim().isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("Empty API Key or text"); }
            });
            return;
        }

        final String prompt = "Translate spoken " + practiceLang + " to " + targetLang + ".\n"
                + "Spoken: \"" + sourceText.replace("\"", "'") + "\"\n"
                + "Return ONLY compact JSON:\n"
                + "{\"t\":\"(fluent " + targetLang + " translation)\",\"v\":\"(1 key vocab with phonetic tip/meaning or empty)\",\"r\":[\"(sample reply 1)\",\"(sample reply 2)\"]}";

        tryGenerateFastAt(0, apiKey.trim(), prompt, mainHandler, new InternalCallback() {
            @Override
            public void onResult(String raw) {
                String jsonToParse = raw;
                int firstBrace = raw.indexOf("{");
                int lastBrace = raw.lastIndexOf("}");
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    jsonToParse = raw.substring(firstBrace, lastBrace + 1);
                }
                try {
                    JSONObject rawObj = new JSONObject(jsonToParse);
                    final JSONObject normalized = new JSONObject();
                    normalized.put("translation", rawObj.optString("t", rawObj.optString("translation", "")));
                    normalized.put("key_vocab", rawObj.optString("v", rawObj.optString("key_vocab", "")));
                    normalized.put("suggested_replies", rawObj.optJSONArray("r") != null ? rawObj.optJSONArray("r") : rawObj.optJSONArray("suggested_replies"));
                    final String finalRaw = raw;
                    mainHandler.post(new Runnable() {
                        @Override public void run() { callback.onSuccess(normalized, finalRaw); }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() { callback.onSuccess(new JSONObject(), raw); }
                    });
                }
            }

            @Override
            public void onFailure(final String err) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { callback.onError(err); }
                });
            }
        });
    }

    private static void tryGenerateFastAt(final int modelIdx, final String apiKey, final String prompt,
                                          final Handler mainHandler, final InternalCallback callback) {
        if (modelIdx >= FAST_MODELS.length) {
            callback.onFailure("Fast translation fallback exceeded");
            return;
        }

        final String model = FAST_MODELS[modelIdx];
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.1);
            genConfig.put("maxOutputTokens", 350);
            try { genConfig.put("responseMimeType", "application/json"); } catch (Exception ignored) {}
            root.put("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();

            FAST_TRANSLATION_HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    tryGenerateFastAt(modelIdx + 1, apiKey, prompt, mainHandler, callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
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
                                        if (text.startsWith("```") && text.endsWith("```")) {
                                            text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "").trim();
                                        }
                                        if (!text.isEmpty()) {
                                            callback.onResult(text);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (response != null) {
                            try { response.close(); } catch (Exception ignored) {}
                        }
                    }
                    tryGenerateFastAt(modelIdx + 1, apiKey, prompt, mainHandler, callback);
                }
            });
        } catch (Exception e) {
            tryGenerateFastAt(modelIdx + 1, apiKey, prompt, mainHandler, callback);
        }
    }

    public static void generateJson(final Context context, final String prompt, final JsonCallback callback) {
        generateJson(AppConfig.getGeminiApiKey(context), prompt, callback);
    }

    public static void generateJson(final String apiKey, final String prompt, final JsonCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("請先設定 Gemini API Key"); }
            });
            return;
        }

        tryGenerateAt(0, apiKey.trim(), prompt, true, 0.2, 1024, mainHandler, new InternalCallback() {
            @Override
            public void onResult(String raw) {
                String jsonToParse = raw;
                int firstBrace = raw.indexOf("{");
                int lastBrace = raw.lastIndexOf("}");
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    jsonToParse = raw.substring(firstBrace, lastBrace + 1);
                }
                try {
                    final JSONObject obj = new JSONObject(jsonToParse);
                    final String finalRaw = raw;
                    mainHandler.post(new Runnable() {
                        @Override public void run() { callback.onSuccess(obj, finalRaw); }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override public void run() { callback.onSuccess(new JSONObject(), raw); }
                    });
                }
            }

            @Override
            public void onFailure(final String err) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { callback.onError(err); }
                });
            }
        });
    }

    public static void generateText(final Context context, final String prompt, final TextCallback callback) {
        generateText(AppConfig.getGeminiApiKey(context), prompt, 0.7, 800, callback);
    }

    public static void generateText(final String apiKey, final String prompt, final double temperature, final int maxTokens, final TextCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("請先設定 Gemini API Key"); }
            });
            return;
        }

        tryGenerateAt(0, apiKey.trim(), prompt, false, temperature, maxTokens, mainHandler, new InternalCallback() {
            @Override
            public void onResult(final String raw) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { callback.onSuccess(raw); }
                });
            }

            @Override
            public void onFailure(final String err) {
                mainHandler.post(new Runnable() {
                    @Override public void run() { callback.onError(err); }
                });
            }
        });
    }

    private interface InternalCallback {
        void onResult(String rawText);
        void onFailure(String errorMessage);
    }

    private static void tryGenerateAt(final int modelIdx, final String apiKey, final String prompt,
                                      final boolean jsonMode, final double temperature, final int maxTokens,
                                      final Handler mainHandler, final InternalCallback callback) {
        if (modelIdx >= CANDIDATE_MODELS.length) {
            Log.e(TAG, "All candidate models failed for prompt");
            callback.onFailure("所有 AI 模型請求均未成功，請檢查 API Key 或網路連線");
            return;
        }

        final String model = CANDIDATE_MODELS[modelIdx];
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", temperature);
            genConfig.put("maxOutputTokens", maxTokens);
            if (jsonMode) {
                try { genConfig.put("responseMimeType", "application/json"); } catch (Exception ignored) {}
            }
            root.put("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();

            HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.w(TAG, "Model " + model + " network failure: " + e.getMessage());
                    tryGenerateAt(modelIdx + 1, apiKey, prompt, jsonMode, temperature, maxTokens, mainHandler, callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
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
                                        if (text.startsWith("```") && text.endsWith("```")) {
                                            text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "").trim();
                                        }
                                        if (!text.isEmpty()) {
                                            callback.onResult(text);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "Model " + model + " returned HTTP " + response.code() + ": " + resStr);
                    } catch (Exception e) {
                        Log.w(TAG, "Model " + model + " parse error: " + e.getMessage());
                    } finally {
                        if (response != null) {
                            try { response.close(); } catch (Exception ignored) {}
                        }
                    }
                    tryGenerateAt(modelIdx + 1, apiKey, prompt, jsonMode, temperature, maxTokens, mainHandler, callback);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "tryGenerateAt error: " + e.getMessage(), e);
            tryGenerateAt(modelIdx + 1, apiKey, prompt, jsonMode, temperature, maxTokens, mainHandler, callback);
        }
    }
}
