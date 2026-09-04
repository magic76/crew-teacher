package com.crewpocket.teacher;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class NativeGeminiLiveClient {
    private static final String TAG = "CrewTeacherLiveClient";
    private static final String HOST = "generativelanguage.googleapis.com";
    private static final String WS_PATH = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";

    public interface Listener {
        void onStatus(String status);
        void onStopped(String reason);
        void onTranscript(String text, String role);
        void onMicrophoneLevel(double dbfs, double gateDbfs, boolean sending);
        void onSpeakingChanged(boolean speaking);
    }

    private final Context context;
    private final String apiKey;
    private final String voiceName;
    private final String tutorLang;
    private final String tutorPersona;
    private String noiseMode;
    private int noiseSuppression;
    private final String liveTone;
    private int interruptionSensitivity;
    private String audioOutput;
    private final String customPrompt;
    private final Listener listener;

    private final OkHttpClient httpClient;
    private WebSocket webSocket;
    private volatile boolean running;
    private volatile boolean setupReady;
    private volatile boolean aiSpeaking;
    private volatile boolean interruptedCurrentTurn;
    private volatile boolean allowVoiceInterruption = true; // 🎙️ 語音插話開關
    private volatile boolean agentMuted = false;

    private AudioRecord recorder;
    private AcousticEchoCanceler aecEffect;
    private NoiseSuppressor nsEffect;
    private AudioTrack player;
    private final Object playerLock = new Object();
    private boolean usingOboeOutput;
    private String audioOutputBackend = "準備中";

    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<byte[]>(64);
    private Thread audioPlaybackThread;
    private volatile boolean audioPlaybackRunning;

    private final Handler interruptionHandler = new Handler(Looper.getMainLooper());
    private final Runnable clearInterruptedFallback = new Runnable() {
        @Override public void run() { interruptedCurrentTurn = false; }
    };

    private volatile long lastPlaybackActiveAt = 0;
    private volatile long lastMeterReportAt = 0;
    private double noiseFloor = 0.015;
    private static final int CALIBRATION_FRAMES = 20; // 800 ms at 40 ms/frame

    private String stage = "初始化";
    private String resumptionHandle;

    public NativeGeminiLiveClient(Context context, String apiKey, String voiceName, String tutorLang, String tutorPersona,
                                  String noiseMode, int noiseSuppression, String liveTone, int interruptionSensitivity,
                                  String audioOutput, String customPrompt, Listener listener) {
        this.context = context;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.voiceName = voiceName == null || voiceName.isEmpty() ? AppConfig.DEFAULT_VOICE : voiceName;
        this.tutorLang = tutorLang == null || tutorLang.isEmpty() ? AppConfig.DEFAULT_TUTOR_LANG : tutorLang;
        this.tutorPersona = tutorPersona == null || tutorPersona.isEmpty() ? AppConfig.DEFAULT_PERSONA : tutorPersona;
        this.noiseMode = noiseMode == null ? "auto" : noiseMode;
        this.noiseSuppression = Math.max(0, Math.min(100, noiseSuppression));
        this.liveTone = liveTone == null ? "warm" : liveTone;
        this.interruptionSensitivity = Math.max(0, Math.min(100, interruptionSensitivity));
        this.audioOutput = "media".equals(audioOutput) ? "media" : "call";
        this.customPrompt = customPrompt == null ? "" : customPrompt.trim();
        this.listener = listener;

        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .build();
    }

    public synchronized void start() {
        if (running) return;
        if (apiKey.isEmpty()) {
            fail("請先在設定中輸入 Gemini API Key", null);
            return;
        }
        running = true;
        setupReady = false;
        reportStage("正在連線至 Gemini Live 語音引擎…");

        String url = "wss://" + HOST + WS_PATH + "?key=" + apiKey;
        Request request = new Request.Builder().url(url).build();
        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket socket, Response response) {
                try {
                    if (!socket.send(buildSetup())) throw new Exception("setup 傳送失敗");
                    reportStage("等待導師連線完成…");
                } catch (Exception e) {
                    fail("初始化失敗：" + e.getMessage(), e);
                }
            }

            @Override public void onMessage(WebSocket socket, String text) {
                handleServerMessage(text);
            }

            @Override public void onMessage(WebSocket socket, ByteString bytes) {
                handleServerMessage(bytes.utf8());
            }

            @Override public void onClosing(WebSocket socket, int code, String reason) {
                reportStage("連線關閉中");
            }

            @Override public void onClosed(WebSocket socket, int code, String reason) {
                if (running) fail("連線已結束：" + reason, null);
            }

            @Override public void onFailure(WebSocket socket, Throwable t, Response response) {
                String detail = t != null ? t.getMessage() : "未知錯誤";
                if (response != null) {
                    detail = "HTTP " + response.code() + " " + response.message() + " (" + detail + ")";
                }
                fail("WebSocket 連線異常：" + detail, t);
            }
        });
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        reportStage("已結束練習");
        if (webSocket != null) {
            try { webSocket.close(1000, "User stopped"); } catch (Exception ignored) {}
            webSocket = null;
        }
        stopAudio();
    }

    public boolean isRunning() { return running; }
    public String getStage() { return stage; }
    public boolean isAiSpeaking() { return aiSpeaking; }
    public boolean isAgentMuted() { return agentMuted; }

    public boolean toggleAgentMute() {
        if (aiSpeaking) {
            triggerLocalInterruption();
            return false;
        }
        agentMuted = !agentMuted;
        if (agentMuted) {
            stopPlayback();
        }
        return agentMuted;
    }

    public void setAudioOutput(String output) {
        this.audioOutput = "media".equals(output) ? "media" : "call";
        if (running) {
            createAudioPlayer();
        }
    }

    public void setAllowVoiceInterruption(boolean allow) { this.allowVoiceInterruption = allow; }
    public boolean isVoiceInterruptionAllowed() { return allowVoiceInterruption; }
    public int getInterruptionSensitivity() { return interruptionSensitivity; }
    public void setInterruptionSensitivity(int val) { this.interruptionSensitivity = Math.max(0, Math.min(100, val)); }

    public void triggerLocalInterruption() {
        interruptedCurrentTurn = true;
        interruptionHandler.removeCallbacks(clearInterruptedFallback);
        interruptionHandler.postDelayed(clearInterruptedFallback, 1500);

        stopPlayback();
        aiSpeaking = false;
        lastPlaybackActiveAt = 0;
        listener.onSpeakingChanged(false);
        reportStage("🎙️ 請說話，導師聆聽中…");

        // Send a brief zeroed silence frame to trigger Gemini server VAD turn cutoff cleanly
        try {
            if (webSocket != null) {
                byte[] silence = new byte[3200];
                JSONObject root = new JSONObject();
                JSONObject audio = new JSONObject();
                audio.put("mimeType", "audio/pcm;rate=16000");
                audio.put("data", Base64.encodeToString(silence, Base64.NO_WRAP));
                root.put("realtimeInput", new JSONObject().put("audio", audio));
                webSocket.send(root.toString());
            }
        } catch (Exception ignored) {}
    }

    private void stopPlayback() {
        audioQueue.clear();
        if (usingOboeOutput) NativeOboeOutput.flush();
        synchronized (playerLock) {
            if (player != null && player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                try { player.pause(); player.flush(); } catch (Exception ignored) {}
            }
        }
    }

    private final StringBuilder currentAiTurnText = new StringBuilder();

    private void handleServerMessage(String text) {
        try {
            JSONObject response = new JSONObject(text);
            if (response.has("setupComplete") || response.has("setup_complete")) {
                setupReady = true;
                reportStage("🎙️ 導師已上線，請開始說話！");
                startAudio();
                return;
            }

            if (response.has("sessionResumptionUpdate")) {
                JSONObject update = response.getJSONObject("sessionResumptionUpdate");
                resumptionHandle = update.optString("handle", resumptionHandle);
            }

            JSONObject server = response.optJSONObject("serverContent");
            if (server == null) server = response.optJSONObject("server_content");

            if (server != null) {
                if (server.optBoolean("interrupted", false)) {
                    stopPlayback();
                    if (aiSpeaking) {
                        aiSpeaking = false;
                        lastPlaybackActiveAt = 0;
                        listener.onSpeakingChanged(false);
                    }
                    interruptionHandler.removeCallbacks(clearInterruptedFallback);
                    interruptedCurrentTurn = false;
                    currentAiTurnText.setLength(0);
                    return;
                }

                JSONObject inputTranscript = server.optJSONObject("inputTranscription");
                if (inputTranscript == null) inputTranscript = server.optJSONObject("input_transcription");
                if (inputTranscript != null && !inputTranscript.optString("text").isEmpty()) {
                    listener.onTranscript(inputTranscript.optString("text"), "user");
                }

                JSONObject outputTranscript = server.optJSONObject("outputTranscription");
                if (outputTranscript == null) outputTranscript = server.optJSONObject("output_transcription");
                if (outputTranscript != null && !outputTranscript.optString("text").isEmpty()) {
                    String t = outputTranscript.optString("text");
                    currentAiTurnText.append(t);
                    listener.onTranscript(t, "ai");
                }

                JSONObject turn = server.optJSONObject("modelTurn");
                if (turn == null) turn = server.optJSONObject("model_turn");
                if (turn != null && !interruptedCurrentTurn) {
                    if (!aiSpeaking) {
                        aiSpeaking = true;
                        listener.onSpeakingChanged(true);
                    }
                    JSONArray parts = turn.optJSONArray("parts");
                    if (parts != null) {
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.getJSONObject(i);
                            if (part.has("text")) {
                                String t = part.getString("text");
                                currentAiTurnText.append(t);
                                listener.onTranscript(t, "ai");
                            }
                            JSONObject inline = part.optJSONObject("inlineData");
                            if (inline == null) inline = part.optJSONObject("inline_data");
                            if (inline != null && "audio/pcm;rate=24000".equals(inline.optString("mimeType"))) {
                                byte[] pcm = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                                enqueueAudio(pcm);
                            }
                        }
                    }
                }

                if (server.optBoolean("turnComplete", server.optBoolean("turn_complete", false))) {
                    if (usingOboeOutput) NativeOboeOutput.finishTurn();
                    interruptedCurrentTurn = false;
                    interruptionHandler.removeCallbacks(clearInterruptedFallback);
                    lastPlaybackActiveAt = 0;
                    if (aiSpeaking) {
                        aiSpeaking = false;
                        listener.onSpeakingChanged(false);
                    }
                    final String completeTurnText = currentAiTurnText.toString().trim();
                    currentAiTurnText.setLength(0);
                    String mode = AppConfig.getTeachingMode(context);
                    if (!"immersion".equals(mode) && !completeTurnText.isEmpty()) {
                        translateAsync(completeTurnText);
                    }
                }
            }

            if (response.has("toolCall")) {
                JSONObject call = response.getJSONObject("toolCall");
                JSONArray calls = call.optJSONArray("functionCalls");
                if (calls != null && calls.length() > 0) {
                    for (int i = 0; i < calls.length(); i++) {
                        JSONObject fc = calls.getJSONObject(i);
                        String name = fc.optString("name", "");
                        String id = fc.optString("id", "call_0");
                        if ("end_voice_session".equals(name)) {
                            sendToolResponse(id, name, new JSONObject().put("success", true).put("message", "對話已結束"));
                            interruptionHandler.postDelayed(new Runnable() {
                                @Override public void run() { stop(); }
                            }, 500);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析伺服器訊息錯誤：" + e.getMessage());
        }
    }

    private static String mapToSupportedVoice(String name) {
        if (name == null || name.trim().isEmpty()) return "Kore";
        String v = name.trim();
        // Google Gemini Live WebSocket backend officially supports: "Puck", "Charon", "Kore", "Fenrir", "Aoede"
        if ("Aoede".equalsIgnoreCase(v) || "Leda".equalsIgnoreCase(v) || "Europa".equalsIgnoreCase(v) ||
            "Io".equalsIgnoreCase(v) || "Tethys".equalsIgnoreCase(v) || "Ariel".equalsIgnoreCase(v) ||
            "Sycorax".equalsIgnoreCase(v) || "Titania".equalsIgnoreCase(v) || "Despina".equalsIgnoreCase(v)) {
            return "Aoede";
        }
        if ("Puck".equalsIgnoreCase(v) || "Zephyr".equalsIgnoreCase(v) || "Hyperion".equalsIgnoreCase(v) ||
            "Enceladus".equalsIgnoreCase(v) || "Mimas".equalsIgnoreCase(v)) {
            return "Puck";
        }
        if ("Charon".equalsIgnoreCase(v) || "Orus".equalsIgnoreCase(v) || "Ganymede".equalsIgnoreCase(v) ||
            "Iapetus".equalsIgnoreCase(v) || "Aegaeon".equalsIgnoreCase(v) || "Umbriel".equalsIgnoreCase(v) ||
            "Prospero".equalsIgnoreCase(v)) {
            return "Charon";
        }
        if ("Fenrir".equalsIgnoreCase(v) || "Titan".equalsIgnoreCase(v) || "Caliban".equalsIgnoreCase(v)) {
            return "Fenrir";
        }
        return "Kore"; // Default fallback (Kore, Callisto, Rhea, Dione, Miranda, Galatea)
    }

    private String buildSetup() throws Exception {
        JSONObject root = new JSONObject();
        JSONObject setup = new JSONObject();
        setup.put("model", "models/gemini-3.1-flash-live-preview");

        JSONObject generation = new JSONObject();
        generation.put("responseModalities", new JSONArray().put("AUDIO"));
        String safeVoice = mapToSupportedVoice(voiceName);
        generation.put("speechConfig", new JSONObject().put("voiceConfig", new JSONObject().put("prebuiltVoiceConfig", new JSONObject().put("voiceName", safeVoice))));
        setup.put("generationConfig", generation);

        setup.put("contextWindowCompression", new JSONObject().put("slidingWindow", new JSONObject()));
        if (resumptionHandle != null && !resumptionHandle.isEmpty()) {
            setup.put("sessionResumption", new JSONObject().put("handle", resumptionHandle));
        } else {
            setup.put("sessionResumption", new JSONObject());
        }
        setup.put("inputAudioTranscription", new JSONObject());
        setup.put("outputAudioTranscription", new JSONObject());

        // Simple tool: end call
        JSONArray tools = new JSONArray();
        tools.put(new JSONObject().put("name", "end_voice_session")
                .put("description", "End the tutoring voice call when the user says goodbye, hang up, or exit (e.g. 結束, 掛斷, 再見, 先這樣, bye)."));
        setup.put("tools", new JSONArray().put(new JSONObject().put("functionDeclarations", tools)));

        // Dedicated Tutor Instruction
        String langName = getLanguageDisplayName(tutorLang);
        String teachingMode = AppConfig.getTeachingMode(context);
        boolean isUiEn = I18n.isEnglish(context);
        String nativeLang = (isUiEn || "zh".equalsIgnoreCase(tutorLang)) ? "English" : "Traditional Chinese (繁體中文/國語)";

        String personaDetail = "daily".equals(tutorPersona) ? "Daily life, hobbies, current events, and casual chats." :
                ("travel".equals(tutorPersona) ? "Travel scenarios (airport, hotel, ordering food, asking directions)." :
                ("business".equals(tutorPersona) ? "Professional business language, meetings, presentations, and email writing." :
                ("exam".equals(tutorPersona) ? "Oral exam preparation / certification with structured questions and feedback." : "Friendly conversational practice.")));

        String modeInstruction;
        String rules;

        if ("immersion".equals(teachingMode)) {
            // 全外語沉浸模式：100% 目標語言，不附帶螢幕翻譯
            modeInstruction = "【Teaching Mode: 100% FULL IMMERSION (全外語沉浸模式)】\n"
                    + "ABSOLUTE RULE: Speak ONLY in 100% natural, fluent, native " + langName + " throughout the ENTIRE session.\n"
                    + "NEVER speak " + nativeLang + ", NEVER provide translations in speech, and create an authentic immersion environment in " + langName + ".";
            rules = "CRITICAL CONVERSATIONAL RULES (FULL IMMERSION):\n"
                    + "1. 100% " + langName + " EXCLUSIVELY in audio: Do NOT output any " + nativeLang + " words or spoken translations.\n"
                    + "2. Keep responses natural, engaging, and concise (1-2 sentences in " + langName + ").\n"
                    + "3. Always end your turn with an engaging open-ended question in " + langName + " to keep the conversation flowing.\n"
                    + "4. GENTLE RECAST: If the student makes mistakes in " + langName + ", model the correct phrasing naturally in pure " + langName + ".\n"
                    + "5. When the student says goodbye or wants to exit, say a warm farewell in " + langName + " and call 'end_voice_session'.";
        } else if ("beginner".equals(teachingMode)) {
            // 零基礎引導模式：純外語短句示範，螢幕自動即時翻譯
            modeInstruction = "【Teaching Mode: BEGINNER STEP-BY-STEP (零基礎引導模式)】\n"
                    + "ABSOLUTE AUDIO RULE: Speak ONE short, practical, easy-to-repeat sentence in 100% pure " + langName + ".\n"
                    + "NEVER pronounce Chinese words in the audio stream. Pure audio only.";
            rules = "CRITICAL CONVERSATIONAL RULES (BEGINNER):\n"
                    + "1. Speak short, crystal-clear practical phrases in pure " + langName + " for the learner to mimic.\n"
                    + "2. DO NOT pronounce or speak any Chinese words out loud in audio.\n"
                    + "3. Encourage the student warmly whenever they try speaking.\n"
                    + "4. When the student says goodbye or wants to exit, say a warm farewell and call 'end_voice_session'.";
        } else {
            // 雙語對照模式（預設推薦）：純外語語音發音 + 螢幕字幕中文即時對照（耳聽純外語，眼看中文字幕）
            modeInstruction = "【Teaching Mode: PURE SPOKEN TARGET LANGUAGE (純外語發音口說模式)】\n"
                    + "CRITICAL SPOKEN AUDIO RULE: You MUST speak EXCLUSIVELY in 100% pure, natural, fluent " + langName + " (絕對嚴禁在語音中說出中文！)\n"
                    + "The student wants to train listening to pure " + langName + ". Translations are provided on screen visually.\n"
                    + "NEVER pronounce, read, or speak any Chinese words out loud in audio.\n"
                    + "Example spoken response:\n"
                    + "'I really love spending my weekends hiking in the mountains. How about you? What do you usually like to do on weekends?'";
            rules = "CRITICAL CONVERSATIONAL RULES:\n"
                    + "1. 100% PURE " + langName + " IN AUDIO: Speak entirely in natural " + langName + " with standard pronunciation.\n"
                    + "2. Keep spoken responses natural and concise (1-2 sentences in " + langName + ").\n"
                    + "3. Always end your turn with a clear, open-ended question in " + langName + " so the student has an easy cue to reply in " + langName + ".\n"
                    + "4. GENTLE RECAST: If the student makes grammatical mistakes, model the correct phrasing naturally in pure " + langName + ".\n"
                    + "5. When the student says goodbye or wants to exit, say a warm farewell in " + langName + " and call 'end_voice_session'.";
        }

        String baseInstruction = "You are 'Crew Teacher', an encouraging, empathetic, and friendly 1-on-1 language tutor. "
                + "Your mission is to help the user learn and speak " + langName + " naturally and fluently with zero pressure.\n"
                + "Topic / Scenario: " + personaDetail + "\n\n"
                + modeInstruction + "\n\n"
                + rules;

        if (!customPrompt.isEmpty()) {
            baseInstruction = "【User Custom Tutor Prompt】\n" + customPrompt + "\n\n" + baseInstruction;
        }

        setup.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", baseInstruction))));
        root.put("setup", setup);
        return root.toString();
    }

    private void translateAsync(final String sourceText) {
        if (apiKey == null || apiKey.isEmpty() || sourceText.isEmpty()) return;
        boolean isUiEn = I18n.isEnglish(context);
        String targetLang = isUiEn ? "English" : "Traditional Chinese (繁體中文)";
        if ("zh".equalsIgnoreCase(tutorLang) && !isUiEn) {
            targetLang = "English";
        }
        final String prompt = "Translate the following speech text into natural, concise " + targetLang + ".\n"
                + "Output ONLY the direct translation text with no markdown formatting, no explanations, and no quotes:\n\n" + sourceText;
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {}
                @Override public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
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
                                        String translation = resParts.getJSONObject(0).optString("text", "").trim();
                                        if (!translation.isEmpty()) {
                                            listener.onTranscript(translation, "translation");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}
    }

    private static String getLanguageDisplayName(String code) {
        if ("zh".equalsIgnoreCase(code) || "cmn".equalsIgnoreCase(code) || "chinese".equalsIgnoreCase(code) || "mandarin".equalsIgnoreCase(code)) return "Standard Mandarin Chinese (國語/華語/普通話)";
        if ("nan".equalsIgnoreCase(code) || "hokkien".equalsIgnoreCase(code) || "taiwanese".equalsIgnoreCase(code)) return "Taiwanese Hokkien / Southern Min (閩南語/台灣話/台語)";
        if ("hak".equalsIgnoreCase(code) || "hakka".equalsIgnoreCase(code)) return "Hakka (客家語/客語)";
        if ("yue".equalsIgnoreCase(code) || "cantonese".equalsIgnoreCase(code) || "zh-HK".equalsIgnoreCase(code)) return "Cantonese (粵語/廣東話)";
        if ("ar".equalsIgnoreCase(code)) return "Arabic (العربية)";
        if ("hi".equalsIgnoreCase(code)) return "Hindi (हिन्दी)";
        if ("ms".equalsIgnoreCase(code)) return "Malay (Bahasa Melayu)";
        if ("vi".equalsIgnoreCase(code)) return "Vietnamese (Tiếng Việt)";
        if ("ko".equalsIgnoreCase(code)) return "Korean (한국어)";
        if ("th".equalsIgnoreCase(code)) return "Thai (ภาษาไทย)";
        if ("pt".equalsIgnoreCase(code)) return "Portuguese (Português)";
        if ("ru".equalsIgnoreCase(code)) return "Russian (Русский)";
        if ("ja".equalsIgnoreCase(code)) return "Japanese (日本語)";
        if ("es".equalsIgnoreCase(code)) return "Spanish (Español)";
        if ("fr".equalsIgnoreCase(code)) return "French (Français)";
        if ("de".equalsIgnoreCase(code)) return "German (Deutsch)";
        if ("it".equalsIgnoreCase(code)) return "Italian (Italiano)";
        if ("id".equalsIgnoreCase(code)) return "Indonesian (Bahasa Indonesia)";
        return "English";
    }

    private void sendToolResponse(String id, String name, JSONObject result) {
        try {
            JSONObject item = new JSONObject().put("response", new JSONObject().put("result", result)).put("id", id).put("name", name);
            if (webSocket != null) {
                webSocket.send(new JSONObject().put("toolResponse", new JSONObject().put("functionResponses", new JSONArray().put(item))).toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "sendToolResponse error: " + e.getMessage());
        }
    }

    private void startAudio() {
        if (!running || recorder != null) return;
        int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferBytes = Math.max(min * 4, 8192);
        try {
            // 🎙️ VOICE_COMMUNICATION engages Android's hardware DSP full-duplex AEC
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
        } catch (Exception e) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
        }

        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aecEffect = AcousticEchoCanceler.create(recorder.getAudioSessionId());
                if (aecEffect != null) aecEffect.setEnabled(true);
            }
            if (NoiseSuppressor.isAvailable()) {
                nsEffect = NoiseSuppressor.create(recorder.getAudioSessionId());
                if (nsEffect != null) nsEffect.setEnabled(true);
            }
        } catch (Exception ignored) {}

        createAudioPlayer();
        startPlaybackWorker();
        recorder.startRecording();
        new Thread(new Runnable() { @Override public void run() { sendMic(); } }, "crew-teacher-mic").start();
    }

    private void createAudioPlayer() {
        usingOboeOutput = NativeOboeOutput.start(audioOutput);
        if (usingOboeOutput) {
            audioOutputBackend = "Oboe/AAudio Low-Latency";
            return;
        }
        audioOutputBackend = "AudioTrack Fallback";
        synchronized (playerLock) {
            try { if (player != null) { player.stop(); player.release(); } } catch (Exception ignored) {}
            int outMin = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(outMin * 8, 48000);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage("media".equals(audioOutput) ? AudioAttributes.USAGE_MEDIA : AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            AudioFormat format = new AudioFormat.Builder().setSampleRate(24000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            player = new AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(format)
                    .setBufferSizeInBytes(bufferBytes).setTransferMode(AudioTrack.MODE_STREAM).build();
        }
    }

    private void startPlaybackWorker() {
        audioQueue.clear();
        if (usingOboeOutput) { audioPlaybackRunning = true; return; }
        audioPlaybackRunning = true;
        audioPlaybackThread = new Thread(new Runnable() {
            @Override public void run() { runPlaybackLoop(); }
        }, "crew-teacher-playback");
        audioPlaybackThread.start();
    }

    private void runPlaybackLoop() {
        boolean started = false;
        while (audioPlaybackRunning) {
            try {
                byte[] first = audioQueue.poll(300, TimeUnit.MILLISECONDS);
                if (first == null) continue;
                if (!started) {
                    ArrayList<byte[]> initial = new ArrayList<byte[]>();
                    initial.add(first);
                    int bytes = first.length;
                    long deadline = System.currentTimeMillis() + 180;
                    while (bytes < 9600 && System.currentTimeMillis() < deadline) {
                        byte[] next = audioQueue.poll(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                        if (next == null) break;
                        initial.add(next); bytes += next.length;
                    }
                    synchronized (playerLock) { if (player != null) player.play(); }
                    started = true;
                    for (byte[] chunk : initial) writeAudioChunk(chunk);
                } else {
                    writeAudioChunk(first);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception error) {
                recoverAudioPlayer();
                started = false;
            }
        }
    }

    private void writeAudioChunk(byte[] pcm) {
        if (pcm == null || pcm.length == 0 || interruptedCurrentTurn || agentMuted) return;
        int written;
        synchronized (playerLock) {
            if (player != null && player.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                try { player.play(); } catch (Exception ignored) {}
            }
            written = player == null ? AudioTrack.ERROR_INVALID_OPERATION : player.write(pcm, 0, pcm.length);
        }
        if (written < 0) recoverAudioPlayer();
    }

    private void recoverAudioPlayer() {
        if (!audioPlaybackRunning || !running) return;
        createAudioPlayer();
    }

    private void enqueueAudio(byte[] pcm) {
        if (agentMuted || interruptedCurrentTurn || pcm == null || pcm.length == 0) return;
        long durationMs = pcm.length * 1000L / (24000 * 2);
        lastPlaybackActiveAt = Math.max(System.currentTimeMillis(), lastPlaybackActiveAt) + durationMs;
        if (usingOboeOutput) { NativeOboeOutput.write(pcm); return; }
        if (!audioQueue.offer(pcm)) {
            audioQueue.poll();
            audioQueue.offer(pcm);
        }
    }

    public boolean isAudioActuallyPlaying() {
        if (!running) return false;
        long now = System.currentTimeMillis();
        if (usingOboeOutput) {
            return NativeOboeOutput.getBufferedMs() > 0 || now < lastPlaybackActiveAt + 150;
        }
        return !audioQueue.isEmpty() || now < lastPlaybackActiveAt + 150;
    }

    private double calculateRms(byte[] pcm, int count) {
        if (count < 2) return 0;
        long sum = 0;
        int samples = count / 2;
        for (int i = 0; i < count - 1; i += 2) {
            short val = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            sum += (long) val * val;
        }
        return Math.sqrt((double) sum / samples) / 32768.0;
    }

    private void sendMic() {
        byte[] pcm = new byte[1280]; // 40ms @ 16kHz
        int consecutiveVoiceFrames = 0;
        int calibrationFrames = 0;
        double[] calibrationSamples = new double[CALIBRATION_FRAMES];

        while (running && recorder != null && webSocket != null) {
            int count = recorder.read(pcm, 0, pcm.length);
            if (count <= 0) {
                try { Thread.sleep(20); } catch (Exception ignored) {}
                continue;
            }
            if (agentMuted) continue;

            long now = System.currentTimeMillis();
            boolean currentlyPlaying = isAudioActuallyPlaying();

            // Watchdog: If AI finished playing all audio frames for >250ms, auto-release aiSpeaking state
            if (aiSpeaking && !currentlyPlaying && now > lastPlaybackActiveAt + 250) {
                aiSpeaking = false;
                lastPlaybackActiveAt = 0;
                interruptedCurrentTurn = false;
                interruptionHandler.removeCallbacks(clearInterruptedFallback);
                listener.onSpeakingChanged(false);
            }

            double rms = calculateRms(pcm, count);
            String mode = noiseMode;
            int suppression = noiseSuppression;

            // Acoustic calibration at start
            if (calibrationFrames < CALIBRATION_FRAMES) {
                calibrationSamples[calibrationFrames] = rms;
                calibrationFrames++;
                if (calibrationFrames == CALIBRATION_FRAMES) {
                    Arrays.sort(calibrationSamples);
                    double baseline = 0;
                    for (int i = 0; i < 12; i++) baseline += calibrationSamples[i];
                    noiseFloor = Math.min(0.025, Math.max(0.005, baseline / 12.0));
                    listener.onStatus("環境降噪已校正（" + mode + "）");
                }
            }

            double modeBase = "noisy".equals(mode) ? 1.35 : ("quiet".equals(mode) ? 0.60 : 0.85);
            double gateMultiplier = modeBase + suppression * 0.006;
            double minBase = "noisy".equals(mode) ? 0.018 : ("quiet".equals(mode) ? 0.002 : 0.005);
            double minGate = minBase + suppression * 0.00008;
            double gateThreshold = Math.max(minGate, noiseFloor * gateMultiplier);
            boolean speechCandidate = rms >= gateThreshold;

            if (!speechCandidate && !currentlyPlaying) {
                // Decay noise floor safely, strictly capped below speech level (0.035)
                double clampedRms = Math.min(rms, 0.030);
                noiseFloor = Math.min(0.035, noiseFloor * 0.985 + clampedRms * 0.015);
            }

            // Only enforce interruption gate when the speaker is PHYSICALLY PLAYING audio right now
            if (currentlyPlaying) {
                // If sensitivity <= 25 (Shield Mode) or interruption disabled:
                // Completely protect the tutor speech from speaker acoustic loopback
                if (!allowVoiceInterruption || interruptionSensitivity <= 25) {
                    consecutiveVoiceFrames = 0;
                    reportMicrophoneLevel(rms, gateThreshold, false);
                    continue;
                }

                // If user enabled high sensitivity interruption:
                // Require significant volume overcoming phone speakerphone acoustic leakage (>=0.12 - 0.22 RMS)
                double sensitivity = interruptionSensitivity / 100.0;
                int requiredVoiceFrames = 8 + Math.round((1.0f - (float) sensitivity) * 8.0f); // 320ms - 640ms
                double baseInterrupt = 0.12 + (1.0 - sensitivity) * 0.10;
                double interruptThreshold = Math.max(baseInterrupt, noiseFloor * 2.5);

                if (speechCandidate && rms >= interruptThreshold) {
                    consecutiveVoiceFrames++;
                    if (consecutiveVoiceFrames >= requiredVoiceFrames) {
                        triggerLocalInterruption();
                        consecutiveVoiceFrames = 0;
                    }
                } else {
                    consecutiveVoiceFrames = 0;
                }

                // While AI is playing audio, NEVER send microphone echo to Gemini WebSocket
                reportMicrophoneLevel(rms, gateThreshold, false);
                continue;
            } else {
                consecutiveVoiceFrames = 0;
            }

            // Check WebSocket queue backlog to prevent buffer freeze on slow network
            if (webSocket != null && webSocket.queueSize() > 64 * 1024 && !speechCandidate) {
                reportMicrophoneLevel(rms, gateThreshold, false);
                continue;
            }

            byte[] chunk = (count == pcm.length) ? pcm.clone() : Arrays.copyOf(pcm, count);
            reportMicrophoneLevel(rms, gateThreshold, true);

            // 🎙️ 連續即時串流給 Gemini Live
            try {
                JSONObject root = new JSONObject();
                JSONObject audio = new JSONObject();
                audio.put("mimeType", "audio/pcm;rate=16000");
                audio.put("data", Base64.encodeToString(chunk, Base64.NO_WRAP));
                root.put("realtimeInput", new JSONObject().put("audio", audio));
                if (!webSocket.send(root.toString())) throw new Exception("audio send failed");
            } catch (Exception error) {
                fail("麥克風傳輸失敗：" + error.getMessage(), error);
            }
        }
    }

    private void reportMicrophoneLevel(double rms, double gate, boolean sending) {
        long now = System.currentTimeMillis();
        if (now - lastMeterReportAt < 180) return;
        lastMeterReportAt = now;
        double dbfs = rms <= 0.000001 ? -96.0 : Math.max(-96.0, 20.0 * Math.log10(rms));
        double gateDbfs = gate <= 0.000001 ? -96.0 : Math.max(-96.0, 20.0 * Math.log10(gate));
        listener.onMicrophoneLevel(dbfs, gateDbfs, sending);
    }

    private void reportStage(String text) {
        stage = text;
        listener.onStatus(text);
    }

    private synchronized void fail(String message, Throwable error) {
        if (!running) return;
        if (error != null) Log.e(TAG, message, error); else Log.e(TAG, message);
        running = false;
        interruptionHandler.removeCallbacks(clearInterruptedFallback);
        stopAudio();
        listener.onStopped(message);
    }

    private void stopAudio() {
        audioPlaybackRunning = false;
        audioQueue.clear();
        if (usingOboeOutput) { NativeOboeOutput.stop(); usingOboeOutput = false; }
        try { if (audioPlaybackThread != null) audioPlaybackThread.interrupt(); } catch (Exception ignored) {}
        audioPlaybackThread = null;
        if (aecEffect != null) { try { aecEffect.release(); } catch (Exception ignored) {} aecEffect = null; }
        if (nsEffect != null) { try { nsEffect.release(); } catch (Exception ignored) {} nsEffect = null; }
        try { if (recorder != null) { recorder.stop(); recorder.release(); recorder = null; } } catch (Exception ignored) {}
        synchronized (playerLock) {
            try { if (player != null) { player.stop(); player.release(); player = null; } } catch (Exception ignored) {}
        }
    }
}
