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
        default void onSubtitleData(String targetText, String nativeTranslation, String keyVocab, java.util.List<String> suggestedReplies) {}
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
    private volatile long totalFramesWritten = 0;

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
        totalFramesWritten = 0;
        lastPlaybackActiveAt = 0;
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
                boolean hasOutputTranscript = false;
                if (outputTranscript != null && !outputTranscript.optString("text").isEmpty()) {
                    String t = outputTranscript.optString("text");
                    currentAiTurnText.append(t);
                    listener.onTranscript(t, "ai");
                    hasOutputTranscript = true;
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
                            JSONObject inline = part.optJSONObject("inlineData");
                            if (inline == null) inline = part.optJSONObject("inline_data");
                            if (inline != null && "audio/pcm;rate=24000".equals(inline.optString("mimeType"))) {
                                byte[] pcm = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                                enqueueAudio(pcm);
                            } else if (part.has("text") && !hasOutputTranscript) {
                                String t = part.getString("text");
                                currentAiTurnText.append(t);
                                listener.onTranscript(t, "ai");
                            }
                        }
                    }
                }

                if (server.optBoolean("turnComplete", server.optBoolean("turn_complete", false))) {
                    if (usingOboeOutput) NativeOboeOutput.finishTurn();
                    interruptedCurrentTurn = false;
                    interruptionHandler.removeCallbacks(clearInterruptedFallback);
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
                            sendToolResponse(id, name, new JSONObject().put("status", "ended"));
                            interruptionHandler.postDelayed(new Runnable() {
                                @Override public void run() { stop(); }
                            }, 500);
                        } else {
                            sendToolResponse(id, name, new JSONObject().put("status", "ok"));
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

        // Dedicated Tutor Instruction
        String langName = getLanguageDisplayName(tutorLang);
        String teachingMode = AppConfig.getTeachingMode(context);
        String nativeLang = AppConfig.getStudentLanguageDisplayName(context);

        // Tool declarations
        JSONArray tools = new JSONArray();
        tools.put(new JSONObject().put("name", "end_voice_session")
                .put("description", "End the tutoring voice call when the user says goodbye, hang up, or exit (e.g. 結束, 掛斷, 再見, 先這樣, bye)."));

        setup.put("tools", new JSONArray().put(new JSONObject().put("functionDeclarations", tools)));

        String personaDetail;
        if ("travel".equals(tutorPersona)) {
            personaDetail = "Travel scenarios (airport customs, hotel check-in, ordering food in restaurants, asking directions, transportation). Act as locals, flight attendants, hotel receptionists, or waiters.";
        } else if ("business".equals(tutorPersona)) {
            personaDetail = "Professional business communication (project updates, sprint planning, client negotiations, business presentations, cross-cultural teamwork). Maintain a professional, concise, and structured workplace tone.";
        } else if ("interview".equals(tutorPersona)) {
            personaDetail = "Realistic job interview simulation. Act as a demanding but encouraging interviewer. Ask behavioral questions using the STAR framework, probe into background, strengths, problem-solving, and salary expectations.";
        } else if ("exam".equals(tutorPersona)) {
            personaDetail = "Standardized speaking exam simulation (IELTS Speaking Part 1/2/3, TOEFL iBT, TOEIC). Give prompt cards, ask in-depth abstract follow-up questions, and evaluate fluency, lexical resource, and coherence.";
        } else if ("shopping".equals(tutorPersona)) {
            personaDetail = "Shopping, retail and negotiation scenarios (asking for sizes/discounts, bargaining at markets, processing tax refunds, handling returns and exchanges). Act as shop assistants and cashiers.";
        } else if ("medical".equals(tutorPersona)) {
            personaDetail = "Medical and pharmacy scenarios (describing physical symptoms, visiting a clinic/hospital, consulting a pharmacist, dental visits). Act as doctors, nurses, or pharmacists.";
        } else if ("housing".equals(tutorPersona)) {
            personaDetail = "Apartment hunting and tenancy scenarios (inquiring about apartment listings, negotiating lease terms, discussing house rules with roommates, requesting repairs with landlord).";
        } else if ("dating".equals(tutorPersona)) {
            personaDetail = "Social mingling, coffee chats, and dating scenarios (breaking the ice, casual banter, sharing interesting life stories, finding common hobbies). Keep the mood friendly, humorous, and natural.";
        } else if ("tech".equals(tutorPersona)) {
            personaDetail = "Tech, software engineering, and AI discussions (system architecture, coding best practices, machine learning, generative AI, tech news, startup ecosystem). Use authentic technical vocabulary.";
        } else {
            personaDetail = "Daily life, hobbies, current events, weekend plans, and casual friendly chats.";
        }

        String modeInstruction;
        String rules;

        if ("shadowing".equals(teachingMode)) {
            String readingContent = AppConfig.getReadingText(context);
            modeInstruction = "【Teaching Mode: STRICT SPARTAN PRONUNCIATION COACH (斯巴達嚴格朗讀糾音特訓)】\n"
                    + "Mission: You are an exacting, sharp-eared native pronunciation coach with zero tolerance for sloppy articulation or false flattery. The student will read the following passage aloud:\n\n"
                    + "--- REFERENCE TEXT ---\n"
                    + readingContent + "\n"
                    + "----------------------\n\n"
                    + "STRICT COACHING PROTOCOL:\n"
                    + "1. ATTENTIVE SCRUTINY: Listen with microscopic precision to their pronunciation, syllable stress, vowel clarity, consonant endings (-ed, -s, -th, -l/-r), linking/liaison, and natural cadence.\n"
                    + "2. ZERO EMPTY FLATTERY: Do NOT say generic compliments like 'Good job!' or 'Awesome!'. Go straight to the sharp phonetic critique.\n"
                    + "3. PINPOINT & REPEAT DRILL (Mandatory):\n"
                    + "   - Identify 1 to 2 exact words/phrases where the student had phonetic flaws, displaced syllable stress, or swallowed endings.\n"
                    + "   - Clearly explain the exact phonetic fault (e.g. \"Watch your stress on 'comfortable' — stress the first syllable: /ˈkʌmftəbl/, not com-for-TA-ble.\")\n"
                    + "   - Model the accurate pronunciation crisply and command the student to repeat it after you: \"Repeat after me: [Word]. Say it twice!\"\n"
                    + "4. AUDIO LANGUAGE RULE: Speak 100% in natural, sharp, professional " + langName + ". Never pronounce Chinese in audio.";
            rules = "CRITICAL COACHING RULES:\n"
                    + "1. 100% " + langName + " IN AUDIO: Spoken feedback must be in crisp, articulate " + langName + ".\n"
                    + "2. Strict and professional: prioritize precision, syllable stress, and rhythm over politeness.\n"
                    + "3. When the student says goodbye or wants to exit, say a concise farewell in " + langName + " and call 'end_voice_session'.";
        } else if ("immersion".equals(teachingMode)) {
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
            modeInstruction = "【Teaching Mode: BEGINNER STEP-BY-STEP (零基礎引導模式)】\n"
                    + "1. Speak ONE short, crystal-clear, practical sentence in 100% pure " + langName + " for the learner to mimic.\n"
                    + "2. Never pronounce Chinese words out loud in audio.\n"
                    + "3. Warmly encourage the student to repeat after you.";
            rules = "CRITICAL CONVERSATIONAL RULES (BEGINNER):\n"
                    + "1. Keep sentences short and clear in pure " + langName + ".\n"
                    + "2. DO NOT pronounce or speak any " + nativeLang + " words out loud in audio.\n"
                    + "3. When the student says goodbye or wants to exit, say a warm farewell and call 'end_voice_session'.";
        } else {
            modeInstruction = "【Teaching Mode: BILINGUAL CONVERSATIONAL PRACTICE (雙語對照教學模式)】\n"
                    + "1. AUDIO RULE (Pure Target Language): Speak 100% in natural, fluent, native " + langName + " (絕對嚴禁在語音中說出 " + nativeLang + "！耳聽純外語沉浸).\n"
                    + "2. Keep spoken responses conversational and concise (1-2 sentences in " + langName + ").\n"
                    + "3. Always end your spoken sentence with an engaging open-ended question in " + langName + " so the student has an easy cue to reply in " + langName + ".";
            rules = "CRITICAL BILINGUAL RULES:\n"
                    + "1. 100% PURE " + langName + " IN AUDIO: Never speak " + nativeLang + " in the audio stream.\n"
                    + "2. Keep spoken responses natural and concise (1-2 sentences in " + langName + ").\n"
                    + "3. ACTIVE RECAST: If the student makes mistakes in " + langName + ", model the corrected sentence naturally in " + langName + ".\n"
                    + "4. When the student says goodbye or wants to exit, say farewell in " + langName + " and call 'end_voice_session'.";
        }

        String recastProtocol = "\n\n【NATURAL RECAST & PRONUNCIATION MODELING PROTOCOL】:\n"
                + "1. GENTLE RECAST: If the student speaks with grammar flaws, unnatural collocations, or mispronounced/stumbled words, seamlessly RECAST and model the authentic native phrasing and pronunciation naturally in your conversational reply before moving the topic forward.\n"
                + "   Example: If the student says 'I very like comfortable island', naturally echo: 'Oh, you really loved that comfortable [ˈkʌmftəbl] island [ˈaɪlənd]? What made it so special?'\n"
                + "2. Maintain natural, engaging dialogue flow while giving crystal-clear phonetic modeling.\n"
                + "3. 100% PURE " + langName + " IN AUDIO: Never speak " + nativeLang + " words in audio.";

        String baseInstruction = "You are 'Crew Teacher', an insightful, precise, and rigorous 1-on-1 language coach. "
                + "Your mission is to help the user master authentic native " + langName + " with accurate pronunciation, rhythm, and natural expressions.\n"
                + "Topic / Scenario: " + personaDetail + "\n\n"
                + modeInstruction + "\n\n"
                + rules
                + recastProtocol;

        if (!customPrompt.isEmpty()) {
            baseInstruction = "【User Custom Tutor Prompt】\n" + customPrompt + "\n\n" + baseInstruction;
        }

        setup.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", baseInstruction))));
        root.put("setup", setup);
        return root.toString();
    }

    private static final String[] TRANSLATION_MODELS = {
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite"
    };

    private void translateAsync(final String sourceText) {
        if (apiKey == null || apiKey.isEmpty() || sourceText.isEmpty()) return;
        String targetLang = AppConfig.getStudentLanguageDisplayName(context);
        String practiceLang = getLanguageDisplayName(tutorLang);
        final String prompt = "You are an expert oral language tutor assistant.\n"
                + "Spoken sentence in " + practiceLang + ":\n\"" + sourceText + "\"\n\n"
                + "Student native language: " + targetLang + "\n\n"
                + "Return a strictly valid JSON object with EXACTLY these keys:\n"
                + "{\n"
                + "  \"translation\": \"(fluent translation in " + targetLang + ")\",\n"
                + "  \"key_vocab\": \"(1-2 key vocabulary words with phonetic/pronunciation tips or grammar notes in " + targetLang + ", e.g. 'comfortable (/ˈkʌmftəbl/ 重音在第一音節) - 舒適的', or empty)\",\n"
                + "  \"suggested_replies\": [\n"
                + "    \"(Sample reply 1 in " + practiceLang + ") ((translation in " + targetLang + "))\",\n"
                + "    \"(Sample reply 2 in " + practiceLang + ") ((translation in " + targetLang + "))\"\n"
                + "  ]\n"
                + "}\n"
                + "Output ONLY the JSON object without markdown fences or code blocks.";
        tryTranslateAt(0, sourceText, prompt);
    }

    private void tryTranslateAt(final int modelIdx, final String sourceText, final String prompt) {
        if (modelIdx >= TRANSLATION_MODELS.length) {
            Log.e(TAG, "All translation models failed for prompt");
            return;
        }
        final String model = TRANSLATION_MODELS[modelIdx];
        try {
            JSONObject root = new JSONObject();
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", parts));
            root.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.2);
            genConfig.put("maxOutputTokens", 1024);
            root.put("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), root.toString());
            Request req = new Request.Builder().url(url).post(body).build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.w(TAG, "Translation model " + model + " failed: " + e.getMessage());
                    tryTranslateAt(modelIdx + 1, sourceText, prompt);
                }
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
                                        String raw = resParts.getJSONObject(0).optString("text", "").trim();
                                        if (raw.startsWith("```")) {
                                            raw = raw.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
                                        }
                                        try {
                                            JSONObject obj = new JSONObject(raw);
                                            String mainTrans = obj.optString("translation", "").trim();
                                            String keyVocab = obj.optString("key_vocab", "").trim();
                                            JSONArray hintsArr = obj.optJSONArray("suggested_replies");
                                            java.util.List<String> hints = new java.util.ArrayList<String>();
                                            if (hintsArr != null) {
                                                for (int h = 0; h < hintsArr.length(); h++) {
                                                    String hint = hintsArr.optString(h, "").trim();
                                                    if (!hint.isEmpty()) hints.add(hint);
                                                }
                                            }
                                            if (!mainTrans.isEmpty()) {
                                                listener.onSubtitleData(sourceText, mainTrans, keyVocab, hints);
                                                return;
                                            }
                                        } catch (Exception parseJsonErr) {
                                            // Fallback: extract fields by regex
                                            String mainTrans = extractFieldByRegex(raw, "translation");
                                            String keyVocab = extractFieldByRegex(raw, "key_vocab");
                                            if (!mainTrans.isEmpty()) {
                                                listener.onSubtitleData(sourceText, mainTrans, keyVocab, new java.util.ArrayList<String>());
                                                return;
                                            }
                                            // Fallback if plain text returned without JSON formatting
                                            if (!raw.isEmpty() && !raw.startsWith("{")) {
                                                listener.onSubtitleData(sourceText, raw, "", new java.util.ArrayList<String>());
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "Translation model " + model + " returned code " + response.code());
                    } catch (Exception e) {
                        Log.w(TAG, "Translation parse error for " + model + ": " + e.getMessage());
                    }
                    tryTranslateAt(modelIdx + 1, sourceText, prompt);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "tryTranslateAt error: " + e.getMessage());
            tryTranslateAt(modelIdx + 1, sourceText, prompt);
        }
    }

    private static String extractFieldByRegex(String text, String field) {
        if (text == null || field == null) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1).replace("\\n", "\n").replace("\\\"", "\"").trim();
            }
        } catch (Exception ignored) {}
        return "";
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
        totalFramesWritten = 0;
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
            if (written > 0) {
                totalFramesWritten += (written / 2);
            }
        }
        if (written < 0) recoverAudioPlayer();
    }

    private void recoverAudioPlayer() {
        if (!audioPlaybackRunning || !running) return;
        totalFramesWritten = 0;
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
            return NativeOboeOutput.getBufferedMs() > 0 || (lastPlaybackActiveAt > 0 && now < lastPlaybackActiveAt + 150);
        }
        boolean trackActive = false;
        synchronized (playerLock) {
            if (player != null && player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                long head = player.getPlaybackHeadPosition() & 0xFFFFFFFFL;
                if (totalFramesWritten > head + 240 && (lastPlaybackActiveAt == 0 || now < lastPlaybackActiveAt + 200)) {
                    trackActive = true;
                }
            }
        }
        return !audioQueue.isEmpty() || trackActive || (lastPlaybackActiveAt > 0 && now < lastPlaybackActiveAt + 150);
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
            boolean inPlaybackOrEchoTail = currentlyPlaying || (lastPlaybackActiveAt > 0 && now < lastPlaybackActiveAt + 400);

            // Watchdog: If AI finished playing all audio frames for >400ms, auto-release aiSpeaking state
            if (aiSpeaking && !inPlaybackOrEchoTail) {
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

            // Only enforce interruption gate when the speaker is PHYSICALLY PLAYING audio right now or in reverberation tail
            if (inPlaybackOrEchoTail) {
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

                // While AI is playing audio or in echo tail, NEVER send microphone echo to Gemini WebSocket
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
