package com.crewpocket.teacher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NativeLiveActivity extends Activity {
    private static final int REQUEST_RECORD_AUDIO = 301;

    private TextView statusDot;
    private TextView statusText;
    private TextView transcript;
    private ScrollView transcriptScrollView;
    private TextView meterText;
    private Button callButton;
    private Button muteButton;
    private NativeGeminiLiveClient client;
    private final Handler handler = new Handler();

    // ── 📖 Option 1: Word-by-Word Real-Time Color Feedback & Diagnostic Card ──
    private boolean isShadowingMode = false;
    private String fullReadingText = "";
    private String[] rawWords;
    private String[] cleanWords;
    private int[] wordStates; // 0 = Pending (grey), 1 = Correct (green), 2 = Mispronounced/Skipped (red), 3 = Current (cyan)
    private final List<String> spokenTokenHistory = new ArrayList<String>();
    private TextView readingBoardText;
    private LinearLayout diagnosticCard;
    private TextView scoreBadge;
    private LinearLayout troubleWordsContainer;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private int dp(float val) {
        return CrewTheme.dp(this, val);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(CrewTheme.BG_PRIMARY);
            getWindow().setNavigationBarColor(CrewTheme.BG_PRIMARY);
        }
        getWindow().getDecorView().setBackgroundColor(CrewTheme.BG_PRIMARY);

        final boolean en = I18n.isEnglish(this);
        String teachingMode = AppConfig.getTeachingMode(this);
        isShadowingMode = "shadowing".equals(teachingMode);
        fullReadingText = AppConfig.getReadingText(this);

        initTts();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(20));
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Back button & Title
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText(en ? "‹ Back" : "‹ 返回");
        backBtn.setTextSize(15);
        backBtn.setTextColor(CrewTheme.INDIGO_400);
        backBtn.setTypeface(Typeface.DEFAULT_BOLD);
        backBtn.setPadding(0, 0, dp(14), 0);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(backBtn);

        TextView title = new TextView(this);
        title.setText(isShadowingMode
                ? (en ? "📖 Reading & Pronunciation Coach" : "📖 朗讀高亮與發音診斷")
                : (en ? "🎓 Oral Practice Classroom" : "🎓 口語即時對話教室"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);
        root.addView(header);

        // 2. Status Row
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.HORIZONTAL);
        statusBox.setGravity(Gravity.CENTER_VERTICAL);
        statusBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(Color.parseColor("#1E293B"));
        sbg.setCornerRadius(dp(12));
        statusBox.setBackground(sbg);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(12), 0, dp(12));
        statusBox.setLayoutParams(sLp);

        statusDot = new TextView(this);
        statusDot.setText("●");
        statusDot.setTextSize(13);
        statusDot.setTextColor(CrewTheme.TEXT_MUTED);
        statusDot.setPadding(0, 0, dp(8), 0);
        statusBox.addView(statusDot);

        statusText = new TextView(this);
        statusText.setText(en ? "Standby (Tap Start below)" : "待命中（點擊下方開始練習）");
        statusText.setTextColor(CrewTheme.TEXT_SECONDARY);
        statusText.setTextSize(12);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusBox.addView(statusText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        meterText = new TextView(this);
        meterText.setText("🎙️ -- dB");
        meterText.setTextColor(CrewTheme.TEXT_MUTED);
        meterText.setTextSize(11);
        statusBox.addView(meterText);
        root.addView(statusBox);

        // 3. Main Area: If Shadowing mode -> Interactive Reading Board + Diagnostic Card
        if (isShadowingMode) {
            setupReadingData();

            ScrollView readingScroll = new ScrollView(this);
            readingScroll.setFillViewport(true);
            LinearLayout.LayoutParams rslp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            rslp.setMargins(0, 0, 0, dp(14));
            readingScroll.setLayoutParams(rslp);

            LinearLayout readingContainer = new LinearLayout(this);
            readingContainer.setOrientation(LinearLayout.VERTICAL);

            // 3.1 Word-by-Word Color Board Card
            LinearLayout boardCard = new LinearLayout(this);
            boardCard.setOrientation(LinearLayout.VERTICAL);
            boardCard.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable bBg = new GradientDrawable();
            bBg.setColor(Color.parseColor("#0F172A"));
            bBg.setCornerRadius(dp(16));
            bBg.setStroke(dp(1), Color.parseColor("#334155"));
            boardCard.setBackground(bBg);

            LinearLayout bHeader = new LinearLayout(this);
            bHeader.setOrientation(LinearLayout.HORIZONTAL);
            bHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView bTitle = new TextView(this);
            bTitle.setText(en ? "📖 Real-time Reading Board (Colors as you speak)" : "📖 即時朗讀板（點字聽發音，說話自動變色）");
            bTitle.setTextSize(12);
            bTitle.setTextColor(Color.parseColor("#38BDF8"));
            bTitle.setTypeface(Typeface.DEFAULT_BOLD);
            bHeader.addView(bTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // ✨ AI New Passage Button
            Button aiGenBtn = new Button(this);
            aiGenBtn.setText(en ? "✨ AI New" : "✨ 換一篇");
            aiGenBtn.setTextSize(11);
            aiGenBtn.setTextColor(Color.WHITE);
            GradientDrawable aBg = new GradientDrawable();
            aBg.setColor(Color.parseColor("#4F46E5"));
            aBg.setCornerRadius(dp(8));
            aiGenBtn.setBackground(aBg);
            aiGenBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    triggerAiNewPassage();
                }
            });
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(dp(72), dp(32));
            aLp.setMargins(0, 0, dp(4), 0);
            bHeader.addView(aiGenBtn, aLp);

            // ✏️ Custom Passage Button
            Button customBtn = new Button(this);
            customBtn.setText(en ? "✏️ Custom" : "✏️ 自訂");
            customBtn.setTextSize(11);
            customBtn.setTextColor(Color.WHITE);
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(Color.parseColor("#0D9488"));
            cBg.setCornerRadius(dp(8));
            customBtn.setBackground(cBg);
            customBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showCustomReadingDialog();
                }
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(dp(68), dp(32));
            cLp.setMargins(0, 0, dp(4), 0);
            bHeader.addView(customBtn, cLp);

            // Reset / Restart Button
            Button resetBtn = new Button(this);
            resetBtn.setText(en ? "🔄 Restart" : "🔄 重新");
            resetBtn.setTextSize(11);
            resetBtn.setTextColor(Color.WHITE);
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(Color.parseColor("#334155"));
            rBg.setCornerRadius(dp(8));
            resetBtn.setBackground(rBg);
            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    resetReadingBoard();
                    Toast.makeText(NativeLiveActivity.this, en ? "Reading board reset! Start reading from beginning." : "已重置朗讀板！請隨時從頭開口朗讀。", Toast.LENGTH_SHORT).show();
                }
            });
            bHeader.addView(resetBtn, new LinearLayout.LayoutParams(dp(68), dp(32)));
            boardCard.addView(bHeader);

            // Legend Row
            LinearLayout legendRow = new LinearLayout(this);
            legendRow.setOrientation(LinearLayout.HORIZONTAL);
            legendRow.setPadding(0, dp(6), 0, dp(8));
            legendRow.addView(makeLegendDot("#22C55E", en ? "Correct" : "正確"));
            legendRow.addView(makeLegendDot("#EF4444", en ? "Mispronounced / Skipped" : "發音偏差/漏字"));
            boardCard.addView(legendRow);

            readingBoardText = new TextView(this);
            readingBoardText.setTextSize(16);
            readingBoardText.setLineSpacing(dp(6), 1.25f);
            readingBoardText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            readingBoardText.setPadding(0, dp(6), 0, dp(6));
            readingBoardText.setMovementMethod(LinkMovementMethod.getInstance());
            boardCard.addView(readingBoardText);
            renderReadingBoardSpannable();

            readingContainer.addView(boardCard);

            // 3.2 Pronunciation Diagnostic Card
            diagnosticCard = new LinearLayout(this);
            diagnosticCard.setOrientation(LinearLayout.VERTICAL);
            diagnosticCard.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable dBg = new GradientDrawable();
            dBg.setColor(Color.parseColor("#1E1B4B"));
            dBg.setCornerRadius(dp(16));
            dBg.setStroke(dp(1), Color.parseColor("#6366F1"));
            diagnosticCard.setBackground(dBg);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dLp.setMargins(0, dp(12), 0, 0);
            diagnosticCard.setLayoutParams(dLp);

            LinearLayout dHeader = new LinearLayout(this);
            dHeader.setOrientation(LinearLayout.HORIZONTAL);
            dHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView dTitle = new TextView(this);
            dTitle.setText(en ? "🎯 Pronunciation Assessment" : "🎯 發音體檢診斷卡");
            dTitle.setTextSize(13);
            dTitle.setTextColor(Color.parseColor("#C7D2FE"));
            dTitle.setTypeface(Typeface.DEFAULT_BOLD);
            dHeader.addView(dTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            scoreBadge = new TextView(this);
            scoreBadge.setText(en ? "Score: --" : "準確率：--");
            scoreBadge.setTextSize(12);
            scoreBadge.setTextColor(Color.parseColor("#34D399"));
            scoreBadge.setTypeface(Typeface.DEFAULT_BOLD);
            dHeader.addView(scoreBadge);
            diagnosticCard.addView(dHeader);

            troubleWordsContainer = new LinearLayout(this);
            troubleWordsContainer.setOrientation(LinearLayout.VERTICAL);
            troubleWordsContainer.setPadding(0, dp(8), 0, 0);

            TextView hintMsg = new TextView(this);
            hintMsg.setText(en
                    ? "Read the text above naturally. When you speak, live phonetic analysis and IPA tips will update here!"
                    : "請直接對著手機自然大聲朗讀上方文字。朗讀時，系統會即時分析並在此生成發音偏差單字與標準音標！");
            hintMsg.setTextSize(12);
            hintMsg.setTextColor(Color.parseColor("#94A3B8"));
            troubleWordsContainer.addView(hintMsg);

            diagnosticCard.addView(troubleWordsContainer);
            readingContainer.addView(diagnosticCard);

            transcript = new TextView(this);
            transcript.setVisibility(View.GONE);

            readingScroll.addView(readingContainer);
            root.addView(readingScroll);

        } else {
            // Standard Conversational Classroom Transcript Card
            LinearLayout transcriptCard = new LinearLayout(this);
            transcriptCard.setOrientation(LinearLayout.VERTICAL);
            transcriptCard.setPadding(dp(16), dp(14), dp(16), dp(14));
            transcriptCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_CARD, CrewTheme.BORDER_DEFAULT, 16));
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            tLp.setMargins(0, 0, 0, dp(18));
            transcriptCard.setLayoutParams(tLp);

            TextView tTitle = new TextView(this);
            tTitle.setText(en ? "💬 Real-time Transcript" : "💬 即時對話逐字紀錄");
            tTitle.setTextSize(12);
            tTitle.setTextColor(CrewTheme.TEXT_MUTED);
            tTitle.setTypeface(Typeface.DEFAULT_BOLD);
            transcriptCard.addView(tTitle);

            transcriptScrollView = new ScrollView(this);
            transcriptScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            transcriptScrollView.setPadding(0, dp(4), 0, 0);

            chatCardsContainer = new LinearLayout(this);
            chatCardsContainer.setOrientation(LinearLayout.VERTICAL);
            chatCardsContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            transcriptScrollView.addView(chatCardsContainer);
            transcriptCard.addView(transcriptScrollView);
            root.addView(transcriptCard);
            renderChatCards();
        }

        // 4. Control Buttons Row
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        callButton = new Button(this);
        callButton.setText(isShadowingMode
                ? (en ? "🎙️ Start Reading" : "🎙️ 開始朗讀練習")
                : (en ? "🎙️ Start Practice" : "🎙️ 開始口語對話"));
        callButton.setTextColor(Color.WHITE);
        callButton.setTextSize(15);
        callButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable callBg = new GradientDrawable();
        callBg.setColor(Color.parseColor("#2563EB"));
        callBg.setCornerRadius(dp(14));
        callButton.setBackground(callBg);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleCall(); }
        });
        controls.addView(callButton, new LinearLayout.LayoutParams(0, dp(50), 1.5f));

        muteButton = new Button(this);
        muteButton.setText(en ? "Mute" : "靜音");
        muteButton.setTextColor(Color.WHITE);
        muteButton.setTextSize(13);
        GradientDrawable muteBg = new GradientDrawable();
        muteBg.setColor(Color.parseColor("#334155"));
        muteBg.setCornerRadius(dp(14));
        muteButton.setBackground(muteBg);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        mLp.setMargins(dp(10), 0, 0, 0);
        muteButton.setLayoutParams(mLp);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (client != null) {
                    boolean muted = client.toggleAgentMute();
                    boolean isEn = I18n.isEnglish(NativeLiveActivity.this);
                    muteButton.setText(muted ? (isEn ? "Unmute" : "取消靜音") : (isEn ? "Mute" : "靜音"));
                    ((GradientDrawable) muteButton.getBackground()).setColor(muted ? Color.parseColor("#D97706") : Color.parseColor("#334155"));
                }
            }
        });
        controls.addView(muteButton);

        root.addView(controls);
        setContentView(root);

        if (isShadowingMode && (fullReadingText == null || fullReadingText.trim().isEmpty())) {
            if (!AppConfig.getGeminiApiKey(this).isEmpty()) {
                triggerAiNewPassage();
            }
        }
    }

    private View makeLegendDot(String colorHex, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, dp(12), 0);

        TextView dot = new TextView(this);
        dot.setText("● ");
        dot.setTextSize(10);
        dot.setTextColor(Color.parseColor(colorHex));
        row.addView(dot);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(10);
        lbl.setTextColor(Color.parseColor("#94A3B8"));
        row.addView(lbl);
        return row;
    }

    private void setupReadingData() {
        if (fullReadingText == null || fullReadingText.trim().isEmpty()) {
            rawWords = new String[0];
            cleanWords = new String[0];
            wordStates = new int[0];
            spokenTokenHistory.clear();
            return;
        }
        rawWords = fullReadingText.trim().split("\\s+");
        cleanWords = new String[rawWords.length];
        wordStates = new int[rawWords.length];
        for (int i = 0; i < rawWords.length; i++) {
            cleanWords[i] = rawWords[i].replaceAll("[^a-zA-Z0-9\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uac00-\ud7af]", "").toLowerCase();
            wordStates[i] = 0; // pending
        }
        spokenTokenHistory.clear();
    }

    private void resetReadingBoard() {
        spokenTokenHistory.clear();
        if (wordStates != null) {
            for (int i = 0; i < wordStates.length; i++) {
                wordStates[i] = 0;
            }
            renderReadingBoardSpannable();
        }
        if (troubleWordsContainer != null) {
            troubleWordsContainer.removeAllViews();
            boolean en = I18n.isEnglish(this);
            scoreBadge.setText(en ? "Score: --" : "準確率：--");
            TextView hintMsg = new TextView(this);
            hintMsg.setText(en ? "Start reading whenever you're ready!" : "請隨時開口朗讀，系統將即時高亮！");
            hintMsg.setTextSize(12);
            hintMsg.setTextColor(Color.parseColor("#94A3B8"));
            troubleWordsContainer.addView(hintMsg);
        }
    }

    private void renderReadingBoardSpannable() {
        if (readingBoardText == null) return;
        if (rawWords == null || rawWords.length == 0) {
            boolean en = I18n.isEnglish(this);
            readingBoardText.setText(en ? "⚪ (No reading passage loaded. Tap '✨ AI New' or '✏️ Custom' above)" : "⚪ (尚未載入朗讀教材，請點擊上方「✨ 換一篇」或「✏️ 自訂」)");
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        for (int i = 0; i < rawWords.length; i++) {
            final int wordIndex = i;
            final String rawWord = rawWords[i];
            final String cleanWord = cleanWords[i];
            int start = ssb.length();
            ssb.append(rawWord);
            int end = ssb.length();

            int state = wordStates[i];
            if (state == 1) {
                // Correct -> Vibrant Green
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#22C55E")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (state == 2) {
                // Mispronounced / Deviation -> Vivid Red + Underline
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#EF4444")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Default / Unread -> Clean Silver White
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#E2E8F0")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // ClickableSpan so tapping ANY word speaks it and shows IPA & tips!
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    speakWord(cleanWord);
                    String ipa = getIpaForWord(cleanWord);
                    String tip = getTipForWord(cleanWord);
                    Toast.makeText(NativeLiveActivity.this, rawWord + " " + ipa + "\n" + tip, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(state == 2);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (i < rawWords.length - 1) {
                ssb.append(" ");
            }
        }
        readingBoardText.setText(ssb);
    }

    /**
     * Dynamic Sequence Alignment:
     * Robustly aligns cumulative/incremental spoken tokens with reference text.
     * Handles stumbles, repeats, self-corrections, and multi-run loops without getting stuck!
     */
    private synchronized void processSpokenTextForShadowing(String spokenChunk) {
        if (spokenChunk == null || cleanWords == null || cleanWords.length == 0) return;
        String[] newTokens = spokenChunk.trim().split("\\s+");

        for (String t : newTokens) {
            String clean = t.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (!clean.isEmpty()) {
                spokenTokenHistory.add(clean);
            }
        }

        if (spokenTokenHistory.isEmpty()) return;

        // 🔄 Multi-run Auto-restart Detection:
        // If user already reached towards the end, but now spoke the first 1-2 words of the text, auto-reset history!
        int currentFurthest = 0;
        for (int i = 0; i < wordStates.length; i++) {
            if (wordStates[i] == 1 || wordStates[i] == 2) currentFurthest = i;
        }
        if (currentFurthest >= Math.max(2, cleanWords.length - 3)) {
            // Check if latest 3 spoken tokens match the beginning of cleanWords
            int histSize = spokenTokenHistory.size();
            for (int i = Math.max(0, histSize - 4); i < histSize; i++) {
                if (isWordMatch(spokenTokenHistory.get(i), cleanWords[0])) {
                    // User restarted reading from the beginning!
                    List<String> freshTokens = new ArrayList<String>(spokenTokenHistory.subList(i, histSize));
                    spokenTokenHistory.clear();
                    spokenTokenHistory.addAll(freshTokens);
                    for (int k = 0; k < wordStates.length; k++) wordStates[k] = 0;
                    break;
                }
            }
        }

        // Perform Sliding Window Dynamic Alignment between cleanWords and spokenTokenHistory
        int spokenIdx = 0;
        int lastMatchedRefIdx = -1;

        // Reset states
        for (int i = 0; i < cleanWords.length; i++) {
            wordStates[i] = 0;
        }

        for (int refIdx = 0; refIdx < cleanWords.length && spokenIdx < spokenTokenHistory.size(); refIdx++) {
            String target = cleanWords[refIdx];
            int bestSpokenMatch = -1;

            // Search within forward window in spoken tokens (up to 4 tokens)
            int window = Math.min(spokenTokenHistory.size(), spokenIdx + 4);
            for (int s = spokenIdx; s < window; s++) {
                if (isWordMatch(spokenTokenHistory.get(s), target)) {
                    bestSpokenMatch = s;
                    break;
                }
            }

            if (bestSpokenMatch != -1) {
                wordStates[refIdx] = 1; // Green
                spokenIdx = bestSpokenMatch + 1;
                lastMatchedRefIdx = refIdx;
            } else {
                // If we have advanced past this word in spoken stream, mark red
                if (spokenIdx > 0 && spokenIdx < spokenTokenHistory.size()) {
                    wordStates[refIdx] = 2; // Red (deviation/skipped)
                }
            }
        }

        renderReadingBoardSpannable();
        showPronunciationDiagnosticReport();
    }

    private boolean isWordMatch(String spoken, String target) {
        if (spoken == null || target == null) return false;
        if (spoken.equals(target)) return true;
        if (spoken.contains(target) || target.contains(spoken)) return true;
        return getLevenshteinDistance(spoken, target) <= 1;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private void showPronunciationDiagnosticReport() {
        if (troubleWordsContainer == null || cleanWords == null) return;
        troubleWordsContainer.removeAllViews();
        final boolean en = I18n.isEnglish(this);

        int correctCount = 0;
        int totalRead = 0;
        List<String> troubleWords = new ArrayList<String>();
        for (int i = 0; i < cleanWords.length; i++) {
            if (wordStates[i] == 1) {
                correctCount++;
                totalRead++;
            } else if (wordStates[i] == 2) {
                totalRead++;
                if (!troubleWords.contains(cleanWords[i])) {
                    troubleWords.add(cleanWords[i]);
                }
            }
        }

        if (totalRead == 0) {
            scoreBadge.setText(en ? "Score: --" : "準確率：--");
            TextView hintMsg = new TextView(this);
            hintMsg.setText(en ? "Start reading whenever you're ready!" : "請隨時開口朗讀，系統將即時分析！");
            hintMsg.setTextSize(12);
            hintMsg.setTextColor(Color.parseColor("#94A3B8"));
            troubleWordsContainer.addView(hintMsg);
            return;
        }

        int score = (int) Math.round((double) correctCount * 100.0 / Math.max(1, totalRead));
        String scoreMsg = (score >= 85 ? (en ? "🌟 Excellent (" : "🌟 發音優異 (") : (score >= 70 ? (en ? "👍 Good (" : "👍 良好 (") : (en ? "💪 Keep Practicing (" : "💪 再接再厲 ("))) + score + "% · " + correctCount + "/" + totalRead + ")";
        scoreBadge.setText(scoreMsg);

        if (troubleWords.isEmpty()) {
            TextView perfectText = new TextView(this);
            perfectText.setText(en ? "🎉 100% Perfect Pronunciation! Every word was accurate." : "🎉 完美發音！所有單字皆精準到位、咬字流暢！");
            perfectText.setTextSize(12);
            perfectText.setTextColor(Color.parseColor("#34D399"));
            perfectText.setPadding(0, dp(4), 0, dp(4));
            troubleWordsContainer.addView(perfectText);
            return;
        }

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(en ? "⚠️ Words needing practice (Tap 🔊 to listen):" : "⚠️ 需加強單字清單（點擊 🔊 聽標準發音）：");
        sectionTitle.setTextSize(12);
        sectionTitle.setTextColor(Color.parseColor("#FCA5A5"));
        sectionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sectionTitle.setPadding(0, 0, 0, dp(6));
        troubleWordsContainer.addView(sectionTitle);

        for (final String word : troubleWords) {
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(dp(10), dp(6), dp(10), dp(6));
            GradientDrawable iBg = new GradientDrawable();
            iBg.setColor(Color.parseColor("#312E81"));
            iBg.setCornerRadius(dp(10));
            itemRow.setBackground(iBg);
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            iLp.setMargins(0, 0, 0, dp(6));
            itemRow.setLayoutParams(iLp);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView wordName = new TextView(this);
            wordName.setText(word);
            wordName.setTextSize(14);
            wordName.setTextColor(Color.WHITE);
            wordName.setTypeface(Typeface.DEFAULT_BOLD);
            textCol.addView(wordName);

            String ipa = getIpaForWord(word);
            String tip = getTipForWord(word);
            TextView ipaText = new TextView(this);
            ipaText.setText(ipa + " · " + tip);
            ipaText.setTextSize(11);
            ipaText.setTextColor(Color.parseColor("#A5B4FC"));
            textCol.addView(ipaText);

            itemRow.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button playBtn = new Button(this);
            playBtn.setText("🔊 示範");
            playBtn.setTextSize(11);
            playBtn.setTextColor(Color.WHITE);
            GradientDrawable pBg = new GradientDrawable();
            pBg.setColor(Color.parseColor("#4F46E5"));
            pBg.setCornerRadius(dp(8));
            playBtn.setBackground(pBg);
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    speakWord(word);
                }
            });
            itemRow.addView(playBtn, new LinearLayout.LayoutParams(dp(72), dp(32)));
            troubleWordsContainer.addView(itemRow);
        }
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        tts.setLanguage(Locale.US);
                        ttsReady = true;
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    private void speakWord(String word) {
        if (tts != null && ttsReady) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "pronounce_" + word);
        } else {
            Toast.makeText(this, word, Toast.LENGTH_SHORT).show();
        }
    }

    // ── 📚 Comprehensive English IPA Phonetic Knowledge Base ──
    private static final Map<String, String> IPA_MAP = new HashMap<String, String>();
    private static final Map<String, String> TIP_MAP = new HashMap<String, String>();

    static {
        // Challenging phonetic traps & silent letters
        IPA_MAP.put("subtle", "/ˈsʌt.l/"); TIP_MAP.put("subtle", "b 不發音，重音在第一音節");
        IPA_MAP.put("comfortable", "/ˈkʌm.fər.tə.bəl/"); TIP_MAP.put("comfortable", "重音在 COM-，常縮唸成三音節");
        IPA_MAP.put("thorough", "/ˈθʌr.oʊ/"); TIP_MAP.put("thorough", "th 咬舌音，ough 發 /oʊ/");
        IPA_MAP.put("photographer", "/fəˈtɑː.ɡrə.fər/"); TIP_MAP.put("photographer", "重音在 pho-TO-gra-pher 第二音節");
        IPA_MAP.put("depth", "/depθ/"); TIP_MAP.put("depth", "尾音 th 輕輕咬舌");
        IPA_MAP.put("exhausted", "/ɪɡˈzɔː.stɪd/"); TIP_MAP.put("exhausted", "ex 發 /ɪɡz/ 音，h 不發音");
        IPA_MAP.put("scenic", "/ˈsiː.nɪk/"); TIP_MAP.put("scenic", "c 不發音，發 /ˈsiː-/");
        IPA_MAP.put("mountain", "/ˈmaʊn.tən/"); TIP_MAP.put("mountain", "注意 tain 發 /tən/ 輕音");
        IPA_MAP.put("recipe", "/ˈres.ə.pi/"); TIP_MAP.put("recipe", "三音節，結尾發 /pi/");
        IPA_MAP.put("climb", "/klaɪm/"); TIP_MAP.put("climb", "b 不發音");
        IPA_MAP.put("climbed", "/klaɪmd/"); TIP_MAP.put("climbed", "b 不發音，ed 發 /d/");
        IPA_MAP.put("island", "/ˈaɪ.lənd/"); TIP_MAP.put("island", "s 不發音");
        IPA_MAP.put("iron", "/ˈaɪ.ərn/"); TIP_MAP.put("iron", "r 弱化，唸 /ˈaɪ.ərn/");
        IPA_MAP.put("chaos", "/ˈkeɪ.ɑːs/"); TIP_MAP.put("chaos", "ch 發 /k/ 音");
        IPA_MAP.put("schedule", "/ˈskedʒ.uːl/"); TIP_MAP.put("schedule", "美式唸 /ˈskedʒ.uːl/");
        IPA_MAP.put("colonel", "/ˈkɜː.nəl/"); TIP_MAP.put("colonel", "發音等同 kernel");
        IPA_MAP.put("choir", "/ˈkwaɪ.ər/"); TIP_MAP.put("choir", "ch 發 /kw/ 音");
        IPA_MAP.put("vehicle", "/ˈviː.ə.kəl/"); TIP_MAP.put("vehicle", "h 不發音，重音在第一音節");
        IPA_MAP.put("salmon", "/ˈsæm.ən/"); TIP_MAP.put("salmon", "l 不發音");
        IPA_MAP.put("almond", "/ˈɑː.mənd/"); TIP_MAP.put("almond", "l 不發音");
        IPA_MAP.put("sword", "/sɔːrd/"); TIP_MAP.put("sword", "w 不發音");
        IPA_MAP.put("tomb", "/tuːm/"); TIP_MAP.put("tomb", "b 不發音");
        IPA_MAP.put("womb", "/wuːm/"); TIP_MAP.put("womb", "b 不發音");
        IPA_MAP.put("debt", "/det/"); TIP_MAP.put("debt", "b 不發音");
        IPA_MAP.put("doubt", "/daʊt/"); TIP_MAP.put("doubt", "b 不發音");
        IPA_MAP.put("although", "/ɔːlˈðoʊ/"); TIP_MAP.put("although", "th 發濁音 /ð/，ough 發 /oʊ/");
        IPA_MAP.put("throughout", "/θruːˈaʊt/"); TIP_MAP.put("throughout", "th 咬舌，out 為雙元音");
        IPA_MAP.put("research", "/ˈriː.sɜːtʃ/"); TIP_MAP.put("research", "重音在第一或第二音節");
        IPA_MAP.put("capture", "/ˈkæp.tʃər/"); TIP_MAP.put("capture", "ture 發 /tʃər/ 音");
        IPA_MAP.put("incredible", "/ɪnˈkred.ə.bəl/"); TIP_MAP.put("incredible", "重音在 kred-");
        IPA_MAP.put("clarity", "/ˈklær.ə.ti/"); TIP_MAP.put("clarity", "重音在第一音節");
        IPA_MAP.put("presentation", "/ˌprez.ənˈteɪ.ʃən/"); TIP_MAP.put("presentation", "重音在 -ta-");
        IPA_MAP.put("milestones", "/ˈmaɪl.stoʊnz/"); TIP_MAP.put("milestones", "複合字，重音在 mile-");
        IPA_MAP.put("forecasts", "/ˈfɔːr.kæsts/"); TIP_MAP.put("forecasts", "注意尾音 sts");
        IPA_MAP.put("leveraging", "/ˈlev.ər.ɪ.dʒɪŋ/"); TIP_MAP.put("leveraging", "重音在 lev-");
        IPA_MAP.put("streamlined", "/ˈstriːm.laɪnd/"); TIP_MAP.put("streamlined", "注意 /str/ 連音");
        IPA_MAP.put("productivity", "/ˌproʊ.dʌkˈtɪv.ə.ti/"); TIP_MAP.put("productivity", "重音在 -tiv-");
        IPA_MAP.put("artificial", "/ˌɑːr.t̬əˈfɪʃ.əl/"); TIP_MAP.put("artificial", "ti 發 /ʃ/ 輕音");
        IPA_MAP.put("intelligence", "/ɪnˈtel.ə.dʒəns/"); TIP_MAP.put("intelligence", "重音在 tel-");
        IPA_MAP.put("dogma", "/ˈdɑːɡ.mə/"); TIP_MAP.put("dogma", "重音在 第一音節");
        IPA_MAP.put("hungry", "/ˈhʌŋ.ɡri/"); TIP_MAP.put("hungry", "ng 發 /ŋg/ 音");
        IPA_MAP.put("foolish", "/ˈfuː.lɪʃ/"); TIP_MAP.put("foolish", "oo 發長母音 /uː/");
    }

    private String getIpaForWord(String word) {
        if (word == null || word.isEmpty()) return "//";
        String clean = word.toLowerCase().trim();
        if (IPA_MAP.containsKey(clean)) return IPA_MAP.get(clean);

        // Algorithmic G2P standard IPA generator for any other English words
        StringBuilder ipa = new StringBuilder("/");
        String w = clean;
        w = w.replaceAll("tion$", "ʃən");
        w = w.replaceAll("sion$", "ʒən");
        w = w.replaceAll("ture$", "tʃər");
        w = w.replaceAll("ph", "f");
        w = w.replaceAll("sh", "ʃ");
        w = w.replaceAll("ch", "tʃ");
        w = w.replaceAll("th", "θ");
        w = w.replaceAll("ee", "iː");
        w = w.replaceAll("oo", "uː");
        w = w.replaceAll("ing$", "ɪŋ");
        w = w.replaceAll("ed$", "d");
        ipa.append(w);
        ipa.append("/");
        return ipa.toString();
    }

    private String getTipForWord(String word) {
        if (word == null) return "注意母音與重音位置";
        String clean = word.toLowerCase().trim();
        if (TIP_MAP.containsKey(clean)) return TIP_MAP.get(clean);

        if (clean.endsWith("tion") || clean.endsWith("sion")) return "重音在倒數第二音節";
        if (clean.contains("th")) return "注意 th 舌尖輕觸上齒";
        if (clean.endsWith("ed")) return "注意結尾 /t/ 或 /d/ 輕音";
        if (clean.endsWith("ing")) return "注意尾音 /ŋ/ 鼻音";
        return "注意母音飽滿度與音節重音";
    }

    private void toggleCall() {
        if (client != null && client.isRunning()) {
            stopClient("練習已結束");
            if (isShadowingMode) {
                showPronunciationDiagnosticReport();
            }
        } else {
            startClient();
        }
    }

    private void startClient() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        String apiKey = AppConfig.getGeminiApiKey(this);
        if (apiKey.isEmpty()) {
            showApiKeyDialog();
            return;
        }

        if (isShadowingMode) {
            resetReadingBoard();
        }

        String voice = AppConfig.getVoiceName(this);
        String lang = AppConfig.getTutorLanguage(this);
        String persona = AppConfig.getTutorPersona(this);
        String noiseMode = AppConfig.getNoiseMode(this);
        int noiseSuppression = AppConfig.getNoiseSuppression(this);
        String liveTone = AppConfig.getLiveTone(this);
        int interruptionSensitivity = AppConfig.getInterruptionSensitivity(this);
        String audioOutput = AppConfig.getAudioOutput(this);
        String customPrompt = AppConfig.getCustomSystemPrompt(this);

        updateStatus(CrewTheme.AMBER_400, "正在連線至語音引擎…");
        callButton.setText(isShadowingMode ? "完成朗讀 / 結束" : "掛斷對話");
        ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#E11D48"));

        client = new NativeGeminiLiveClient(this, apiKey, voice, lang, persona, noiseMode,
                noiseSuppression, liveTone, interruptionSensitivity, audioOutput, customPrompt,
                new NativeGeminiLiveClient.Listener() {
                    @Override
                    public void onStatus(final String status) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                updateStatus(CrewTheme.EMERALD_400, status);
                            }
                        });
                    }

                    @Override
                    public void onStopped(final String reason) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                updateStatus(CrewTheme.TEXT_MUTED, "已結束：" + reason);
                                callButton.setText(isShadowingMode ? "🎙️ 開始朗讀練習" : "🎙️ 開始口語對話");
                                ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#2563EB"));
                            }
                        });
                    }

                    @Override
                    public void onTranscript(final String text, final String role) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                if (isShadowingMode && "user".equalsIgnoreCase(role)) {
                                    processSpokenTextForShadowing(text);
                                } else if (!isShadowingMode) {
                                    appendLiveTranscript(text, role);
                                }
                            }
                        });
                    }

                    @Override
                    public void onSubtitleData(final String targetText, final String nativeTranslation, final String keyVocab, final java.util.List<String> suggestedReplies) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                if (!isShadowingMode) {
                                    applyStructuredSubtitleData(targetText, nativeTranslation, keyVocab, suggestedReplies);
                                }
                            }
                        });
                    }

                    @Override
                    public void onMicrophoneLevel(final double dbfs, final double gateDbfs, final boolean sending) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                meterText.setText(sending ? String.format("🎙️ %.0f dB", dbfs) : "🔇 降噪中");
                            }
                        });
                    }

                    @Override
                    public void onSpeakingChanged(final boolean speaking) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                if (speaking) {
                                    if (currentChatTurn != null && "ai".equals(currentChatTurn.role)) {
                                        currentChatTurn.translationRevealed = false;
                                    }
                                    updateStatus(CrewTheme.CYAN_400, isShadowingMode ? "🔊 AI 導師講評示範中…" : "🔊 導師回答中…");
                                    if (isShadowingMode) {
                                        showPronunciationDiagnosticReport();
                                    }
                                } else {
                                    if (currentChatTurn != null && "ai".equals(currentChatTurn.role)) {
                                        currentChatTurn.translationRevealed = true;
                                    }
                                    renderChatCards();
                                    updateStatus(CrewTheme.EMERALD_400, isShadowingMode ? "🎙️ 請大聲朗讀，即時分析中…" : "🎙️ 導師聆聽中，請說話");
                                }
                            }
                        });
                    }
                });

        client.start();
    }

    private void stopClient(String reason) {
        if (client != null) {
            client.stop();
            client = null;
        }
        updateStatus(CrewTheme.TEXT_MUTED, reason);
        callButton.setText(isShadowingMode ? "🎙️ 開始朗讀練習" : "🎙️ 開始口語對話");
        ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#2563EB"));
    }

    private void updateStatus(int dotColor, String text) {
        statusDot.setTextColor(dotColor);
        statusText.setText(text);
    }

    public static class ChatTurn {
        public String role = "ai"; // "user" or "ai"
        public StringBuilder spoken = new StringBuilder();
        public String translation = "";
        public String keyVocab = "";
        public java.util.List<String> hints = new java.util.ArrayList<String>();
        public boolean translationRevealed = false;
    }

    private final java.util.List<ChatTurn> turnHistory = new java.util.ArrayList<ChatTurn>();
    private ChatTurn currentChatTurn = null;
    private LinearLayout chatCardsContainer;

    private void appendLiveTranscript(String text, String role) {
        if (text == null || text.isEmpty()) return;

        if ("translation".equalsIgnoreCase(role)) {
            applyTranslation(text);
            return;
        }

        boolean isAi = "ai".equalsIgnoreCase(role) || "Gemini".equalsIgnoreCase(role);
        String roleKey = isAi ? "ai" : "user";

        if (currentChatTurn == null || !roleKey.equals(currentChatTurn.role)) {
            if (currentChatTurn != null && currentChatTurn.spoken.length() > 0) {
                turnHistory.add(currentChatTurn);
            }
            currentChatTurn = new ChatTurn();
            currentChatTurn.role = roleKey;
            currentChatTurn.translationRevealed = !isAi;
        }

        currentChatTurn.spoken.append(text);
        renderChatCards();
    }

    private void applyStructuredSubtitleData(String targetText, String nativeTrans, String keyVocab, java.util.List<String> hints) {
        if (currentChatTurn == null || !"ai".equals(currentChatTurn.role)) {
            if (currentChatTurn != null && currentChatTurn.spoken.length() > 0) {
                turnHistory.add(currentChatTurn);
            }
            currentChatTurn = new ChatTurn();
            currentChatTurn.role = "ai";
        }
        currentChatTurn.translation = nativeTrans != null ? nativeTrans.trim() : "";
        currentChatTurn.keyVocab = keyVocab != null ? keyVocab.trim() : "";
        currentChatTurn.hints.clear();
        if (hints != null) currentChatTurn.hints.addAll(hints);
        if (client != null && !client.isAiSpeaking()) {
            currentChatTurn.translationRevealed = true;
        }
        renderChatCards();
    }

    private void applyTranslation(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String clean = text.trim();
        if (currentChatTurn != null && "ai".equals(currentChatTurn.role)) {
            if (currentChatTurn.translation.isEmpty()) {
                currentChatTurn.translation = clean;
            }
            if (client != null && !client.isAiSpeaking()) {
                currentChatTurn.translationRevealed = true;
            }
            renderChatCards();
        } else {
            for (int i = turnHistory.size() - 1; i >= 0; i--) {
                ChatTurn t = turnHistory.get(i);
                if ("ai".equals(t.role) && t.translation.isEmpty()) {
                    t.translation = clean;
                    t.translationRevealed = true;
                    renderChatCards();
                    return;
                }
            }
            if (currentChatTurn != null && currentChatTurn.spoken.length() > 0) {
                turnHistory.add(currentChatTurn);
            }
            currentChatTurn = new ChatTurn();
            currentChatTurn.role = "ai";
            currentChatTurn.translation = clean;
            if (client != null && !client.isAiSpeaking()) {
                currentChatTurn.translationRevealed = true;
            }
            renderChatCards();
        }
    }

    private void renderChatCards() {
        if (chatCardsContainer == null) return;
        chatCardsContainer.removeAllViews();
        final boolean en = I18n.isEnglish(this);

        if (turnHistory.isEmpty() && currentChatTurn == null) {
            LinearLayout welcomeCard = new LinearLayout(this);
            welcomeCard.setOrientation(LinearLayout.VERTICAL);
            welcomeCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            GradientDrawable wBg = new GradientDrawable();
            wBg.setColor(Color.parseColor("#1E293B"));
            wBg.setCornerRadius(dp(14));
            wBg.setStroke(dp(1), Color.parseColor("#334155"));
            welcomeCard.setBackground(wBg);

            TextView wTitle = new TextView(this);
            wTitle.setText(en ? "👋 Welcome to Crew Teacher 1-on-1 Oral Tutor!" : "👋 歡迎使用 Crew Teacher 一對一外語教練！");
            wTitle.setTextSize(14);
            wTitle.setTextColor(Color.parseColor("#38BDF8"));
            wTitle.setTypeface(Typeface.DEFAULT_BOLD);
            welcomeCard.addView(wTitle);

            TextView wBody = new TextView(this);
            wBody.setText(en
                    ? "Tap 'Start Practice' below to connect with Gemini Live.\nSpeak naturally and the AI tutor will respond in independent chat cards with translations and smart reply hints!"
                    : "點擊下方「開始口語對話」連線至 Gemini Live 語音引擎。\n直接對著手機說話，AI 導師會以獨立對話卡片即時回應，並在說完後提供母語翻譯與回答小抄！");
            wBody.setTextSize(13);
            wBody.setTextColor(Color.parseColor("#94A3B8"));
            wBody.setLineSpacing(dp(3), 1.2f);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(0, dp(6), 0, 0);
            welcomeCard.addView(wBody, bLp);

            chatCardsContainer.addView(welcomeCard);
            return;
        }

        for (ChatTurn turn : turnHistory) {
            chatCardsContainer.addView(buildTurnCardView(turn, false));
        }

        if (currentChatTurn != null && (currentChatTurn.spoken.length() > 0 || !currentChatTurn.translation.isEmpty())) {
            boolean isSpeaking = client != null && client.isAiSpeaking() && "ai".equals(currentChatTurn.role);
            chatCardsContainer.addView(buildTurnCardView(currentChatTurn, isSpeaking));
        }

        if (transcriptScrollView != null) {
            transcriptScrollView.post(new Runnable() {
                @Override public void run() {
                    if (transcriptScrollView != null) {
                        transcriptScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                }
            });
        }
    }

    private View buildTurnCardView(ChatTurn turn, boolean isLiveSpeaking) {
        boolean isAi = "ai".equals(turn.role);
        final boolean en = I18n.isEnglish(this);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cLp);

        GradientDrawable cBg = new GradientDrawable();
        if (isAi) {
            cBg.setColor(Color.parseColor("#0F172A")); // Slate 900
            cBg.setCornerRadii(new float[]{dp(4), dp(4), dp(16), dp(16), dp(16), dp(16), dp(16), dp(16)});
            cBg.setStroke(dp(1), isLiveSpeaking ? Color.parseColor("#0284C7") : Color.parseColor("#1E293B"));
        } else {
            cBg.setColor(Color.parseColor("#1E293B")); // Slate 800
            cBg.setCornerRadii(new float[]{dp(16), dp(16), dp(4), dp(4), dp(16), dp(16), dp(16), dp(16)});
            cBg.setStroke(dp(1), Color.parseColor("#334155"));
        }
        card.setBackground(cBg);

        // Header Row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView roleLabel = new TextView(this);
        roleLabel.setText(isAi ? (en ? "🤖 AI Tutor" : "🤖 AI 導師") : (en ? "🗣️ You" : "🗣️ 學生"));
        roleLabel.setTextSize(11);
        roleLabel.setTextColor(isAi ? Color.parseColor("#38BDF8") : Color.parseColor("#A5B4FC"));
        roleLabel.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(roleLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (isLiveSpeaking) {
            TextView liveBadge = new TextView(this);
            liveBadge.setText(en ? "🔊 Speaking…" : "🔊 正在回答…");
            liveBadge.setTextSize(10);
            liveBadge.setTextColor(Color.parseColor("#38BDF8"));
            header.addView(liveBadge);
        }
        card.addView(header);

        // Spoken content
        TextView spokenTv = new TextView(this);
        String spokenStr = turn.spoken.toString().trim();
        if (spokenStr.isEmpty() && isAi && isLiveSpeaking) {
            spokenTv.setText("🎙️ ...");
        } else {
            spokenTv.setText(spokenStr);
        }
        spokenTv.setTextSize(isAi ? 15 : 14);
        spokenTv.setTextColor(Color.WHITE);
        spokenTv.setLineSpacing(dp(3), 1.25f);
        spokenTv.setTypeface(Typeface.create(Typeface.DEFAULT, isAi ? Typeface.NORMAL : Typeface.NORMAL));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(4), 0, 0);
        card.addView(spokenTv, sLp);

        // AI Scaffolding Sub-card (Translation + Vocab + Hints)
        if (isAi && turn.translationRevealed && (!turn.translation.isEmpty() || !turn.keyVocab.isEmpty() || !turn.hints.isEmpty())) {
            LinearLayout subCard = new LinearLayout(this);
            subCard.setOrientation(LinearLayout.VERTICAL);
            subCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(Color.parseColor("#111827")); // Gray 900
            sBg.setCornerRadius(dp(10));
            sBg.setStroke(dp(1), Color.parseColor("#1F2937"));
            subCard.setBackground(sBg);
            LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scLp.setMargins(0, dp(10), 0, 0);
            subCard.setLayoutParams(scLp);

            // 1. Translation
            if (!turn.translation.isEmpty()) {
                TextView tTitle = new TextView(this);
                tTitle.setText(en ? "📖 Translation" : "📖 母語翻譯");
                tTitle.setTextSize(11);
                tTitle.setTextColor(Color.parseColor("#60A5FA"));
                tTitle.setTypeface(Typeface.DEFAULT_BOLD);
                subCard.addView(tTitle);

                TextView tText = new TextView(this);
                tText.setText(turn.translation);
                tText.setTextSize(13);
                tText.setTextColor(Color.parseColor("#E2E8F0"));
                tText.setLineSpacing(dp(2), 1.2f);
                LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ttLp.setMargins(0, dp(2), 0, 0);
                subCard.addView(tText, ttLp);
            }

            // 2. Key Vocab
            if (!turn.keyVocab.isEmpty()) {
                TextView vTitle = new TextView(this);
                vTitle.setText(en ? "💡 Key Vocabulary & Notes" : "💡 重點單字與句型");
                vTitle.setTextSize(11);
                vTitle.setTextColor(Color.parseColor("#FBBF24"));
                vTitle.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams vtLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vtLp.setMargins(0, dp(8), 0, 0);
                subCard.addView(vTitle, vtLp);

                TextView vText = new TextView(this);
                vText.setText(turn.keyVocab);
                vText.setTextSize(12);
                vText.setTextColor(Color.parseColor("#FEF08A"));
                vText.setLineSpacing(dp(2), 1.2f);
                subCard.addView(vText);
            }

            // 3. Hints
            if (!turn.hints.isEmpty()) {
                TextView hTitle = new TextView(this);
                hTitle.setText(en ? "💬 Smart Reply Hints (Tap 🔊 to listen):" : "💬 建議回答小抄（點擊 🔊 聽示範）：");
                hTitle.setTextSize(11);
                hTitle.setTextColor(Color.parseColor("#34D399"));
                hTitle.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                htLp.setMargins(0, dp(8), 0, dp(4));
                subCard.addView(hTitle, htLp);

                for (final String hint : turn.hints) {
                    LinearLayout pill = new LinearLayout(this);
                    pill.setOrientation(LinearLayout.HORIZONTAL);
                    pill.setGravity(Gravity.CENTER_VERTICAL);
                    pill.setPadding(dp(10), dp(6), dp(10), dp(6));
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#1E293B"));
                    pBg.setCornerRadius(dp(8));
                    pBg.setStroke(dp(1), Color.parseColor("#334155"));
                    pill.setBackground(pBg);
                    LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    pLp.setMargins(0, 0, 0, dp(6));
                    pill.setLayoutParams(pLp);

                    TextView pText = new TextView(this);
                    pText.setText("• " + hint);
                    pText.setTextSize(12);
                    pText.setTextColor(Color.parseColor("#E0E7FF"));
                    pill.addView(pText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    Button pBtn = new Button(this);
                    pBtn.setText("🔊");
                    pBtn.setTextSize(11);
                    pBtn.setTextColor(Color.WHITE);
                    GradientDrawable bBg = new GradientDrawable();
                    bBg.setColor(Color.parseColor("#4F46E5"));
                    bBg.setCornerRadius(dp(6));
                    pBtn.setBackground(bBg);
                    pBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            String speakPart = hint.contains("(") ? hint.substring(0, hint.indexOf("(")).trim() : hint;
                            speakWord(speakPart);
                        }
                    });
                    pill.addView(pBtn, new LinearLayout.LayoutParams(dp(44), dp(28)));
                    subCard.addView(pill);
                }
            }

            card.addView(subCard);
        }

        return card;
    }

    private void showApiKeyDialog() {
        final boolean en = I18n.isEnglish(this);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(en ? "🔑 Enter Gemini API Key" : "🔑 請輸入 Gemini API Key");
        builder.setMessage(en ? "Gemini Live voice tutor requires a Gemini API Key to connect." : "連線 Gemini Live 口說導師需要填入您的 Gemini API Key 才能開始。");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("AIzaSy...");
        input.setText(AppConfig.getGeminiApiKey(this));
        input.setTextColor(Color.WHITE);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        builder.setView(input);

        builder.setPositiveButton(en ? "Save & Start" : "儲存並連線", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                AppConfig.setGeminiApiKey(NativeLiveActivity.this, key);
                if (!key.isEmpty()) {
                    startClient();
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private void showCustomReadingDialog() {
        final boolean en = I18n.isEnglish(this);
        String currentLang = AppConfig.getTutorLanguage(this);
        String langLabel = MainActivity.getLanguageLabel(currentLang);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(en ? ("✏️ Custom Reading Text (" + langLabel + ")") : ("✏️ 自訂「" + langLabel + "」朗讀文章"));
        builder.setMessage(en ? ("Paste or enter the " + langLabel + " passage you want to practice:") : ("請輸入或貼上您想練習朗讀的「" + langLabel + "」短文："));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        final EditText input = new EditText(this);
        input.setText(fullReadingText);
        input.setHint(en ? ("Paste " + langLabel + " text here...") : ("在此貼上「" + langLabel + "」文章…"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(13);
        input.setMinLines(4);
        input.setGravity(Gravity.TOP);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable iBg = new GradientDrawable();
        iBg.setColor(Color.parseColor("#1E293B"));
        iBg.setCornerRadius(dp(10));
        input.setBackground(iBg);
        layout.addView(input);

        Button pasteBtn = new Button(this);
        pasteBtn.setText(en ? "📋 Paste from Clipboard" : "📋 從剪貼簿貼上");
        pasteBtn.setTextSize(11);
        pasteBtn.setTextColor(Color.WHITE);
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(Color.parseColor("#334155"));
        pBg.setCornerRadius(dp(8));
        pasteBtn.setBackground(pBg);
        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                        CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
                        if (clip != null) {
                            input.setText(clip.toString().trim());
                            Toast.makeText(NativeLiveActivity.this, en ? "Pasted" : "已貼上", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        pLp.setMargins(0, dp(6), 0, 0);
        layout.addView(pasteBtn, pLp);

        builder.setView(layout);
        builder.setPositiveButton(en ? "Save & Apply" : "套用文章", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                String custom = input.getText().toString().trim();
                if (!custom.isEmpty()) {
                    AppConfig.setReadingText(NativeLiveActivity.this, custom);
                    fullReadingText = custom;
                    setupReadingData();
                    renderReadingBoardSpannable();
                    resetReadingBoard();
                    Toast.makeText(NativeLiveActivity.this, en ? "Custom reading text applied!" : "已套用自訂朗讀文章！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private void triggerAiNewPassage() {
        final boolean en = I18n.isEnglish(this);
        String currentLang = AppConfig.getTutorLanguage(this);
        String langLabel = MainActivity.getLanguageLabel(currentLang);
        Toast.makeText(this, en ? "🤖 AI is generating new " + langLabel + " reading passage..." : "🤖 AI 外師正在生成新的「" + langLabel + "」朗讀文章…", Toast.LENGTH_SHORT).show();
        String persona = AppConfig.getTutorPersona(this);

        ReadingMaterialGenerator.generateAsync(this, persona, "intermediate", new ReadingMaterialGenerator.GenerateCallback() {
            @Override public void onSuccess(String text) {
                AppConfig.setReadingText(NativeLiveActivity.this, text);
                fullReadingText = text;
                setupReadingData();
                renderReadingBoardSpannable();
                resetReadingBoard();
                Toast.makeText(NativeLiveActivity.this, en ? "✨ New passage generated! Start reading whenever ready." : "✨ 新朗讀教材生成完畢！請直接開口朗讀。", Toast.LENGTH_SHORT).show();
            }

            @Override public void onError(String error) {
                Toast.makeText(NativeLiveActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopClient("離開頁面");
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {}
            tts = null;
        }
        super.onDestroy();
    }
}

