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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 201;

    private int currentMainTab = 0; // 0: Practice (對話教練), 1: Settings (偏好設定)

    private TextView statusDot;
    private TextView statusText;
    private TextView statusDetail;
    private LinearLayout statusCard;
    private LinearLayout pageContent;
    private LinearLayout bottomNav;

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

        // 2. Bottom Navigation Tab Bar
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER_VERTICAL);
        bottomNav.setPadding(dp(12), dp(6), dp(12), dp(8));
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(Color.parseColor("#0B1120")); // Deep Navy
        navBg.setStroke(dp(1), Color.parseColor("#1E293B"));
        bottomNav.setBackground(navBg);
        root.addView(bottomNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));

        setContentView(root);
        renderCurrentPage();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
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

        bottomNav.addView(buildTabItem("🎓", en ? "Practice Hub" : "對話教室", 0, currentMainTab == 0));
        bottomNav.addView(buildTabItem("⚙️", en ? "Preferences" : "偏好設定", 1, currentMainTab == 1));
    }

    private LinearLayout buildTabItem(String icon, String label, final int tabIndex, boolean active) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(0, dp(4), 0, dp(4));

        GradientDrawable tBg = new GradientDrawable();
        if (active) {
            tBg.setColor(Color.parseColor("#1E293B"));
            tBg.setCornerRadius(dp(12));
            tBg.setStroke(dp(1), Color.parseColor("#38BDF8"));
        } else {
            tBg.setColor(Color.TRANSPARENT);
        }
        tab.setBackground(tBg);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(16);
        tab.addView(iconTv);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(11);
        labelTv.setTextColor(active ? Color.parseColor("#38BDF8") : Color.parseColor("#94A3B8"));
        labelTv.setTypeface(Typeface.DEFAULT_BOLD);
        labelTv.setPadding(0, dp(2), 0, 0);
        tab.addView(labelTv);

        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentMainTab != tabIndex) {
                    currentMainTab = tabIndex;
                    renderCurrentPage();
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(6), 0, dp(6), 0);
        tab.setLayoutParams(lp);
        return tab;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🌟 TAB 0: 對話教練 / Practice Hub
    // ══════════════════════════════════════════════════════════════════════════
    private void renderPracticePage(final boolean en) {
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
                renderCurrentPage();
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
        statusCardLp.setMargins(0, dp(12), 0, dp(12));
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

        // 2.5 Streak & Progress Card
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

        int goalMin = LearningDataManager.getDailyGoalMinutes(this);
        int todaySec = streak.todayPracticeSeconds;
        int pct = Math.min(100, Math.round((todaySec * 100.0f) / (goalMin * 60.0f)));

        TextView goalTv = new TextView(this);
        goalTv.setText((en ? "Daily Goal: " : "每日目標：") + (todaySec / 60) + " / " + goalMin + (en ? " mins (" : " 分鐘 (") + pct + "%)");
        goalTv.setTextSize(11);
        goalTv.setTextColor(Color.parseColor("#94A3B8"));
        goalTv.setPadding(0, dp(6), 0, dp(4));
        streakCard.addView(goalTv);

        // Progress Track
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable trBg = new GradientDrawable();
        trBg.setColor(Color.parseColor("#1E293B"));
        trBg.setCornerRadius(dp(4));
        track.setBackground(trBg);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        track.setLayoutParams(trLp);

        View fill = new View(this);
        GradientDrawable fBg = new GradientDrawable();
        fBg.setColor(Color.parseColor("#38BDF8"));
        fBg.setCornerRadius(dp(4));
        fill.setBackground(fBg);
        float weight = Math.max(0.01f, Math.min(1.0f, pct / 100.0f));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f - weight));
        streakCard.addView(track);
        pageContent.addView(streakCard);

        // 3. Main Action: Start Practice Button
        Button startBtn = new Button(this);
        String teachingMode = AppConfig.getTeachingMode(this);
        boolean isShadowing = "shadowing".equals(teachingMode);
        startBtn.setText(isShadowing
                ? (en ? "📖 Start Reading & Pronunciation Lab" : "📖 開始朗讀高亮與糾音實驗室")
                : (en ? "🎙️ Start 1-on-1 Oral Practice" : "🎙️ 開始 1-on-1 即時外語對話"));
        startBtn.setTextSize(15);
        startBtn.setTextColor(Color.WHITE);
        startBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColors(isShadowing
                ? new int[]{Color.parseColor("#7C3AED"), Color.parseColor("#4F46E5")}
                : new int[]{Color.parseColor("#2563EB"), Color.parseColor("#1D4ED8")});
        sbBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        sbBg.setCornerRadius(dp(14));
        startBtn.setBackground(sbBg);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        sbLp.setMargins(0, 0, 0, dp(12));
        startBtn.setLayoutParams(sbLp);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NativeLiveActivity.class);
                startActivity(intent);
            }
        });
        pageContent.addView(startBtn);

        // 4. Quick Mode & Scenario Grid
        TextView modeHeading = new TextView(this);
        modeHeading.setText(en ? "🎯 Practice Modes & Scenarios" : "🎯 練習模式與情境");
        modeHeading.setTextSize(12);
        modeHeading.setTextColor(Color.parseColor("#64748B"));
        modeHeading.setTypeface(Typeface.DEFAULT_BOLD);
        modeHeading.setPadding(0, dp(4), 0, dp(8));
        pageContent.addView(modeHeading);

        LinearLayout grid1 = new LinearLayout(this);
        grid1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams gLp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gLp1.setMargins(0, 0, 0, dp(10));
        grid1.setLayoutParams(gLp1);

        // Structured Mission Map
        LinearLayout pod1 = makePodItem("🎯", en ? "Lesson Missions" : "闖關模式", en ? "Structured" : "循序漸進", CrewTheme.CYAN_400, new View.OnClickListener() {
            @Override public void onClick(View v) {
                CourseMapDialog.show(MainActivity.this, null);
            }
        });
        grid1.addView(pod1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Free Scenario Switcher
        String persona = AppConfig.getTutorPersona(this);
        LinearLayout pod2 = makePodItem("🎭", en ? "Scenario" : "情境劇本", getPersonaLabel(persona, en), CrewTheme.AMBER_400, new View.OnClickListener() {
            @Override public void onClick(View v) { showPersonaDialog(); }
        });
        LinearLayout.LayoutParams p2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p2Lp.setMargins(dp(10), 0, 0, 0);
        grid1.addView(pod2, p2Lp);
        pageContent.addView(grid1);

        // 5. Study Tools Row: Starred Phrasebook & Session History
        TextView toolsHeading = new TextView(this);
        toolsHeading.setText(en ? "📚 Notebook & Diagnostics" : "📚 生詞本與學習診斷");
        toolsHeading.setTextSize(12);
        toolsHeading.setTextColor(Color.parseColor("#64748B"));
        toolsHeading.setTypeface(Typeface.DEFAULT_BOLD);
        toolsHeading.setPadding(0, dp(6), 0, dp(8));
        pageContent.addView(toolsHeading);

        int starredCount = LearningDataManager.getStarredItems(this).size();
        pageContent.addView(makeActionCard("⭐", en ? "My Starred Phrasebook" : "⭐ 我的個人生詞與金句本",
                (starredCount > 0 ? ((en ? "Saved " : "已收藏 ") + starredCount + (en ? " phrases · Tap to drill & quiz" : " 條精選生詞金句 · 點擊跟讀與翻卡")) : (en ? "No saved phrases yet" : "尚無收藏 · 對話中點擊 ★ 隨時加入")),
                Color.parseColor("#FBBF24"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                PhrasebookDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() { renderCurrentPage(); }
                });
            }
        }));

        int sessionCount = LearningDataManager.getSessionHistory(this).size();
        pageContent.addView(makeActionCard("📊", en ? "Session History & Diagnostics" : "📊 歷史對話與成效報告",
                (sessionCount > 0 ? ((en ? "Total " : "累計 ") + sessionCount + (en ? " tutoring sessions recorded" : " 次對話課堂記錄")) : (en ? "No sessions yet" : "尚無記錄 · 完成練習自動生成")) + (en ? " · Tap to review" : " · 點擊查看診斷"),
                Color.parseColor("#38BDF8"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                SessionHistoryDialog.show(MainActivity.this, new SessionHistoryDialog.ReportViewListener() {
                    @Override public void onOpenReport(LearningDataManager.SessionRecord record) {
                        SessionReportDialog.show(MainActivity.this, record, false, null);
                    }
                });
            }
        }));

        refreshStatus();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ⚙️ TAB 1: 偏好設定 / Settings Hub
    // ══════════════════════════════════════════════════════════════════════════
    private void renderSettingsPage(final boolean en) {
        TextView heading = new TextView(this);
        heading.setText(en ? "⚙️ Preferences & Configuration" : "⚙️ 偏好設定與語音配置");
        heading.setTextSize(18);
        heading.setTextColor(CrewTheme.TEXT_PRIMARY);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setPadding(0, 0, 0, dp(14));
        pageContent.addView(heading);

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
                ? (en ? "🎵 Media Audio (Bluetooth / High Quality)" : "🎵 媒體音訊 (藍牙耳機高音質)")
                : (en ? "📞 Voice Call (Hardware AEC & Noise Cancelling)" : "📞 通話音訊 (硬體 AEC 回音消除，推薦)");
        pageContent.addView(makeActionCard("🔊", en ? "Audio Output Channel" : "語音輸出通道", outputLabel, CrewTheme.INDIGO_300, new View.OnClickListener() {
            @Override public void onClick(View v) { showAudioOutputDialog(); }
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
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🛠️ DIALOGS & ACTION CARDS
    // ══════════════════════════════════════════════════════════════════════════
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

    private LinearLayout makeActionCard(String icon, String title, String summary, int accentColor, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B"));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#334155"));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        card.setClickable(true);
        card.setFocusable(true);

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
        sumTv.setTextColor(accentColor);
        sumTv.setPadding(0, dp(2), 0, 0);
        textCol.addView(sumTv);

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(18);
        arrow.setTextColor(Color.parseColor("#64748B"));
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
                "🇺🇸 English (英語)", "🇯🇵 日本語 (Japanese)", "🇰🇷 한국어 (Korean)",
                "🇪🇸 Español (Spanish)", "🇫🇷 Français (French)", "🇩🇪 Deutsch (German)",
                "🇨🇳 普通話 / 國語 (Mandarin Chinese)", "🇭🇰 粵語 (Cantonese)",
                "🇹🇼 閩南語 / 台語 (Taiwanese Hokkien)", "🇻🇳 Tiếng Việt (Vietnamese)"
        };
        final String[] values = {"en", "ja", "ko", "es", "fr", "de", "zh", "yue", "nan", "vi"};
        String current = AppConfig.getTutorLanguage(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) { selected = i; break; }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🌐 Choose Target Practice Language" : "🌐 選擇學習目標外語");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTutorLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
                Toast.makeText(MainActivity.this, (en ? "Target language set to: " : "學習目標外語已設定為：") + items[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showStudentLanguageDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                "🇹🇼 繁體中文 (Traditional Chinese - Taiwan)",
                "🇨🇳 簡體中文 (Simplified Chinese)",
                "🇺🇸 English", "🇯🇵 日本語 (Japanese)", "🇰🇷 한국어 (Korean)",
                "🇻🇳 Tiếng Việt (Vietnamese)", "🇮🇩 Bahasa Indonesia",
                "🇪🇸 Español (Spanish)", "🇫🇷 Français (French)", "🇩🇪 Deutsch (German)",
                "🇭🇰 粵語 (Cantonese)", "🇹🇼 閩南語 / 台語 (Taiwanese Hokkien)"
        };
        final String[] values = {"zh-TW", "zh-CN", "en", "ja", "ko", "vi", "id", "es", "fr", "de", "yue", "nan"};
        String current = AppConfig.getStudentLanguage(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) { selected = i; break; }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🗣️ Choose Student Native Language" : "🗣️ 選擇學生母語（對照翻譯語言）");
        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setStudentLanguage(MainActivity.this, values[which]);
                dialog.dismiss();
                renderCurrentPage();
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
                en ? "💬 Dating & Social Mingling (Icebreakers, casual banter, hobbies, parties)" : "💬 社交破冰 (Dating & Social: 社交破冰、約會聊天、認識新朋友)",
                en ? "🤖 Tech & AI Trends (Software development, LLMs, future tech trends)" : "🤖 科技創新 (Tech & AI: 軟體開發、人工智慧、科技創新)"
        };
        final String[] values = {"daily", "travel", "business", "interview", "exam", "shopping", "medical", "housing", "dating", "tech"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🎭 Choose Conversation Scenario" : "🎭 選擇實戰情境劇本");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                AppConfig.setTutorPersona(MainActivity.this, values[which]);
                renderCurrentPage();
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
        if ("shadowing".equalsIgnoreCase(mode)) return en ? "📖 Reading & Pronunciation Coach" : "📖 朗讀糾音教練模式";
        if ("beginner".equalsIgnoreCase(mode)) return en ? "🌱 Beginner Step-by-Step" : "🌱 零基礎引導模式";
        if ("immersion".equalsIgnoreCase(mode)) return en ? "🌊 100% Full Immersion" : "🌊 全外語沉浸模式";
        return en ? "✨ Bilingual Scaffolding (Recommended)" : "✨ 雙語對照模式（推薦）";
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
