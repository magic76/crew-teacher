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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 201;

    private int currentMainTab = 0; // 0: Practice (對話教練), 1: Settings (偏好設定)
    private final Stack<Integer> tabHistory = new Stack<Integer>();

    private TextView statusDot;
    private TextView statusText;
    private TextView statusDetail;
    private LinearLayout statusCard;
    private LinearLayout pageContent;
    private LinearLayout bottomNav;

    private int dp(float val) {
        return CrewTheme.dp(this, val);
    }

    private void switchTab(int targetTab, boolean addToHistory) {
        if (currentMainTab != targetTab) {
            if (addToHistory) {
                tabHistory.push(currentMainTab);
            }
            currentMainTab = targetTab;
            renderCurrentPage();
        }
    }

    @Override
    public void onBackPressed() {
        if (!tabHistory.isEmpty()) {
            int prevTab = tabHistory.pop();
            switchTab(prevTab, false);
        } else if (currentMainTab != 0) {
            switchTab(0, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(CrewTheme.BG_PRIMARY);
            getWindow().setNavigationBarColor(CrewTheme.BG_PRIMARY);
        }
        getWindow().getDecorView().setBackgroundColor(CrewTheme.BG_PRIMARY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Scrollable Content Area
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CrewTheme.BG_PRIMARY);

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(18), dp(24), dp(18), dp(20));
        scroll.addView(pageContent);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 2. Bottom Navigation Tab Bar (Modern Capsule Dock)
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(14), dp(8), dp(14), dp(10));
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(Color.parseColor("#090D16")); // Deep Slate / Black
        navBg.setStroke(dp(1), Color.parseColor("#1E293B"));
        bottomNav.setBackground(navBg);
        root.addView(bottomNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        setContentView(root);
        renderCurrentPage();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        if (AppConfig.getGeminiApiKey(this).isEmpty() && !isFinishing()) {
            pageContent.post(new Runnable() {
                @Override public void run() {
                    if (AppConfig.getGeminiApiKey(MainActivity.this).isEmpty() && !isFinishing()) {
                        WelcomeGuideDialog.show(MainActivity.this, new Runnable() {
                            @Override public void run() {
                                renderCurrentPage();
                            }
                        });
                    }
                }
            });
        }
    }

    private void renderCurrentPage() {
        pageContent.removeAllViews();
        final boolean en = I18n.isEnglish(this);

        // Render Bottom Navigation Tabs
        renderBottomNav(en);

        if (currentMainTab == 0) {
            renderPracticePage(en);
        } else {
            renderSettingsPage(en);
        }
    }

    private void renderBottomNav(final boolean en) {
        if (bottomNav == null) return;
        bottomNav.removeAllViews();

        bottomNav.addView(buildTabItem("●", en ? "Practice" : "練習", 0, currentMainTab == 0));
        bottomNav.addView(buildTabItem("⚙", en ? "Settings" : "設定", 1, currentMainTab == 1));
    }

    private LinearLayout buildTabItem(String icon, String label, final int tabIndex, boolean active) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(16), dp(10), dp(16), dp(10));

        GradientDrawable tBg = new GradientDrawable();
        tBg.setCornerRadius(dp(14));
        if (active) {
            tBg.setColor(Color.parseColor("#1E293B"));
            tBg.setStroke(dp(1), Color.parseColor("#6366F1")); // Indigo Accent
        } else {
            tBg.setColor(Color.parseColor("#0F172A"));
            tBg.setStroke(dp(1), Color.parseColor("#1E293B"));
        }
        tab.setBackground(tBg);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(16);
        iconTv.setPadding(0, 0, dp(8), 0);
        tab.addView(iconTv);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(13);
        labelTv.setTextColor(active ? Color.parseColor("#38BDF8") : Color.parseColor("#94A3B8"));
        labelTv.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tab.addView(labelTv);

        if (active) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(8);
            dot.setTextColor(Color.parseColor("#38BDF8"));
            dot.setPadding(dp(6), 0, 0, 0);
            tab.addView(dot);
        }

        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab(tabIndex, true);
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(6), 0, dp(6), 0);
        tab.setLayoutParams(lp);
        return tab;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🌟 TAB 0: 對話教練 / Practice Hub
    // ══════════════════════════════════════════════════════════════════════════
    private void renderPracticePage(final boolean en) {
        // Product principle: launch practice in two taps; move diagnostics and tooling below the fold.
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));

        LinearLayout brandTextCol = new LinearLayout(this);
        brandTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(en ? "What do you want to practice?" : "今天想練什麼？");
        title.setTextSize(24);
        title.setTextColor(CrewTheme.TEXT_PRIMARY);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        brandTextCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(en ? "Start speaking first. Configure later." : "先開口，再調整。兩步內開始練習。 ");
        subtitle.setTextSize(12);
        subtitle.setTextColor(CrewTheme.TEXT_SECONDARY);
        subtitle.setPadding(0, dp(3), 0, 0);
        brandTextCol.addView(subtitle);
        headerRow.addView(brandTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final boolean bubbleActive = NativeLiveService.isActive() || FloatingBubbleManager.getInstance(this).isBubbleShowing();
        final TextView menuBtn = new TextView(this);
        menuBtn.setText("⋮");
        menuBtn.setTextSize(22);
        menuBtn.setTextColor(Color.parseColor("#CBD5E1"));
        menuBtn.setGravity(Gravity.CENTER);
        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setColor(Color.parseColor("#111827"));
        menuBg.setCornerRadius(dp(12));
        menuBg.setStroke(dp(1), Color.parseColor("#263244"));
        menuBtn.setBackground(menuBg);
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showTopHeaderMenu(v); }
        });
        headerRow.addView(menuBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
        pageContent.addView(headerRow);

        // Error-only setup banner. Healthy connection state stays out of the user's way.
        statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setPadding(dp(14), dp(11), dp(14), dp(11));
        statusDot = new TextView(this);
        statusDot.setText("●");
        statusDot.setTextSize(12);
        statusDot.setPadding(0, 0, dp(8), 0);
        statusCard.addView(statusDot);
        LinearLayout statusCol = new LinearLayout(this);
        statusCol.setOrientation(LinearLayout.VERTICAL);
        statusText = new TextView(this);
        statusText.setTextSize(12);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusCol.addView(statusText);
        statusDetail = new TextView(this);
        statusDetail.setTextSize(10);
        statusCol.addView(statusDetail);
        statusCard.addView(statusCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (AppConfig.getGeminiApiKey(this).isEmpty()) {
            LinearLayout.LayoutParams errorLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            errorLp.setMargins(0, dp(8), 0, dp(14));
            statusCard.setLayoutParams(errorLp);
            pageContent.addView(statusCard);
        }
        refreshStatus();

        // Primary action: free conversation.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(17), dp(18), dp(17));
        GradientDrawable heroBg = new GradientDrawable();
        heroBg.setColor(Color.parseColor("#172554"));
        heroBg.setCornerRadius(dp(20));
        heroBg.setStroke(dp(1), Color.parseColor("#3B82F6"));
        hero.setBackground(heroBg);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, dp(6), 0, dp(12));
        hero.setLayoutParams(heroLp);

        TextView heroEyebrow = new TextView(this);
        heroEyebrow.setText(en ? "QUICK START" : "快速開始");
        heroEyebrow.setTextSize(10);
        heroEyebrow.setTextColor(Color.parseColor("#93C5FD"));
        heroEyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        hero.addView(heroEyebrow);

        TextView heroTitle = new TextView(this);
        heroTitle.setText(en ? "Free conversation" : "自由對話");
        heroTitle.setTextSize(20);
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTypeface(Typeface.DEFAULT_BOLD);
        heroTitle.setPadding(0, dp(5), 0, dp(3));
        hero.addView(heroTitle);

        TextView heroBody = new TextView(this);
        heroBody.setText(en ? "Talk naturally with your AI tutor. Hints stay available when you need them." : "直接和 AI 老師開聊，需要時再叫出提詞，不先塞滿設定。 ");
        heroBody.setTextSize(12);
        heroBody.setTextColor(Color.parseColor("#CBD5E1"));
        hero.addView(heroBody);

        Button heroButton = new Button(this);
        heroButton.setText(en ? "Start speaking  →" : "開始說話  →");
        heroButton.setTextSize(14);
        heroButton.setTextColor(Color.WHITE);
        heroButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable heroButtonBg = new GradientDrawable();
        heroButtonBg.setColor(Color.parseColor("#2563EB"));
        heroButtonBg.setCornerRadius(dp(12));
        heroButton.setBackground(heroButtonBg);
        LinearLayout.LayoutParams heroButtonLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        heroButtonLp.setMargins(0, dp(14), 0, 0);
        heroButton.setLayoutParams(heroButtonLp);
        heroButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if ("shadowing".equals(AppConfig.getTeachingMode(MainActivity.this))) {
                    AppConfig.setTeachingMode(MainActivity.this, "bilingual");
                }
                startActivity(new Intent(MainActivity.this, NativeLiveActivity.class));
            }
        });
        hero.addView(heroButton);
        pageContent.addView(hero);

        TextView activityHeading = new TextView(this);
        activityHeading.setText(en ? "Choose an activity" : "選一種練習");
        activityHeading.setTextSize(13);
        activityHeading.setTextColor(Color.parseColor("#94A3B8"));
        activityHeading.setTypeface(Typeface.DEFAULT_BOLD);
        activityHeading.setPadding(0, dp(4), 0, dp(8));
        pageContent.addView(activityHeading);

        LinearLayout activityRow = new LinearLayout(this);
        activityRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout lessonPod = makePodItem("◎", en ? "Lesson" : "情境課程", en ? "Guided missions" : "任務式角色扮演", Color.parseColor("#60A5FA"), new View.OnClickListener() {
            @Override public void onClick(View v) { CourseMapDialog.show(MainActivity.this, null); }
        });
        activityRow.addView(lessonPod, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout readingPod = makePodItem("Aa", en ? "Pronunciation" : "朗讀糾音", en ? "Read · diagnose · retry" : "朗讀 · 診斷 · 重練", Color.parseColor("#A78BFA"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppConfig.setTeachingMode(MainActivity.this, "shadowing");
                startActivity(new Intent(MainActivity.this, NativeLiveActivity.class));
            }
        });
        LinearLayout.LayoutParams readingLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        readingLp.setMargins(dp(10), 0, 0, 0);
        activityRow.addView(readingPod, readingLp);
        pageContent.addView(activityRow);

        // Scenario becomes a lightweight activity selector instead of a system "mode".
        String persona = AppConfig.getTutorPersona(this);
        LinearLayout.LayoutParams scenarioLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scenarioLp.setMargins(0, dp(10), 0, dp(10));
        LinearLayout scenarioCard = makeActionCard("◌", en ? "Conversation scenario" : "對話情境",
                getPersonaLabel(persona, en) + (en ? " · Tap to change" : " · 點一下切換"),
                Color.parseColor("#38BDF8"), new View.OnClickListener() {
            @Override public void onClick(View v) { showPersonaDialog(); }
        });
        scenarioCard.setLayoutParams(scenarioLp);
        pageContent.addView(scenarioCard);

        // Compact progress: useful, but secondary to speaking.
        LearningDataManager.StreakInfo streak = LearningDataManager.getStreakInfo(this);
        int goalMin = LearningDataManager.getDailyGoalMinutes(this);
        int todayMin = streak.todayPracticeSeconds / 60;
        int pct = Math.min(100, Math.round((streak.todayPracticeSeconds * 100.0f) / Math.max(60.0f, goalMin * 60.0f)));

        LinearLayout progressCard = new LinearLayout(this);
        progressCard.setOrientation(LinearLayout.VERTICAL);
        progressCard.setPadding(dp(15), dp(13), dp(15), dp(13));
        GradientDrawable progressBg = new GradientDrawable();
        progressBg.setColor(Color.parseColor("#111827"));
        progressBg.setCornerRadius(dp(16));
        progressBg.setStroke(dp(1), Color.parseColor("#263244"));
        progressCard.setBackground(progressBg);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressLp.setMargins(0, 0, 0, dp(12));
        progressCard.setLayoutParams(progressLp);

        LinearLayout progressTop = new LinearLayout(this);
        progressTop.setOrientation(LinearLayout.HORIZONTAL);
        progressTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView today = new TextView(this);
        today.setText(en ? "Today" : "今天");
        today.setTextSize(12);
        today.setTextColor(Color.WHITE);
        today.setTypeface(Typeface.DEFAULT_BOLD);
        progressTop.addView(today, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView streakTv = new TextView(this);
        streakTv.setText((streak.streakDays > 0 ? "🔥 " + streak.streakDays : "○") + (en ? " day streak" : " 天連續"));
        streakTv.setTextSize(11);
        streakTv.setTextColor(Color.parseColor("#94A3B8"));
        progressTop.addView(streakTv);
        progressCard.addView(progressTop);

        TextView goalTv = new TextView(this);
        goalTv.setText(todayMin + " / " + goalMin + (en ? " min · " : " 分鐘 · ") + pct + "%");
        goalTv.setTextSize(11);
        goalTv.setTextColor(Color.parseColor("#94A3B8"));
        goalTv.setPadding(0, dp(7), 0, dp(6));
        progressCard.addView(goalTv);

        LinearLayout track = new LinearLayout(this);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(Color.parseColor("#253047"));
        trackBg.setCornerRadius(dp(4));
        track.setBackground(trackBg);
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        track.setLayoutParams(trackLp);
        View fill = new View(this);
        GradientDrawable fillBg = new GradientDrawable();
        fillBg.setColor(Color.parseColor("#3B82F6"));
        fillBg.setCornerRadius(dp(4));
        fill.setBackground(fillBg);
        float weight = Math.max(0.01f, Math.min(1.0f, pct / 100.0f));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f - weight));
        progressCard.addView(track);
        pageContent.addView(progressCard);

        TextView toolsHeading = new TextView(this);
        toolsHeading.setText(en ? "Review" : "複習");
        toolsHeading.setTextSize(13);
        toolsHeading.setTextColor(Color.parseColor("#94A3B8"));
        toolsHeading.setTypeface(Typeface.DEFAULT_BOLD);
        toolsHeading.setPadding(0, dp(2), 0, dp(8));
        pageContent.addView(toolsHeading);

        int sessionCount = LearningDataManager.getSessionHistory(this).size();
        pageContent.addView(makeActionCard("↗", en ? "Session reports" : "練習報告",
                sessionCount > 0 ? ((en ? "Review " : "查看 ") + sessionCount + (en ? " sessions and feedback" : " 次對話與改善建議")) : (en ? "Your feedback appears here after practice" : "完成練習後，回饋會集中在這裡"),
                Color.parseColor("#60A5FA"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                SessionHistoryDialog.show(MainActivity.this, new SessionHistoryDialog.ReportViewListener() {
                    @Override public void onOpenReport(LearningDataManager.SessionRecord record) {
                        SessionReportDialog.show(MainActivity.this, record, false, null);
                    }
                });
            }
        }));

        int starredCount = LearningDataManager.getStarredItems(this).size();
        pageContent.addView(makeActionCard("★", en ? "Saved phrases" : "收藏片語",
                starredCount > 0 ? ((en ? "Saved " : "已收藏 ") + starredCount + (en ? " phrases" : " 條片語")) : (en ? "Save useful phrases during a session" : "對話中收藏值得重練的句子"),
                Color.parseColor("#F59E0B"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                PhrasebookDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() { renderCurrentPage(); }
                });
            }
        }));

        // Floating bubble is an accessory feature, not a headline feature.
        boolean hasOverlayPerm = FloatingBubbleManager.getInstance(this).canDrawOverlays();
        pageContent.addView(makeActionCard("◉", en ? "Floating tutor" : "懸浮助教",
                bubbleActive ? (en ? "Active · Tap to stop" : "運行中 · 點擊關閉")
                        : (hasOverlayPerm ? (en ? "Practice across other apps" : "跨 App 隨時叫出助教")
                        : (en ? "Overlay permission required" : "需要懸浮視窗權限")),
                Color.parseColor("#64748B"), new View.OnClickListener() {
            @Override public void onClick(View v) { toggleFloatingBubbleService(); }
        }));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ⚙️ TAB 1: 偏好設定 / Settings Hub
    // ══════════════════════════════════════════════════════════════════════════
    private void renderSettingsPage(final boolean en) {
        LinearLayout sHeadRow = new LinearLayout(this);
        sHeadRow.setOrientation(LinearLayout.HORIZONTAL);
        sHeadRow.setGravity(Gravity.CENTER_VERTICAL);
        sHeadRow.setPadding(0, 0, 0, dp(14));

        TextView heading = new TextView(this);
        heading.setText(en ? "⚙️ Preferences" : "⚙️ 偏好設定與語音配置");
        heading.setTextSize(18);
        heading.setTextColor(CrewTheme.TEXT_PRIMARY);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        sHeadRow.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button guideBtn = new Button(this);
        guideBtn.setText(en ? "🧭 Guide" : "🧭 入學指南");
        guideBtn.setTextSize(11);
        guideBtn.setTextColor(Color.parseColor("#E0E7FF"));
        guideBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gBg = new GradientDrawable();
        gBg.setColor(Color.parseColor("#312E81")); // Indigo 900
        gBg.setCornerRadius(dp(12));
        gBg.setStroke(dp(1), Color.parseColor("#6366F1")); // Indigo 500
        guideBtn.setBackground(gBg);
        guideBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                WelcomeGuideDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() { renderCurrentPage(); }
                });
            }
        });
        sHeadRow.addView(guideBtn, new LinearLayout.LayoutParams(dp(84), dp(34)));
        pageContent.addView(sHeadRow);

        // Section 1: Audio & Hardware
        TextView audioSec = new TextView(this);
        audioSec.setText(en ? "🔊 Audio & Hardware Shield" : "🔊 音訊與防打斷保護");
        audioSec.setTextSize(12);
        audioSec.setTextColor(Color.parseColor("#64748B"));
        audioSec.setTypeface(Typeface.DEFAULT_BOLD);
        audioSec.setPadding(0, dp(4), 0, dp(6));
        pageContent.addView(audioSec);

        // Anti-Interruption Shield
        int sensitivity = AppConfig.getInterruptionSensitivity(this);
        String shieldSummary = sensitivity == 0 ? (en ? "🚫 Complete Lock (Zero voice interruption)" : "🚫 完全禁止插話 (AI 說話時完全鎖定)")
                : (sensitivity <= 25 ? (en ? "🛡️ Heavy Shield" : "🛡️ 高強度防打斷")
                : (sensitivity >= 70 ? (en ? "⚡ Fast Interruption" : "⚡ 極速插話") : (en ? "⚖️ Standard Balanced" : "⚖️ 標準平衡模式")));
        pageContent.addView(makeActionCard("🛡️", en ? "Anti-Interruption Shield" : "防插話與抗迴音保護", shieldSummary + (en ? " · Tap to adjust" : " · 點擊切換"), CrewTheme.ROSE_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showInterruptionShieldDialog(); }
        }));

        // Audio Route
        String currentOutput = AppConfig.getAudioOutput(this);
        String outputLabel = "media".equals(currentOutput)
                ? (en ? "🎵 Media Audio (Bluetooth / High Quality, Default)" : "🎵 媒體音訊 (藍牙耳機高音質 · 預設推薦)")
                : (en ? "📞 Voice Call (Hardware AEC & Noise Cancelling)" : "📞 通話音訊 (硬體 AEC 回音消除)");
        pageContent.addView(makeActionCard("🔊", en ? "Audio Output Channel" : "語音輸出通道", outputLabel, CrewTheme.INDIGO_300, new View.OnClickListener() {
            @Override public void onClick(View v) { showAudioOutputDialog(); }
        }));

        // Floating Bubble Tutor (桌面懸浮球助教)
        boolean bubbleRunning = NativeLiveService.isActive();
        boolean hasOverlayPerm = FloatingBubbleManager.getInstance(this).canDrawOverlays();
        String bubbleSummary = bubbleRunning ? (en ? "🟢 Running · Tap to manage or stop" : "🟢 運行中 · 點擊管理或關閉")
                : (hasOverlayPerm ? (en ? "⚪ Ready · Tap to launch overlay bubble" : "⚪ 待命中 · 點擊一鍵啟動懸浮球")
                : (en ? "⚠️ Permission Required · Tap to grant overlay" : "⚠️ 需開啟懸浮視窗權限 · 點擊前往設定"));
        pageContent.addView(makeActionCard("🫧", en ? "Floating Bubble Tutor" : "桌面懸浮球助教 (跨 App 練習)", bubbleSummary, Color.parseColor("#38BDF8"), new View.OnClickListener() {
            @Override public void onClick(View v) { showFloatingBubbleDialog(); }
        }));

        // Section 2: Tutor & Language Settings
        TextView langSec = new TextView(this);
        langSec.setText(en ? "🌐 Tutor & Language Settings" : "🌐 導師與語言設定");
        langSec.setTextSize(12);
        langSec.setTextColor(Color.parseColor("#64748B"));
        langSec.setTypeface(Typeface.DEFAULT_BOLD);
        langSec.setPadding(0, dp(12), 0, dp(6));
        pageContent.addView(langSec);

        // Target Practice Language
        String tutorLang = AppConfig.getTutorLanguageDisplayName(this);
        pageContent.addView(makeActionCard("🌐", en ? "Target Practice Language" : "學習目標外語", tutorLang + (en ? " · Tap to change" : " · 點擊切換學習語言"), CrewTheme.INDIGO_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showTutorLanguageDialog(); }
        }));

        // Student Native Language
        String studentLangLabel = AppConfig.getStudentLanguageDisplayName(this);
        pageContent.addView(makeActionCard("🗣️", en ? "Student Native Language" : "學生母語（對照翻譯語言）", studentLangLabel + (en ? " · Subtitles & notes language" : " · 即時字幕對照與單字註釋語言"), CrewTheme.EMERALD_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showStudentLanguageDialog(); }
        }));

        // Voice Persona (30 Voices)
        String currentVoice = AppConfig.getVoiceName(this);
        String voiceSummary = VoicePersonaDialog.getVoiceDisplayName(currentVoice, en);
        pageContent.addView(makeActionCard("🎙️", en ? "Tutor Voice Persona (30 Voices)" : "導師語音音色 (全 30 款)", voiceSummary + (en ? " · Tap to choose & listen" : " · 點擊選用與試聽"), CrewTheme.CYAN_400, new View.OnClickListener() {
            @Override public void onClick(View v) {
                VoicePersonaDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() { renderCurrentPage(); }
                });
            }
        }));

        // Teaching Mode
        pageContent.addView(makeActionCard("🎓", en ? "Teaching Method & Level" : "教學引導模式 (難易度)", getTeachingModeSummary(AppConfig.getTeachingMode(this), en), Color.parseColor("#A855F7"), new View.OnClickListener() {
            @Override public void onClick(View v) { showTeachingModeDialog(); }
        }));

        // Helpful tip card for Native language support
        LinearLayout tipCard = new LinearLayout(this);
        tipCard.setOrientation(LinearLayout.VERTICAL);
        tipCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable tipBg = new GradientDrawable();
        tipBg.setColor(Color.parseColor("#064E3B"));
        tipBg.setCornerRadius(dp(12));
        tipBg.setStroke(dp(1), Color.parseColor("#059669"));
        tipCard.setBackground(tipBg);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tipLp.setMargins(0, 0, 0, dp(10));
        tipCard.setLayoutParams(tipLp);

        TextView tipTitle = new TextView(this);
        tipTitle.setText(en ? "💡 Pro Tip: Need Tutor to Explain in Your Language?" : "💡 實用小秘訣：想聽外師用母語/中文解釋？");
        tipTitle.setTextSize(12);
        tipTitle.setTextColor(Color.WHITE);
        tipTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tipCard.addView(tipTitle);

        TextView tipDesc = new TextView(this);
        tipDesc.setText(en ? "In 'Bilingual & Native Support' or 'Beginner' mode, you can directly ask the tutor in your native language (e.g. '請用中文解釋' or '這是什麼意思？'), and the tutor will immediately explain the vocabulary & grammar in your native language!"
                : "在「雙語母語輔助」或「零基礎引導」模式下，您可以隨時對外師說『請用中文解釋』或『這是什麼意思？』，外師會立即切換母語為您詳細解析！");
        tipDesc.setTextSize(11);
        tipDesc.setTextColor(Color.parseColor("#A7F3D0"));
        tipDesc.setLineSpacing(dp(2), 1.15f);
        tipDesc.setPadding(0, dp(2), 0, 0);
        tipCard.addView(tipDesc);
        pageContent.addView(tipCard);

        // Section 3: Advanced System & API Key
        TextView advSec = new TextView(this);
        advSec.setText(en ? "🔑 Advanced & API Key" : "🔑 進階配置與 API Key");
        advSec.setTextSize(12);
        advSec.setTextColor(Color.parseColor("#64748B"));
        advSec.setTypeface(Typeface.DEFAULT_BOLD);
        advSec.setPadding(0, dp(12), 0, dp(6));
        pageContent.addView(advSec);

        // API Key
        String apiKey = AppConfig.getGeminiApiKey(this);
        String keySummary = apiKey.isEmpty()
                ? (en ? "⚠️ API Key not configured" : "⚠️ 尚未設定 API Key")
                : (en ? "✅ Key configured: " : "✅ 已設定 Key: ") + apiKey.substring(0, Math.min(8, apiKey.length())) + "…";
        pageContent.addView(makeActionCard("🔑", "Gemini API Key (BYOK)", keySummary, CrewTheme.AMBER_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showApiKeyDialog(); }
        }));

        // Custom Prompt
        String customPrompt = AppConfig.getCustomPrompt(this);
        String promptSummary = customPrompt.isEmpty()
                ? (en ? "Default rigorous coach profile" : "預設專業導師人設 · 點擊自訂 Prompt")
                : (en ? "Custom prompt active: " : "已啟用自訂 Prompt: ") + (customPrompt.length() > 20 ? customPrompt.substring(0, 20) + "…" : customPrompt);
        pageContent.addView(makeActionCard("✍️", en ? "Custom AI Tutor Prompt" : "自訂 AI 導師 Prompt 人設", promptSummary, Color.parseColor("#38BDF8"), new View.OnClickListener() {
            @Override public void onClick(View v) { showCustomPromptDialog(); }
        }));

        // Welcome Guide & Onboarding Replay
        pageContent.addView(makeActionCard("🧭", en ? "Welcome Guide & AI Advisor" : "新手入學導覽與 AI 顧問 (重開導覽)",
                en ? "View feature highlights or replay 1st lesson onboarding" : "查看功能特色說明、API Key 免費領取教學或重開新手引導課",
                Color.parseColor("#A855F7"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                WelcomeGuideDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() { renderCurrentPage(); }
                });
            }
        }));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🛠️ DIALOGS & ACTION CARDS
    // ══════════════════════════════════════════════════════════════════════════
    private LinearLayout makePodItem(String icon, String title, String tag, int color, View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#111827"));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#1F2937"));
        item.setBackground(bg);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(18);
        topRow.addView(iconTv);

        TextView tagTv = new TextView(this);
        tagTv.setText(tag);
        tagTv.setTextSize(10);
        tagTv.setTextColor(color);
        tagTv.setGravity(Gravity.END);
        topRow.addView(tagTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        item.addView(topRow);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(12);
        titleTv.setTextColor(CrewTheme.TEXT_PRIMARY);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, dp(6), 0, 0);
        item.addView(titleTv);

        item.setOnClickListener(onClick);
        return item;
    }

    private LinearLayout makeActionCard(String icon, String title, String summary, int accentColor, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#111827"));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#1F2937"));
        card.setBackground(bg);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(clp);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(20);
        iconTv.setPadding(0, 0, dp(12), 0);
        card.addView(iconTv);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(13);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(titleTv);

        TextView sumTv = new TextView(this);
        sumTv.setText(summary);
        sumTv.setTextSize(11);
        sumTv.setTextColor(Color.parseColor("#94A3B8"));
        sumTv.setPadding(0, dp(2), 0, 0);
        textCol.addView(sumTv);

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(20);
        arrow.setTextColor(Color.parseColor("#64748B"));
        arrow.setPadding(dp(8), 0, 0, 0);
        card.addView(arrow);

        card.setOnClickListener(onClick);
        return card;
    }

    private void showInterruptionShieldDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "🚫 Complete Lock (AI cannot be interrupted at all by voice)" : "🚫 完全禁止插話 (AI 說話時完全鎖定，完全無法插話，講完才能說)",
                en ? "🛡️ Heavy Shield (Recommended for loudspeaker, loud voice to interrupt)" : "🛡️ 高強度防打斷 (不易被外放迴音或雜音誤觸，大聲說話可插話)",
                en ? "⚖️ Standard Balanced (Default, normal speaking allows interruption)" : "⚖️ 標準平衡模式 (預設，正常說話可插話)",
                en ? "⚡ Fast Interruption (Ideal for earphones/quiet rooms)" : "⚡ 靈敏插話模式 (適合安靜環境或耳機，微弱聲音即插話)"
        };
        final int[] sensitivities = {0, 20, 50, 80};
        int current = AppConfig.getInterruptionSensitivity(this);
        int selected = current == 0 ? 0 : (current <= 25 ? 1 : (current >= 70 ? 3 : 2));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🛡️ Interruption Shield & Sensitivity" : "🛡️ 插話防護與靈敏度");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setInterruptionSensitivity(MainActivity.this, sensitivities[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Interruption sensitivity updated" : "已更新防打斷靈敏度", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showAudioOutputDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "📞 Voice Call (Hardware AEC, Best for speakerphone)" : "📞 通話音訊 (硬體 AEC 回音消除，揚聲器首選推薦)",
                en ? "🎵 Media Audio (High fidelity, best for Bluetooth/headphones)" : "🎵 媒體音訊 (高保真音質，耳機/藍牙音箱推薦)"
        };
        final String[] values = {"call", "media"};
        int selected = "media".equals(AppConfig.getAudioOutput(this)) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🔊 Choose Audio Output Channel" : "🔊 選擇語音輸出通道");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setAudioOutput(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Audio channel updated" : "已更新音訊輸出通道", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showTutorLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇺🇸 English (英語)",
                "🇯🇵 日本語 (Japanese)",
                "🇰🇷 한국어 (Korean)",
                "🇪🇸 Español (Spanish)",
                "🇫🇷 Français (French)",
                "🇩🇪 Deutsch (German)",
                "🇮🇹 Italiano (Italian)",
                "🇻🇳 Tiếng Việt (Vietnamese)",
                "🇹🇭 ภาษาไทย (Thai)",
                "🇨🇳 中文 (Mandarin Chinese)"
        };
        final String[] values = {"en", "ja", "ko", "es", "fr", "de", "it", "vi", "th", "zh"};
        String current = AppConfig.getTutorLanguage(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                selected = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🌍 Select Target Language to Practice" : "🌍 選擇想要練習的外語目標");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTutorLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Target language updated" : "已更新練習語言", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showStudentLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇹🇼 繁體中文 (Traditional Chinese)",
                "🇨🇳 簡體中文 (Simplified Chinese)",
                "🇺🇸 English (英語)",
                "🇯🇵 日本語 (Japanese)",
                "🇰🇷 한국어 (Korean)",
                "🇻🇳 Tiếng Việt (Vietnamese)",
                "🇮🇩 Bahasa Indonesia (Indonesian)",
                "🇪🇸 Español (Spanish)",
                "🇹🇭 ภาษาไทย (Thai)"
        };
        final String[] values = {"zh-TW", "zh-CN", "en", "ja", "ko", "vi", "id", "es", "th"};
        String current = AppConfig.getStudentLanguage(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                selected = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🗣️ Select Your Native Language" : "🗣️ 選擇學員母語 (雙語翻譯與講解)");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setStudentLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Native language updated" : "已更新學員母語", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showPersonaDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "☕ Daily Life & Casual Banter" : "☕ 日常生活與閒聊 (興趣、週末計畫、時事)",
                en ? "✈️ Travel Scenarios (Airport, Hotel, Restaurant)" : "✈️ 出國旅遊場景 (機場、海關、飯店、點餐、問路)",
                en ? "💼 Business & Workplace Communication" : "💼 職場商務溝通 (專案匯報、客戶談判、跨國協作)",
                en ? "👔 Job Interview Simulation" : "👔 求職英文面試 (STAR 結構化提問、經歷深挖)",
                en ? "🎯 Standardized Speaking Exam (IELTS / TOEFL)" : "🎯 檢定口說備考 (雅思 Part 1/2/3、托福模擬)",
                en ? "🛍️ Shopping & Bargaining" : "🛍️ 購物與退稅 (殺價、問尺寸、退換貨)",
                en ? "🏥 Medical & Clinic Consultation" : "🏥 醫院看診諮詢 (描述症狀、藥局諮詢)",
                en ? "🏠 Apartment Hunting & Tenancy" : "🏠 租屋看房與生活 (問租金、室友公約、修繕)",
                en ? "💬 Dating & Social Mingling" : "💬 社交破冰與約會 (咖啡廳偶遇、興趣交流)",
                en ? "🤖 Tech & AI Innovations" : "🤖 科技創新與程式 (架構討論、生成式 AI)"
        };
        final String[] values = {"daily", "travel", "business", "interview", "exam", "shopping", "medical", "housing", "dating", "tech"};
        String current = AppConfig.getTutorPersona(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                selected = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🎭 Choose Tutoring Scenario" : "🎭 選擇口說練習情境劇本");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTutorPersona(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Scenario updated" : "已切換情境劇本", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showTeachingModeDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "✨ Bilingual & Native Support (Tutor uses native explanations when requested, Recommended)"
                   : "✨ 雙語母語輔助 (外師主要說外語，可隨時用母語解釋詞彙與文法 · 推薦)",
                en ? "🌱 Beginner Step-by-Step (Tutor patiently explains in native language + models phrase by phrase)"
                   : "🌱 零基礎手把手 (外師以母語為主詳細講解，逐句示範帶讀)",
                en ? "🌊 100% Full Immersion (Tutor speaks ONLY target language, no native language in audio)"
                   : "🌊 100% 全外語沉浸 (外師語音嚴格僅說外語，絕不使用中文)",
                en ? "📖 Spartan Pronunciation Coach (Read aloud & instant phonetic critique on stress/liaison)"
                   : "📖 斯巴達朗讀糾音 (嚴格糾正發音、重音與連音，即時打斷示範跟讀)"
        };
        final String[] values = {"bilingual", "beginner", "immersion", "shadowing"};
        String current = AppConfig.getTeachingMode(this);
        int selected = "beginner".equals(current) ? 1 : ("immersion".equals(current) ? 2 : ("shadowing".equals(current) ? 3 : 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "💡 Choose Teaching Method & Level" : "💡 選擇教學引導模式 (難易度)");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTeachingMode(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Teaching mode updated" : "已切換教學模式", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showApiKeyDialog() {
        final boolean en = I18n.isEnglish(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🔑 Set Gemini API Key (BYOK)" : "🔑 設定 Gemini API Key");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView hint = new TextView(this);
        hint.setText(en ? "Get a free key from Google AI Studio (aistudio.google.com):" : "從 Google AI Studio 免費取得 API Key (aistudio.google.com)：");
        hint.setTextSize(12);
        hint.setTextColor(Color.parseColor("#94A3B8"));
        hint.setPadding(0, 0, 0, dp(8));
        layout.addView(hint);

        final EditText input = new EditText(this);
        input.setHint("AIzaSy...");
        input.setText(AppConfig.getGeminiApiKey(this));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#64748B"));
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton(en ? "Save" : "儲存", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                AppConfig.setGeminiApiKey(MainActivity.this, key);
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "API Key saved" : "API Key 已儲存", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private void showCustomPromptDialog() {
        final boolean en = I18n.isEnglish(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "✍️ Custom AI Tutor System Prompt" : "✍️ 自訂 AI 導師 Prompt 人設");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        final EditText input = new EditText(this);
        input.setHint(en ? "e.g. Act as a demanding Cambridge IELTS examiner..." : "例：請扮演一位嚴格的劍橋雅思口說考官，專門挑出我的時態與單字錯誤...");
        input.setText(AppConfig.getCustomPrompt(this));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#64748B"));
        input.setMinLines(4);
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton(en ? "Save" : "儲存", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String p = input.getText().toString().trim();
                AppConfig.setCustomPrompt(MainActivity.this, p);
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Custom prompt updated" : "自訂 Prompt 已更新", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNeutralButton(en ? "Reset" : "清空重設", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setCustomPrompt(MainActivity.this, "");
                renderCurrentPage();
                Toast.makeText(MainActivity.this, en ? "Prompt reset to default" : "已重設為預設導師人設", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private String getPersonaLabel(String persona, boolean en) {
        if ("travel".equalsIgnoreCase(persona)) return en ? "✈️ Travel" : "✈️ 出國旅遊";
        if ("business".equalsIgnoreCase(persona)) return en ? "💼 Business" : "💼 職場商務";
        if ("interview".equalsIgnoreCase(persona)) return en ? "👔 Interview" : "👔 求職面試";
        if ("exam".equalsIgnoreCase(persona)) return en ? "🎯 Exam Prep" : "🎯 口說備考";
        if ("shopping".equalsIgnoreCase(persona)) return en ? "🛍️ Shopping" : "🛍️ 購物退稅";
        if ("medical".equalsIgnoreCase(persona)) return en ? "🏥 Medical" : "🏥 醫院看診";
        if ("housing".equalsIgnoreCase(persona)) return en ? "🏠 Housing" : "🏠 租屋看房";
        if ("dating".equalsIgnoreCase(persona)) return en ? "💬 Dating" : "💬 社交破冰";
        if ("tech".equalsIgnoreCase(persona)) return en ? "🤖 Tech & AI" : "🤖 科技創新";
        return en ? "☕ Daily Life" : "☕ 日常生活";
    }

    private String getTeachingModeSummary(String mode, boolean en) {
        if ("shadowing".equalsIgnoreCase(mode)) return en ? "📖 Reading & Pronunciation Coach" : "📖 斯巴達朗讀糾音";
        if ("beginner".equalsIgnoreCase(mode)) return en ? "🌱 Beginner Step-by-Step (Native Explained)" : "🌱 零基礎手把手 (母語講解)";
        if ("immersion".equalsIgnoreCase(mode)) return en ? "🌊 100% Full Immersion" : "🌊 100% 全外語沉浸 (不講中文)";
        return en ? "✨ Bilingual & Native Support (Recommended)" : "✨ 雙語母語輔助 (外師可中文解說 · 推薦)";
    }

    private void refreshStatus() {
        if (statusCard == null || statusDot == null || statusText == null || statusDetail == null) return;
        final boolean en = I18n.isEnglish(this);
        String apiKey = AppConfig.getGeminiApiKey(this);
        GradientDrawable sbg = new GradientDrawable();
        sbg.setCornerRadius(dp(14));

        if (apiKey.isEmpty()) {
            sbg.setColor(Color.parseColor("#451A03")); // Amber 950
            sbg.setStroke(dp(1), Color.parseColor("#B45309"));
            statusCard.setBackground(sbg);
            statusDot.setTextColor(Color.parseColor("#F59E0B"));
            statusText.setText(en ? "⚠️ API Key Required (BYOK)" : "⚠️ 請先設定 Gemini API Key");
            statusText.setTextColor(Color.parseColor("#FDE68A"));
            statusDetail.setText(en ? "Tap Preferences tab below to paste your key" : "點擊下方「偏好設定」分頁填入 API Key 即可開始練習");
            statusDetail.setTextColor(Color.parseColor("#FCD34D"));
        } else {
            sbg.setColor(Color.parseColor("#064E3B")); // Emerald 950
            sbg.setStroke(dp(1), Color.parseColor("#059669"));
            statusCard.setBackground(sbg);
            statusDot.setTextColor(Color.parseColor("#10B981"));
            statusText.setText(en ? "✨ Ready for 1-on-1 Tutoring" : "✨ AI 口說教練已就緒");
            statusText.setTextColor(Color.parseColor("#A7F3D0"));
            statusDetail.setText(en ? "Gemini 3.1 Live Preview · Tap Start above" : "Gemini 3.1 Live Preview 語音引擎 · 點擊開始練習");
            statusDetail.setTextColor(Color.parseColor("#6EE7B7"));
        }
    }

    private void showTopHeaderMenu(View anchor) {
        final boolean en = I18n.isEnglish(this);
        PopupMenu popup = new PopupMenu(this, anchor);
        boolean bubbleActive = NativeLiveService.isActive() || FloatingBubbleManager.getInstance(this).isBubbleShowing();

        popup.getMenu().add(0, 1, 0, bubbleActive ? (en ? "🫧 Turn Off Floating Bubble" : "🫧 關閉桌面懸浮球")
                                                  : (en ? "🫧 Turn On Floating Bubble" : "🫧 開啟桌面懸浮球"));
        popup.getMenu().add(0, 2, 1, en ? "🧭 Onboarding Guide" : "🧭 新手引導指南");
        popup.getMenu().add(0, 3, 2, en ? "🇨🇳 切換為繁體中文" : "🇺🇸 Switch to English");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 1) {
                    toggleFloatingBubbleService();
                    return true;
                } else if (id == 2) {
                    WelcomeGuideDialog.show(MainActivity.this, new Runnable() {
                        @Override public void run() { renderCurrentPage(); }
                    });
                    return true;
                } else if (id == 3) {
                    AppConfig.setUiLanguage(MainActivity.this, en ? "zh" : "en");
                    renderCurrentPage();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void toggleFloatingBubbleService() {
        final boolean en = I18n.isEnglish(this);
        if (!FloatingBubbleManager.getInstance(this).canDrawOverlays()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(en ? "🔑 Overlay Permission Required" : "🔑 需開啟懸浮視窗權限");
            builder.setMessage(en ? "To display the floating AI tutor bubble over other apps (e.g. YouTube, Browser, Social Media), please grant 'Display over other apps' permission."
                    : "要在其他 App（如 YouTube、瀏覽器、社群軟體）上方顯示 AI 外語懸浮球並進行即時口說練習，需要先開啟「允許顯示在其他應用程式上層」權限。");
            builder.setPositiveButton(en ? "Go to Settings" : "前往開啟權限", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception ignored) {}
                    }
                }
            });
            builder.setNegativeButton(en ? "Cancel" : "取消", null);
            builder.show();
            return;
        }

        boolean isActive = NativeLiveService.isActive() || FloatingBubbleManager.getInstance(this).isBubbleShowing();
        if (isActive) {
            NativeLiveService.stop(this);
            FloatingBubbleManager.getInstance(this).hideBubble();
            Toast.makeText(this, en ? "Floating Bubble stopped" : "已關閉懸浮球助教", Toast.LENGTH_SHORT).show();
        } else {
            String apiKey = AppConfig.getGeminiApiKey(this);
            if (apiKey.isEmpty()) {
                Toast.makeText(this, en ? "Please configure Gemini API Key first" : "請先在偏好設定中填入 Gemini API Key", Toast.LENGTH_LONG).show();
                showApiKeyDialog();
                return;
            }
            NativeLiveService.start(this);
            Toast.makeText(this, en ? "Floating Bubble started! Tap bubble to talk anytime" : "🚀 懸浮球助教已啟動！點擊懸浮球隨時展開語音對話", Toast.LENGTH_SHORT).show();
        }
        renderCurrentPage();
    }

    private void showFloatingBubbleDialog() {
        final boolean en = I18n.isEnglish(this);
        boolean hasOverlay = FloatingBubbleManager.getInstance(this).canDrawOverlays();
        boolean isRunning = NativeLiveService.isActive() || FloatingBubbleManager.getInstance(this).isBubbleShowing();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🫧 Floating Bubble Tutor" : "🫧 桌面懸浮球外語助教");

        StringBuilder msg = new StringBuilder();
        msg.append(en ? "The Floating Bubble allows you to practice speaking anywhere on your phone while browsing articles, watching videos, or social chatting.\n\n"
                      : "桌面懸浮球讓您在手機任何畫面（看英文文章、看 YouTube、跨 App 聊天）隨時點擊外師展開 1 對 1 即時語音互動與同步字幕翻譯。\n\n");
        msg.append(en ? "• Status: " : "• 運行狀態：").append(isRunning ? (en ? "🟢 Running" : "🟢 運行中") : (en ? "⚪ Inactive" : "⚪ 待命中")).append("\n");
        msg.append(en ? "• Overlay Permission: " : "• 懸浮視窗權限：").append(hasOverlay ? (en ? "✅ Granted" : "✅ 已授權") : (en ? "❌ Not Granted" : "❌ 尚未授權"));

        builder.setMessage(msg.toString());

        if (isRunning) {
            builder.setPositiveButton(en ? "⏹️ Stop Bubble" : "⏹️ 關閉懸浮球", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    NativeLiveService.stop(MainActivity.this);
                    FloatingBubbleManager.getInstance(MainActivity.this).hideBubble();
                    renderCurrentPage();
                    Toast.makeText(MainActivity.this, en ? "Floating Bubble stopped" : "已關閉懸浮球助教", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            builder.setPositiveButton(en ? "🚀 Launch Bubble" : "🚀 啟動懸浮球", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    toggleFloatingBubbleService();
                }
            });
        }

        builder.setNeutralButton(en ? "🔑 Permission" : "🔑 權限設定", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception ignored) {}
                }
            }
        });

        builder.setNegativeButton(en ? "Close" : "關閉", null);
        builder.show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> perms = new ArrayList<String>();
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                perms.add(android.Manifest.permission.RECORD_AUDIO);
            }
            if (!perms.isEmpty()) {
                requestPermissions(perms.toArray(new String[0]), REQUEST_PERMISSIONS);
            }
        }
    }

    public static String getLanguageLabel(String code) {
        return AppConfig.getLanguageLabel(code);
    }
}
