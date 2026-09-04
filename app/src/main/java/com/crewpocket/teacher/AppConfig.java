package com.crewpocket.teacher;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    public static final String PREFS_NAME = "crew_teacher_config";
    public static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    public static final String KEY_VOICE_NAME = "live_voice_name";
    public static final String KEY_NOISE_MODE = "noise_mode";
    public static final String KEY_NOISE_SUPPRESSION = "noise_suppression";
    public static final String KEY_LIVE_TONE = "live_tone";
    public static final String KEY_INTERRUPTION_SENSITIVITY = "interruption_sensitivity";
    public static final String KEY_AUDIO_OUTPUT = "audio_output";
    public static final String KEY_TUTOR_LANGUAGE = "tutor_language";
    public static final String KEY_TUTOR_PERSONA = "tutor_persona";
    public static final String KEY_CUSTOM_PROMPT = "custom_system_prompt";
    public static final String KEY_UI_LANGUAGE = "ui_language";
    public static final String KEY_TEACHING_MODE = "teaching_mode";
    public static final String KEY_STUDENT_LANGUAGE = "student_language";
    public static final String KEY_READING_TEXT = "reading_text";

    public static final String DEFAULT_VOICE = "Kore";
    public static final String DEFAULT_TUTOR_LANG = "en"; // en, ja, ko, es, zh
    public static final String DEFAULT_STUDENT_LANG = "zh-TW"; // zh-TW, zh-CN, en, ja, ko, vi, id, es, fr, de, th
    public static final String DEFAULT_PERSONA = "daily"; // daily, travel, business, exam, friendly
    public static final String DEFAULT_UI_LANG = "zh"; // zh, en
    public static final String DEFAULT_TEACHING_MODE = "bilingual"; // beginner (零基礎引導), bilingual (雙語對照), immersion (全外語沉浸), shadowing (朗讀糾音教練)
    public static final String DEFAULT_READING_TEXT = "";

    public static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── 0. UI Language (Bilingual App UI - Auto follows system locale if unset) ──
    public static String getUiLanguage(Context context) {
        if (context == null) return getSystemDefaultLanguage();
        String saved = getPrefs(context).getString(KEY_UI_LANGUAGE, "");
        if (saved != null && !saved.isEmpty()) {
            return "en".equalsIgnoreCase(saved) ? "en" : "zh";
        }
        return getSystemDefaultLanguage();
    }

    private static String getSystemDefaultLanguage() {
        try {
            String sysLang = java.util.Locale.getDefault().getLanguage();
            if (sysLang != null && sysLang.toLowerCase().startsWith("zh")) {
                return "zh";
            }
        } catch (Exception ignored) {}
        return "en";
    }

    public static void setUiLanguage(Context context, String lang) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_UI_LANGUAGE, "en".equalsIgnoreCase(lang) ? "en" : "zh").apply();
    }

    // ── 1. Gemini API Key (BYOK) ──
    public static String getGeminiApiKey(Context context) {
        if (context == null) return "";
        String key = getPrefs(context).getString(KEY_GEMINI_API_KEY, "");
        if (key.isEmpty()) {
            // Also check legacy or shared key if available
            key = context.getSharedPreferences("crew_helper_config", Context.MODE_PRIVATE).getString("gemini_api_key", "");
        }
        if (key.isEmpty()) {
            key = context.getSharedPreferences("crew_native_live", Context.MODE_PRIVATE).getString("gemini_live_key", "");
        }
        return key;
    }

    public static void setGeminiApiKey(Context context, String key) {
        if (context == null) return;
        String cleanKey = key == null ? "" : key.trim();
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, cleanKey).apply();
    }

    // ── 2. Voice Persona ──
    public static String getVoiceName(Context context) {
        if (context == null) return DEFAULT_VOICE;
        return getPrefs(context).getString(KEY_VOICE_NAME, DEFAULT_VOICE);
    }

    public static void setVoiceName(Context context, String voice) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_VOICE_NAME, voice == null ? DEFAULT_VOICE : voice.trim()).apply();
    }

    // ── 3. Target Language & Persona ──
    public static String getTutorLanguage(Context context) {
        if (context == null) return DEFAULT_TUTOR_LANG;
        return getPrefs(context).getString(KEY_TUTOR_LANGUAGE, DEFAULT_TUTOR_LANG);
    }

    public static void setTutorLanguage(Context context, String lang) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_TUTOR_LANGUAGE, lang == null ? DEFAULT_TUTOR_LANG : lang.trim()).apply();
    }

    public static String getTutorPersona(Context context) {
        if (context == null) return DEFAULT_PERSONA;
        return getPrefs(context).getString(KEY_TUTOR_PERSONA, DEFAULT_PERSONA);
    }

    public static void setTutorPersona(Context context, String persona) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_TUTOR_PERSONA, persona == null ? DEFAULT_PERSONA : persona.trim()).apply();
    }

    // ── 3.2 Student Native Language (for bilingual translation & notes) ──
    public static String getStudentLanguage(Context context) {
        if (context == null) return DEFAULT_STUDENT_LANG;
        String lang = getPrefs(context).getString(KEY_STUDENT_LANGUAGE, DEFAULT_STUDENT_LANG);
        return (lang == null || lang.trim().isEmpty()) ? DEFAULT_STUDENT_LANG : lang.trim();
    }

    public static void setStudentLanguage(Context context, String lang) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_STUDENT_LANGUAGE, lang == null ? DEFAULT_STUDENT_LANG : lang.trim()).apply();
    }

    public static String getStudentLanguageDisplayName(Context context) {
        String code = getStudentLanguage(context);
        return getStudentLanguageLabel(code);
    }

    public static String getStudentLanguageLabel(String code) {
        if ("zh-TW".equalsIgnoreCase(code) || "zh-HK".equalsIgnoreCase(code) || "zh_Hant".equalsIgnoreCase(code)) return "Traditional Chinese (繁體中文)";
        if ("zh-CN".equalsIgnoreCase(code) || "zh".equalsIgnoreCase(code) || "zh_Hans".equalsIgnoreCase(code)) return "Simplified Chinese (簡體中文)";
        if ("en".equalsIgnoreCase(code)) return "English";
        if ("ja".equalsIgnoreCase(code)) return "Japanese (日本語)";
        if ("ko".equalsIgnoreCase(code)) return "Korean (한국어)";
        if ("vi".equalsIgnoreCase(code)) return "Vietnamese (Tiếng Việt)";
        if ("id".equalsIgnoreCase(code)) return "Indonesian (Bahasa Indonesia)";
        if ("es".equalsIgnoreCase(code)) return "Spanish (Español)";
        if ("fr".equalsIgnoreCase(code)) return "French (Français)";
        if ("de".equalsIgnoreCase(code)) return "German (Deutsch)";
        if ("th".equalsIgnoreCase(code)) return "Thai (ภาษาไทย)";
        if ("pt".equalsIgnoreCase(code)) return "Portuguese (Português)";
        if ("ru".equalsIgnoreCase(code)) return "Russian (Русский)";
        if ("it".equalsIgnoreCase(code)) return "Italian (Italiano)";
        if ("ar".equalsIgnoreCase(code)) return "Arabic (العربية)";
        if ("hi".equalsIgnoreCase(code)) return "Hindi (हिन्दी)";
        if ("ms".equalsIgnoreCase(code)) return "Malay (Bahasa Melayu)";
        if ("nan".equalsIgnoreCase(code) || "hokkien".equalsIgnoreCase(code)) return "Taiwanese Hokkien (台灣話/台語)";
        if ("yue".equalsIgnoreCase(code) || "cantonese".equalsIgnoreCase(code)) return "Cantonese (粵語/廣東話)";
        return "Traditional Chinese (繁體中文)";
    }

    public static String getTeachingMode(Context context) {
        if (context == null) return DEFAULT_TEACHING_MODE;
        String mode = getPrefs(context).getString(KEY_TEACHING_MODE, DEFAULT_TEACHING_MODE);
        return ("beginner".equals(mode) || "immersion".equals(mode) || "shadowing".equals(mode)) ? mode : "bilingual";
    }

    public static void setTeachingMode(Context context, String mode) {
        if (context == null) return;
        String clean = ("beginner".equals(mode) || "immersion".equals(mode) || "shadowing".equals(mode)) ? mode : "bilingual";
        getPrefs(context).edit().putString(KEY_TEACHING_MODE, clean).apply();
    }

    // ── 3.5 Reading Material for Shadowing / Pronunciation Coach ──
    public static String getReadingText(Context context) {
        if (context == null) return "";
        String text = getPrefs(context).getString(KEY_READING_TEXT, "");
        return text == null ? "" : text.trim();
    }

    public static void setReadingText(Context context, String text) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_READING_TEXT, text == null ? "" : text.trim()).apply();
    }

    // ── 4. Noise suppression ──
    public static String getNoiseMode(Context context) {
        if (context == null) return "auto";
        String mode = getPrefs(context).getString(KEY_NOISE_MODE, "auto");
        return "quiet".equals(mode) || "noisy".equals(mode) ? mode : "auto";
    }

    public static void setNoiseMode(Context context, String mode) {
        if (context == null) return;
        String clean = "quiet".equals(mode) || "noisy".equals(mode) ? mode : "auto";
        getPrefs(context).edit().putString(KEY_NOISE_MODE, clean).apply();
    }

    public static int getNoiseSuppression(Context context) {
        if (context == null) return 35;
        int value = getPrefs(context).getInt(KEY_NOISE_SUPPRESSION, 35);
        return Math.max(0, Math.min(100, value));
    }

    public static void setNoiseSuppression(Context context, int value) {
        if (context == null) return;
        getPrefs(context).edit().putInt(KEY_NOISE_SUPPRESSION, Math.max(0, Math.min(100, value))).apply();
    }

    // ── 5. Live speaking style ──
    public static String getLiveTone(Context context) {
        if (context == null) return "warm";
        String tone = getPrefs(context).getString(KEY_LIVE_TONE, "warm");
        return isLiveTone(tone) ? tone : "warm";
    }

    public static void setLiveTone(Context context, String tone) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_LIVE_TONE, isLiveTone(tone) ? tone : "warm").apply();
    }

    private static boolean isLiveTone(String tone) {
        return "natural".equals(tone) || "warm".equals(tone) || "lively".equals(tone)
                || "professional".equals(tone) || "calm".equals(tone);
    }

    // ── 6. Interruption sensitivity & Audio routing ──
    public static int getInterruptionSensitivity(Context context) {
        if (context == null) return 20;
        return Math.max(0, Math.min(100, getPrefs(context).getInt(KEY_INTERRUPTION_SENSITIVITY, 20)));
    }

    public static void setInterruptionSensitivity(Context context, int value) {
        if (context == null) return;
        getPrefs(context).edit().putInt(KEY_INTERRUPTION_SENSITIVITY, Math.max(0, Math.min(100, value))).apply();
    }

    public static String getAudioOutput(Context context) {
        if (context == null) return "call";
        return "media".equals(getPrefs(context).getString(KEY_AUDIO_OUTPUT, "call")) ? "media" : "call";
    }

    public static void setAudioOutput(Context context, String output) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_AUDIO_OUTPUT, "media".equals(output) ? "media" : "call").apply();
    }

    // ── 7. User Custom System Prompt ──
    public static String getCustomSystemPrompt(Context context) {
        if (context == null) return "";
        return getPrefs(context).getString(KEY_CUSTOM_PROMPT, "");
    }

    public static void setCustomSystemPrompt(Context context, String prompt) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_CUSTOM_PROMPT, prompt == null ? "" : prompt.trim()).apply();
    }
}
