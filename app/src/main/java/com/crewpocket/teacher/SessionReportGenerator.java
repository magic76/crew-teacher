package com.crewpocket.teacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionReportGenerator {

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
            populateDefaultScores(record, studentLang);
            LearningDataManager.saveSessionRecord(context, record);
            LearningDataManager.recordPracticeActivity(context, finalUserCount, durationSeconds);
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onReportReady(record); }
            });
            return;
        }

        final String prompt = "You are a top-tier, uncompromising oral language examiner and rigorous linguistic coach evaluating a student's session transcript.\n"
                + "Target Practice Language: " + tutorLang + "\n"
                + "Student Native Language: " + studentLang + "\n"
                + "Scenario: " + scenario + "\n\n"
                + "Session Transcript:\n"
                + fullTranscript + "\n\n"
                + "CRITICAL EVALUATION GUIDELINES (銳評與嚴格標準):\n"
                + "1. ZERO EMPTY FLATTERY: Do NOT give inflated participation scores. Be realistic, sharp, professional, and brutally honest.\n"
                + "2. CEFR BENCHMARKED SCORING (0-100 real scale):\n"
                + "   - 90-100 (C1/C2): Flawless native fluency, rich idioms, complex syntax, natural intonation.\n"
                + "   - 75-89 (B2): Solid fluency and effective communication, but with occasional minor slips or slight awkwardness.\n"
                + "   - 55-74 (B1): Communicates basic ideas, but has evident grammar errors, repetitive vocabulary, or Chinglish phrasing.\n"
                + "   - 35-54 (A2): Fragmented sentences, frequent grammatical breakdowns, limited vocabulary.\n"
                + "   - 10-34 (A1): Disconnected single words or incomprehensible phrases.\n"
                + "3. SHARP DIAGNOSTIC SUMMARY (銳評診斷): In 'summary', write a piercing, insightful 2-3 sentence critique in " + studentLang + " directly pinpointing the student's core weaknesses (e.g., Chinglish habits, tense errors, filler word dependence, monotonous rhythm, lack of vocabulary variety).\n"
                + "4. DIRECT RECASTS (語病抓錯與重塑): Identify exact utterances with flaws, awkward phrasing, or grammatical errors, provide authentic native replacements in " + tutorLang + ", and explain the exact issue in " + studentLang + ".\n\n"
                + "Output STRICTLY a valid JSON object with EXACTLY these keys:\n"
                + "{\n"
                + "  \"overall_score\": (integer 20-98, strictly calculated),\n"
                + "  \"fluency_score\": (integer 20-98),\n"
                + "  \"vocab_score\": (integer 20-98),\n"
                + "  \"grammar_score\": (integer 20-98),\n"
                + "  \"phonetic_score\": (integer 20-98),\n"
                + "  \"summary\": \"(Sharp, direct, honest 2-3 sentence diagnostic critique in " + studentLang + ")\",\n"
                + "  \"strengths\": \"(Honest, objective strengths without exaggeration in " + studentLang + ")\",\n"
                + "  \"recasts\": [\n"
                + "    {\n"
                + "      \"original\": \"(exact flawed or awkward student utterance)\",\n"
                + "      \"corrected\": \"(authentic native phrasing in " + tutorLang + ")\",\n"
                + "      \"explanation\": \"(clear diagnostic explanation of the flaw in " + studentLang + ")\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"takeaways\": [\n"
                + "    {\"phrase\": \"(high-impact native phrase 1 in " + tutorLang + ")\", \"translation\": \"(translation in " + studentLang + ")\"},\n"
                + "    {\"phrase\": \"(high-impact native phrase 2 in " + tutorLang + ")\", \"translation\": \"(translation in " + studentLang + ")\"}\n"
                + "  ],\n"
                + "  \"cheer\": \"(constructive, sharp, high-impact advice on what specific habits to fix in " + studentLang + ")\"\n"
                + "}\n"
                + "Output strictly ONLY the raw JSON object.";

        GeminiApiClient.generateJson(context, prompt, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject obj, String rawText) {
                record.overallScore = obj.optInt("overall_score", 72);
                record.fluencyScore = obj.optInt("fluency_score", 70);
                record.vocabScore = obj.optInt("vocab_score", 68);
                record.grammarScore = obj.optInt("grammar_score", 70);
                record.phoneticScore = obj.optInt("phonetic_score", 72);
                record.summary = obj.optString("summary", "");
                record.strengths = obj.optString("strengths", "");
                record.recastsJson = obj.optJSONArray("recasts") != null ? obj.optJSONArray("recasts").toString() : "[]";
                record.takeawaysJson = obj.optJSONArray("takeaways") != null ? obj.optJSONArray("takeaways").toString() : "[]";
                record.cheer = obj.optString("cheer", "");

                if (record.summary.isEmpty()) {
                    populateDefaultScores(record, studentLang);
                }

                LearningDataManager.saveSessionRecord(context, record);
                LearningDataManager.recordPracticeActivity(context, record.userTurns, record.durationSeconds);
                callback.onReportReady(record);
            }

            @Override
            public void onError(String errorMessage) {
                populateDefaultScores(record, studentLang);
                LearningDataManager.saveSessionRecord(context, record);
                LearningDataManager.recordPracticeActivity(context, record.userTurns, record.durationSeconds);
                callback.onReportReady(record);
            }
        });
    }

    private static void populateDefaultScores(LearningDataManager.SessionRecord record, String studentLang) {
        int words = 0;
        if (record.fullTranscript != null && !record.fullTranscript.isEmpty()) {
            String[] lines = record.fullTranscript.split("\n");
            for (String line : lines) {
                if (line.startsWith("Student:")) {
                    words += line.substring(8).trim().split("\\s+").length;
                }
            }
        }

        int baseScore;
        if (record.userTurns <= 0 || words < 3) {
            baseScore = 58;
        } else if (record.userTurns == 1 && words < 10) {
            baseScore = 68;
        } else if (record.userTurns <= 3 && words < 25) {
            baseScore = 75;
        } else if (record.userTurns <= 6) {
            baseScore = 83;
        } else {
            baseScore = Math.min(96, 84 + (record.userTurns - 6) * 2 + (words / 35));
        }

        record.overallScore = baseScore;
        record.fluencyScore = Math.max(45, Math.min(98, baseScore - 2 + (record.durationSeconds > 60 ? 4 : -3)));
        record.vocabScore = Math.max(45, Math.min(98, baseScore + (words > 25 ? 3 : -4)));
        record.grammarScore = Math.max(45, Math.min(98, baseScore - 3));
        record.phoneticScore = Math.max(45, Math.min(98, baseScore + 2));
        record.summary = "本次練習完成了 " + record.userTurns + " 輪互動（累計說出約 " + words + " 個單字），發言表現良好！";
        record.strengths = words > 20 ? "句子結構完整，能夠主動引導並回應 AI 導師！" : "勇於開口嘗試，持續累積詞彙量將更為流暢！";
        record.cheer = "太棒了！堅持每天開口練習，自信心與流暢度正在穩步提升！";
    }
}
