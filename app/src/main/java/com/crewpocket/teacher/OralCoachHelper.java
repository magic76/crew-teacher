package com.crewpocket.teacher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OralCoachHelper {

    private static TextToSpeech tts;
    private static boolean ttsReady = false;

    private static void initTts(Context context) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        tts.setLanguage(Locale.US);
                        ttsReady = true;
                    }
                }
            });
        }
    }

    public static void speak(Context ctx, String text, float speed) {
        initTts(ctx);
        if (tts != null && ttsReady && text != null) {
            tts.setSpeechRate(speed);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_" + text.hashCode());
        }
    }

    public static void disableScrollbars(ScrollView scroll) {
        if (scroll == null) return;
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    public static void disableScrollbars(android.widget.HorizontalScrollView scroll) {
        if (scroll == null) return;
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    // ── 💡 Priority 1: In-Call Teleprompter & Response Starters ──
    public static void showHintsBottomSheet(final Activity activity, final CourseModel.Lesson lesson, final String scenario) {
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D9090D16")));
        }

        ScrollView scroll = new ScrollView(activity);
        disableScrollbars(scroll);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 30), dp(activity, 16), dp(activity, 20));

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#3B82F6"));
        container.setBackground(cBg);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(activity);
        titleTv.setText("💡 " + (en ? "Speaking Inspiration & Starters" : "即時提詞靈感 · 救命開口句"));
        titleTv.setTextSize(15);
        titleTv.setTextColor(Color.parseColor("#38BDF8"));
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        topRow.addView(closeBtn);
        container.addView(topRow);

        TextView subTv = new TextView(activity);
        subTv.setText(en ? "Card stuck? Pick any starter below to keep conversation flowing naturally:"
                : "大腦卡住不知道回什麼？挑選以下任一句型直接開口念出來：");
        subTv.setTextSize(11);
        subTv.setTextColor(Color.parseColor("#94A3B8"));
        subTv.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        container.addView(subTv);

        final LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        final Runnable renderList = new Runnable() {
            @Override public void run() {
                listContainer.removeAllViews();
                String currentTutorLang = AppConfig.getTutorLanguage(activity);
                List<IcebreakerManager.Icebreaker> items = new ArrayList<IcebreakerManager.Icebreaker>();

                if (lesson != null && lesson.warmupPhrases != null && !lesson.warmupPhrases.isEmpty()) {
                    for (CourseModel.WarmupPhrase wp : lesson.warmupPhrases) {
                        items.add(new IcebreakerManager.Icebreaker("⚡", wp.en, (en ? wp.en : wp.zh)));
                    }
                } else {
                    items.addAll(IcebreakerManager.getIcebreakersForScenario(scenario, currentTutorLang));
                }

                for (final IcebreakerManager.Icebreaker p : items) {
                    LinearLayout pCard = new LinearLayout(activity);
                    pCard.setOrientation(LinearLayout.VERTICAL);
                    pCard.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                    GradientDrawable pbg = new GradientDrawable();
                    pbg.setColor(Color.parseColor("#1E293B"));
                    pbg.setCornerRadius(dp(activity, 12));
                    pbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                    pCard.setBackground(pbg);

                    LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    plp.setMargins(0, 0, 0, dp(activity, 8));
                    pCard.setLayoutParams(plp);

                    LinearLayout phraseTop = new LinearLayout(activity);
                    phraseTop.setOrientation(LinearLayout.HORIZONTAL);
                    phraseTop.setGravity(Gravity.CENTER_VERTICAL);

                    TextView enTv = new TextView(activity);
                    enTv.setText(p.emoji + " " + p.targetPhrase);
                    enTv.setTextSize(13);
                    enTv.setTextColor(Color.WHITE);
                    enTv.setTypeface(Typeface.DEFAULT_BOLD);
                    phraseTop.addView(enTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView playBtn = new TextView(activity);
                    playBtn.setText("🔊 聽發音");
                    playBtn.setTextSize(11);
                    playBtn.setTextColor(Color.parseColor("#38BDF8"));
                    playBtn.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));
                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { speak(activity, p.targetPhrase, 1.0f); }
                    });
                    phraseTop.addView(playBtn);
                    pCard.addView(phraseTop);

                    TextView zhTv = new TextView(activity);
                    zhTv.setText(p.nativeHint);
                    zhTv.setTextSize(11);
                    zhTv.setTextColor(Color.parseColor("#94A3B8"));
                    zhTv.setPadding(0, dp(activity, 4), 0, 0);
                    pCard.addView(zhTv);

                    listContainer.addView(pCard);
                }
            }
        };

        // Top Action: AI Generate New Starters Button
        final Button aiGenBtn = new Button(activity);
        aiGenBtn.setText(en ? "✨ AI Generate New Sentence Starters" : "✨ 叫 AI 生成全新提詞範例句");
        aiGenBtn.setTextSize(12);
        aiGenBtn.setTextColor(Color.WHITE);
        aiGenBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable agBg = new GradientDrawable();
        agBg.setColors(new int[]{Color.parseColor("#4F46E5"), Color.parseColor("#7C3AED")});
        agBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        agBg.setCornerRadius(dp(activity, 12));
        aiGenBtn.setBackground(agBg);
        LinearLayout.LayoutParams agLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 42));
        agLp.setMargins(0, 0, 0, dp(activity, 12));
        aiGenBtn.setLayoutParams(agLp);
        aiGenBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppConfig.getGeminiApiKey(activity).isEmpty()) {
                    Toast.makeText(activity, en ? "Please configure Gemini API Key first" : "請先設定 Gemini API Key", Toast.LENGTH_SHORT).show();
                    return;
                }
                aiGenBtn.setText(en ? "⏳ AI is generating..." : "⏳ AI 正在生成全新範例句…");
                aiGenBtn.setEnabled(false);
                String tLang = AppConfig.getTutorLanguage(activity);
                String sLang = AppConfig.getStudentLanguage(activity);
                IcebreakerManager.generateAsync(activity, scenario, tLang, sLang, "", new IcebreakerManager.GenerateCallback() {
                    @Override public void onSuccess(List<IcebreakerManager.Icebreaker> generatedList) {
                        aiGenBtn.setEnabled(true);
                        aiGenBtn.setText(en ? "✨ AI Generate New Sentence Starters" : "✨ 叫 AI 生成全新提詞範例句");
                        renderList.run();
                        Toast.makeText(activity, en ? "✨ Generated " + generatedList.size() + " new starter phrases!" : "✨ 已為您生成 " + generatedList.size() + " 句全新開口金句！", Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onError(String errorMessage) {
                        aiGenBtn.setEnabled(true);
                        aiGenBtn.setText(en ? "✨ AI Generate New Sentence Starters" : "✨ 叫 AI 生成全新提詞範例句");
                        Toast.makeText(activity, "⚠️ " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        container.addView(aiGenBtn);

        renderList.run();
        container.addView(listContainer);

        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    // ── 🎙️ Priority 2 & 3: Pronunciation & Muscle Memory Drill Dialog ──
    public static void showPronunciationDrillDialog(final Activity activity, final String targetText, final String subtitle, final String note) {
        if (targetText == null || targetText.trim().isEmpty()) return;
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E6090D16")));
        }

        ScrollView scroll = new ScrollView(activity);
        disableScrollbars(scroll);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 20));

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#4F46E5"));
        container.setBackground(cBg);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(activity);
        titleTv.setText("🎙️ " + (en ? "Pronunciation & Muscle Memory Drill" : "發音跟讀與肌肉記憶重練"));
        titleTv.setTextSize(14);
        titleTv.setTextColor(Color.parseColor("#A5B4FC"));
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        topRow.addView(closeBtn);
        container.addView(topRow);

        LinearLayout targetCard = new LinearLayout(activity);
        targetCard.setOrientation(LinearLayout.VERTICAL);
        targetCard.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
        GradientDrawable tBg = new GradientDrawable();
        tBg.setColor(Color.parseColor("#1E293B"));
        tBg.setCornerRadius(dp(activity, 14));
        tBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
        targetCard.setBackground(tBg);

        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(activity, 12), 0, dp(activity, 12));
        targetCard.setLayoutParams(tlp);

        final TextView targetTv = new TextView(activity);
        targetTv.setText(targetText);
        targetTv.setTextSize(15);
        targetTv.setTextColor(Color.WHITE);
        targetTv.setTypeface(Typeface.DEFAULT_BOLD);
        targetCard.addView(targetTv);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subTv = new TextView(activity);
            subTv.setText(subtitle);
            subTv.setTextSize(12);
            subTv.setTextColor(Color.parseColor("#94A3B8"));
            subTv.setPadding(0, dp(activity, 4), 0, 0);
            targetCard.addView(subTv);
        }

        if (note != null && !note.isEmpty()) {
            TextView noteTv = new TextView(activity);
            noteTv.setText("💡 " + note);
            noteTv.setTextSize(11);
            noteTv.setTextColor(Color.parseColor("#38BDF8"));
            noteTv.setPadding(0, dp(activity, 4), 0, 0);
            targetCard.addView(noteTv);
        }

        LinearLayout listenRow = new LinearLayout(activity);
        listenRow.setOrientation(LinearLayout.HORIZONTAL);
        listenRow.setPadding(0, dp(activity, 8), 0, 0);

        Button listenNormal = new Button(activity);
        listenNormal.setText("🔊 標準發音");
        listenNormal.setTextSize(11);
        listenNormal.setTextColor(Color.WHITE);
        GradientDrawable nBg = new GradientDrawable();
        nBg.setColor(Color.parseColor("#4F46E5"));
        nBg.setCornerRadius(dp(activity, 8));
        listenNormal.setBackground(nBg);
        listenNormal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { speak(activity, targetText, 1.0f); }
        });
        listenRow.addView(listenNormal, new LinearLayout.LayoutParams(0, dp(activity, 34), 1f));

        Button listenSlow = new Button(activity);
        listenSlow.setText("🐢 慢速示範 (0.7x)");
        listenSlow.setTextSize(11);
        listenSlow.setTextColor(Color.WHITE);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(Color.parseColor("#334155"));
        sBg.setCornerRadius(dp(activity, 8));
        listenSlow.setBackground(sBg);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(activity, 34), 1f);
        slp.setMargins(dp(activity, 8), 0, 0, 0);
        listenSlow.setLayoutParams(slp);
        listenSlow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { speak(activity, targetText, 0.7f); }
        });
        listenRow.addView(listenSlow);
        targetCard.addView(listenRow);
        container.addView(targetCard);

        final TextView resultTv = new TextView(activity);
        resultTv.setText(en ? "Tap Start Recording below and read aloud!" : "點擊下方「🎙️ 開始跟讀」，大聲讀出上方句子！");
        resultTv.setTextSize(13);
        resultTv.setTextColor(Color.parseColor("#94A3B8"));
        resultTv.setGravity(Gravity.CENTER);
        resultTv.setPadding(0, dp(activity, 8), 0, dp(activity, 14));
        container.addView(resultTv);

        final Button recordBtn = new Button(activity);
        recordBtn.setText(en ? "🎙️ Start Practice (Tap to speak)" : "🎙️ 開始跟讀（點擊開口錄音）");
        recordBtn.setTextSize(14);
        recordBtn.setTextColor(Color.WHITE);
        recordBtn.setTypeface(Typeface.DEFAULT_BOLD);
        final GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(Color.parseColor("#2563EB"));
        rBg.setCornerRadius(dp(activity, 14));
        recordBtn.setBackground(rBg);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48));
        recordBtn.setLayoutParams(rlp);

        final SpeechRecognizer[] recognizer = new SpeechRecognizer[1];

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (SpeechRecognizer.isRecognitionAvailable(activity)) {
                    if (recognizer[0] != null) {
                        try { recognizer[0].destroy(); } catch (Exception ignored) {}
                    }
                    recognizer[0] = SpeechRecognizer.createSpeechRecognizer(activity);

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                    recognizer[0].setRecognitionListener(new RecognitionListener() {
                        @Override public void onReadyForSpeech(Bundle params) {
                            recordBtn.setText(en ? "🎙️ Listening... Speak now!" : "🎙️ 正在聆聽，請大聲念出句子！");
                            rBg.setColor(Color.parseColor("#DC2626"));
                        }
                        @Override public void onBeginningOfSpeech() {}
                        @Override public void onRmsChanged(float rmsdB) {}
                        @Override public void onBufferReceived(byte[] buffer) {}
                        @Override public void onEndOfSpeech() {
                            recordBtn.setText(en ? "Analyzing..." : "正在評估發音吻合度…");
                            rBg.setColor(Color.parseColor("#4F46E5"));
                        }
                        @Override public void onError(int error) {
                            recordBtn.setText(en ? "🎙️ Try Again" : "🎙️ 再試一次");
                            rBg.setColor(Color.parseColor("#2563EB"));
                            resultTv.setText(en ? "Did not hear clearly. Please try again!" : "未偵測到清晰發音，請再試一次！");
                        }
                        @Override public void onResults(Bundle results) {
                            recordBtn.setText(en ? "🎙️ Try Again" : "🎙️ 再試一次");
                            rBg.setColor(Color.parseColor("#2563EB"));

                            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) {
                                String spoken = matches.get(0);
                                evaluateDrillSpoken(spoken, targetText, targetTv, resultTv, en);
                            }
                        }
                        @Override public void onPartialResults(Bundle partialResults) {}
                        @Override public void onEvent(int eventType, Bundle params) {}
                    });

                    recognizer[0].startListening(intent);
                } else {
                    Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        container.addView(recordBtn);
        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static void evaluateDrillSpoken(String spoken, String target, TextView targetTv, TextView resultTv, boolean en) {
        String[] targetWords = target.trim().split("\\s+");
        String[] spokenWords = spoken.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        int matchCount = 0;
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        for (int i = 0; i < targetWords.length; i++) {
            String origWord = targetWords[i];
            String cleanTarget = origWord.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");

            boolean matched = false;
            for (String sw : spokenWords) {
                if (sw.equals(cleanTarget)) {
                    matched = true;
                    break;
                }
            }

            int start = ssb.length();
            ssb.append(origWord);
            int end = ssb.length();

            if (matched) {
                matchCount++;
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#34D399")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#F87171")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < targetWords.length - 1) {
                ssb.append(" ");
            }
        }

        targetTv.setText(ssb);

        int accuracy = (int) (((float) matchCount / Math.max(1, targetWords.length)) * 100);
        if (accuracy >= 80) {
            resultTv.setText("🎯 " + (en ? "Mastery Accuracy: " : "發音掌握度：") + accuracy + "% · " + (en ? "Outstanding pronunciation!" : "發音非常地道，肌肉記憶達成！🌟"));
            resultTv.setTextColor(Color.parseColor("#34D399"));
        } else {
            resultTv.setText("🎯 " + (en ? "Mastery Accuracy: " : "發音掌握度：") + accuracy + "% · " + (en ? "Keep practicing red words!" : "紅色單字注意發音與連音，再試一次！💪"));
            resultTv.setTextColor(Color.parseColor("#FBBF24"));
        }
    }

    // ── 🎙️ Shadowing / Read-Along Lab (朗讀跟讀練功房) ──

    public static class ShadowingItem {
        public String english;
        public String chinese;
        public String tip;

        public ShadowingItem(String eng, String chi, String tip) {
            this.english = eng;
            this.chinese = chi;
            this.tip = tip;
        }
    }

    public static void showShadowingLabDialog(final Activity activity, final List<ShadowingItem> initialItems, final String initialCategoryName) {
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#EB080D1A")));
        }

        final ScrollView scroll = new ScrollView(activity);
        disableScrollbars(scroll);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 20));

        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(Color.parseColor("#0F172A"));
        rBg.setCornerRadius(dp(activity, 20));
        rBg.setStroke(dp(activity, 1), Color.parseColor("#3B82F6"));
        root.setBackground(rBg);

        final String[] categories = new String[]{
                en ? "⭐ My Starred" : "⭐ 我的收藏金句",
                en ? "☕ Daily & Cafe" : "☕ 生活點餐與日常",
                en ? "💼 Business & Work" : "💼 職場商務與會議",
                en ? "✈️ Travel & Hotel" : "✈️ 旅遊出境與飯店",
                en ? "💬 Small Talk" : "💬 地道社交閒聊",
                en ? "💡 Asking & Help" : "💡 求助與提問萬用句"
        };

        final int[] curCatIdx = new int[]{0};
        final int[] curItemIdx = new int[]{0};
        final boolean[] showTrans = new boolean[]{true};
        final int[] totalScores = new int[]{0};
        final int[] scoredCount = new int[]{0};
        final SpeechRecognizer[] activeRecognizer = new SpeechRecognizer[1];

        if (initialCategoryName != null) {
            for (int c = 0; c < categories.length; c++) {
                if (categories[c].contains(initialCategoryName)) {
                    curCatIdx[0] = c;
                    break;
                }
            }
        }

        final Runnable renderShadowingView = new Runnable() {
            @Override public void run() {
                root.removeAllViews();

                LinearLayout topRow = new LinearLayout(activity);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView titleTv = new TextView(activity);
                titleTv.setText("🎙️ " + (en ? "Shadowing & Read-Along Lab" : "朗讀跟讀練功房 · 逐句帶讀"));
                titleTv.setTextSize(15);
                titleTv.setTextColor(Color.parseColor("#38BDF8"));
                titleTv.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView closeBtn = new TextView(activity);
                closeBtn.setText("✕");
                closeBtn.setTextSize(18);
                closeBtn.setTextColor(Color.parseColor("#94A3B8"));
                closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (activeRecognizer[0] != null) {
                            try { activeRecognizer[0].destroy(); } catch (Exception ignored) {}
                        }
                        dialog.dismiss();
                    }
                });
                topRow.addView(closeBtn);
                root.addView(topRow);

                android.widget.HorizontalScrollView catScroll = new android.widget.HorizontalScrollView(activity);
                disableScrollbars(catScroll);
                catScroll.setPadding(0, dp(activity, 10), 0, dp(activity, 12));

                LinearLayout catRow = new LinearLayout(activity);
                catRow.setOrientation(LinearLayout.HORIZONTAL);

                for (int i = 0; i < categories.length; i++) {
                    final int catI = i;
                    final boolean selected = (curCatIdx[0] == catI);
                    TextView chip = new TextView(activity);
                    chip.setText(categories[i]);
                    chip.setTextSize(11);
                    chip.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    chip.setTextColor(selected ? Color.WHITE : Color.parseColor("#94A3B8"));
                    chip.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));

                    GradientDrawable chipBg = new GradientDrawable();
                    chipBg.setColor(selected ? Color.parseColor("#2563EB") : Color.parseColor("#1E293B"));
                    chipBg.setCornerRadius(dp(activity, 16));
                    if (!selected) chipBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                    chip.setBackground(chipBg);

                    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    clp.setMargins(0, 0, dp(activity, 8), 0);
                    chip.setLayoutParams(clp);

                    chip.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            if (curCatIdx[0] != catI) {
                                curCatIdx[0] = catI;
                                curItemIdx[0] = 0;
                                totalScores[0] = 0;
                                scoredCount[0] = 0;
                                run();
                            }
                        }
                    });
                    catRow.addView(chip);
                }
                catScroll.addView(catRow);
                root.addView(catScroll);

                final List<ShadowingItem> items = getCategoryItems(activity, curCatIdx[0], initialItems);

                if (items.isEmpty()) {
                    LinearLayout emptyBox = new LinearLayout(activity);
                    emptyBox.setOrientation(LinearLayout.VERTICAL);
                    emptyBox.setGravity(Gravity.CENTER);
                    emptyBox.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));

                    TextView emptyTv = new TextView(activity);
                    emptyTv.setText("⭐ " + (en ? "No Starred Sentences yet!" : "生詞金句本目前沒有收藏句子！"));
                    emptyTv.setTextSize(14);
                    emptyTv.setTextColor(Color.WHITE);
                    emptyTv.setTypeface(Typeface.DEFAULT_BOLD);
                    emptyTv.setGravity(Gravity.CENTER);
                    emptyBox.addView(emptyTv);

                    TextView emptySub = new TextView(activity);
                    emptySub.setText(en ? "Switch to other categories above to start read-along practice!"
                            : "請切換上方其他主題（如日常點餐、職場商務）開始跟讀練習！");
                    emptySub.setTextSize(12);
                    emptySub.setTextColor(Color.parseColor("#94A3B8"));
                    emptySub.setGravity(Gravity.CENTER);
                    emptySub.setPadding(0, dp(activity, 6), 0, dp(activity, 16));
                    emptyBox.addView(emptySub);

                    Button switchBtn = new Button(activity);
                    switchBtn.setText("☕ " + (en ? "Practice Daily Cafe Sentences" : "練習生活點餐金句"));
                    switchBtn.setTextSize(13);
                    switchBtn.setTextColor(Color.WHITE);
                    GradientDrawable swBg = new GradientDrawable();
                    swBg.setColor(Color.parseColor("#2563EB"));
                    swBg.setCornerRadius(dp(activity, 10));
                    switchBtn.setBackground(swBg);
                    switchBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            curCatIdx[0] = 1;
                            curItemIdx[0] = 0;
                            run();
                        }
                    });
                    emptyBox.addView(switchBtn, new LinearLayout.LayoutParams(dp(activity, 220), dp(activity, 44)));

                    root.addView(emptyBox);
                    return;
                }

                if (curItemIdx[0] >= items.size()) {
                    LinearLayout finishBox = new LinearLayout(activity);
                    finishBox.setOrientation(LinearLayout.VERTICAL);
                    finishBox.setGravity(Gravity.CENTER);
                    finishBox.setPadding(dp(activity, 20), dp(activity, 24), dp(activity, 20), dp(activity, 24));

                    TextView fIcon = new TextView(activity);
                    fIcon.setText("🏆");
                    fIcon.setTextSize(40);
                    fIcon.setGravity(Gravity.CENTER);
                    finishBox.addView(fIcon);

                    TextView fTitle = new TextView(activity);
                    fTitle.setText(en ? "Sentence Deck Mastered!" : "太棒了！本主題跟讀練習圓滿完成！");
                    fTitle.setTextSize(16);
                    fTitle.setTextColor(Color.WHITE);
                    fTitle.setTypeface(Typeface.DEFAULT_BOLD);
                    fTitle.setGravity(Gravity.CENTER);
                    fTitle.setPadding(0, dp(activity, 8), 0, 0);
                    finishBox.addView(fTitle);

                    int avgScore = scoredCount[0] > 0 ? (totalScores[0] / scoredCount[0]) : 92;
                    TextView fScore = new TextView(activity);
                    fScore.setText("🎯 " + (en ? "Average Fluency: " : "平均發音掌握度：") + avgScore + "%");
                    fScore.setTextSize(14);
                    fScore.setTextColor(Color.parseColor("#34D399"));
                    fScore.setTypeface(Typeface.DEFAULT_BOLD);
                    fScore.setGravity(Gravity.CENTER);
                    fScore.setPadding(0, dp(activity, 4), 0, dp(activity, 16));
                    finishBox.addView(fScore);

                    Button restartBtn = new Button(activity);
                    restartBtn.setText("🔄 " + (en ? "Practice Again" : "再練一輪"));
                    restartBtn.setTextSize(13);
                    restartBtn.setTextColor(Color.WHITE);
                    GradientDrawable rb = new GradientDrawable();
                    rb.setColor(Color.parseColor("#2563EB"));
                    rb.setCornerRadius(dp(activity, 10));
                    restartBtn.setBackground(rb);
                    restartBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            curItemIdx[0] = 0;
                            totalScores[0] = 0;
                            scoredCount[0] = 0;
                            run();
                        }
                    });
                    finishBox.addView(restartBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 44)));

                    root.addView(finishBox);
                    return;
                }

                final ShadowingItem cur = items.get(curItemIdx[0]);

                LinearLayout progRow = new LinearLayout(activity);
                progRow.setOrientation(LinearLayout.HORIZONTAL);
                progRow.setGravity(Gravity.CENTER_VERTICAL);
                progRow.setPadding(0, 0, 0, dp(activity, 8));

                TextView counterTv = new TextView(activity);
                counterTv.setText((en ? "Sentence " : "第 ") + (curItemIdx[0] + 1) + " / " + items.size() + (en ? "" : " 句"));
                counterTv.setTextSize(12);
                counterTv.setTextColor(Color.parseColor("#38BDF8"));
                counterTv.setTypeface(Typeface.DEFAULT_BOLD);
                progRow.addView(counterTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                final Button starBtn = new Button(activity);
                final boolean isSt = LearningDataManager.isStarred(activity, cur.english);
                starBtn.setText(isSt ? "★ 已收藏" : "☆ 收藏");
                starBtn.setTextSize(11);
                starBtn.setTextColor(isSt ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                starBtn.setBackground(null);
                starBtn.setPadding(0, 0, 0, 0);
                starBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean now = LearningDataManager.toggleStarItem(activity, cur.english, cur.chinese, "phrase", cur.tip);
                        starBtn.setText(now ? "★ 已收藏" : "☆ 收藏");
                        starBtn.setTextColor(now ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                        Toast.makeText(activity, now ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                    }
                });
                progRow.addView(starBtn);
                root.addView(progRow);

                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
                GradientDrawable cbg = new GradientDrawable();
                cbg.setColor(Color.parseColor("#1E293B"));
                cbg.setCornerRadius(dp(activity, 14));
                cbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                card.setBackground(cbg);

                final TextView targetTv = new TextView(activity);
                targetTv.setText(cur.english);
                targetTv.setTextSize(17);
                targetTv.setTextColor(Color.WHITE);
                targetTv.setTypeface(Typeface.DEFAULT_BOLD);
                targetTv.setLineSpacing(dp(activity, 2), 1.25f);
                card.addView(targetTv);

                final TextView zhTv = new TextView(activity);
                zhTv.setText(cur.chinese != null ? cur.chinese : "");
                zhTv.setTextSize(13);
                zhTv.setTextColor(Color.parseColor("#94A3B8"));
                zhTv.setPadding(0, dp(activity, 6), 0, 0);
                zhTv.setVisibility(showTrans[0] ? View.VISIBLE : View.GONE);
                card.addView(zhTv);

                if (cur.tip != null && !cur.tip.isEmpty()) {
                    TextView tipTv = new TextView(activity);
                    tipTv.setText("💡 " + cur.tip);
                    tipTv.setTextSize(11);
                    tipTv.setTextColor(Color.parseColor("#A78BFA"));
                    tipTv.setPadding(0, dp(activity, 4), 0, 0);
                    card.addView(tipTv);
                }

                LinearLayout audioRow = new LinearLayout(activity);
                audioRow.setOrientation(LinearLayout.HORIZONTAL);
                audioRow.setPadding(0, dp(activity, 12), 0, 0);

                Button listenNormal = new Button(activity);
                listenNormal.setText("🔊 原速 1.0x");
                listenNormal.setTextSize(11);
                listenNormal.setTextColor(Color.WHITE);
                GradientDrawable nbg = new GradientDrawable();
                nbg.setColor(Color.parseColor("#4F46E5"));
                nbg.setCornerRadius(dp(activity, 8));
                listenNormal.setBackground(nbg);
                listenNormal.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { speak(activity, cur.english, 1.0f); }
                });
                audioRow.addView(listenNormal, new LinearLayout.LayoutParams(0, dp(activity, 34), 1f));

                Button listenSlow = new Button(activity);
                listenSlow.setText("🐢 慢速 0.7x");
                listenSlow.setTextSize(11);
                listenSlow.setTextColor(Color.WHITE);
                GradientDrawable sbg = new GradientDrawable();
                sbg.setColor(Color.parseColor("#334155"));
                sbg.setCornerRadius(dp(activity, 8));
                listenSlow.setBackground(sbg);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(activity, 34), 1f);
                slp.setMargins(dp(activity, 8), 0, 0, 0);
                listenSlow.setLayoutParams(slp);
                listenSlow.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { speak(activity, cur.english, 0.7f); }
                });
                audioRow.addView(listenSlow);

                final TextView toggleTransBtn = new TextView(activity);
                toggleTransBtn.setText(showTrans[0] ? "👁️ 隱藏譯文" : "👁️ 顯示譯文");
                toggleTransBtn.setTextSize(11);
                toggleTransBtn.setTextColor(Color.parseColor("#38BDF8"));
                toggleTransBtn.setPadding(dp(activity, 10), 0, 0, 0);
                toggleTransBtn.setGravity(Gravity.CENTER_VERTICAL);
                toggleTransBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showTrans[0] = !showTrans[0];
                        zhTv.setVisibility(showTrans[0] ? View.VISIBLE : View.GONE);
                        toggleTransBtn.setText(showTrans[0] ? "👁️ 隱藏譯文" : "👁️ 顯示譯文");
                    }
                });
                audioRow.addView(toggleTransBtn);

                card.addView(audioRow);
                root.addView(card);

                final TextView resultTv = new TextView(activity);
                resultTv.setText(en ? "Tap 'Start Speaking' below and read aloud!" : "點擊下方「🎙️ 開始跟讀朗讀」，對著手機清楚念出上方句子！");
                resultTv.setTextSize(12);
                resultTv.setTextColor(Color.parseColor("#94A3B8"));
                resultTv.setGravity(Gravity.CENTER);
                resultTv.setPadding(0, dp(activity, 12), 0, dp(activity, 12));
                root.addView(resultTv);

                final Button speakBtn = new Button(activity);
                speakBtn.setText(en ? "🎙️ Start Read-Along (Tap to Speak)" : "🎙️ 開始朗讀跟讀（點擊開口）");
                speakBtn.setTextSize(14);
                speakBtn.setTextColor(Color.WHITE);
                speakBtn.setTypeface(Typeface.DEFAULT_BOLD);
                final GradientDrawable spkBg = new GradientDrawable();
                spkBg.setColor(Color.parseColor("#2563EB"));
                spkBg.setCornerRadius(dp(activity, 12));
                speakBtn.setBackground(spkBg);

                LinearLayout.LayoutParams spkLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 46));
                speakBtn.setLayoutParams(spkLp);

                speakBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
                            if (activeRecognizer[0] != null) {
                                try { activeRecognizer[0].destroy(); } catch (Exception ignored) {}
                            }
                            activeRecognizer[0] = SpeechRecognizer.createSpeechRecognizer(activity);

                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                            activeRecognizer[0].setRecognitionListener(new RecognitionListener() {
                                @Override public void onReadyForSpeech(Bundle params) {
                                    speakBtn.setText(en ? "🎙️ Listening... Read the sentence now!" : "🎙️ 正在聆聽中… 請朗讀句子！");
                                    spkBg.setColor(Color.parseColor("#DC2626"));
                                }
                                @Override public void onBeginningOfSpeech() {}
                                @Override public void onRmsChanged(float rmsdB) {}
                                @Override public void onBufferReceived(byte[] buffer) {}
                                @Override public void onEndOfSpeech() {
                                    speakBtn.setText(en ? "Evaluating..." : "正在評估發音吻合度…");
                                    spkBg.setColor(Color.parseColor("#4F46E5"));
                                }
                                @Override public void onError(int error) {
                                    speakBtn.setText(en ? "🎙️ Read Again" : "🎙️ 再試一次");
                                    spkBg.setColor(Color.parseColor("#2563EB"));
                                    resultTv.setText(en ? "Did not catch speech. Tap to try again!" : "未偵測到清晰語音，請點擊按鈕重試！");
                                }
                                @Override public void onResults(Bundle results) {
                                    speakBtn.setText(en ? "🎙️ Read Again" : "🎙️ 再練一次");
                                    spkBg.setColor(Color.parseColor("#2563EB"));

                                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                    if (matches != null && !matches.isEmpty()) {
                                        String spoken = matches.get(0);
                                        evaluateShadowingSpoken(spoken, cur.english, targetTv, resultTv, en, totalScores, scoredCount);
                                    }
                                }
                                @Override public void onPartialResults(Bundle partialResults) {}
                                @Override public void onEvent(int eventType, Bundle params) {}
                            });

                            activeRecognizer[0].startListening(intent);
                        } else {
                            Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                root.addView(speakBtn);

                LinearLayout navRow = new LinearLayout(activity);
                navRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                navLp.setMargins(0, dp(activity, 12), 0, 0);
                navRow.setLayoutParams(navLp);

                if (curItemIdx[0] > 0) {
                    Button prevBtn = new Button(activity);
                    prevBtn.setText("⬅️ " + (en ? "Prev" : "上一句"));
                    prevBtn.setTextSize(12);
                    prevBtn.setTextColor(Color.WHITE);
                    GradientDrawable pb = new GradientDrawable();
                    pb.setColor(Color.parseColor("#334155"));
                    pb.setCornerRadius(dp(activity, 10));
                    prevBtn.setBackground(pb);
                    prevBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            curItemIdx[0]--;
                            run();
                        }
                    });
                    navRow.addView(prevBtn, new LinearLayout.LayoutParams(0, dp(activity, 42), 1f));
                }

                Button nextBtn = new Button(activity);
                nextBtn.setText((curItemIdx[0] + 1 < items.size() ? ("➡️ " + (en ? "Next Sentence" : "下一句")) : ("🏆 " + (en ? "Finish" : "完成結算"))));
                nextBtn.setTextSize(12);
                nextBtn.setTextColor(Color.WHITE);
                GradientDrawable nb = new GradientDrawable();
                nb.setColor(Color.parseColor("#059669"));
                nb.setCornerRadius(dp(activity, 10));
                nextBtn.setBackground(nb);
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, dp(activity, 42), 1.2f);
                if (curItemIdx[0] > 0) nlp.setMargins(dp(activity, 8), 0, 0, 0);
                nextBtn.setLayoutParams(nlp);
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        curItemIdx[0]++;
                        run();
                    }
                });
                navRow.addView(nextBtn);

                root.addView(navRow);
            }
        };

        renderShadowingView.run();
        scroll.addView(root);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static void evaluateShadowingSpoken(String spoken, String target, TextView targetTv, TextView resultTv, boolean en, int[] totalScores, int[] scoredCount) {
        String[] targetWords = target.trim().split("\\s+");
        String[] spokenWords = spoken.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        int matchCount = 0;
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        for (int i = 0; i < targetWords.length; i++) {
            String origWord = targetWords[i];
            String cleanTarget = origWord.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");

            boolean matched = false;
            for (String sw : spokenWords) {
                if (sw.equals(cleanTarget)) {
                    matched = true;
                    break;
                }
            }

            int start = ssb.length();
            ssb.append(origWord);
            int end = ssb.length();

            if (matched) {
                matchCount++;
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#34D399")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#F87171")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < targetWords.length - 1) {
                ssb.append(" ");
            }
        }

        targetTv.setText(ssb);

        int accuracy = (int) (((float) matchCount / Math.max(1, targetWords.length)) * 100);
        totalScores[0] += accuracy;
        scoredCount[0]++;

        if (accuracy >= 85) {
            resultTv.setText("🎯 " + (en ? "Pronunciation Accuracy: " : "發音掌握度：") + accuracy + "% · " + (en ? "🌟 Perfect! Authentic intonation!" : "🌟 太棒了！發音非常地道，連音到位！"));
            resultTv.setTextColor(Color.parseColor("#34D399"));
        } else if (accuracy >= 65) {
            resultTv.setText("🎯 " + (en ? "Pronunciation Accuracy: " : "發音掌握度：") + accuracy + "% · " + (en ? "👍 Good effort! Listen closely to the red words." : "👍 表現良好！點擊 🐢 聽清楚紅色單字再試一次！"));
            resultTv.setTextColor(Color.parseColor("#FBBF24"));
        } else {
            resultTv.setText("🎯 " + (en ? "Pronunciation Accuracy: " : "發音掌握度：") + accuracy + "% · " + (en ? "💪 Keep going! Try listening at 0.7x speed." : "💪 勇於開口！建議點擊 🐢 慢速聽清楚再念一遍！"));
            resultTv.setTextColor(Color.parseColor("#F87171"));
        }
    }

    private static List<ShadowingItem> getCategoryItems(Context context, int catIdx, List<ShadowingItem> customItems) {
        List<ShadowingItem> list = new ArrayList<>();
        if (catIdx == 0) {
            List<LearningDataManager.StarredItem> starred = LearningDataManager.getStarredItems(context);
            for (LearningDataManager.StarredItem s : starred) {
                if (s.originalText != null && !s.originalText.trim().isEmpty()) {
                    list.add(new ShadowingItem(s.originalText, s.translation, s.notes));
                }
            }
            if (list.isEmpty() && customItems != null) {
                list.addAll(customItems);
            }
        } else if (catIdx == 1) {
            list.add(new ShadowingItem("Could I get an iced latte with oat milk, please?", "可以給我一杯燕麥奶冰拿鐵嗎？", "注意 get an 連音 /ɡɛt ən/"));
            list.add(new ShadowingItem("Could we have the check, please? We'd like to split it.", "麻煩幫我們買單，我們想分開結帳。", "注意 split it 連音"));
            list.add(new ShadowingItem("What do you recommend as the house special today?", "你推薦今天的哪道招牌菜呢？", "recommend 重音在第三音節"));
            list.add(new ShadowingItem("Excuse me, is this seat taken?", "不好意思，請問這個位子有人坐嗎？", "taken 尾音輕讀"));
            list.add(new ShadowingItem("Could you make that extra hot and with less sugar?", "可以幫我做特熱、少糖嗎？", "extra hot 語調自然上揚"));
        } else if (catIdx == 2) {
            list.add(new ShadowingItem("I'd like to touch base with you regarding next quarter's roadmap.", "我想跟您對焦一下下個季度的產品規劃。", "touch base 常用商務片語"));
            list.add(new ShadowingItem("Could you please elaborate on that point in more detail?", "可以請您針對那一項細節再多加說明嗎？", "elaborate /ɪˈlæbəreɪt/"));
            list.add(new ShadowingItem("Let's circle back on this after the stakeholder meeting.", "我們在利害關係人會議後再回來討論這個案子。", "circle back 表示稍後再議"));
            list.add(new ShadowingItem("From my perspective, this approach mitigates the risk significantly.", "從我的觀點來看，這個做法能大幅降低風險。", "mitigate /ˈmɪtɪɡeɪt/ 緩解"));
            list.add(new ShadowingItem("I completely agree with your proposal, let's move forward with it.", "我非常贊同你的提議，讓我們按此推進吧。", "move forward 往前推進"));
        } else if (catIdx == 3) {
            list.add(new ShadowingItem("Excuse me, could you tell me where baggage claim for flight AA123 is?", "不好意思，請問 AA123 班機的行李領取處在哪？", "baggage claim 行李提領處"));
            list.add(new ShadowingItem("I have a reservation under the name Alex for two nights.", "我有預訂兩晚房間，名字是 Alex。", "under the name... 用...名字登記"));
            list.add(new ShadowingItem("Is breakfast included, and what time is checkout tomorrow?", "請問有附早餐嗎？明天的退房時間是幾點？", "included /ɪnˈkluːdɪd/"));
            list.add(new ShadowingItem("Could you please call a taxi for me to the airport?", "可以麻煩幫我叫一台去機場的計程車嗎？", "taxi to the airport"));
            list.add(new ShadowingItem("Where is the nearest subway station from here?", "請問離這裡最近的地鐵站在哪裡？", "nearest /ˈnɪərɪst/"));
        } else if (catIdx == 4) {
            list.add(new ShadowingItem("It is so good to finally meet you in person!", "真高興終於能見到你本人！", "in person 當面/親自"));
            list.add(new ShadowingItem("How has your week been going so far?", "你這週過得怎麼樣？", "so far 到目前為止"));
            list.add(new ShadowingItem("That sounds fascinating, how did you get into that field?", "聽起來太有趣了，你當初是怎麼進入這個領域的？", "fascinating /ˈfæsɪneɪtɪŋ/"));
            list.add(new ShadowingItem("Do you have any exciting plans for the upcoming weekend?", "這個即將到來的週末你有什麼精彩計畫嗎？", "upcoming /ˈʌpkʌmɪŋ/"));
            list.add(new ShadowingItem("I know exactly what you mean, that happens to me all the time.", "我完全懂你的意思，我也常常遇到這種情況。", "exactly /ɪɡˈzæktli/"));
        } else {
            list.add(new ShadowingItem("Could you please say that in simpler words?", "可以請您用更簡單的單字再說一次嗎？", "simpler words 救命句"));
            list.add(new ShadowingItem("Pardon me, could you speak a little slower, please?", "不好意思，可以請您說得稍微慢一點點嗎？", "a little slower 慢一點"));
            list.add(new ShadowingItem("How do you pronounce this word in native English?", "母語者通常怎麼發這個單字的音？", "pronounce /prəˈnaʊns/"));
            list.add(new ShadowingItem("What is the difference between these two phrases?", "這兩個短語之間有什麼差別呢？", "difference between..."));
            list.add(new ShadowingItem("Did I use the right grammar in that sentence?", "我剛才那句話文法用得正確嗎？", "right grammar 正確文法"));
        }
        return list;
    }

    // ── 🃏 Priority 4: Flashcard Quiz Dialog ──
    public static void showFlashcardQuizDialog(final Activity activity) {
        final boolean en = I18n.isEnglish(activity);
        final List<LearningDataManager.StarredItem> items = LearningDataManager.getStarredItems(activity);

        if (items.isEmpty()) {
            Toast.makeText(activity, en ? "No starred phrases yet! Star phrases during practice first." : "生詞本尚無收藏！請在對話或報告中點擊 ⭐ 收藏生詞後再來自測。", Toast.LENGTH_LONG).show();
            return;
        }

        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E6090D16")));
        }

        ScrollView scroll = new ScrollView(activity);
        disableScrollbars(scroll);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 20));

        final LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#6366F1"));
        container.setBackground(cBg);

        final int[] curIdx = new int[]{0};
        final boolean[] isRevealed = new boolean[]{false};

        final Runnable renderCard = new Runnable() {
            @Override public void run() {
                container.removeAllViews();

                if (curIdx[0] >= items.size()) {
                    dialog.dismiss();
                    return;
                }

                final LearningDataManager.StarredItem item = items.get(curIdx[0]);

                LinearLayout topRow = new LinearLayout(activity);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView titleTv = new TextView(activity);
                titleTv.setText("🃏 " + (en ? "Flashcard Review (" : "生詞翻卡測驗 (") + (curIdx[0] + 1) + "/" + items.size() + ")");
                titleTv.setTextSize(14);
                titleTv.setTextColor(Color.parseColor("#A5B4FC"));
                titleTv.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView closeBtn = new TextView(activity);
                closeBtn.setText("✕");
                closeBtn.setTextSize(18);
                closeBtn.setTextColor(Color.parseColor("#94A3B8"));
                closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { dialog.dismiss(); }
                });
                topRow.addView(closeBtn);
                container.addView(topRow);

                LinearLayout cardBox = new LinearLayout(activity);
                cardBox.setOrientation(LinearLayout.VERTICAL);
                cardBox.setGravity(Gravity.CENTER);
                cardBox.setPadding(dp(activity, 20), dp(activity, 28), dp(activity, 20), dp(activity, 28));
                GradientDrawable cdBg = new GradientDrawable();
                cdBg.setColor(Color.parseColor("#1E293B"));
                cdBg.setCornerRadius(dp(activity, 16));
                cdBg.setStroke(dp(activity, 1), isRevealed[0] ? Color.parseColor("#34D399") : Color.parseColor("#475569"));
                cardBox.setBackground(cdBg);

                LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cblp.setMargins(0, dp(activity, 16), 0, dp(activity, 16));
                cardBox.setLayoutParams(cblp);

                TextView phraseTv = new TextView(activity);
                phraseTv.setText(item.originalText);
                phraseTv.setTextSize(18);
                phraseTv.setTextColor(Color.WHITE);
                phraseTv.setTypeface(Typeface.DEFAULT_BOLD);
                phraseTv.setGravity(Gravity.CENTER);
                cardBox.addView(phraseTv);

                Button playAudio = new Button(activity);
                playAudio.setText("🔊 聽原生發音");
                playAudio.setTextSize(11);
                playAudio.setTextColor(Color.WHITE);
                GradientDrawable pbg = new GradientDrawable();
                pbg.setColor(Color.parseColor("#4F46E5"));
                pbg.setCornerRadius(dp(activity, 8));
                playAudio.setBackground(pbg);
                playAudio.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { speak(activity, item.originalText, 1.0f); }
                });
                LinearLayout.LayoutParams palb = new LinearLayout.LayoutParams(dp(activity, 120), dp(activity, 32));
                palb.setMargins(0, dp(activity, 12), 0, dp(activity, 10));
                playAudio.setLayoutParams(palb);
                cardBox.addView(playAudio);

                if (!isRevealed[0]) {
                    Button revealBtn = new Button(activity);
                    revealBtn.setText("👁️ 點擊翻轉查看中文與解析");
                    revealBtn.setTextSize(12);
                    revealBtn.setTextColor(Color.parseColor("#38BDF8"));
                    revealBtn.setBackground(null);
                    revealBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            isRevealed[0] = true;
                            run();
                        }
                    });
                    cardBox.addView(revealBtn);
                } else {
                    TextView meanTv = new TextView(activity);
                    meanTv.setText(item.translation != null && !item.translation.isEmpty() ? item.translation : (en ? "Authentic Native Expression" : "道地母語表達"));
                    meanTv.setTextSize(14);
                    meanTv.setTextColor(Color.parseColor("#34D399"));
                    meanTv.setTypeface(Typeface.DEFAULT_BOLD);
                    meanTv.setPadding(0, dp(activity, 8), 0, 0);
                    cardBox.addView(meanTv);

                    if (item.notes != null && !item.notes.isEmpty()) {
                        TextView expTv = new TextView(activity);
                        expTv.setText("💡 " + item.notes);
                        expTv.setTextSize(11);
                        expTv.setTextColor(Color.parseColor("#C7D2FE"));
                        expTv.setPadding(0, dp(activity, 4), 0, 0);
                        cardBox.addView(expTv);
                    }
                }

                container.addView(cardBox);

                LinearLayout actRow = new LinearLayout(activity);
                actRow.setOrientation(LinearLayout.HORIZONTAL);

                Button againBtn = new Button(activity);
                againBtn.setText("🔄 稍後再練");
                againBtn.setTextSize(12);
                againBtn.setTextColor(Color.WHITE);
                GradientDrawable agBg = new GradientDrawable();
                agBg.setColor(Color.parseColor("#334155"));
                agBg.setCornerRadius(dp(activity, 10));
                againBtn.setBackground(agBg);
                againBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        isRevealed[0] = false;
                        curIdx[0] = (curIdx[0] + 1) % items.size();
                        run();
                    }
                });
                actRow.addView(againBtn, new LinearLayout.LayoutParams(0, dp(activity, 44), 1f));

                Button masterBtn = new Button(activity);
                masterBtn.setText("✅ 已掌握 (下一張)");
                masterBtn.setTextSize(12);
                masterBtn.setTextColor(Color.WHITE);
                GradientDrawable mBg = new GradientDrawable();
                mBg.setColor(Color.parseColor("#059669"));
                mBg.setCornerRadius(dp(activity, 10));
                masterBtn.setBackground(mBg);
                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, dp(activity, 44), 1.2f);
                mlp.setMargins(dp(activity, 8), 0, 0, 0);
                masterBtn.setLayoutParams(mlp);
                masterBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        isRevealed[0] = false;
                        if (curIdx[0] + 1 < items.size()) {
                            curIdx[0]++;
                            run();
                        } else {
                            Toast.makeText(activity, en ? "🎉 All cards reviewed!" : "🎉 恭喜完成本輪所有生詞複習！", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }
                });
                actRow.addView(masterBtn);
                container.addView(actRow);
            }
        };

        renderCard.run();
        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
