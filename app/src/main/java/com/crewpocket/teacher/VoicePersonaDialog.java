package com.crewpocket.teacher;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class VoicePersonaDialog {

    private static int currentVoiceFilterTab = 0; // 0: All, 1: Female, 2: Male
    private static TextToSpeech previewTts;

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

    public static String getVoiceDisplayName(String voiceName, boolean en) {
        if (voiceName == null || voiceName.isEmpty()) voiceName = AppConfig.DEFAULT_VOICE;
        for (VoiceInfo v : ALL_VOICES) {
            if (v.name.equalsIgnoreCase(voiceName)) {
                return (v.isFemale ? "👩 " : "👨 ") + v.name + " · " + (en ? v.enDesc : v.zhDesc);
            }
        }
        return "👩 " + voiceName;
    }

    private static void playAudition(final Context context, final VoiceInfo voice) {
        if (voice == null) return;
        if (previewTts == null) {
            previewTts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        speakVoiceSample(context, voice);
                    }
                }
            });
        } else {
            speakVoiceSample(context, voice);
        }
    }

    private static void speakVoiceSample(Context context, VoiceInfo voice) {
        if (previewTts == null || voice == null) return;
        try {
            previewTts.stop();
            previewTts.setPitch(voice.pitch);
            previewTts.setSpeechRate(1.0f);
            if (I18n.isEnglish(context)) {
                previewTts.setLanguage(Locale.US);
                previewTts.speak("Hello! I am " + voice.name + ", your 1-on-1 language tutor. Let's practice speaking!", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            } else {
                previewTts.setLanguage(Locale.TRADITIONAL_CHINESE);
                previewTts.speak("你好！我是 " + voice.name + "，你的 AI 一對一口語導師，讓我們開始練習吧！", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            }
        } catch (Exception ignored) {}
    }

    public static void show(final Context context, final Runnable onVoiceSelected) {
        final boolean en = I18n.isEnglish(context);
        final int dp16 = dp(context, 16);
        final int dp10 = dp(context, 10);
        final int dp12 = dp(context, 12);
        final int dp8 = dp(context, 8);
        final int dp4 = dp(context, 4);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp16, dp16, dp16, dp10);
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);

        TextView titleView = new TextView(context);
        titleView.setText(en ? "🗣️ Select Tutor Voice (30 Voices)" : "🗣️ 導師語音音色選擇 (全 30 款)");
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(CrewTheme.TEXT_PRIMARY);
        titleView.setPadding(0, 0, 0, dp4);
        root.addView(titleView);

        TextView subtitleView = new TextView(context);
        subtitleView.setText(en ? "Tap '▶️ Play' to preview pitch/tone. Tap card to select." : "點擊「▶️ 試聽」可播放聲音，點擊卡片直接選用。");
        subtitleView.setTextSize(11);
        subtitleView.setTextColor(CrewTheme.TEXT_SECONDARY);
        subtitleView.setPadding(0, 0, 0, dp12);
        root.addView(subtitleView);

        // Filter Tabs Row
        final LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setPadding(0, 0, 0, dp10);
        root.addView(tabsRow);

        final String currentVoice = AppConfig.getVoiceName(context);
        ScrollView scrollList = new ScrollView(context);
        scrollList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 340)));
        final LinearLayout listContainer = new LinearLayout(context);
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

                    LinearLayout itemCard = new LinearLayout(context);
                    itemCard.setOrientation(LinearLayout.HORIZONTAL);
                    itemCard.setGravity(Gravity.CENTER_VERTICAL);
                    itemCard.setPadding(dp12, dp10, dp10, dp10);
                    int cardBg = isSelected ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A");
                    int borderCol = isSelected ? CrewTheme.CYAN_400 : Color.parseColor("#334155");
                    GradientDrawable itemBg = new GradientDrawable();
                    itemBg.setColor(cardBg);
                    itemBg.setCornerRadius(dp12);
                    itemBg.setStroke(dp(context, isSelected ? 2 : 1), borderCol);
                    itemCard.setBackground(itemBg);

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    cardLp.setMargins(0, 0, 0, dp8);
                    itemCard.setLayoutParams(cardLp);

                    // Indicator
                    TextView indicator = new TextView(context);
                    indicator.setText(isSelected ? "●" : "○");
                    indicator.setTextSize(14);
                    indicator.setTextColor(isSelected ? CrewTheme.CYAN_400 : CrewTheme.TEXT_MUTED);
                    indicator.setPadding(0, 0, dp10, 0);
                    itemCard.addView(indicator);

                    // Text Info
                    LinearLayout infoCol = new LinearLayout(context);
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    itemCard.addView(infoCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                    TextView nameView = new TextView(context);
                    nameView.setText((voice.isFemale ? "👩 " : "👨 ") + voice.name);
                    nameView.setTextSize(13);
                    nameView.setTypeface(Typeface.DEFAULT_BOLD);
                    nameView.setTextColor(isSelected ? CrewTheme.CYAN_400 : CrewTheme.TEXT_PRIMARY);
                    infoCol.addView(nameView);

                    TextView descView = new TextView(context);
                    descView.setText(en ? voice.enDesc : voice.zhDesc);
                    descView.setTextSize(10);
                    descView.setTextColor(CrewTheme.TEXT_SECONDARY);
                    descView.setPadding(0, dp(context, 2), 0, 0);
                    infoCol.addView(descView);

                    // Audition Button
                    Button previewBtn = new Button(context);
                    previewBtn.setText("▶️ " + (en ? "Play" : "試聽"));
                    previewBtn.setTextSize(11);
                    previewBtn.setTextColor(Color.WHITE);
                    previewBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    previewBtn.setAllCaps(false);
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#1E293B"));
                    pBg.setCornerRadius(dp8);
                    pBg.setStroke(dp(context, 1), Color.parseColor("#475569"));
                    previewBtn.setBackground(pBg);
                    previewBtn.setPadding(dp8, dp4, dp8, dp4);
                    previewBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            playAudition(context, voice);
                        }
                    });
                    itemCard.addView(previewBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 34)));

                    // Click item to select
                    itemCard.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            AppConfig.setVoiceName(context, voice.name);
                            Toast.makeText(context, (en ? "✅ Switched voice to: " : "✅ 已選用音色：") + voice.name, Toast.LENGTH_SHORT).show();
                            if (dialogRef[0] != null) dialogRef[0].dismiss();
                            if (onVoiceSelected != null) onVoiceSelected.run();
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
            Button tabBtn = new Button(context);
            tabBtn.setText(tabLabels[t]);
            tabBtn.setTextSize(10);
            tabBtn.setAllCaps(false);
            tabButtons[t] = tabBtn;

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(context, 32), 1f);
            if (t > 0) tlp.setMargins(dp4, 0, 0, 0);
            tabBtn.setLayoutParams(tlp);

            tabBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    currentVoiceFilterTab = tabIndex;
                    for (int i = 0; i < 3; i++) {
                        boolean active = i == currentVoiceFilterTab;
                        GradientDrawable tabBg = new GradientDrawable();
                        tabBg.setColor(active ? Color.parseColor("#0284C7") : Color.parseColor("#1E293B"));
                        tabBg.setCornerRadius(dp8);
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
            tabBg.setCornerRadius(dp8);
            tabButtons[i].setBackground(tabBg);
            tabButtons[i].setTextColor(Color.WHITE);
        }

        refreshList.run();

        builder.setView(root);
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        dialogRef[0] = builder.show();
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
