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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

        if ("shadowing".equals(currentTeachingMode)) {
            LinearLayout readingCard = new LinearLayout(this);
            readingCard.setOrientation(LinearLayout.VERTICAL);
            readingCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(Color.parseColor("#1E1B4B"));
            rBg.setCornerRadius(dp(12));
            rBg.setStroke(dp(1), Color.parseColor("#6366F1"));
            readingCard.setBackground(rBg);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, dp(10), 0, 0);
            readingCard.setLayoutParams(rLp);

            LinearLayout rHeader = new LinearLayout(this);
            rHeader.setOrientation(LinearLayout.HORIZONTAL);
            rHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView rTitle = new TextView(this);
            rTitle.setText(en ? "📖 Reading Material (Tap to change)" : "📖 朗讀文章 (點擊可切換/貼上)");
            rTitle.setTextSize(11);
            rTitle.setTextColor(Color.parseColor("#A5B4FC"));
            rTitle.setTypeface(Typeface.DEFAULT_BOLD);
            rHeader.addView(rTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView rEdit = new TextView(this);
            rEdit.setText(en ? "✏️ Choose Text" : "✏️ 選擇/自訂文章");
            rEdit.setTextSize(11);
            rEdit.setTextColor(Color.parseColor("#38BDF8"));
            rHeader.addView(rEdit);
            readingCard.addView(rHeader);

            String readingSnippet = AppConfig.getReadingText(this);
            TextView rText = new TextView(this);
            if (readingSnippet.isEmpty()) {
                rText.setText(en ? "⚪ (No reading text yet. Tap to generate with AI or paste custom text)" : "⚪ (尚未設定文章，點擊此處由 AI 生成或自訂貼上)");
                rText.setTextColor(Color.parseColor("#94A3B8"));
            } else {
                rText.setText("\"" + readingSnippet + "\"");
                rText.setTextColor(Color.parseColor("#E2E8F0"));
            }
            rText.setTextSize(11);
            rText.setMaxLines(3);
            rText.setEllipsize(TextUtils.TruncateAt.END);
            rText.setPadding(0, dp(4), 0, 0);
            readingCard.addView(rText);

            readingCard.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showReadingTextDialog();
                }
            });
            lessonPod.addView(readingCard);
        }
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

        // 6. 【底層進階設定抽屜列】Advanced Settings Footer Row
        TextView advHeading = new TextView(this);
        advHeading.setText(en ? "⚙️ Audio & System Preferences" : "⚙️ 音訊與系統進階設定");
        advHeading.setTextSize(12);
        advHeading.setTextColor(Color.parseColor("#64748B"));
        advHeading.setTypeface(Typeface.DEFAULT_BOLD);
        advHeading.setPadding(0, dp(6), 0, dp(8));
        pageContent.addView(advHeading);

        // Voice Persona (30 Voices)
        String currentVoice = AppConfig.getVoiceName(this);
        String voiceSummary = getVoiceDisplayName(currentVoice, en);
        pageContent.addView(makeActionCard("🗣️", en ? "Tutor Voice Persona (30 Voices)" : "導師語音音色 (全 30 款)", voiceSummary + (en ? " · Tap to choose & listen" : " · 點擊選用與試聽"), CrewTheme.CYAN_400, new View.OnClickListener() {
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
        if ("travel".equals(persona)) return en ? "Travel & Dining" : "出國旅遊 (Travel & Dining)";
        if ("business".equals(persona)) return en ? "Business English" : "職場商務 (Business English)";
        if ("exam".equals(persona)) return en ? "TOEIC / IELTS Speaking" : "口說備考 (TOEIC/IELTS)";
        return en ? "Daily Life & Hobbies" : "日常閒聊 (Daily Life)";
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

    private void showPersonaDialog() {
        final boolean en = I18n.isEnglish(this);
        final String[] items = {
                en ? "Daily Life & Hobbies" : "日常閒聊 (Daily Life & Hobbies)",
                en ? "Travel Scenarios & Dining" : "出國旅遊 (Travel & Dining)",
                en ? "Business & Professional English" : "職場商務 (Business English)",
                en ? "Exam & Certification Prep (TOEIC/IELTS)" : "考試備考 (TOEIC/IELTS Speaking)"
        };
        final String[] values = {"daily", "travel", "business", "exam"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🎭 Choose Conversation Scenario" : "🎭 選擇情境模式");
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
                if ("shadowing".equals(values[which])) {
                    showReadingTextDialog();
                }
            }
        });
        builder.show();
    }

    private void showReadingTextDialog() {
        final boolean en = I18n.isEnglish(this);
        String currentLang = AppConfig.getTutorLanguage(this);
        String langLabel = getLanguageLabel(currentLang);

        final String[] options = {
                en ? "✨ AI Story: Daily Life & Hobbies (Fluent Paragraph, 4-6 sentences)" : "✨ AI 生成：日常生活與愛好故事 (4~6句流暢段落)",
                en ? "✨ AI Story: Travel Adventure & Dining (4-6 sentences)" : "✨ AI 生成：出國旅行與在地探索 (4~6句流暢段落)",
                en ? "✨ AI Story: Workplace, Tech & Vision (Advanced Flow)" : "✨ AI 生成：職場觀點與科技革新 (進階段落)",
                en ? "🔥 AI Drill: Spartan Linking & Phonetics Challenge" : "🔥 AI 生成：斯巴達發音重音與連音特訓 (高語流)",
                en ? "🎯 AI Custom Scenario / Topic..." : "🎯 AI 自訂情境出題（輸入任意主題…）",
                en ? "📋 Paste / Enter Custom Passage" : "📋 手動輸入 / 從剪貼簿貼上文章…"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "📖 Select or Generate Reading Text" : "📖 選擇或生成「" + langLabel + "」朗讀教材");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    triggerAiGenerateMaterial("daily", "intermediate");
                } else if (which == 1) {
                    triggerAiGenerateMaterial("travel", "intermediate");
                } else if (which == 2) {
                    triggerAiGenerateMaterial("business", "advanced");
                } else if (which == 3) {
                    triggerAiGenerateMaterial("phonetics", "advanced");
                } else if (which == 4) {
                    showCustomAiPromptDialog();
                } else {
                    showCustomReadingInputDialog();
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private void triggerAiGenerateMaterial(String topic, String level) {
        final boolean en = I18n.isEnglish(this);
        Toast.makeText(this, en ? "🤖 AI tutor is generating reading passage..." : "🤖 AI 外師正在為您生成量身練習文章…", Toast.LENGTH_SHORT).show();
        ReadingMaterialGenerator.generateAsync(this, topic, level, new ReadingMaterialGenerator.GenerateCallback() {
            @Override public void onSuccess(String generatedText) {
                AppConfig.setReadingText(MainActivity.this, generatedText);
                renderHomePage();
                Toast.makeText(MainActivity.this, en ? "✨ AI Reading material generated!" : "✨ AI 朗讀教材生成成功！", Toast.LENGTH_SHORT).show();
            }

            @Override public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCustomAiPromptDialog() {
        final boolean en = I18n.isEnglish(this);
        String currentLang = AppConfig.getTutorLanguage(this);
        String langLabel = getLanguageLabel(currentLang);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? "🎯 AI Custom Topic Generator" : "🎯 AI 自訂主題出題 (" + langLabel + ")");
        builder.setMessage(en ? "Enter the topic or scenario you want the AI tutor to generate (e.g. 'ordering ramen in Tokyo', 'job interview', 'buying coffee'):"
                : "請輸入任何您想練習的情境主題（例如：日本拉麵點餐、機場辦理登機、咖啡廳聊天、面試自我介紹）：");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        final EditText input = new EditText(this);
        input.setHint(en ? "e.g. Ordering food at an Italian restaurant" : "例如：在義大利餐廳點餐、週末爬山放鬆");
        input.setTextColor(Color.WHITE);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable iBg = new GradientDrawable();
        iBg.setColor(Color.parseColor("#1E293B"));
        iBg.setCornerRadius(dp(10));
        input.setBackground(iBg);
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton(en ? "✨ Generate with AI" : "✨ AI 立即出題", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String prompt = input.getText().toString().trim();
                if (!prompt.isEmpty()) {
                    triggerAiGenerateMaterial(prompt, "intermediate");
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    private void showCustomReadingInputDialog() {
        final boolean en = I18n.isEnglish(this);
        String currentLang = AppConfig.getTutorLanguage(this);
        String langLabel = getLanguageLabel(currentLang);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(en ? ("✏️ Custom Reading Text (" + langLabel + ")") : ("✏️ 自訂「" + langLabel + "」朗讀短文"));
        builder.setMessage(en ? ("Paste or enter the " + langLabel + " passage you want to practice reading aloud:") : ("請輸入或貼上您想大聲朗讀練習的「" + langLabel + "」文章："));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        final EditText input = new EditText(this);
        input.setText(AppConfig.getReadingText(this));
        input.setHint(en ? ("Paste " + langLabel + " text here...") : ("在此貼上「" + langLabel + "」練習短文…"));
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
        builder.setPositiveButton(en ? "Save & Apply" : "套用文章", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String custom = input.getText().toString().trim();
                if (!custom.isEmpty()) {
                    AppConfig.setReadingText(MainActivity.this, custom);
                    renderHomePage();
                    Toast.makeText(MainActivity.this, en ? "Custom reading text applied!" : "已套用自訂朗讀文章！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
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

    @Override
    protected void onDestroy() {
        if (previewTts != null) {
            try { previewTts.stop(); previewTts.shutdown(); } catch (Exception ignored) {}
            previewTts = null;
        }
        super.onDestroy();
    }
}
