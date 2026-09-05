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

    // ── 💡 Priority 1: In-Call Teleprompter & Response Starters ──
    public static void showHintsBottomSheet(final Activity activity, final CourseModel.Lesson lesson, final String scenario) {
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D9090D16")));
        }

        ScrollView scroll = new ScrollView(activity);
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

        List<CourseModel.WarmupPhrase> suggestions = new ArrayList<>();
        if (lesson != null && !lesson.warmupPhrases.isEmpty()) {
            suggestions.addAll(lesson.warmupPhrases);
        } else {
            suggestions.add(new CourseModel.WarmupPhrase("Could you tell me more about that?", "可以多跟我分享一些細節嗎？", "/kʊd juː tɛl miː mɔːr əˈbaʊt ðæt/", "引導對方繼續說"));
            suggestions.add(new CourseModel.WarmupPhrase("In my opinion, I prefer to...", "依我看，我比較偏好...", "/ɪn maɪ əˈpɪnjən, aɪ prɪˈfɜːr tuː/", "表達個人看法"));
            suggestions.add(new CourseModel.WarmupPhrase("That is really interesting! How does that work?", "太有意思了！那具體是怎麼運作的？", "/ðæts ˈrɪəli ˈɪntrəstɪŋ/", "表達驚喜讚賞"));
            suggestions.add(new CourseModel.WarmupPhrase("To be honest, I haven not thought about that before.", "老實說，我之前還真沒想過這個問題。", "/tuː biː ˈɒnɪst, aɪ hævnt θɔːt...", "爭取思考時間神句"));
        }

        for (final CourseModel.WarmupPhrase p : suggestions) {
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
            enTv.setText(p.en);
            enTv.setTextSize(13);
            enTv.setTextColor(Color.WHITE);
            enTv.setTypeface(Typeface.DEFAULT_BOLD);
            phraseTop.addView(enTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView playBtn = new TextView(activity);
            playBtn.setText("🔊 聽發音");
            playBtn.setTextSize(11);
            playBtn.setTextColor(Color.parseColor("#38BDF8"));
            playBtn.setPadding(dp(activity, 6), dp(activity, 2), dp(activity, 6), dp(activity, 2));
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { speak(activity, p.en, 1.0f); }
            });
            phraseTop.addView(playBtn);
            pCard.addView(phraseTop);

            TextView zhTv = new TextView(activity);
            zhTv.setText(p.zh);
            zhTv.setTextSize(11);
            zhTv.setTextColor(Color.parseColor("#94A3B8"));
            zhTv.setPadding(0, dp(activity, 2), 0, 0);
            pCard.addView(zhTv);

            if (p.ipa != null && !p.ipa.isEmpty()) {
                TextView ipaTv = new TextView(activity);
                ipaTv.setText(p.ipa);
                ipaTv.setTextSize(10);
                ipaTv.setTextColor(Color.parseColor("#A78BFA"));
                pCard.addView(ipaTv);
            }

            container.addView(pCard);
        }

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

        final int[] curIdx = new int[]{0};
        final boolean[] isRevealed = new boolean[]{false};

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 20));

        final LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#F59E0B"));
        container.setBackground(cBg);

        final Runnable renderCard = new Runnable() {
            @Override public void run() {
                container.removeAllViews();
                final LearningDataManager.StarredItem item = items.get(curIdx[0]);

                LinearLayout topRow = new LinearLayout(activity);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView titleTv = new TextView(activity);
                titleTv.setText("🃏 " + (en ? "Flashcard Drill (" : "抽卡自測小遊戲 (") + (curIdx[0] + 1) + "/" + items.size() + ")");
                titleTv.setTextSize(14);
                titleTv.setTextColor(Color.parseColor("#FBBF24"));
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
                GradientDrawable fcBg = new GradientDrawable();
                fcBg.setColor(Color.parseColor("#1E293B"));
                fcBg.setCornerRadius(dp(activity, 16));
                fcBg.setStroke(dp(activity, 2), isRevealed[0] ? Color.parseColor("#10B981") : Color.parseColor("#3B82F6"));
                cardBox.setBackground(fcBg);

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
