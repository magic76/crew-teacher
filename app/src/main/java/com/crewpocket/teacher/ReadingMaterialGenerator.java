package com.crewpocket.teacher;

import android.content.Context;

public class ReadingMaterialGenerator {

    public interface GenerateCallback {
        void onSuccess(String generatedText);
        void onError(String error);
    }

    public static void generateAsync(final Context context, final String topic, final String level, final GenerateCallback callback) {
        String targetLangCode = AppConfig.getTutorLanguage(context);
        String targetLangName = MainActivity.getLanguageLabel(targetLangCode);

        String topicDesc = "daily".equalsIgnoreCase(topic) ? "Daily life, weekend morning routine, memorable coffee shop experience, or relaxing hobbies" :
                ("travel".equalsIgnoreCase(topic) ? "Authentic travel scenario, navigating a foreign airport, exploring historic streets, or tasting local cuisine" :
                ("business".equalsIgnoreCase(topic) ? "Professional workplace communication, product innovation, AI technology impact, or project milestone" :
                ("phonetics".equalsIgnoreCase(topic) ? "Challenging pronunciation drill with varied syllable stresses, liaison/linking sounds, and subtle vowel contrasts" :
                (topic != null && !topic.isEmpty() ? topic : "Engaging conversational story with natural emotional arc and cultural atmosphere"))));

        String levelDesc = "beginner".equalsIgnoreCase(level) ? "Beginner-Intermediate (Clear, natural sentences with practical everyday vocabulary, A2-B1 level)" :
                ("advanced".equalsIgnoreCase(level) ? "Advanced (Rich expressive vocabulary, sophisticated clause structures, authentic native cadence, C1 level)" : "Intermediate (Smooth narrative flow, diverse vocabulary, natural rhythm and intonation, B1-B2 level)");

        final String prompt = "You are a master native language tutor crafting an immersive oral reading and pronunciation practice paragraph for a student learning " + targetLangName + " (" + targetLangCode + ").\n"
                + "Topic: " + topicDesc + "\n"
                + "Difficulty Level: " + levelDesc + "\n\n"
                + "REQUIREMENTS:\n"
                + "1. Write a cohesive, engaging, and beautifully flowing 4-to-6 sentence paragraph in " + targetLangName + " (around 80-120 words).\n"
                + "2. It MUST tell a complete mini-story or express a coherent thought with clear rhythm, natural pauses, and excellent cadence for oral reading.\n"
                + "3. It MUST be 100% written in authentic native script of " + targetLangName + " (e.g. standard kanji/kana for ja, Hangul for ko, Spanish for es, English for en, etc.).\n"
                + "4. Do NOT include romaji, pinyin, phonetic guides, Chinese translations, quotes, bullet points, titles, or markdown.\n"
                + "5. Output ONLY the raw " + targetLangName + " paragraph text directly.";

        GeminiApiClient.generateText(context, prompt, new GeminiApiClient.TextCallback() {
            @Override
            public void onSuccess(String rawText) {
                callback.onSuccess(rawText);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}
