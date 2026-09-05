package com.crewpocket.teacher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private long sessionStartTime = 0;
    private boolean sessionEvaluated = false;
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

    // ── 🎯 Option: Structured Lesson Mode ──
    private boolean isLessonMode = false;
    private String lessonId = null;
    private CourseModel.Lesson currentLesson = null;
    private LinearLayout missionHudCard;
    private TextView missionHudTitle;
    private final List<TextView> missionCheckBoxes = new ArrayList<TextView>();

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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_LESSON_ID")) {
            lessonId = intent.getStringExtra("EXTRA_LESSON_ID");
            currentLesson = CourseManager.getLessonById(lessonId);
            if (currentLesson != null) {
                isLessonMode = true;
                for (CourseModel.Mission m : currentLesson.missions) {
                    m.achieved = false;
                }
            }
        }

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
        if (isLessonMode && currentLesson != null) {
            title.setText("🎯 " + currentLesson.getTitle(en));
        } else {
            title.setText(isShadowingMode
                    ? (en ? "📖 Reading & Pronunciation Coach" : "📖 朗讀高亮與發音診斷")
                    : (en ? "🎓 Oral Practice Classroom" : "🎓 口語即時對話教室"));
        }
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
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

        // 2.5 Structured Lesson Mission HUD
        if (isLessonMode && currentLesson != null) {
            setupMissionHud(root, en);
        }

        // 3. Main Area: If Shadowing mode -> Interactive Reading Board + Diagnostic Card
        if (isShadowingMode) {
            setupReadingData();

            ScrollView readingScroll = new ScrollView(this);
            readingScroll.setVerticalScrollBarEnabled(false);
            readingScroll.setHorizontalScrollBarEnabled(false);
            readingScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
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
            transcriptScrollView.setVerticalScrollBarEnabled(false);
            transcriptScrollView.setHorizontalScrollBarEnabled(false);
            transcriptScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
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

        // 3.5 【情境一鍵話題破冰泡泡】Icebreaker Topic Chips Row
        if (!isShadowingMode) {
            HorizontalScrollView iceScroll = new HorizontalScrollView(this);
            iceScroll.setHorizontalScrollBarEnabled(false);
            iceScroll.setVerticalScrollBarEnabled(false);
            iceScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            iLp.setMargins(0, 0, 0, dp(10));
            iceScroll.setLayoutParams(iLp);

            LinearLayout iceRow = new LinearLayout(this);
            iceRow.setOrientation(LinearLayout.HORIZONTAL);
            iceRow.setGravity(Gravity.CENTER_VERTICAL);

            String tutorLang = AppConfig.getTutorLanguage(this);
            String persona = AppConfig.getTutorPersona(this);
            List<IcebreakerManager.Icebreaker> icebreakers = IcebreakerManager.getIcebreakersForScenario(persona, tutorLang);

            for (final IcebreakerManager.Icebreaker ib : icebreakers) {
                LinearLayout chip = new LinearLayout(this);
                chip.setOrientation(LinearLayout.HORIZONTAL);
                chip.setGravity(Gravity.CENTER_VERTICAL);
                chip.setPadding(dp(12), dp(7), dp(12), dp(7));
                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(Color.parseColor("#1E293B"));
                cBg.setCornerRadius(dp(16));
                cBg.setStroke(dp(1), Color.parseColor("#334155"));
                chip.setBackground(cBg);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cLp.setMargins(0, 0, dp(8), 0);
                chip.setLayoutParams(cLp);

                TextView cText = new TextView(this);
                cText.setText(ib.emoji + " " + ib.targetPhrase);
                cText.setTextSize(11);
                cText.setTextColor(Color.parseColor("#E0E7FF"));
                chip.addView(cText);

                chip.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        speakWord(ib.targetPhrase);
                        Toast.makeText(NativeLiveActivity.this, "💡 " + ib.nativeHint + "\n(" + (en ? "Read aloud into mic to start!" : "可直接對麥克風朗讀開始！") + ")", Toast.LENGTH_SHORT).show();
                    }
                });
                iceRow.addView(chip);
            }
            iceScroll.addView(iceRow);
            root.addView(iceScroll);
        }

        // 3.8 In-Call Quick Scaffolding Bar (💡 提詞靈感 | 🐢 慢速重聽 | 🔄 換句簡單的)
        if (!isShadowingMode) {
            LinearLayout scaffoldBar = new LinearLayout(this);
            scaffoldBar.setOrientation(LinearLayout.HORIZONTAL);
            scaffoldBar.setGravity(Gravity.CENTER_VERTICAL);
            scaffoldBar.setPadding(0, dp(4), 0, dp(8));

            Button hintBtn = new Button(this);
            hintBtn.setText(en ? "💡 Hints" : "💡 提詞靈感");
            hintBtn.setTextSize(11);
            hintBtn.setTextColor(Color.WHITE);
            GradientDrawable hBg = new GradientDrawable();
            hBg.setColor(Color.parseColor("#1E293B"));
            hBg.setCornerRadius(dp(10));
            hBg.setStroke(dp(1), Color.parseColor("#38BDF8"));
            hintBtn.setBackground(hBg);
            hintBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    OralCoachHelper.showHintsBottomSheet(NativeLiveActivity.this, currentLesson, AppConfig.getTutorPersona(NativeLiveActivity.this));
                }
            });
            scaffoldBar.addView(hintBtn, new LinearLayout.LayoutParams(0, dp(36), 1f));

            Button slowBtn = new Button(this);
            slowBtn.setText(en ? "🐢 Replay (0.7x)" : "🐢 慢速重聽");
            slowBtn.setTextSize(11);
            slowBtn.setTextColor(Color.WHITE);
            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(Color.parseColor("#1E293B"));
            sBg.setCornerRadius(dp(10));
            sBg.setStroke(dp(1), Color.parseColor("#A855F7"));
            slowBtn.setBackground(sBg);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            slp.setMargins(dp(6), 0, dp(6), 0);
            slowBtn.setLayoutParams(slp);
            slowBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String lastAi = "";
                    for (int i = turnHistory.size() - 1; i >= 0; i--) {
                        if ("ai".equals(turnHistory.get(i).role) && turnHistory.get(i).spoken.length() > 0) {
                            lastAi = turnHistory.get(i).spoken.toString();
                            break;
                        }
                    }
                    if (lastAi.isEmpty() && currentChatTurn != null && "ai".equals(currentChatTurn.role)) {
                        lastAi = currentChatTurn.spoken.toString();
                    }
                    if (!lastAi.isEmpty()) {
                        Toast.makeText(NativeLiveActivity.this, en ? "🐢 Replaying last sentence at 0.7x..." : "🐢 正在以 0.7x 慢速重播導師剛才的發言…", Toast.LENGTH_SHORT).show();
                        playTtsWithMicSuppression(lastAi, 0.7f);
                    } else {
                        Toast.makeText(NativeLiveActivity.this, en ? "No tutor speech to replay yet" : "尚無導師發音記錄可重播", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            scaffoldBar.addView(slowBtn);

            Button simplerBtn = new Button(this);
            simplerBtn.setText(en ? "🔄 Pardon?" : "🔄 換句簡單的");
            simplerBtn.setTextSize(11);
            simplerBtn.setTextColor(Color.WHITE);
            GradientDrawable simBg = new GradientDrawable();
            simBg.setColor(Color.parseColor("#1E293B"));
            simBg.setCornerRadius(dp(10));
            simBg.setStroke(dp(1), Color.parseColor("#10B981"));
            simplerBtn.setBackground(simBg);
            simplerBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Toast.makeText(NativeLiveActivity.this, en ? "Tip: Say 'Could you please explain that in simpler words?' to your tutor!" : "開口小秘訣：可直接對導師說「Could you please explain that in simpler words?」", Toast.LENGTH_LONG).show();
                    playTtsWithMicSuppression("Could you please explain that in simpler words?", 1.0f);
                }
            });
            scaffoldBar.addView(simplerBtn, new LinearLayout.LayoutParams(0, dp(36), 1f));

            root.addView(scaffoldBar);
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

    private void setupMissionHud(LinearLayout root, final boolean en) {
        missionHudCard = new LinearLayout(this);
        missionHudCard.setOrientation(LinearLayout.VERTICAL);
        missionHudCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable mbg = new GradientDrawable();
        mbg.setColor(Color.parseColor("#0F172A"));
        mbg.setCornerRadius(dp(14));
        mbg.setStroke(dp(1), Color.parseColor("#3B82F6"));
        missionHudCard.setBackground(mbg);

        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.setMargins(0, 0, 0, dp(10));
        missionHudCard.setLayoutParams(mlp);

        // Header Row
        LinearLayout mHead = new LinearLayout(this);
        mHead.setOrientation(LinearLayout.HORIZONTAL);
        mHead.setGravity(Gravity.CENTER_VERTICAL);

        missionHudTitle = new TextView(this);
        missionHudTitle.setText("🎯 " + (en ? "Mission Goals (0/" : "挑戰目標 (0/") + currentLesson.missions.size() + (en ? " done)" : " 完成)"));
        missionHudTitle.setTextSize(12);
        missionHudTitle.setTextColor(Color.parseColor("#38BDF8"));
        missionHudTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mHead.addView(missionHudTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView toggleBtn = new TextView(this);
        toggleBtn.setText("▾");
        toggleBtn.setTextSize(14);
        toggleBtn.setTextColor(Color.parseColor("#94A3B8"));
        mHead.addView(toggleBtn);
        missionHudCard.addView(mHead);

        final LinearLayout itemsCol = new LinearLayout(this);
        itemsCol.setOrientation(LinearLayout.VERTICAL);
        itemsCol.setPadding(0, dp(6), 0, 0);

        missionCheckBoxes.clear();
        for (int i = 0; i < currentLesson.missions.size(); i++) {
            CourseModel.Mission m = currentLesson.missions.get(i);
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(0, dp(2), 0, dp(2));

            TextView checkTv = new TextView(this);
            checkTv.setText("[ ] ");
            checkTv.setTextSize(12);
            checkTv.setTextColor(Color.parseColor("#94A3B8"));
            checkTv.setTypeface(Typeface.MONOSPACE);
            missionCheckBoxes.add(checkTv);
            itemRow.addView(checkTv);

            TextView descTv = new TextView(this);
            descTv.setText(m.getDesc(en));
            descTv.setTextSize(11);
            descTv.setTextColor(Color.parseColor("#E2E8F0"));
            itemRow.addView(descTv);

            itemsCol.addView(itemRow);
        }
        missionHudCard.addView(itemsCol);

        mHead.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean isVis = itemsCol.getVisibility() == View.VISIBLE;
                itemsCol.setVisibility(isVis ? View.GONE : View.VISIBLE);
                toggleBtn.setText(isVis ? "▸" : "▾");
            }
        });

        root.addView(missionHudCard);
    }

    private void checkMissionsProgress(String userText) {
        if (!isLessonMode || currentLesson == null || userText == null) return;
        final boolean en = I18n.isEnglish(this);
        String lower = userText.toLowerCase(java.util.Locale.US);
        int doneCount = 0;
        boolean newlyAchieved = false;

        for (int i = 0; i < currentLesson.missions.size(); i++) {
            CourseModel.Mission m = currentLesson.missions.get(i);
            if (!m.achieved && m.targetKeywords != null) {
                for (String kw : m.targetKeywords) {
                    if (lower.contains(kw.toLowerCase(java.util.Locale.US))) {
                        m.achieved = true;
                        newlyAchieved = true;
                        break;
                    }
                }
            }
            if (m.achieved) {
                doneCount++;
                if (i < missionCheckBoxes.size()) {
                    TextView cb = missionCheckBoxes.get(i);
                    cb.setText("[✓] ");
                    cb.setTextColor(Color.parseColor("#34D399"));
                }
            }
        }

        if (missionHudTitle != null) {
            missionHudTitle.setText("🎯 " + (en ? "Mission Goals (" : "挑戰目標 (") + doneCount + "/" + currentLesson.missions.size() + (en ? " done)" : " 完成)"));
            if (doneCount == currentLesson.missions.size()) {
                missionHudTitle.setTextColor(Color.parseColor("#34D399"));
            }
        }

        if (newlyAchieved) {
            Toast.makeText(this, en ? "🎯 Mission Goal Achieved!" : "🎯 達成一項挑戰目標！繼續加油", Toast.LENGTH_SHORT).show();
        }
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

    private final Runnable unmuteMicRunnable = new Runnable() {
        @Override public void run() {
            if (client != null) {
                client.setMicMuted(false);
            }
        }
    };

    private void playTtsWithMicSuppression(final String text, final float rate) {
        if (text == null || text.trim().isEmpty()) return;
        if (client != null) {
            client.setMicMuted(true);
        }
        int wordCount = Math.max(1, text.split("\\s+").length);
        long estimatedDurationMs = (long) ((wordCount * 450L) / Math.max(0.5f, rate)) + 800L;

        if (tts != null && ttsReady) {
            tts.setSpeechRate(rate);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_suppressed_" + System.currentTimeMillis());
        } else {
            OralCoachHelper.speak(this, text, rate);
        }

        handler.removeCallbacks(unmuteMicRunnable);
        handler.postDelayed(unmuteMicRunnable, estimatedDurationMs);
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        tts.setLanguage(Locale.US);
                        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                            @Override public void onStart(String utteranceId) {
                                if (client != null) client.setMicMuted(true);
                            }
                            @Override public void onDone(String utteranceId) {
                                handler.postDelayed(new Runnable() {
                                    @Override public void run() {
                                        if (client != null) client.setMicMuted(false);
                                    }
                                }, 350);
                            }
                            @Override public void onError(String utteranceId) {
                                if (client != null) client.setMicMuted(false);
                            }
                        });
                        ttsReady = true;
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    private void speakWord(String word) {
        if (word == null || word.trim().isEmpty()) return;
        playTtsWithMicSuppression(word, 1.0f);
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
        String persona = isLessonMode && currentLesson != null && currentLesson.scenario != null
                ? currentLesson.scenario : AppConfig.getTutorPersona(this);
        String noiseMode = AppConfig.getNoiseMode(this);
        int noiseSuppression = AppConfig.getNoiseSuppression(this);
        String liveTone = AppConfig.getLiveTone(this);
        int interruptionSensitivity = AppConfig.getInterruptionSensitivity(this);
        String audioOutput = AppConfig.getAudioOutput(this);
        String customPrompt = AppConfig.getCustomSystemPrompt(this);

        if (isLessonMode && currentLesson != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n[STRUCTURED ORAL LESSON ACTIVE]\n");
            sb.append("Lesson Title: ").append(currentLesson.titleEn).append("\n");
            sb.append("Role Context: ").append(currentLesson.promptInstruction).append("\n");
            sb.append("Student Missions to achieve:\n");
            for (int i = 0; i < currentLesson.missions.size(); i++) {
                sb.append(i + 1).append(". ").append(currentLesson.missions.get(i).descEn).append("\n");
            }
            sb.append("Please speak in natural spoken English suitable for this role. Guide the student step by step to complete all missions.");
            customPrompt = (customPrompt != null ? customPrompt : "") + sb.toString();
        }

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

        sessionStartTime = System.currentTimeMillis();
        sessionEvaluated = false;
        client.start();
    }

    private void stopClient(String reason) {
        int durationSec = sessionStartTime > 0 ? (int) ((System.currentTimeMillis() - sessionStartTime) / 1000) : 0;
        sessionStartTime = 0;
        if (client != null) {
            client.stop();
            client = null;
        }
        updateStatus(CrewTheme.TEXT_MUTED, reason);
        callButton.setText(isShadowingMode ? "🎙️ 開始朗讀練習" : "🎙️ 開始口語對話");
        ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#2563EB"));

        if (!isShadowingMode && !sessionEvaluated) {
            sessionEvaluated = true;
            final List<ChatTurn> turnsToEvaluate = new ArrayList<ChatTurn>(turnHistory);
            if (currentChatTurn != null && currentChatTurn.spoken.length() > 0 && !turnsToEvaluate.contains(currentChatTurn)) {
                turnsToEvaluate.add(currentChatTurn);
            }
            if (!turnsToEvaluate.isEmpty()) {
                SessionReportGenerator.generateReportAsync(this, turnsToEvaluate, durationSec, new SessionReportGenerator.ReportCallback() {
                    @Override
                    public void onReportReady(final LearningDataManager.SessionRecord record) {
                        if (!isFinishing()) {
                            showSessionReportDialog(record);
                        }
                    }
                });
            }
        }
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
        if (!isAi) {
            checkMissionsProgress(text);
        }
        renderChatCards();
    }

    private void applyStructuredSubtitleData(String targetText, String nativeTrans, String keyVocab, java.util.List<String> hints) {
        String cleanTrans = nativeTrans != null ? nativeTrans.trim() : "";
        String cleanVocab = keyVocab != null ? keyVocab.trim() : "";

        if (currentChatTurn != null && "ai".equals(currentChatTurn.role)) {
            currentChatTurn.translation = cleanTrans;
            currentChatTurn.keyVocab = cleanVocab;
            currentChatTurn.hints.clear();
            if (hints != null) currentChatTurn.hints.addAll(hints);
            currentChatTurn.translationRevealed = true;
            renderChatCards();
            return;
        }

        for (int i = turnHistory.size() - 1; i >= 0; i--) {
            ChatTurn t = turnHistory.get(i);
            if ("ai".equals(t.role)) {
                t.translation = cleanTrans;
                t.keyVocab = cleanVocab;
                t.hints.clear();
                if (hints != null) t.hints.addAll(hints);
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
        if (targetText != null && !targetText.isEmpty()) currentChatTurn.spoken.append(targetText);
        currentChatTurn.translation = cleanTrans;
        currentChatTurn.keyVocab = cleanVocab;
        currentChatTurn.hints.clear();
        if (hints != null) currentChatTurn.hints.addAll(hints);
        currentChatTurn.translationRevealed = true;
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
        } else if (isAi && turn.spoken.length() > 0) {
            final String aiSpokenText = turn.spoken.toString().trim();
            final String aiTransText = turn.translation != null ? turn.translation.trim() : "";
            final boolean isStarred = LearningDataManager.isStarred(this, aiSpokenText);
            final Button starBtn = new Button(this);
            starBtn.setText(isStarred ? "★" : "☆");
            starBtn.setTextSize(14);
            starBtn.setTextColor(isStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
            starBtn.setBackground(null);
            starBtn.setPadding(0, 0, 0, 0);
            starBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean nowStarred = LearningDataManager.toggleStarItem(NativeLiveActivity.this, aiSpokenText, aiTransText, "phrase", "");
                    starBtn.setText(nowStarred ? "★" : "☆");
                    starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    Toast.makeText(NativeLiveActivity.this, nowStarred ? (en ? "⭐ Saved to Phrasebook" : "⭐ 已收藏至個人生詞金句本") : (en ? "Removed from Phrasebook" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                }
            });
            header.addView(starBtn, new LinearLayout.LayoutParams(dp(36), dp(28)));
        }
        card.addView(header);

        // Spoken content
        TextView spokenTv = new TextView(this);
        String spokenStr = turn.spoken.toString().trim();
        if (!isAi && !spokenStr.isEmpty()) {
            // Interactive Word-by-Word Phonetic IPA Spans for Student's speech
            SpannableStringBuilder userSsb = new SpannableStringBuilder();
            String[] uWords = spokenStr.split("\\s+");
            for (int w = 0; w < uWords.length; w++) {
                final String rawW = uWords[w];
                final String cleanW = rawW.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                int start = userSsb.length();
                userSsb.append(rawW);
                int end = userSsb.length();

                final boolean hasPhoneticTip = IPA_MAP.containsKey(cleanW);
                if (hasPhoneticTip) {
                    userSsb.setSpan(new ForegroundColorSpan(Color.parseColor("#38BDF8")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    userSsb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                userSsb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        speakWord(cleanW.isEmpty() ? rawW : cleanW);
                        String ipa = getIpaForWord(cleanW.isEmpty() ? rawW : cleanW);
                        String tip = getTipForWord(cleanW.isEmpty() ? rawW : cleanW);
                        Toast.makeText(NativeLiveActivity.this, "🗣️ " + rawW + " " + ipa + "\n💡 " + tip, Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setUnderlineText(hasPhoneticTip);
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (w < uWords.length - 1) userSsb.append(" ");
            }
            spokenTv.setText(userSsb);
            spokenTv.setMovementMethod(LinkMovementMethod.getInstance());
        } else if (spokenStr.isEmpty() && isAi && isLiveSpeaking) {
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

        if (!isAi && !spokenStr.isEmpty()) {
            TextView tapHint = new TextView(this);
            tapHint.setText(en ? "💡 Tap any word to listen to standard IPA pronunciation" : "💡 點擊上方任一單字，可聽母語發音與音標小抄");
            tapHint.setTextSize(10);
            tapHint.setTextColor(Color.parseColor("#94A3B8"));
            tapHint.setPadding(0, dp(3), 0, 0);
            card.addView(tapHint);
        }

        // AI Scaffolding Sub-card (Translation + Vocab + Hints)
        if (isAi && (!turn.translation.isEmpty() || !turn.keyVocab.isEmpty() || !turn.hints.isEmpty())) {
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

            // 2. Key Vocab & Pronunciation Notes
            if (!turn.keyVocab.isEmpty()) {
                LinearLayout vHeadRow = new LinearLayout(this);
                vHeadRow.setOrientation(LinearLayout.HORIZONTAL);
                vHeadRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams vhLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vhLp.setMargins(0, dp(8), 0, 0);
                vHeadRow.setLayoutParams(vhLp);

                TextView vTitle = new TextView(this);
                vTitle.setText(en ? "💡 Pronunciation & Vocab Focus" : "💡 發音與重點單字解析");
                vTitle.setTextSize(11);
                vTitle.setTextColor(Color.parseColor("#FBBF24"));
                vTitle.setTypeface(Typeface.DEFAULT_BOLD);
                vHeadRow.addView(vTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                final String vocabText = turn.keyVocab;
                Button vPlayBtn = new Button(this);
                vPlayBtn.setText("🔊");
                vPlayBtn.setTextSize(10);
                vPlayBtn.setTextColor(Color.WHITE);
                GradientDrawable vpBg = new GradientDrawable();
                vpBg.setColor(Color.parseColor("#4F46E5"));
                vpBg.setCornerRadius(dp(6));
                vPlayBtn.setBackground(vpBg);
                vPlayBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String firstWord = vocabText.contains("(") ? vocabText.substring(0, vocabText.indexOf("(")).trim() : (vocabText.contains("-") ? vocabText.substring(0, vocabText.indexOf("-")).trim() : vocabText);
                        speakWord(firstWord);
                    }
                });
                vHeadRow.addView(vPlayBtn, new LinearLayout.LayoutParams(dp(40), dp(26)));

                final boolean isVocabStarred = LearningDataManager.isStarred(this, vocabText);
                final Button vStarBtn = new Button(this);
                vStarBtn.setText(isVocabStarred ? "★" : "☆");
                vStarBtn.setTextSize(12);
                vStarBtn.setTextColor(isVocabStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                vStarBtn.setBackground(null);
                vStarBtn.setPadding(0, 0, 0, 0);
                vStarBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean nowStarred = LearningDataManager.toggleStarItem(NativeLiveActivity.this, vocabText, "", "vocab", "");
                        vStarBtn.setText(nowStarred ? "★" : "☆");
                        vStarBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                        Toast.makeText(NativeLiveActivity.this, nowStarred ? (en ? "⭐ Saved to Phrasebook" : "⭐ 已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                    }
                });
                vHeadRow.addView(vStarBtn, new LinearLayout.LayoutParams(dp(34), dp(26)));
                subCard.addView(vHeadRow);

                TextView vText = new TextView(this);
                vText.setText(turn.keyVocab);
                vText.setTextSize(12);
                vText.setTextColor(Color.parseColor("#FEF08A"));
                vText.setLineSpacing(dp(2), 1.2f);
                vText.setPadding(0, dp(2), 0, 0);
                subCard.addView(vText);
            }

            // 3. Hints
            if (!turn.hints.isEmpty()) {
                TextView hTitle = new TextView(this);
                hTitle.setText(en ? "💬 Smart Reply Hints (Tap 🔊 to listen, ⭐ to save):" : "💬 建議回答小抄（點擊 🔊 聽示範，⭐ 收藏）：");
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

                    final String speakPart = hint.contains("(") ? hint.substring(0, hint.indexOf("(")).trim() : hint;
                    final String transPart = hint.contains("(") && hint.contains(")") ? hint.substring(hint.indexOf("(") + 1, hint.indexOf(")")).trim() : "";

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
                            speakWord(speakPart);
                        }
                    });
                    pill.addView(pBtn, new LinearLayout.LayoutParams(dp(44), dp(28)));

                    final boolean isHintStarred = LearningDataManager.isStarred(NativeLiveActivity.this, speakPart);
                    final Button starHintBtn = new Button(this);
                    starHintBtn.setText(isHintStarred ? "★" : "☆");
                    starHintBtn.setTextSize(13);
                    starHintBtn.setTextColor(isHintStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starHintBtn.setBackground(null);
                    starHintBtn.setPadding(0, 0, 0, 0);
                    starHintBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(NativeLiveActivity.this, speakPart, transPart, "hint", "");
                            starHintBtn.setText(nowStarred ? "★" : "☆");
                            starHintBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(NativeLiveActivity.this, nowStarred ? (en ? "⭐ Saved to Phrasebook" : "⭐ 小抄句已收藏至生詞本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    pill.addView(starHintBtn, new LinearLayout.LayoutParams(dp(36), dp(28)));

                    subCard.addView(pill);
                }
            }

            card.addView(subCard);
        }

        return card;
    }

    public void showSessionReportDialog(final LearningDataManager.SessionRecord record) {
        if (record == null || isFinishing()) return;
        final boolean en = I18n.isEnglish(this);

        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#D0000000")));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(16), dp(24), dp(16), dp(24));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(20));
        cBg.setStroke(dp(1), Color.parseColor("#334155"));
        container.setBackground(cBg);

        final List<LearningDataManager.StarredItem> allReportItems = new ArrayList<LearningDataManager.StarredItem>();
        final List<Button> allStarButtons = new ArrayList<Button>();

        // 1. Header Bar: Title & Close Button
        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(en ? "📊 AI Learning Diagnostic Report" : "📊 課後 AI 學習成效診斷報告");
        title.setTextSize(16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(10), dp(4), dp(4), dp(4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        headRow.addView(closeBtn);
        container.addView(headRow);

        // 1.5 Structured Lesson Achievement Banner
        if (isLessonMode && currentLesson != null) {
            int achievedCount = 0;
            for (CourseModel.Mission m : currentLesson.missions) {
                if (m.achieved) achievedCount++;
            }
            int stars = 0;
            if (achievedCount == currentLesson.missions.size() && record.overallScore >= 70) {
                stars = 3;
            } else if (achievedCount >= 2 || record.overallScore >= 65) {
                stars = 2;
            } else if (achievedCount >= 1 || record.durationSeconds >= 45) {
                stars = 1;
            }
            CourseManager.saveLessonProgress(this, currentLesson.id, stars, record.overallScore);
            CourseModel.LessonProgress prog = CourseManager.getLessonProgress(this, currentLesson.id);

            LinearLayout lessonSuccessCard = new LinearLayout(this);
            lessonSuccessCard.setOrientation(LinearLayout.VERTICAL);
            lessonSuccessCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable lbg = new GradientDrawable();
            lbg.setColor(Color.parseColor("#064E3B"));
            lbg.setCornerRadius(dp(14));
            lbg.setStroke(dp(1), Color.parseColor("#059669"));
            lessonSuccessCard.setBackground(lbg);

            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.setMargins(0, dp(10), 0, 0);
            lessonSuccessCard.setLayoutParams(llp);

            TextView lTitle = new TextView(this);
            StringBuilder sb = new StringBuilder();
            for (int s = 0; s < 3; s++) sb.append(s < prog.stars ? "⭐" : "☆");
            lTitle.setText("🎉 " + (en ? "Lesson Result: " : "關卡通關評定：") + sb.toString());
            lTitle.setTextSize(14);
            lTitle.setTextColor(Color.WHITE);
            lTitle.setTypeface(Typeface.DEFAULT_BOLD);
            lessonSuccessCard.addView(lTitle);

            TextView lSub = new TextView(this);
            lSub.setText((en ? "Missions achieved: " : "達成目標：") + achievedCount + "/" + currentLesson.missions.size()
                    + (prog.stars >= 1 ? (en ? " · Next lesson unlocked! 🔓" : " · 下一關卡已成功解鎖！🔓") : ""));
            lSub.setTextSize(12);
            lSub.setTextColor(Color.parseColor("#A7F3D0"));
            lSub.setPadding(0, dp(2), 0, 0);
            lessonSuccessCard.addView(lSub);

            container.addView(lessonSuccessCard);
        }

        // 2. Score Hero Banner
        LinearLayout heroScoreCard = new LinearLayout(this);
        heroScoreCard.setOrientation(LinearLayout.VERTICAL);
        heroScoreCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColors(new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#312E81")});
        hBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        hBg.setCornerRadius(dp(14));
        hBg.setStroke(dp(1), Color.parseColor("#6366F1"));
        heroScoreCard.setBackground(hBg);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hl.setMargins(0, dp(14), 0, dp(14));
        heroScoreCard.setLayoutParams(hl);

        LinearLayout scoreRow = new LinearLayout(this);
        scoreRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView scoreVal = new TextView(this);
        scoreVal.setText(String.valueOf(record.overallScore));
        scoreVal.setTextSize(36);
        scoreVal.setTextColor(Color.parseColor("#38BDF8"));
        scoreVal.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        scoreRow.addView(scoreVal);

        LinearLayout metaCol = new LinearLayout(this);
        metaCol.setOrientation(LinearLayout.VERTICAL);
        metaCol.setPadding(dp(12), 0, 0, 0);

        TextView ratingTv = new TextView(this);
        String ratingStr = record.overallScore >= 90 ? (en ? "🌟 Outstanding Mastery" : "🌟 表現優異 · 掌握自如")
                : (record.overallScore >= 80 ? (en ? "👍 Great Fluency" : "👍 表達流暢 · 互動良好")
                : (en ? "💪 Keep Practicing" : "💪 持續進步 · 勇於開口"));
        ratingTv.setText(ratingStr);
        ratingTv.setTextSize(14);
        ratingTv.setTextColor(Color.WHITE);
        ratingTv.setTypeface(Typeface.DEFAULT_BOLD);
        metaCol.addView(ratingTv);

        TextView infoTv = new TextView(this);
        int mins = record.durationSeconds / 60;
        int secs = record.durationSeconds % 60;
        String timeStr = mins > 0 ? (mins + "分" + secs + "秒") : (secs + "秒");
        infoTv.setText(record.dateString + " · " + timeStr + " · " + record.userTurns + " 輪互動");
        infoTv.setTextSize(11);
        infoTv.setTextColor(Color.parseColor("#C7D2FE"));
        infoTv.setPadding(0, dp(2), 0, 0);
        metaCol.addView(infoTv);

        scoreRow.addView(metaCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        heroScoreCard.addView(scoreRow);

        // 4 Diagnostic Progress Bars
        LinearLayout barsCol = new LinearLayout(this);
        barsCol.setOrientation(LinearLayout.VERTICAL);
        barsCol.setPadding(0, dp(12), 0, 0);
        barsCol.addView(makeScoreBar(en ? "Fluency 流暢度" : "流暢度 (Fluency)", record.fluencyScore, "#38BDF8"));
        barsCol.addView(makeScoreBar(en ? "Vocabulary 詞彙量" : "詞彙量 (Vocabulary)", record.vocabScore, "#FBBF24"));
        barsCol.addView(makeScoreBar(en ? "Grammar 文法準確" : "文法準確 (Grammar)", record.grammarScore, "#A78BFA"));
        barsCol.addView(makeScoreBar(en ? "Phonetic 發音自然" : "發音自然 (Phonetic)", record.phoneticScore, "#34D399"));
        heroScoreCard.addView(barsCol);
        container.addView(heroScoreCard);

        // 3. Summary & Strengths
        if (!record.summary.isEmpty() || !record.strengths.isEmpty()) {
            LinearLayout sumCard = new LinearLayout(this);
            sumCard.setOrientation(LinearLayout.VERTICAL);
            sumCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(Color.parseColor("#1E293B"));
            sBg.setCornerRadius(dp(12));
            sBg.setStroke(dp(1), Color.parseColor("#334155"));
            sumCard.setBackground(sBg);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(0, 0, 0, dp(12));
            sumCard.setLayoutParams(slp);

            if (!record.summary.isEmpty()) {
                TextView stTitle = new TextView(this);
                stTitle.setText(en ? "📋 Overall Summary" : "📋 課堂綜合評述");
                stTitle.setTextSize(12);
                stTitle.setTextColor(Color.parseColor("#60A5FA"));
                stTitle.setTypeface(Typeface.DEFAULT_BOLD);
                sumCard.addView(stTitle);

                TextView stBody = new TextView(this);
                stBody.setText(record.summary);
                stBody.setTextSize(13);
                stBody.setTextColor(Color.parseColor("#E2E8F0"));
                stBody.setLineSpacing(dp(2), 1.2f);
                stBody.setPadding(0, dp(2), 0, dp(8));
                sumCard.addView(stBody);
            }

            if (!record.strengths.isEmpty()) {
                TextView strTitle = new TextView(this);
                strTitle.setText(en ? "💪 Strengths & Highlights" : "💪 優勢亮點與進步");
                strTitle.setTextSize(12);
                strTitle.setTextColor(Color.parseColor("#34D399"));
                strTitle.setTypeface(Typeface.DEFAULT_BOLD);
                sumCard.addView(strTitle);

                TextView strBody = new TextView(this);
                strBody.setText(record.strengths);
                strBody.setTextSize(13);
                strBody.setTextColor(Color.parseColor("#E2E8F0"));
                strBody.setLineSpacing(dp(2), 1.2f);
                strBody.setPadding(0, dp(2), 0, 0);
                sumCard.addView(strBody);
            }

            container.addView(sumCard);
        }

        // 4. Recasts Corrections (道地重述對照)
        try {
            JSONArray recasts = new JSONArray(record.recastsJson != null ? record.recastsJson : "[]");
            if (recasts.length() > 0) {
                TextView rcTitle = new TextView(this);
                rcTitle.setText(en ? "✨ Native Recast & Fixes (Tap ⭐ to save)" : "✨ 母語者道地重述對照（點擊 ⭐ 收藏）");
                rcTitle.setTextSize(13);
                rcTitle.setTextColor(Color.parseColor("#F472B6"));
                rcTitle.setTypeface(Typeface.DEFAULT_BOLD);
                rcTitle.setPadding(0, dp(4), 0, dp(8));
                container.addView(rcTitle);

                for (int i = 0; i < recasts.length(); i++) {
                    JSONObject rc = recasts.getJSONObject(i);
                    final String orig = rc.optString("original", "");
                    final String corr = rc.optString("corrected", "");
                    final String expl = rc.optString("explanation", "");

                    if (!corr.isEmpty()) {
                        LearningDataManager.StarredItem si = new LearningDataManager.StarredItem();
                        si.originalText = corr;
                        si.translation = orig;
                        si.category = "correction";
                        si.notes = expl;
                        allReportItems.add(si);
                    }

                    LinearLayout rcCard = new LinearLayout(this);
                    rcCard.setOrientation(LinearLayout.VERTICAL);
                    rcCard.setPadding(dp(12), dp(10), dp(12), dp(10));
                    GradientDrawable rBg = new GradientDrawable();
                    rBg.setColor(Color.parseColor("#1E293B"));
                    rBg.setCornerRadius(dp(10));
                    rBg.setStroke(dp(1), Color.parseColor("#374151"));
                    rcCard.setBackground(rBg);
                    LinearLayout.LayoutParams rclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rclp.setMargins(0, 0, 0, dp(8));
                    rcCard.setLayoutParams(rclp);

                    // Student original (red tint)
                    TextView oTv = new TextView(this);
                    oTv.setText("🗣️ " + (en ? "You said: " : "原句：") + orig);
                    oTv.setTextSize(12);
                    oTv.setTextColor(Color.parseColor("#FCA5A5"));
                    rcCard.addView(oTv);

                    // Native recast row (green)
                    LinearLayout corRow = new LinearLayout(this);
                    corRow.setOrientation(LinearLayout.HORIZONTAL);
                    corRow.setGravity(Gravity.CENTER_VERTICAL);
                    corRow.setPadding(0, dp(4), 0, 0);

                    TextView cTv = new TextView(this);
                    cTv.setText("✨ " + (en ? "Native: " : "道地說法：") + corr);
                    cTv.setTextSize(13);
                    cTv.setTextColor(Color.parseColor("#6EE7B7"));
                    cTv.setTypeface(Typeface.DEFAULT_BOLD);
                    corRow.addView(cTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    // Listen button
                    Button playBtn = new Button(this);
                    playBtn.setText("🔊");
                    playBtn.setTextSize(11);
                    playBtn.setTextColor(Color.WHITE);
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#4F46E5"));
                    pBg.setCornerRadius(dp(6));
                    playBtn.setBackground(pBg);
                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { speakWord(corr); }
                    });
                    corRow.addView(playBtn, new LinearLayout.LayoutParams(dp(38), dp(28)));

                    // Drill button (🎙️ 重練跟讀)
                    Button drillBtn = new Button(this);
                    drillBtn.setText("🎙️");
                    drillBtn.setTextSize(11);
                    drillBtn.setTextColor(Color.WHITE);
                    GradientDrawable dBg = new GradientDrawable();
                    dBg.setColor(Color.parseColor("#0D9488"));
                    dBg.setCornerRadius(dp(6));
                    drillBtn.setBackground(dBg);
                    drillBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            OralCoachHelper.showPronunciationDrillDialog(NativeLiveActivity.this, corr, orig, expl);
                        }
                    });
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(38), dp(28));
                    dlp.setMargins(dp(3), 0, 0, 0);
                    corRow.addView(drillBtn, dlp);

                    // Star button
                    final boolean isSt = LearningDataManager.isStarred(NativeLiveActivity.this, corr);
                    final Button starBtn = new Button(this);
                    starBtn.setText(isSt ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isSt ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(NativeLiveActivity.this, corr, orig, "correction", expl);
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(NativeLiveActivity.this, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    corRow.addView(starBtn, new LinearLayout.LayoutParams(dp(34), dp(28)));
                    allStarButtons.add(starBtn);
                    rcCard.addView(corRow);

                    if (!expl.isEmpty()) {
                        TextView eTv = new TextView(this);
                        eTv.setText("💡 " + expl);
                        eTv.setTextSize(11);
                        eTv.setTextColor(Color.parseColor("#94A3B8"));
                        eTv.setPadding(dp(4), dp(4), 0, 0);
                        rcCard.addView(eTv);
                    }

                    container.addView(rcCard);
                }
            }
        } catch (Exception ignored) {}

        // 5. Key Takeaways
        try {
            JSONArray takeaways = new JSONArray(record.takeawaysJson != null ? record.takeawaysJson : "[]");
            if (takeaways.length() > 0) {
                TextView tkTitle = new TextView(this);
                tkTitle.setText(en ? "💡 Key Takeaways & Useful Expressions" : "💡 課後精選實用金句");
                tkTitle.setTextSize(13);
                tkTitle.setTextColor(Color.parseColor("#FBBF24"));
                tkTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tkTitle.setPadding(0, dp(6), 0, dp(8));
                container.addView(tkTitle);

                for (int i = 0; i < takeaways.length(); i++) {
                    JSONObject tk = takeaways.getJSONObject(i);
                    final String phrase = tk.optString("phrase", "");
                    final String trans = tk.optString("translation", "");

                    if (!phrase.isEmpty()) {
                        LearningDataManager.StarredItem si = new LearningDataManager.StarredItem();
                        si.originalText = phrase;
                        si.translation = trans;
                        si.category = "phrase";
                        si.notes = "";
                        allReportItems.add(si);
                    }

                    LinearLayout tkCard = new LinearLayout(this);
                    tkCard.setOrientation(LinearLayout.HORIZONTAL);
                    tkCard.setGravity(Gravity.CENTER_VERTICAL);
                    tkCard.setPadding(dp(12), dp(8), dp(12), dp(8));
                    GradientDrawable tBg = new GradientDrawable();
                    tBg.setColor(Color.parseColor("#1E293B"));
                    tBg.setCornerRadius(dp(8));
                    tBg.setStroke(dp(1), Color.parseColor("#334155"));
                    tkCard.setBackground(tBg);
                    LinearLayout.LayoutParams tklp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tklp.setMargins(0, 0, 0, dp(6));
                    tkCard.setLayoutParams(tklp);

                    LinearLayout textCol = new LinearLayout(this);
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    TextView pTv = new TextView(this);
                    pTv.setText("• " + phrase);
                    pTv.setTextSize(13);
                    pTv.setTextColor(Color.WHITE);
                    pTv.setTypeface(Typeface.DEFAULT_BOLD);
                    textCol.addView(pTv);

                    if (!trans.isEmpty()) {
                        TextView trTv = new TextView(this);
                        trTv.setText(trans);
                        trTv.setTextSize(11);
                        trTv.setTextColor(Color.parseColor("#94A3B8"));
                        trTv.setPadding(0, dp(2), 0, 0);
                        textCol.addView(trTv);
                    }
                    tkCard.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    Button pBtn = new Button(this);
                    pBtn.setText("🔊");
                    pBtn.setTextSize(11);
                    pBtn.setTextColor(Color.WHITE);
                    GradientDrawable pbBg = new GradientDrawable();
                    pbBg.setColor(Color.parseColor("#4F46E5"));
                    pbBg.setCornerRadius(dp(6));
                    pBtn.setBackground(pbBg);
                    pBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { speakWord(phrase); }
                    });
                    tkCard.addView(pBtn, new LinearLayout.LayoutParams(dp(42), dp(28)));

                    final boolean isStarred = LearningDataManager.isStarred(NativeLiveActivity.this, phrase);
                    final Button starBtn = new Button(this);
                    starBtn.setText(isStarred ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(NativeLiveActivity.this, phrase, trans, "phrase", "");
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(NativeLiveActivity.this, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    tkCard.addView(starBtn, new LinearLayout.LayoutParams(dp(36), dp(28)));
                    allStarButtons.add(starBtn);

                    container.addView(tkCard);
                }
            }
        } catch (Exception ignored) {}

        // 6. Tutor Cheer
        if (!record.cheer.isEmpty()) {
            LinearLayout cheerCard = new LinearLayout(this);
            cheerCard.setOrientation(LinearLayout.VERTICAL);
            cheerCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable chBg = new GradientDrawable();
            chBg.setColor(Color.parseColor("#14532D")); // Emerald 900
            chBg.setCornerRadius(dp(12));
            chBg.setStroke(dp(1), Color.parseColor("#16A34A"));
            cheerCard.setBackground(chBg);
            LinearLayout.LayoutParams chlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chlp.setMargins(0, dp(8), 0, dp(14));
            cheerCard.setLayoutParams(chlp);

            TextView chTitle = new TextView(this);
            chTitle.setText(en ? "💌 Tutor Cheer" : "💌 外師課後寄語");
            chTitle.setTextSize(12);
            chTitle.setTextColor(Color.parseColor("#86EFAC"));
            chTitle.setTypeface(Typeface.DEFAULT_BOLD);
            cheerCard.addView(chTitle);

            TextView chBody = new TextView(this);
            chBody.setText(record.cheer);
            chBody.setTextSize(13);
            chBody.setTextColor(Color.WHITE);
            chBody.setLineSpacing(dp(2), 1.2f);
            chBody.setPadding(0, dp(3), 0, 0);
            cheerCard.addView(chBody);

            container.addView(cheerCard);
        }

        // 7. Star All & Close Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        final Button starAllBtn = new Button(this);
        starAllBtn.setText(en ? "⭐ Star All Sentences" : "⭐ 一鍵收藏所有精選句子");
        starAllBtn.setTextSize(12);
        starAllBtn.setTextColor(Color.WHITE);
        starAllBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sab = new GradientDrawable();
        sab.setColor(Color.parseColor("#D97706")); // Amber 600
        sab.setCornerRadius(dp(10));
        starAllBtn.setBackground(sab);
        starAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (allReportItems.isEmpty() && record.fullTranscript != null && !record.fullTranscript.isEmpty()) {
                    String[] lines = record.fullTranscript.split("\n");
                    for (String l : lines) {
                        if (l.startsWith("Tutor:") || l.startsWith("Student:")) {
                            String clean = l.replaceFirst("^(?:Tutor|Student):\\s*", "").trim();
                            if (clean.length() > 5) {
                                LearningDataManager.StarredItem item = new LearningDataManager.StarredItem();
                                item.originalText = clean;
                                item.translation = "";
                                item.category = "phrase";
                                allReportItems.add(item);
                            }
                        }
                    }
                }

                int added = LearningDataManager.addStarredItemsBatch(NativeLiveActivity.this, allReportItems);
                for (Button b : allStarButtons) {
                    if (b != null) {
                        b.setText("★");
                        b.setTextColor(Color.parseColor("#FBBF24"));
                    }
                }
                int displayCount = added > 0 ? added : allReportItems.size();
                starAllBtn.setText(en ? ("✅ All Saved (" + displayCount + ")") : ("✅ 已全部收藏 (" + displayCount + " 句)"));
                Toast.makeText(NativeLiveActivity.this, en ? ("⭐ Saved " + displayCount + " items to Phrasebook!") : ("⭐ 已成功收藏 " + displayCount + " 條精選金句至生詞本！"), Toast.LENGTH_SHORT).show();
            }
        });
        btnRow.addView(starAllBtn, new LinearLayout.LayoutParams(0, dp(44), 1.2f));

        Button okBtn = new Button(this);
        okBtn.setText(en ? "Done" : "完成");
        okBtn.setTextSize(13);
        okBtn.setTextColor(Color.WHITE);
        GradientDrawable ob = new GradientDrawable();
        ob.setColor(Color.parseColor("#2563EB"));
        ob.setCornerRadius(dp(10));
        okBtn.setBackground(ob);
        LinearLayout.LayoutParams oklp = new LinearLayout.LayoutParams(0, dp(44), 0.8f);
        oklp.setMargins(dp(8), 0, 0, 0);
        okBtn.setLayoutParams(oklp);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        btnRow.addView(okBtn);

        container.addView(btnRow);
        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private View makeScoreBar(String label, int score, String colorHex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(2), 0, dp(6));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(11);
        labelTv.setTextColor(Color.parseColor("#C7D2FE"));
        head.addView(labelTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView scoreTv = new TextView(this);
        scoreTv.setText(score + " 分");
        scoreTv.setTextSize(11);
        scoreTv.setTextColor(Color.parseColor(colorHex));
        scoreTv.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(scoreTv);
        row.addView(head);

        // Progress Bar track
        FrameLayout track = new FrameLayout(this);
        GradientDrawable tBg = new GradientDrawable();
        tBg.setColor(Color.parseColor("#1E1B4B"));
        tBg.setCornerRadius(dp(4));
        track.setBackground(tBg);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        trLp.setMargins(0, dp(3), 0, 0);
        track.setLayoutParams(trLp);

        // Fill
        final View fill = new View(this);
        GradientDrawable fBg = new GradientDrawable();
        fBg.setColor(Color.parseColor(colorHex));
        fBg.setCornerRadius(dp(4));
        fill.setBackground(fBg);
        final int fillScore = Math.max(5, Math.min(100, score));
        track.addView(fill);
        track.post(new Runnable() {
            @Override public void run() {
                int parentWidth = ((View) fill.getParent()).getWidth();
                if (parentWidth > 0) {
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) fill.getLayoutParams();
                    p.width = (int) (parentWidth * (fillScore / 100.0));
                    p.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    fill.setLayoutParams(p);
                }
            }
        });

        row.addView(track);
        return row;
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

