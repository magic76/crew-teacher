package com.crewpocket.teacher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 201;

    private TextView statusDot;
    private TextView statusText;
    private TextView statusDetail;
    private LinearLayout statusCard;
    private LinearLayout pageContent;

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

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CrewTheme.BG_PRIMARY);

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(pageContent);

        setContentView(scroll);
        renderHomePage();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void renderHomePage() {
        pageContent.removeAllViews();
        final boolean en = I18n.isEnglish(this);

        // 1. Top Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(4));

        TextView brandIcon = new TextView(this);
        brandIcon.setText("🎓");
        brandIcon.setTextSize(26);
        brandIcon.setPadding(0, 0, dp(10), 0);
        headerRow.addView(brandIcon);

        LinearLayout brandTextCol = new LinearLayout(this);
        brandTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Crew Teacher");
        title.setTextSize(20);
        title.setTextColor(CrewTheme.TEXT_PRIMARY);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        brandTextCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(en ? "AI 1-on-1 Oral Language Tutor" : "隨身 AI 外語口說教練 · 沉浸對話");
        subtitle.setTextSize(11);
        subtitle.setTextColor(CrewTheme.TEXT_SECONDARY);
        brandTextCol.addView(subtitle);

        headerRow.addView(brandTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Quick Language Switch Pill Button
        Button langToggleBtn = new Button(this);
        langToggleBtn.setText(en ? "🇨🇳 中文" : "🇺🇸 EN");
        langToggleBtn.setTextSize(11);
        langToggleBtn.setTextColor(Color.WHITE);
        GradientDrawable ltBg = new GradientDrawable();
        ltBg.setColor(Color.parseColor("#1E293B"));
        ltBg.setCornerRadius(dp(12));
        ltBg.setStroke(dp(1), Color.parseColor("#475569"));
        langToggleBtn.setBackground(ltBg);
        langToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppConfig.setUiLanguage(MainActivity.this, en ? "zh" : "en");
                renderHomePage();
            }
        });
        headerRow.addView(langToggleBtn, new LinearLayout.LayoutParams(dp(72), dp(34)));
        pageContent.addView(headerRow);

        // 2. Status Card Banner
        statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams statusCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusCardLp.setMargins(0, dp(14), 0, dp(14));
        statusCard.setLayoutParams(statusCardLp);

        statusDot = new TextView(this);
        statusDot.setText("●");
        statusDot.setTextSize(13);
        statusDot.setPadding(0, 0, dp(8), 0);
        statusCard.addView(statusDot);

        LinearLayout statusTextCol = new LinearLayout(this);
        statusTextCol.setOrientation(LinearLayout.VERTICAL);

        statusText = new TextView(this);
        statusText.setTextSize(12);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextCol.addView(statusText);

        statusDetail = new TextView(this);
        statusDetail.setTextSize(10);
        statusTextCol.addView(statusDetail);

        statusCard.addView(statusTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pageContent.addView(statusCard);

        // 2.5 【今日口說打卡與學習目標】Streak & Progress Card
        LearningDataManager.StreakInfo streak = LearningDataManager.getStreakInfo(this);
        LinearLayout streakCard = new LinearLayout(this);
        streakCard.setOrientation(LinearLayout.VERTICAL);
        streakCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable skBg = new GradientDrawable();
        skBg.setColors(new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#0F172A")});
        skBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        skBg.setCornerRadius(dp(16));
        skBg.setStroke(dp(1), Color.parseColor("#4338CA"));
        streakCard.setBackground(skBg);
        LinearLayout.LayoutParams skLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skLp.setMargins(0, 0, 0, dp(14));
        streakCard.setLayoutParams(skLp);

        LinearLayout skTopRow = new LinearLayout(this);
        skTopRow.setOrientation(LinearLayout.HORIZONTAL);
        skTopRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView streakBadge = new TextView(this);
        streakBadge.setText(streak.streakDays > 0
                ? ("🔥 " + (en ? "Streak: " : "連續打卡 ") + streak.streakDays + (en ? " Days" : " 天"))
                : ("🔥 " + (en ? "Start Your Streak Today!" : "今日開口，啟動連續打卡！")));
        streakBadge.setTextSize(13);
        streakBadge.setTextColor(Color.parseColor("#F59E0B"));
        streakBadge.setTypeface(Typeface.DEFAULT_BOLD);
        skTopRow.addView(streakBadge, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView timeBadge = new TextView(this);
        timeBadge.setText("⏱️ " + streak.formattedTodayTime);
        timeBadge.setTextSize(11);
        timeBadge.setTextColor(Color.parseColor("#A5B4FC"));
        skTopRow.addView(timeBadge);
        streakCard.addView(skTopRow);

        // Progress text row
        LinearLayout progTextRow = new LinearLayout(this);
        progTextRow.setOrientation(LinearLayout.HORIZONTAL);
        progTextRow.setGravity(Gravity.CENTER_VERTICAL);
        progTextRow.setPadding(0, dp(8), 0, dp(4));

        TextView progTitle = new TextView(this);
        progTitle.setText(en ? "🎯 Daily Goal Progress" : "🎯 今日口說進度");
        progTitle.setTextSize(11);
        progTitle.setTextColor(Color.parseColor("#94A3B8"));
        progTextRow.addView(progTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView progCount = new TextView(this);
        progCount.setText(streak.todayTurns + " / " + streak.dailyGoalTurns + (en ? " turns" : " 句") + (streak.isGoalCompleted ? " (✅ 完成)" : ""));
        progCount.setTextSize(11);
        progCount.setTextColor(streak.isGoalCompleted ? Color.parseColor("#34D399") : Color.parseColor("#38BDF8"));
        progCount.setTypeface(Typeface.DEFAULT_BOLD);
        progTextRow.addView(progCount);
        streakCard.addView(progTextRow);

        // Visual Progress Bar
        FrameLayout progTrack = new FrameLayout(this);
        GradientDrawable ptBg = new GradientDrawable();
        ptBg.setColor(Color.parseColor("#312E81"));
        ptBg.setCornerRadius(dp(4));
        progTrack.setBackground(ptBg);
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progTrack.setLayoutParams(ptLp);

        final View progFill = new View(this);
        GradientDrawable pfBg = new GradientDrawable();
        pfBg.setColors(new int[]{Color.parseColor("#38BDF8"), Color.parseColor("#6366F1")});
        pfBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        pfBg.setCornerRadius(dp(4));
        progFill.setBackground(pfBg);
        final int percent = Math.min(100, Math.max(0, (int) (streak.todayTurns * 100.0 / Math.max(1, streak.dailyGoalTurns))));
        progTrack.addView(progFill);
        progTrack.post(new Runnable() {
            @Override public void run() {
                int parentW = ((View) progFill.getParent()).getWidth();
                if (parentW > 0) {
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) progFill.getLayoutParams();
                    p.width = (int) (parentW * (percent / 100.0));
                    p.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    progFill.setLayoutParams(p);
                }
            }
        });
        streakCard.addView(progTrack);
        pageContent.addView(streakCard);

        // 2.5 【🗺️ 系統口語關卡地圖】Learning Path & Missions
        int totalStars = CourseManager.getTotalStars(this);
        int completedLessons = CourseManager.getCompletedLessonsCount(this);
        int totalLessons = CourseManager.getTotalLessonsCount();

        LinearLayout courseBanner = new LinearLayout(this);
        courseBanner.setOrientation(LinearLayout.VERTICAL);
        courseBanner.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable cbBg = new GradientDrawable();
        cbBg.setColors(new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#0F172A")});
        cbBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        cbBg.setCornerRadius(dp(16));
        cbBg.setStroke(dp(1), Color.parseColor("#6366F1"));
        courseBanner.setBackground(cbBg);

        LinearLayout cbTop = new LinearLayout(this);
        cbTop.setOrientation(LinearLayout.HORIZONTAL);
        cbTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView cbTitle = new TextView(this);
        cbTitle.setText(en ? "🗺️ Learning Path & Missions" : "🗺️ 系統口語關卡地圖");
        cbTitle.setTextSize(14);
        cbTitle.setTextColor(Color.parseColor("#A5B4FC"));
        cbTitle.setTypeface(Typeface.DEFAULT_BOLD);
        cbTop.addView(cbTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView cbBadge = new TextView(this);
        cbBadge.setText("⭐ " + totalStars + " · 🏆 " + completedLessons + "/" + totalLessons);
        cbBadge.setTextSize(12);
        cbBadge.setTextColor(Color.parseColor("#FBBF24"));
        cbBadge.setTypeface(Typeface.DEFAULT_BOLD);
        cbTop.addView(cbBadge);
        courseBanner.addView(cbTop);

        TextView cbSub = new TextView(this);
        cbSub.setText(en ? "Step-by-step oral drills: Travel, Business & Daily · Tap to start闖關"
                : "出國自由行、商務會議、日常社交 · 任務通關解鎖 ›");
        cbSub.setTextSize(11);
        cbSub.setTextColor(Color.parseColor("#94A3B8"));
        cbSub.setPadding(0, dp(4), 0, 0);
        courseBanner.addView(cbSub);

        courseBanner.setClickable(true);
        courseBanner.setFocusable(true);
        courseBanner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CourseMapDialog.show(MainActivity.this, null);
            }
        });

        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cblp.setMargins(0, dp(10), 0, dp(10));
        courseBanner.setLayoutParams(cblp);
        pageContent.addView(courseBanner);

        // 3. 【課前 3 鍵配置艙】Lesson Pod (語言 + 難易度 + 情境)
        LinearLayout lessonPod = new LinearLayout(this);
        lessonPod.setOrientation(LinearLayout.VERTICAL);
        lessonPod.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable podBg = new GradientDrawable();
        podBg.setColor(Color.parseColor("#0F172A"));
        podBg.setCornerRadius(dp(16));
        podBg.setStroke(dp(1), Color.parseColor("#334155"));
        lessonPod.setBackground(podBg);

        LinearLayout podHeader = new LinearLayout(this);
        podHeader.setOrientation(LinearLayout.HORIZONTAL);
        podHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView podTitle = new TextView(this);
        podTitle.setText(en ? "🎯 Lesson Configuration" : "🎯 今日口語練習配置");
        podTitle.setTextSize(12);
        podTitle.setTextColor(Color.parseColor("#94A3B8"));
        podTitle.setTypeface(Typeface.DEFAULT_BOLD);
        podHeader.addView(podTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView podHint = new TextView(this);
        podHint.setText(en ? "Tap item to edit" : "點擊項目可快速切換");
        podHint.setTextSize(10);
        podHint.setTextColor(Color.parseColor("#38BDF8"));
        podHeader.addView(podHint);
        lessonPod.addView(podHeader);

        // 3 Grid Capsules with gap
        LinearLayout capsuleRow = new LinearLayout(this);
        capsuleRow.setOrientation(LinearLayout.HORIZONTAL);
        capsuleRow.setPadding(0, dp(10), 0, 0);

        String currentLang = AppConfig.getTutorLanguage(this);
        String currentTeachingMode = AppConfig.getTeachingMode(this);
        String currentPersona = AppConfig.getTutorPersona(this);

        int gap = dp(5);

        // Pod 1: Language
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.setMargins(0, 0, gap, 0);
        capsuleRow.addView(makePodItem("🌐", getLanguageLabel(currentLang), en ? "Target" : "目標語言", Color.parseColor("#818CF8"), new View.OnClickListener() {
            @Override public void onClick(View v) { showLanguageDialog(); }
        }), lp1);

        // Pod 2: Teaching Mode
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMargins(gap, 0, gap, 0);
        String modeShort = "beginner".equals(currentTeachingMode) ? (en ? "Beginner" : "零基礎帶讀")
                : ("immersion".equals(currentTeachingMode) ? (en ? "Immersion" : "全外語沉浸")
                : ("shadowing".equals(currentTeachingMode) ? (en ? "Reading Coach" : "朗讀糾音") : (en ? "Bilingual" : "雙語對照")));
        capsuleRow.addView(makePodItem("💡", modeShort, en ? "Method" : "教學引導", Color.parseColor("#F59E0B"), new View.OnClickListener() {
            @Override public void onClick(View v) { showTeachingModeDialog(); }
        }), lp2);

        // Pod 3: Scenario
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp3.setMargins(gap, 0, 0, 0);
        String personaShort = getPersonaLabel(currentPersona, en);
        capsuleRow.addView(makePodItem("🎭", personaShort, en ? "Topic" : "情境主題", Color.parseColor("#14B8A6"), new View.OnClickListener() {
            @Override public void onClick(View v) { showPersonaDialog(); }
        }), lp3);

        lessonPod.addView(capsuleRow);
        pageContent.addView(lessonPod);

        // 4. 【核心英雄啟動大圓鍵】Hero Interactive Start Orb
        LinearLayout heroContainer = new LinearLayout(this);
        heroContainer.setOrientation(LinearLayout.VERTICAL);
        heroContainer.setGravity(Gravity.CENTER);
        heroContainer.setPadding(0, dp(24), 0, dp(18));

        Button startOrbBtn = new Button(this);
        startOrbBtn.setText(en ? "🎙️\nSTART\nORAL" : "🎙️\n開始練習\n隨時對話");
        startOrbBtn.setTextSize(13);
        startOrbBtn.setTypeface(Typeface.DEFAULT_BOLD);
        startOrbBtn.setTextColor(Color.WHITE);
        startOrbBtn.setLineSpacing(dp(2), 1.1f);
        GradientDrawable orbBg = new GradientDrawable();
        orbBg.setShape(GradientDrawable.OVAL);
        orbBg.setColors(new int[]{Color.parseColor("#3B82F6"), Color.parseColor("#6366F1"), Color.parseColor("#9333EA")});
        orbBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        orbBg.setStroke(dp(3), Color.parseColor("#38BDF8"));
        startOrbBtn.setBackground(orbBg);
        startOrbBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (AppConfig.getGeminiApiKey(MainActivity.this).isEmpty()) {
                    showApiKeyDialog();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
                    return;
                }
                startActivity(new Intent(MainActivity.this, NativeLiveActivity.class));
            }
        });
        heroContainer.addView(startOrbBtn, new LinearLayout.LayoutParams(dp(110), dp(110)));

        TextView orbSub = new TextView(this);
        orbSub.setText(en ? "Tap big orb to start 1-on-1 voice session" : "點擊中央圓球，立刻與 AI 導師開始一對一口說練習");
        orbSub.setTextSize(11);
        orbSub.setTextColor(Color.parseColor("#94A3B8"));
        orbSub.setPadding(0, dp(10), 0, 0);
        heroContainer.addView(orbSub);

        pageContent.addView(heroContainer);

        // 5. 【桌面懸浮泡泡快捷開關列】Floating Bubble Switch Card
        boolean isBubbleOn = FloatingBubbleManager.getInstance(this).isBubbleShowing();
        LinearLayout bubbleCard = new LinearLayout(this);
        bubbleCard.setOrientation(LinearLayout.HORIZONTAL);
        bubbleCard.setGravity(Gravity.CENTER_VERTICAL);
        bubbleCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bBg = new GradientDrawable();
        bBg.setColor(Color.parseColor("#1E293B"));
        bBg.setCornerRadius(dp(14));
        bBg.setStroke(dp(1), Color.parseColor("#334155"));
        bubbleCard.setBackground(bBg);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, 0, 0, dp(16));
        bubbleCard.setLayoutParams(bLp);

        TextView bIcon = new TextView(this);
        bIcon.setText("💬");
        bIcon.setTextSize(20);
        bIcon.setPadding(0, 0, dp(10), 0);
        bubbleCard.addView(bIcon);

        LinearLayout bTextCol = new LinearLayout(this);
        bTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView bTitle = new TextView(this);
        bTitle.setText(en ? "Desktop Floating Bubble" : "桌面懸浮練習泡泡");
        bTitle.setTextSize(13);
        bTitle.setTextColor(Color.WHITE);
        bTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bTextCol.addView(bTitle);

        TextView bDesc = new TextView(this);
        bDesc.setText(isBubbleOn
                ? (en ? "🟢 Bubble Running on Screen" : "🟢 懸浮泡泡運行中（可在其他 App 上暢聊）")
                : (en ? "⚪ Tap to display on screen" : "⚪ 已關閉（點擊在桌面開啟）"));
        bDesc.setTextSize(10);
        bDesc.setTextColor(Color.parseColor("#94A3B8"));
        bTextCol.addView(bDesc);

        bubbleCard.addView(bTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button bToggleBtn = new Button(this);
        bToggleBtn.setText(isBubbleOn ? (en ? "Close" : "關閉") : (en ? "Open" : "開啟"));
        bToggleBtn.setTextSize(11);
        bToggleBtn.setTextColor(Color.WHITE);
        GradientDrawable btBg = new GradientDrawable();
        btBg.setColor(isBubbleOn ? Color.parseColor("#E11D48") : Color.parseColor("#2563EB"));
        btBg.setCornerRadius(dp(10));
        bToggleBtn.setBackground(btBg);
        bToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!FloatingBubbleManager.getInstance(MainActivity.this).canDrawOverlays()) {
                    requestOverlayPermission();
                    return;
                }
                FloatingBubbleManager.getInstance(MainActivity.this).toggleBubble();
                renderHomePage();
            }
        });
        bubbleCard.addView(bToggleBtn, new LinearLayout.LayoutParams(dp(64), dp(34)));

        pageContent.addView(bubbleCard);

        // 5.5 【個人學習庫與成效記錄】Phrasebook & Session History
        TextView learningHeading = new TextView(this);
        learningHeading.setText(en ? "📚 Learning Hub & Diagnostic History" : "📚 個人學習庫與成效記錄");
        learningHeading.setTextSize(12);
        learningHeading.setTextColor(Color.parseColor("#64748B"));
        learningHeading.setTypeface(Typeface.DEFAULT_BOLD);
        learningHeading.setPadding(0, dp(4), 0, dp(8));
        pageContent.addView(learningHeading);

        int starredCount = LearningDataManager.getStarredItems(this).size();
        pageContent.addView(makeActionCard("⭐", en ? "My Starred Phrasebook & Vocab" : "⭐ 我的個人生詞與金句本",
                (starredCount > 0 ? ((en ? "Collected " : "已收藏 ") + starredCount + (en ? " items" : " 條精選金句/生詞")) : (en ? "0 items · Star phrases in practice" : "尚未收藏 · 練習中點擊 ⭐ 即可收藏")) + (en ? " · Tap to review & listen" : " · 點擊複習與發音"),
                Color.parseColor("#F59E0B"), new View.OnClickListener() {
            @Override public void onClick(View v) { showStarredPhrasebookDialog(); }
        }));

        int sessionCount = LearningDataManager.getSessionHistory(this).size();
        pageContent.addView(makeActionCard("📊", en ? "Session History & Diagnostics" : "📊 歷史對話與成效報告",
                (sessionCount > 0 ? ((en ? "Total " : "累計 ") + sessionCount + (en ? " tutoring sessions recorded" : " 次對話課堂記錄")) : (en ? "No sessions yet" : "尚無記錄 · 完成練習自動生成")) + (en ? " · Tap to review" : " · 點擊查看診斷"),
                Color.parseColor("#38BDF8"), new View.OnClickListener() {
            @Override public void onClick(View v) { showSessionHistoryDialog(); }
        }));

        // 6. 【底層進階設定抽屜列】Advanced Settings Footer Row
        TextView advHeading = new TextView(this);
        advHeading.setText(en ? "⚙️ Audio & System Preferences" : "⚙️ 音訊與系統進階設定");
        advHeading.setTextSize(12);
        advHeading.setTextColor(Color.parseColor("#64748B"));
        advHeading.setTypeface(Typeface.DEFAULT_BOLD);
        advHeading.setPadding(0, dp(6), 0, dp(8));
        pageContent.addView(advHeading);

        // Student Native Language
        String studentLangLabel = AppConfig.getStudentLanguageDisplayName(this);
        pageContent.addView(makeActionCard("🗣️", en ? "Student Native Language" : "學生母語（對照翻譯語言）", studentLangLabel + (en ? " · Subtitles & notes language" : " · 即時字幕對照與單字註釋語言"), CrewTheme.EMERALD_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showStudentLanguageDialog(); }
        }));

        // Voice Persona (30 Voices)
        String currentVoice = AppConfig.getVoiceName(this);
        String voiceSummary = getVoiceDisplayName(currentVoice, en);
        pageContent.addView(makeActionCard("🎙️", en ? "Tutor Voice Persona (30 Voices)" : "導師語音音色 (全 30 款)", voiceSummary + (en ? " · Tap to choose & listen" : " · 點擊選用與試聽"), CrewTheme.CYAN_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showVoicePersonaDialog(); }
        }));

        // Audio Route
        String currentOutput = AppConfig.getAudioOutput(this);
        String outputLabel = "media".equals(currentOutput)
                ? (en ? "🎵 Media Audio (Bluetooth / High Quality)" : "🎵 媒體音訊 (藍牙耳機高音質)")
                : (en ? "📞 Voice Call (Hardware AEC & Noise Cancelling)" : "📞 通話音訊 (硬體 AEC 回音消除，推薦)");
        pageContent.addView(makeActionCard("🔊", en ? "Audio Output Channel" : "語音輸出通道", outputLabel, CrewTheme.INDIGO_300, new View.OnClickListener() {
            @Override public void onClick(View v) { showAudioOutputDialog(); }
        }));

        // Interruption Shield
        int sensitivity = AppConfig.getInterruptionSensitivity(this);
        String shieldSummary = sensitivity <= 20 ? (en ? "🛡️ Heavy Shield" : "🛡️ 高強度防打斷")
                : (sensitivity >= 70 ? (en ? "⚡ Fast Interruption" : "⚡ 極速插話") : (en ? "⚖️ Standard Balanced" : "⚖️ 標準平衡模式"));
        pageContent.addView(makeActionCard("🛡️", en ? "Anti-Interruption & Echo Shield" : "防插話與環境抗迴音保護", shieldSummary + (en ? " · Tap to adjust" : " · 點擊調整"), CrewTheme.ROSE_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showInterruptionShieldDialog(); }
        }));

        // API Key
        String apiKey = AppConfig.getGeminiApiKey(this);
        String keySummary = apiKey.isEmpty()
                ? (en ? "⚠️ API Key not configured" : "⚠️ 尚未設定 API Key")
                : (en ? "✅ Key configured: " : "✅ 已設定 Key: ") + apiKey.substring(0, Math.min(8, apiKey.length())) + "…";
        pageContent.addView(makeActionCard("🔑", "Gemini API Key (BYOK)", keySummary, CrewTheme.AMBER_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showApiKeyDialog(); }
        }));

        refreshStatus();
    }

    private LinearLayout makePodItem(String icon, String title, String tag, int color, View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B"));
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)));
        item.setBackground(bg);
        item.setClickable(true);
        item.setFocusable(true);

        TextView iconV = new TextView(this);
        iconV.setText(icon);
        iconV.setTextSize(16);
        item.addView(iconV);

        TextView titleV = new TextView(this);
        titleV.setText(title);
        titleV.setTextSize(12);
        titleV.setTextColor(Color.WHITE);
        titleV.setTypeface(Typeface.DEFAULT_BOLD);
        titleV.setGravity(Gravity.CENTER);
        titleV.setSingleLine(true);
        titleV.setEllipsize(TextUtils.TruncateAt.END);
        titleV.setPadding(0, dp(3), 0, 0);
        item.addView(titleV);

        TextView tagV = new TextView(this);
        tagV.setText(tag);
        tagV.setTextSize(10);
        tagV.setTextColor(color);
        tagV.setGravity(Gravity.CENTER);
        tagV.setPadding(0, dp(2), 0, 0);
        item.addView(tagV);

        item.setOnClickListener(onClick);
        return item;
    }

    private void refreshStatus() {
        boolean en = I18n.isEnglish(this);
        boolean micGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean overlayGranted = FloatingBubbleManager.getInstance(this).canDrawOverlays();
        boolean hasKey = !AppConfig.getGeminiApiKey(this).isEmpty();

        if (micGranted && overlayGranted && hasKey) {
            statusDot.setTextColor(CrewTheme.EMERALD_400);
            statusText.setText(en ? "Oral tutor ready, start practicing anytime" : "口語教練已就緒，隨時可開始對話");
            statusText.setTextColor(CrewTheme.EMERALD_400);
            statusDetail.setText(en ? "Microphone, floating overlay and API Key are ready." : "麥克風、桌面懸浮窗與 API Key 皆已備妥。");
            statusDetail.setTextColor(CrewTheme.TEXT_SECONDARY);
            statusCard.setBackground(CrewTheme.createCard(this, Color.argb(30, 16, 185, 129), CrewTheme.BORDER_EMERALD, 14));
        } else {
            statusDot.setTextColor(CrewTheme.AMBER_400);
            statusText.setText(en ? "Not completely ready" : "尚未完全就緒");
            statusText.setTextColor(CrewTheme.AMBER_400);
            StringBuilder sb = new StringBuilder();
            if (!micGranted) sb.append(en ? "• Microphone permission required " : "• 請授權麥克風 ");
            if (!overlayGranted) sb.append(en ? "• Floating overlay permission required " : "• 請授權懸浮視窗 ");
            if (!hasKey) sb.append(en ? "• Enter Gemini API Key" : "• 請輸入 Gemini API Key");
            statusDetail.setText(sb.toString().trim());
            statusDetail.setTextColor(CrewTheme.TEXT_SECONDARY);
            statusCard.setBackground(CrewTheme.createCard(this, Color.argb(30, 245, 158, 11), CrewTheme.BORDER_AMBER, 14));
        }
    }

    private LinearLayout makeActionCard(String icon, String title, String desc, int accentColor, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(CrewTheme.createCard(this, CrewTheme.BG_CARD, CrewTheme.BORDER_DEFAULT, 14));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardLp);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(22);
        iconView.setPadding(0, 0, dp(14), 0);
        card.addView(iconView);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(14);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextSize(11);
        descView.setTextColor(CrewTheme.TEXT_SECONDARY);
        descView.setPadding(0, dp(2), 0, 0);
        textCol.addView(descView);

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(onClick);
        return card;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
            }
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void showUiLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇨🇳 繁體/簡體中文 (Traditional & Simplified Chinese)",
                "🇺🇸 English (International)"
        };
        final String[] values = {"zh", "en"};
        int selected = en ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🌍 Choose App UI Language" : "🌍 選擇應用程式介面語言");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setUiLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderHomePage();
                Toast.makeText(MainActivity.this, values[which].equals("en") ? "UI language switched to English" : "介面已切換為中文", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showAudioOutputDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "📞 Voice Call - Recommended (Hardware Noise Suppression & AEC)" : "📞 通話音訊 (Voice Call) - 推薦，具備硬體降噪與回音消除(AEC)",
                en ? "🎵 Media Audio - Media volume channel / Bluetooth headset" : "🎵 媒體音訊 (Media Audio) - 走媒體音量通道/藍牙耳機"
        };
        final String[] values = {"call", "media"};
        int selected = "media".equals(AppConfig.getAudioOutput(this)) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🔊 Audio Output Channel" : "🔊 選擇語音輸出通道");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setAudioOutput(MainActivity.this, values[which]);
                dialog.dismiss();
                renderHomePage();
                Toast.makeText(MainActivity.this, en
                        ? ("Audio output set to: " + ("media".equals(values[which]) ? "Media Audio" : "Voice Call"))
                        : ("已設定為：" + ("media".equals(values[which]) ? "媒體音訊" : "通話音訊")), Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showApiKeyDialog() {
        final boolean en = I18n.isEnglish(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🔑 Enter Gemini API Key" : "🔑 請輸入 Gemini API Key");
        builder.setMessage(en
                ? "Crew Teacher requires your Gemini API Key to connect to the 1-on-1 oral tutor engine."
                : "Crew Teacher 需要填入您的 Gemini API Key 才能連線口說語音引擎。");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        final EditText input = new EditText(this);
        input.setHint("AIzaSy...");
        input.setText(AppConfig.getGeminiApiKey(this));
        input.setTextColor(Color.WHITE);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
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
                            Toast.makeText(MainActivity.this, en ? "Pasted" : "已貼上", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        pLp.setMargins(0, dp(6), 0, 0);
        layout.addView(pasteBtn, pLp);

        builder.setView(layout);

        builder.setPositiveButton(en ? "Save" : "儲存", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setGeminiApiKey(MainActivity.this, input.getText().toString());
                Toast.makeText(MainActivity.this, en ? "API Key saved" : "已儲存 API Key", Toast.LENGTH_SHORT).show();
                renderHomePage();
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    public static String getLanguageLabel(String code) {
        if ("zh".equalsIgnoreCase(code) || "cmn".equalsIgnoreCase(code) || "chinese".equalsIgnoreCase(code) || "mandarin".equalsIgnoreCase(code)) return "中文 (國語/普通話)";
        if ("nan".equalsIgnoreCase(code) || "hokkien".equalsIgnoreCase(code)) return "閩南語 (台語)";
        if ("hak".equalsIgnoreCase(code) || "hakka".equalsIgnoreCase(code)) return "客家語 (客語)";
        if ("yue".equalsIgnoreCase(code) || "cantonese".equalsIgnoreCase(code)) return "粵語 (廣東話)";
        if ("ar".equalsIgnoreCase(code)) return "阿拉伯語 (العربية)";
        if ("hi".equalsIgnoreCase(code)) return "印地語 (हिन्दी)";
        if ("ms".equalsIgnoreCase(code)) return "馬來語 (Melayu)";
        if ("vi".equalsIgnoreCase(code)) return "越南語 (Tiếng Việt)";
        if ("ko".equalsIgnoreCase(code)) return "韓語 (한국어)";
        if ("th".equalsIgnoreCase(code)) return "泰語 (ภาษาไทย)";
        if ("pt".equalsIgnoreCase(code)) return "葡萄牙語 (Português)";
        if ("ru".equalsIgnoreCase(code)) return "俄語 (Русский)";
        if ("ja".equalsIgnoreCase(code)) return "日語 (日本語)";
        if ("es".equalsIgnoreCase(code)) return "西班牙語 (Español)";
        if ("fr".equalsIgnoreCase(code)) return "法語 (Français)";
        if ("de".equalsIgnoreCase(code)) return "德語 (Deutsch)";
        if ("it".equalsIgnoreCase(code)) return "義大利語 (Italiano)";
        if ("id".equalsIgnoreCase(code)) return "印尼語 (Bahasa Indonesia)";
        return "英語 (English)";
    }

    public static String getPersonaLabel(String persona, boolean en) {
        if ("travel".equals(persona)) return en ? "Travel & Dining" : "✈️ 出國旅遊 (Travel & Dining)";
        if ("business".equals(persona)) return en ? "Workplace Business" : "💼 職場商務 (Business English)";
        if ("interview".equals(persona)) return en ? "Job Interview" : "👔 求職面試 (Job Interview)";
        if ("exam".equals(persona)) return en ? "Exam Prep (IELTS/TOEFL)" : "🎯 口說備考 (IELTS/TOEFL/TOEIC)";
        if ("shopping".equals(persona)) return en ? "Shopping & Returns" : "🛍️ 購物退稅 (Shopping & Bargaining)";
        if ("medical".equals(persona)) return en ? "Doctor & Medical" : "🏥 醫院看診 (Doctor & Medical)";
        if ("housing".equals(persona)) return en ? "Renting & Housing" : "🏠 租屋看房 (Apartment & Housing)";
        if ("dating".equals(persona)) return en ? "Dating & Social" : "☕ 交友約會 (Dating & Social)";
        if ("tech".equals(persona)) return en ? "Tech & AI Trends" : "🤖 科技趨勢 (Tech & AI Trends)";
        return en ? "Daily Life & Hobbies" : "☕ 日常閒聊 (Daily Life & Hobbies)";
    }

    private void showLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇨🇳 中文 / 國語 (Mandarin Chinese)",
                "🇺🇸 英語 (English)",
                "🇹🇼 閩南語 / 台語 (Taiwanese Hokkien)",
                "🇹🇼 客家語 (Hakka)",
                "🇭🇰 粵語 / 廣東話 (Cantonese)",
                "🇯🇵 日語 (日本語)",
                "🇰🇷 韓語 (한국어)",
                "🇪🇸 西班牙語 (Español)",
                "🇫🇷 法語 (Français)",
                "🇩🇪 德語 (Deutsch)",
                "🇮🇹 義大利語 (Italiano)",
                "🇸🇦 阿拉伯語 (العربية / Arabic)",
                "🇮🇳 印地語 (हिन्दी / Hindi)",
                "🇲🇾 馬來語 (Bahasa Melayu)",
                "🇻🇳 越南語 (Tiếng Việt)",
                "🇹🇭 泰語 (ภาษาไทย)",
                "🇵🇹 葡萄牙語 (Português)",
                "🇷🇺 俄語 (Русский)",
                "🇮🇩 印尼語 (Bahasa Indonesia)"
        };
        final String[] values = {"zh", "en", "nan", "hak", "yue", "ja", "ko", "es", "fr", "de", "it", "ar", "hi", "ms", "vi", "th", "pt", "ru", "id"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🌐 Target Practice Language" : "🌐 選擇練習目標語言");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String oldLang = AppConfig.getTutorLanguage(MainActivity.this);
                if (!values[which].equalsIgnoreCase(oldLang)) {
                    AppConfig.setTutorLanguage(MainActivity.this, values[which]);
                    AppConfig.setReadingText(MainActivity.this, "");
                }
                renderHomePage();
                Toast.makeText(MainActivity.this, (en ? "Practice language set to: " : "已設定練習語言：") + items[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showStudentLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇹🇼 繁體中文 (Traditional Chinese)",
                "🇨🇳 簡體中文 (Simplified Chinese)",
                "🇺🇸 英語 (English)",
                "🇯🇵 日語 (日本語)",
                "🇰🇷 韓語 (한국어)",
                "🇻🇳 越南語 (Tiếng Việt)",
                "🇮🇩 印尼語 (Bahasa Indonesia)",
                "🇪🇸 西班牙語 (Español)",
                "🇫🇷 法語 (Français)",
                "🇩🇪 德語 (Deutsch)",
                "🇹🇭 泰語 (ภาษาไทย)",
                "🇵🇹 葡萄牙語 (Português)",
                "🇷🇺 俄語 (Русский)",
                "🇭🇰 粵語 / 廣東話 (Cantonese)",
                "🇹🇼 閩南語 / 台語 (Taiwanese Hokkien)"
        };
        final String[] values = {"zh-TW", "zh-CN", "en", "ja", "ko", "vi", "id", "es", "fr", "de", "th", "pt", "ru", "yue", "nan"};
        String current = AppConfig.getStudentLanguage(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                selected = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🗣️ Choose Student Native Language" : "🗣️ 選擇學生母語（對照翻譯語言）");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setStudentLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderHomePage();
                Toast.makeText(MainActivity.this, (en ? "Student native language set to: " : "學生母語已設定為：") + items[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showPersonaDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "☕ Daily Life & Hobbies (Casual chats, food, weekend plans)" : "☕ 日常閒聊 (Daily Life: 興趣、生活、週末規劃)",
                en ? "✈️ Travel & Dining (Airport, hotel check-in, ordering, asking directions)" : "✈️ 出國旅遊 (Travel & Dining: 機場、飯店、點餐、問路)",
                en ? "💼 Workplace & Business (Meetings, presentations, negotiations)" : "💼 職場商務 (Business: 會議匯報、專案討論、商務談判)",
                en ? "👔 Job Interview Simulation (STAR method, career, strengths/weaknesses)" : "👔 求職面試 (Job Interview: STAR 法則、工作經驗、面試應對)",
                en ? "🎯 Exam Prep Simulation (IELTS Speaking Part 2/3, TOEFL, TOEIC)" : "🎯 口說備考 (Exam Prep: 雅思 IELTS / 托福 TOEFL / 多益)",
                en ? "🛍️ Shopping & Returns (Bargaining, sizes, tax refund, returns)" : "🛍️ 購物退稅 (Shopping: 殺價、挑尺寸、退稅、商品換貨)",
                en ? "🏥 Doctor Visit & Medical (Describing symptoms, pharmacy, healthcare)" : "🏥 醫院看診 (Medical: 描述身體症狀、藥局拿藥、看診諮詢)",
                en ? "🏠 Renting & Housing (Apartment hunting, lease terms, landlord repairs)" : "🏠 租屋看房 (Housing: 預約看房、租約討論、水電修繕)",
                en ? "💬 Dating & Social Mingling (Icebreakers, casual banter, hobbies, parties)" : "💬 交友約會 (Dating & Social: 社交破冰、約會聊天、認識新朋友)",
                en ? "🤖 Tech & AI Trends (Software development, LLMs, future tech trends)" : "🤖 科技趨勢 (Tech & AI: 軟體開發、人工智慧、科技創新)"
        };
        final String[] values = {"daily", "travel", "business", "interview", "exam", "shopping", "medical", "housing", "dating", "tech"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🎭 Choose Conversation Scenario" : "🎭 選擇實戰情境劇本");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTutorPersona(MainActivity.this, values[which]);
                renderHomePage();
            }
        });
        builder.show();
    }

    private void showTeachingModeDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "✨ Bilingual Scaffolding (Target Language + Instant Native Translation, Recommended)" : "✨ 雙語對照模式 (外語說完立即附帶中文口譯，推薦)",
                en ? "📖 Reading & Pronunciation Coach (Read aloud & AI actively interrupts to correct pronunciation)" : "📖 朗讀糾音教練 (邊朗讀邊糾錯，發音/重音有誤時 AI 即時插話糾正)",
                en ? "🌱 Beginner Coaching (One phrase at a time + Repeat after tutor with native guidance)" : "🌱 零基礎引導模式 (一句母語說明 + 一句短句示範帶讀)",
                en ? "🌊 Full Immersion (100% Target Language, for intermediate/advanced learners)" : "🌊 全外語沉浸模式 (100% 全外語對談，適合進階練習)"
        };
        final String[] values = {"bilingual", "shadowing", "beginner", "immersion"};
        String current = AppConfig.getTeachingMode(this);
        int selected = "shadowing".equals(current) ? 1 : ("beginner".equals(current) ? 2 : ("immersion".equals(current) ? 3 : 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "💡 Choose Teaching Method & Level" : "💡 選擇教學引導模式 (難易度)");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTeachingMode(MainActivity.this, values[which]);
                dialog.dismiss();
                renderHomePage();
                Toast.makeText(MainActivity.this, en ? "Teaching mode updated" : "已切換教學模式", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showInterruptionShieldDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "🛡️ Heavy Shield (Recommended for loudspeaker)" : "🛡️ 高強度防打斷 (不易被外放迴音或周圍雜音打斷，推薦揚聲器外放)",
                en ? "⚖️ Standard Balanced (Normal speaking allows interruption)" : "⚖️ 標準平衡模式 (預設，正常說話可插話)",
                en ? "⚡ Fast Interruption (Ideal for earphones/quiet rooms)" : "⚡ 靈敏插話模式 (適合安靜環境或耳機，微弱聲音即插話)"
        };
        final int[] sensitivities = {20, 50, 80};
        int current = AppConfig.getInterruptionSensitivity(this);
        int selected = current <= 25 ? 0 : (current >= 70 ? 2 : 1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🛡️ Interruption Shield & Sensitivity" : "🛡️ 插話防護與靈敏度");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setInterruptionSensitivity(MainActivity.this, sensitivities[which]);
                dialog.dismiss();
                renderHomePage();
                Toast.makeText(MainActivity.this, en ? "Interruption sensitivity updated" : "已更新防打斷靈敏度", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private int currentVoiceFilterTab = 0; // 0: All, 1: Female, 2: Male
    private TextToSpeech previewTts;

    public static class VoiceInfo {
        public final String name;
        public final boolean isFemale;
        public final String zhDesc;
        public final String enDesc;
        public final float pitch;

        public VoiceInfo(String name, boolean isFemale, String zhDesc, String enDesc, float pitch) {
            this.name = name;
            this.isFemale = isFemale;
            this.zhDesc = zhDesc;
            this.enDesc = enDesc;
            this.pitch = pitch;
        }
    }

    public static final VoiceInfo[] ALL_VOICES = new VoiceInfo[]{
        // Female (15)
        new VoiceInfo("Kore", true, "自然放鬆 · 預設推薦", "Relaxed & Natural · Recommended", 1.15f),
        new VoiceInfo("Aoede", true, "清澈優雅 · 溫柔細膩", "Breathy & Gentle", 1.18f),
        new VoiceInfo("Leda", true, "年輕活潑 · 朝氣蓬勃", "Youthful & Bright", 1.25f),
        new VoiceInfo("Callisto", true, "沉著清晰 · 俐落流暢", "Smooth & Articulate", 1.05f),
        new VoiceInfo("Europa", true, "活力親切 · 陽光開朗", "Energetic & Friendly", 1.20f),
        new VoiceInfo("Io", true, "俐落敏銳 · 熱情自信", "Crisp & Enthusiastic", 1.22f),
        new VoiceInfo("Rhea", true, "溫暖包容 · 慈祥親和", "Warm & Supportive", 1.02f),
        new VoiceInfo("Dione", true, "輕柔安撫 · 靜謐舒緩", "Soft & Reassuring", 1.10f),
        new VoiceInfo("Tethys", true, "靈動生動 · 抑揚頓挫", "Vibrant & Animated", 1.16f),
        new VoiceInfo("Ariel", true, "歡快輕盈 · 清新純淨", "Cheerful & Light", 1.28f),
        new VoiceInfo("Miranda", true, "真誠細膩 · 娓娓道來", "Friendly & Expressive", 1.12f),
        new VoiceInfo("Sycorax", true, "氣場強大 · 自信威嚴", "Expressive & Commanding", 0.98f),
        new VoiceInfo("Titania", true, "優雅華貴 · 典雅端莊", "Luminous & Graceful", 1.14f),
        new VoiceInfo("Despina", true, "明亮敏捷 · 節奏輕快", "Bright & Agile", 1.24f),
        new VoiceInfo("Galatea", true, "柔和流暢 · 舒適悅耳", "Gentle & Flowing", 1.08f),

        // Male (15)
        new VoiceInfo("Puck", false, "活力俏皮 · 幽默隨和", "Playful & Engaging", 0.95f),
        new VoiceInfo("Charon", false, "沉穩專業 · 冷靜自信", "Deep & Confident", 0.80f),
        new VoiceInfo("Fenrir", false, "磁性堅定 · 威嚴有力", "Authoritative & Strong", 0.75f),
        new VoiceInfo("Orus", false, "沉著清晰 · 條理分明", "Firm & Clear", 0.88f),
        new VoiceInfo("Zephyr", false, "溫暖平靜 · 撫慰人心", "Warm & Calm", 0.92f),
        new VoiceInfo("Ganymede", false, "醇厚穩重 · 磁性迷人", "Rich & Deep", 0.78f),
        new VoiceInfo("Titan", false, "渾厚有力 · 磅礴大氣", "Resonant & Powerful", 0.72f),
        new VoiceInfo("Hyperion", false, "朝氣蓬勃 · 積極果斷", "Dynamic & Energetic", 0.96f),
        new VoiceInfo("Iapetus", false, "踏實沉著 · 值得信賴", "Grounded & Measured", 0.82f),
        new VoiceInfo("Enceladus", false, "健談親近 · 鄰家隨和", "Conversational & Warm", 0.90f),
        new VoiceInfo("Mimas", false, "靈活好奇 · 輕快幽默", "Curious & Lively", 1.00f),
        new VoiceInfo("Aegaeon", false, "深沉撫慰 · 靜心放鬆", "Deep & Soothing", 0.76f),
        new VoiceInfo("Umbriel", false, "深邃靜謐 · 哲思冷靜", "Reflective & Calm", 0.84f),
        new VoiceInfo("Caliban", false, "果斷直率 · 剛毅堅強", "Bold & Direct", 0.78f),
        new VoiceInfo("Prospero", false, "智慧博學 · 沉著大方", "Wise & Articulate", 0.86f)
    };

    private String getVoiceDisplayName(String voiceName, boolean en) {
        if (voiceName == null || voiceName.isEmpty()) voiceName = AppConfig.DEFAULT_VOICE;
        for (VoiceInfo v : ALL_VOICES) {
            if (v.name.equalsIgnoreCase(voiceName)) {
                return (v.isFemale ? "👩 " : "👨 ") + v.name + " · " + (en ? v.enDesc : v.zhDesc);
            }
        }
        return "👩 " + voiceName;
    }

    private void playAudition(VoiceInfo voice) {
        if (voice == null) return;
        if (previewTts == null) {
            final VoiceInfo target = voice;
            previewTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        speakVoiceSample(target);
                    }
                }
            });
        } else {
            speakVoiceSample(voice);
        }
    }

    private void speakVoiceSample(VoiceInfo voice) {
        if (previewTts == null || voice == null) return;
        try {
            previewTts.stop();
            previewTts.setPitch(voice.pitch);
            previewTts.setSpeechRate(1.0f);
            if (I18n.isEnglish(this)) {
                previewTts.setLanguage(Locale.US);
                previewTts.speak("Hello! I am " + voice.name + ", your 1-on-1 language tutor. Let's practice speaking!", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            } else {
                previewTts.setLanguage(Locale.TRADITIONAL_CHINESE);
                previewTts.speak("你好！我是 " + voice.name + "，你的 AI 一對一口語導師，讓我們開始練習吧！", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            }
        } catch (Exception ignored) {}
    }

    private void showVoicePersonaDialog() {
        final boolean en = I18n.isEnglish(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(10));
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);

        TextView titleView = new TextView(this);
        titleView.setText(en ? "🗣️ Select Tutor Voice (30 Voices)" : "🗣️ 導師語音音色選擇 (全 30 款)");
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(CrewTheme.TEXT_PRIMARY);
        titleView.setPadding(0, 0, 0, dp(4));
        root.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(en ? "Tap '▶️ Play' to preview pitch/tone. Tap card to select." : "點擊「▶️ 試聽」可播放聲音，點擊卡片直接選用。");
        subtitleView.setTextSize(11);
        subtitleView.setTextColor(CrewTheme.TEXT_SECONDARY);
        subtitleView.setPadding(0, 0, 0, dp(12));
        root.addView(subtitleView);

        // Filter Tabs Row
        final LinearLayout tabsRow = new LinearLayout(this);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setPadding(0, 0, 0, dp(10));
        root.addView(tabsRow);

        final String currentVoice = AppConfig.getVoiceName(this);
        ScrollView scrollList = new ScrollView(this);
        scrollList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(340)));
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollList.addView(listContainer);
        root.addView(scrollList);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        final Runnable refreshList = new Runnable() {
            @Override public void run() {
                listContainer.removeAllViews();
                for (int i = 0; i < ALL_VOICES.length; i++) {
                    final VoiceInfo voice = ALL_VOICES[i];
                    if (currentVoiceFilterTab == 1 && !voice.isFemale) continue;
                    if (currentVoiceFilterTab == 2 && voice.isFemale) continue;

                    final boolean isSelected = voice.name.equalsIgnoreCase(currentVoice);

                    LinearLayout itemCard = new LinearLayout(MainActivity.this);
                    itemCard.setOrientation(LinearLayout.HORIZONTAL);
                    itemCard.setGravity(Gravity.CENTER_VERTICAL);
                    itemCard.setPadding(dp(12), dp(10), dp(10), dp(10));
                    int cardBg = isSelected ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A");
                    int borderCol = isSelected ? CrewTheme.CYAN_400 : Color.parseColor("#334155");
                    GradientDrawable itemBg = new GradientDrawable();
                    itemBg.setColor(cardBg);
                    itemBg.setCornerRadius(dp(12));
                    itemBg.setStroke(dp(isSelected ? 2 : 1), borderCol);
                    itemCard.setBackground(itemBg);

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    cardLp.setMargins(0, 0, 0, dp(8));
                    itemCard.setLayoutParams(cardLp);

                    // Indicator
                    TextView indicator = new TextView(MainActivity.this);
                    indicator.setText(isSelected ? "●" : "○");
                    indicator.setTextSize(14);
                    indicator.setTextColor(isSelected ? CrewTheme.CYAN_400 : CrewTheme.TEXT_MUTED);
                    indicator.setPadding(0, 0, dp(10), 0);
                    itemCard.addView(indicator);

                    // Text Info
                    LinearLayout infoCol = new LinearLayout(MainActivity.this);
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    itemCard.addView(infoCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                    TextView nameView = new TextView(MainActivity.this);
                    nameView.setText((voice.isFemale ? "👩 " : "👨 ") + voice.name);
                    nameView.setTextSize(13);
                    nameView.setTypeface(Typeface.DEFAULT_BOLD);
                    nameView.setTextColor(isSelected ? CrewTheme.CYAN_400 : CrewTheme.TEXT_PRIMARY);
                    infoCol.addView(nameView);

                    TextView descView = new TextView(MainActivity.this);
                    descView.setText(en ? voice.enDesc : voice.zhDesc);
                    descView.setTextSize(10);
                    descView.setTextColor(CrewTheme.TEXT_SECONDARY);
                    descView.setPadding(0, dp(2), 0, 0);
                    infoCol.addView(descView);

                    // Audition Button
                    Button previewBtn = new Button(MainActivity.this);
                    previewBtn.setText("▶️ " + (en ? "Play" : "試聽"));
                    previewBtn.setTextSize(11);
                    previewBtn.setTextColor(Color.WHITE);
                    previewBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    previewBtn.setAllCaps(false);
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#1E293B"));
                    pBg.setCornerRadius(dp(8));
                    pBg.setStroke(dp(1), Color.parseColor("#475569"));
                    previewBtn.setBackground(pBg);
                    previewBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
                    previewBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            playAudition(voice);
                        }
                    });
                    itemCard.addView(previewBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));

                    // Click item to select
                    itemCard.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            AppConfig.setVoiceName(MainActivity.this, voice.name);
                            Toast.makeText(MainActivity.this, (en ? "✅ Switched voice to: " : "✅ 已選用音色：") + voice.name, Toast.LENGTH_SHORT).show();
                            if (dialogRef[0] != null) dialogRef[0].dismiss();
                            renderHomePage();
                        }
                    });

                    listContainer.addView(itemCard);
                }
            }
        };

        // Filter Tabs
        final String[] tabLabels = new String[]{
                en ? "🌟 All (30)" : "🌟 全部 (30)",
                en ? "👩 Female (15)" : "👩 女性 (15)",
                en ? "👨 Male (15)" : "👨 男性 (15)"
        };

        final Button[] tabButtons = new Button[3];
        for (int t = 0; t < 3; t++) {
            final int tabIndex = t;
            Button tabBtn = new Button(this);
            tabBtn.setText(tabLabels[t]);
            tabBtn.setTextSize(10);
            tabBtn.setAllCaps(false);
            tabButtons[t] = tabBtn;

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(32), 1f);
            if (t > 0) tlp.setMargins(dp(4), 0, 0, 0);
            tabBtn.setLayoutParams(tlp);

            tabBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    currentVoiceFilterTab = tabIndex;
                    for (int i = 0; i < 3; i++) {
                        boolean active = i == currentVoiceFilterTab;
                        GradientDrawable tabBg = new GradientDrawable();
                        tabBg.setColor(active ? Color.parseColor("#0284C7") : Color.parseColor("#1E293B"));
                        tabBg.setCornerRadius(dp(8));
                        tabButtons[i].setBackground(tabBg);
                        tabButtons[i].setTextColor(Color.WHITE);
                    }
                    refreshList.run();
                }
            });
            tabsRow.addView(tabBtn);
        }

        // Initialize active tab styling
        for (int i = 0; i < 3; i++) {
            boolean active = i == currentVoiceFilterTab;
            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setColor(active ? Color.parseColor("#0284C7") : Color.parseColor("#1E293B"));
            tabBg.setCornerRadius(dp(8));
            tabButtons[i].setBackground(tabBg);
            tabButtons[i].setTextColor(Color.WHITE);
        }

        refreshList.run();

        builder.setView(root);
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        dialogRef[0] = builder.show();
    }

    private void speakTts(final String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String clean = text.trim();
        if (previewTts == null) {
            previewTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && previewTts != null) {
                        previewTts.setLanguage(Locale.US);
                        previewTts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_" + System.currentTimeMillis());
                    }
                }
            });
        } else {
            try {
                previewTts.stop();
                previewTts.setLanguage(Locale.US);
                previewTts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_" + System.currentTimeMillis());
            } catch (Exception ignored) {}
        }
    }

    private void showStarredPhrasebookDialog() {
        final boolean en = I18n.isEnglish(this);
        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#D0000000")));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(16), dp(24), dp(16), dp(24));

        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(20));
        cBg.setStroke(dp(1), Color.parseColor("#334155"));
        container.setBackground(cBg);

        final Runnable renderList = new Runnable() {
            @Override public void run() {
                container.removeAllViews();

                // Header
                LinearLayout headRow = new LinearLayout(MainActivity.this);
                headRow.setOrientation(LinearLayout.HORIZONTAL);
                headRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView title = new TextView(MainActivity.this);
                title.setText(en ? "⭐ My Starred Phrasebook" : "⭐ 我的個人生詞與金句本");
                title.setTextSize(16);
                title.setTextColor(Color.WHITE);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                headRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView closeBtn = new TextView(MainActivity.this);
                closeBtn.setText("✕");
                closeBtn.setTextSize(18);
                closeBtn.setTextColor(Color.parseColor("#94A3B8"));
                closeBtn.setPadding(dp(10), dp(4), dp(4), dp(4));
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                        renderHomePage();
                    }
                });
                headRow.addView(closeBtn);
                container.addView(headRow);

                final List<LearningDataManager.StarredItem> items = LearningDataManager.getStarredItems(MainActivity.this);

                TextView subtitle = new TextView(MainActivity.this);
                subtitle.setText(en ? ("Collected " + items.size() + " items · Tap 🔊 to listen to authentic pronunciation")
                        : ("累計收藏 " + items.size() + " 條精選金句與生詞 · 點擊 🔊 聽母語發音"));
                subtitle.setTextSize(11);
                subtitle.setTextColor(Color.parseColor("#94A3B8"));
                subtitle.setPadding(0, dp(4), 0, dp(10));
                container.addView(subtitle);

                if (!items.isEmpty()) {
                    Button quizBtn = new Button(MainActivity.this);
                    quizBtn.setText(en ? "🃏 30s Flashcard Quiz Game" : "🃏 30 秒抽卡自測小遊戲");
                    quizBtn.setTextSize(12);
                    quizBtn.setTextColor(Color.WHITE);
                    quizBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable qbg = new GradientDrawable();
                    qbg.setColor(Color.parseColor("#4F46E5"));
                    qbg.setCornerRadius(dp(10));
                    quizBtn.setBackground(qbg);
                    quizBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            OralCoachHelper.showFlashcardQuizDialog(MainActivity.this);
                        }
                    });
                    LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
                    qlp.setMargins(0, 0, 0, dp(12));
                    quizBtn.setLayoutParams(qlp);
                    container.addView(quizBtn);
                }

                if (items.isEmpty()) {
                    LinearLayout emptyCard = new LinearLayout(MainActivity.this);
                    emptyCard.setOrientation(LinearLayout.VERTICAL);
                    emptyCard.setPadding(dp(20), dp(24), dp(20), dp(24));
                    emptyCard.setGravity(Gravity.CENTER);
                    GradientDrawable eBg = new GradientDrawable();
                    eBg.setColor(Color.parseColor("#1E293B"));
                    eBg.setCornerRadius(dp(14));
                    emptyCard.setBackground(eBg);

                    TextView eIcon = new TextView(MainActivity.this);
                    eIcon.setText("⭐");
                    eIcon.setTextSize(32);
                    emptyCard.addView(eIcon);

                    TextView eText = new TextView(MainActivity.this);
                    eText.setText(en ? "No starred items yet.\nDuring conversation practice or in session diagnostic reports, tap ⭐ to collect practical sentences here!"
                            : "尚無收藏項目。\n在口語對話教室或課後 AI 診斷報告中，點擊 ★ 即可收藏至專屬生詞本！");
                    eText.setTextSize(12);
                    eText.setTextColor(Color.parseColor("#94A3B8"));
                    eText.setGravity(Gravity.CENTER);
                    eText.setLineSpacing(dp(3), 1.2f);
                    eText.setPadding(0, dp(8), 0, 0);
                    emptyCard.addView(eText);

                    container.addView(emptyCard);
                } else {
                    for (final LearningDataManager.StarredItem item : items) {
                        LinearLayout itemCard = new LinearLayout(MainActivity.this);
                        itemCard.setOrientation(LinearLayout.VERTICAL);
                        itemCard.setPadding(dp(14), dp(12), dp(14), dp(12));
                        GradientDrawable iBg = new GradientDrawable();
                        iBg.setColor(Color.parseColor("#1E293B"));
                        iBg.setCornerRadius(dp(12));
                        iBg.setStroke(dp(1), Color.parseColor("#334155"));
                        itemCard.setBackground(iBg);
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        ilp.setMargins(0, 0, 0, dp(10));
                        itemCard.setLayoutParams(ilp);

                        // Tag & Action row
                        LinearLayout tagRow = new LinearLayout(MainActivity.this);
                        tagRow.setOrientation(LinearLayout.HORIZONTAL);
                        tagRow.setGravity(Gravity.CENTER_VERTICAL);

                        TextView tagTv = new TextView(MainActivity.this);
                        String catLabel = "correction".equals(item.category) ? (en ? "✨ Native Recast" : "✨ 道地糾錯")
                                : ("hint".equals(item.category) ? (en ? "💡 Reply Hint" : "💡 回答小抄")
                                : ("vocab".equals(item.category) ? (en ? "📖 Vocabulary" : "📖 重點單字") : (en ? "💬 Phrase" : "💬 實用金句")));
                        tagTv.setText(catLabel);
                        tagTv.setTextSize(10);
                        tagTv.setTextColor("correction".equals(item.category) ? Color.parseColor("#F472B6") : Color.parseColor("#FBBF24"));
                        tagTv.setTypeface(Typeface.DEFAULT_BOLD);
                        tagRow.addView(tagTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                        Button playBtn = new Button(MainActivity.this);
                        playBtn.setText("🔊");
                        playBtn.setTextSize(11);
                        playBtn.setTextColor(Color.WHITE);
                        GradientDrawable pbBg = new GradientDrawable();
                        pbBg.setColor(Color.parseColor("#4F46E5"));
                        pbBg.setCornerRadius(dp(6));
                        playBtn.setBackground(pbBg);
                        playBtn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) { speakTts(item.originalText); }
                        });
                        tagRow.addView(playBtn, new LinearLayout.LayoutParams(dp(42), dp(28)));

                        Button delBtn = new Button(MainActivity.this);
                        delBtn.setText("🗑️");
                        delBtn.setTextSize(11);
                        delBtn.setTextColor(Color.WHITE);
                        GradientDrawable dbBg = new GradientDrawable();
                        dbBg.setColor(Color.parseColor("#334155"));
                        dbBg.setCornerRadius(dp(6));
                        delBtn.setBackground(dbBg);
                        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(38), dp(28));
                        dlp.setMargins(dp(6), 0, 0, 0);
                        delBtn.setLayoutParams(dlp);
                        delBtn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                LearningDataManager.removeStarredItemById(MainActivity.this, item.id);
                                run();
                            }
                        });
                        tagRow.addView(delBtn);

                        itemCard.addView(tagRow);

                        // Original text
                        TextView textTv = new TextView(MainActivity.this);
                        textTv.setText(item.originalText);
                        textTv.setTextSize(14);
                        textTv.setTextColor(Color.WHITE);
                        textTv.setTypeface(Typeface.DEFAULT_BOLD);
                        textTv.setPadding(0, dp(4), 0, dp(2));
                        itemCard.addView(textTv);

                        // Translation
                        if (!item.translation.isEmpty()) {
                            TextView transTv = new TextView(MainActivity.this);
                            transTv.setText(item.translation);
                            transTv.setTextSize(12);
                            transTv.setTextColor(Color.parseColor("#94A3B8"));
                            transTv.setPadding(0, 0, 0, dp(2));
                            itemCard.addView(transTv);
                        }

                        // Notes
                        if (!item.notes.isEmpty()) {
                            TextView notesTv = new TextView(MainActivity.this);
                            notesTv.setText("💡 " + item.notes);
                            notesTv.setTextSize(11);
                            notesTv.setTextColor(Color.parseColor("#A5B4FC"));
                            itemCard.addView(notesTv);
                        }

                        container.addView(itemCard);
                    }
                }
            }
        };

        renderList.run();
        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private void showSessionHistoryDialog() {
        final boolean en = I18n.isEnglish(this);
        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#D0000000")));
        }

        ScrollView scroll = new ScrollView(this);
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

        // Header
        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(en ? "📊 Session History & Diagnostics" : "📊 歷史對話與成效報告");
        title.setTextSize(16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final List<LearningDataManager.SessionRecord> history = LearningDataManager.getSessionHistory(this);

        if (!history.isEmpty()) {
            TextView clearBtn = new TextView(this);
            clearBtn.setText(en ? "🗑️ Clear" : "🗑️ 清空");
            clearBtn.setTextSize(12);
            clearBtn.setTextColor(Color.parseColor("#F87171"));
            clearBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(en ? "Clear Session History" : "清空歷史記錄")
                            .setMessage(en ? "Are you sure you want to delete all past tutoring records?" : "確定要清除所有過往口語對話歷史記錄嗎？")
                            .setPositiveButton(en ? "Clear" : "確認清空", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int which) {
                                    LearningDataManager.clearSessionHistory(MainActivity.this);
                                    dialog.dismiss();
                                    showSessionHistoryDialog();
                                }
                            })
                            .setNegativeButton(en ? "Cancel" : "取消", null)
                            .show();
                }
            });
            headRow.addView(clearBtn);
        }

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

        TextView subtitle = new TextView(this);
        subtitle.setText(en ? ("Total " + history.size() + " completed tutoring sessions · Tap any card to review full report")
                : ("累計 " + history.size() + " 堂完成對話 · 點擊任一卡片可隨時重溫完整診斷報告"));
        subtitle.setTextSize(11);
        subtitle.setTextColor(Color.parseColor("#94A3B8"));
        subtitle.setPadding(0, dp(4), 0, dp(12));
        container.addView(subtitle);

        if (history.isEmpty()) {
            LinearLayout emptyCard = new LinearLayout(this);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setPadding(dp(20), dp(24), dp(20), dp(24));
            emptyCard.setGravity(Gravity.CENTER);
            GradientDrawable eBg = new GradientDrawable();
            eBg.setColor(Color.parseColor("#1E293B"));
            eBg.setCornerRadius(dp(14));
            emptyCard.setBackground(eBg);

            TextView eIcon = new TextView(this);
            eIcon.setText("📊");
            eIcon.setTextSize(32);
            emptyCard.addView(eIcon);

            TextView eText = new TextView(this);
            eText.setText(en ? "No tutoring sessions yet.\nStart a practice session from home to generate your first AI diagnostic report!"
                    : "尚無課堂記錄。\n從首頁點擊開始口語對話，完成後將自動生成專屬學習成效診斷報告！");
            eText.setTextSize(12);
            eText.setTextColor(Color.parseColor("#94A3B8"));
            eText.setGravity(Gravity.CENTER);
            eText.setLineSpacing(dp(3), 1.2f);
            eText.setPadding(0, dp(8), 0, 0);
            emptyCard.addView(eText);

            container.addView(emptyCard);
        } else {
            for (final LearningDataManager.SessionRecord record : history) {
                LinearLayout itemCard = new LinearLayout(this);
                itemCard.setOrientation(LinearLayout.VERTICAL);
                itemCard.setPadding(dp(14), dp(12), dp(14), dp(12));
                GradientDrawable iBg = new GradientDrawable();
                iBg.setColor(Color.parseColor("#1E293B"));
                iBg.setCornerRadius(dp(12));
                iBg.setStroke(dp(1), Color.parseColor("#334155"));
                itemCard.setBackground(iBg);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ilp.setMargins(0, 0, 0, dp(10));
                itemCard.setLayoutParams(ilp);
                itemCard.setClickable(true);
                itemCard.setFocusable(true);

                // Top row: Scenario + Score
                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView scenTv = new TextView(this);
                scenTv.setText(getPersonaLabel(record.scenario, en));
                scenTv.setTextSize(13);
                scenTv.setTextColor(Color.parseColor("#38BDF8"));
                scenTv.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(scenTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView scoreBadge = new TextView(this);
                scoreBadge.setText("🎯 " + record.overallScore + " 分");
                scoreBadge.setTextSize(12);
                scoreBadge.setTextColor(record.overallScore >= 85 ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24"));
                scoreBadge.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(scoreBadge);

                itemCard.addView(topRow);

                // Meta row: Date + duration + turns
                int mins = record.durationSeconds / 60;
                int secs = record.durationSeconds % 60;
                String timeStr = mins > 0 ? (mins + "分" + secs + "秒") : (secs + "秒");
                TextView metaTv = new TextView(this);
                metaTv.setText(record.dateString + " · ⏱️ " + timeStr + " · 💬 " + record.userTurns + " 輪對話");
                metaTv.setTextSize(11);
                metaTv.setTextColor(Color.parseColor("#94A3B8"));
                metaTv.setPadding(0, dp(2), 0, dp(4));
                itemCard.addView(metaTv);

                // Summary snippet
                if (!record.summary.isEmpty()) {
                    TextView sumTv = new TextView(this);
                    sumTv.setText(record.summary);
                    sumTv.setTextSize(12);
                    sumTv.setTextColor(Color.parseColor("#E2E8F0"));
                    sumTv.setMaxLines(2);
                    sumTv.setEllipsize(TextUtils.TruncateAt.END);
                    sumTv.setPadding(0, 0, 0, dp(4));
                    itemCard.addView(sumTv);
                }

                // View Details link
                TextView viewDetailTv = new TextView(this);
                viewDetailTv.setText(en ? "Tap to view full diagnostic report ›" : "點擊查看完整診斷報告與生詞 ›");
                viewDetailTv.setTextSize(11);
                viewDetailTv.setTextColor(Color.parseColor("#818CF8"));
                viewDetailTv.setGravity(Gravity.RIGHT);
                itemCard.addView(viewDetailTv);

                itemCard.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showSessionReportDialog(record);
                    }
                });

                container.addView(itemCard);
            }
        }

        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    public void showSessionReportDialog(final LearningDataManager.SessionRecord record) {
        if (record == null || isFinishing()) return;
        final boolean en = I18n.isEnglish(this);

        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#D0000000")));
        }

        ScrollView scroll = new ScrollView(this);
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
            JSONArray recasts = new JSONArray(record.recastsJson);
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
                        @Override public void onClick(View v) { speakTts(corr); }
                    });
                    corRow.addView(playBtn, new LinearLayout.LayoutParams(dp(42), dp(28)));

                    // Star button
                    final boolean isSt = LearningDataManager.isStarred(MainActivity.this, corr);
                    final Button starBtn = new Button(this);
                    starBtn.setText(isSt ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isSt ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(MainActivity.this, corr, orig, "correction", expl);
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(MainActivity.this, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    corRow.addView(starBtn, new LinearLayout.LayoutParams(dp(36), dp(28)));
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
            JSONArray takeaways = new JSONArray(record.takeawaysJson);
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
                        @Override public void onClick(View v) { speakTts(phrase); }
                    });
                    tkCard.addView(pBtn, new LinearLayout.LayoutParams(dp(42), dp(28)));

                    final boolean isStarred = LearningDataManager.isStarred(MainActivity.this, phrase);
                    final Button starBtn = new Button(this);
                    starBtn.setText(isStarred ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(MainActivity.this, phrase, trans, "phrase", "");
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(MainActivity.this, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    tkCard.addView(starBtn, new LinearLayout.LayoutParams(dp(36), dp(28)));

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

        Button starAllBtn = new Button(this);
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
                int count = 0;
                try {
                    JSONArray recasts = new JSONArray(record.recastsJson);
                    for (int i = 0; i < recasts.length(); i++) {
                        JSONObject rc = recasts.getJSONObject(i);
                        String corr = rc.optString("corrected", "");
                        String orig = rc.optString("original", "");
                        if (!corr.isEmpty() && !LearningDataManager.isStarred(MainActivity.this, corr)) {
                            LearningDataManager.toggleStarItem(MainActivity.this, corr, orig, "correction", "");
                            count++;
                        }
                    }
                    JSONArray takeaways = new JSONArray(record.takeawaysJson);
                    for (int i = 0; i < takeaways.length(); i++) {
                        JSONObject tk = takeaways.getJSONObject(i);
                        String phrase = tk.optString("phrase", "");
                        String trans = tk.optString("translation", "");
                        if (!phrase.isEmpty() && !LearningDataManager.isStarred(MainActivity.this, phrase)) {
                            LearningDataManager.toggleStarItem(MainActivity.this, phrase, trans, "phrase", "");
                            count++;
                        }
                    }
                } catch (Exception ignored) {}
                Toast.makeText(MainActivity.this, en ? ("⭐ " + count + " items added to Phrasebook!") : ("⭐ 已收藏 " + count + " 條精選金句至生詞本！"), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        if (previewTts != null) {
            try { previewTts.stop(); previewTts.shutdown(); } catch (Exception ignored) {}
            previewTts = null;
        }
        super.onDestroy();
    }
}
