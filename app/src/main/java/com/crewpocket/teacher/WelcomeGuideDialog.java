package com.crewpocket.teacher;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Modern, interactive Welcome & Onboarding Wizard (Option 1).
 * Guides new users to grab their free Gemini API Key, explains core value props,
 * and launches the AI Onboarding Guide Tutor in 1 click!
 */
public class WelcomeGuideDialog {

    private static int dp(Context context, float val) {
        return CrewTheme.dp(context, val);
    }

    public static void show(final Activity activity, final Runnable onDismissCallback) {
        if (activity == null || activity.isFinishing()) return;
        final boolean en = I18n.isEnglish(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E6060911")));
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 20));

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 20), dp(activity, 18), dp(activity, 20));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A")); // Slate 900
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
        container.setBackground(cBg);

        // 1. Header with App Brand
        LinearLayout headRow = new LinearLayout(activity);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView brandIcon = new TextView(activity);
        brandIcon.setText("🎓");
        brandIcon.setTextSize(26);
        brandIcon.setPadding(0, 0, dp(activity, 10), 0);
        headRow.addView(brandIcon);

        LinearLayout brandCol = new LinearLayout(activity);
        brandCol.setOrientation(LinearLayout.VERTICAL);

        TextView brandTitle = new TextView(activity);
        brandTitle.setText(en ? "Welcome to Crew Teacher" : "歡迎加入 Crew Teacher");
        brandTitle.setTextSize(18);
        brandTitle.setTextColor(Color.WHITE);
        brandTitle.setTypeface(Typeface.DEFAULT_BOLD);
        brandCol.addView(brandTitle);

        TextView brandSub = new TextView(activity);
        brandSub.setText(en ? "AI 1-on-1 Oral Coach · Fast Onboarding" : "隨身 AI 外語口說教練 · 1 分鐘新手入學指南");
        brandSub.setTextSize(11);
        brandSub.setTextColor(Color.parseColor("#94A3B8"));
        brandCol.addView(brandSub);
        headRow.addView(brandCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeX = new TextView(activity);
        closeX.setText("✕");
        closeX.setTextSize(18);
        closeX.setTextColor(Color.parseColor("#64748B"));
        closeX.setPadding(dp(activity, 8), 0, dp(activity, 4), 0);
        closeX.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                if (onDismissCallback != null) onDismissCallback.run();
            }
        });
        headRow.addView(closeX);
        container.addView(headRow);

        // 2. Welcome Hero Card
        LinearLayout heroCard = new LinearLayout(activity);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColors(new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#312E81")});
        hBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        hBg.setCornerRadius(dp(activity, 14));
        hBg.setStroke(dp(activity, 1), Color.parseColor("#6366F1"));
        heroCard.setBackground(hBg);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(0, dp(activity, 14), 0, dp(activity, 12));
        heroCard.setLayoutParams(hLp);

        TextView hTitle = new TextView(activity);
        hTitle.setText(en ? "✨ Your 1-on-1 AI Tutor is Ready Online!" : "✨ 您的專屬 1 對 1 外師已在線待命！");
        hTitle.setTextSize(15);
        hTitle.setTextColor(Color.parseColor("#38BDF8"));
        hTitle.setTypeface(Typeface.DEFAULT_BOLD);
        heroCard.addView(hTitle);

        TextView hDesc = new TextView(activity);
        hDesc.setText(en ? "Practice real-time speaking anywhere with natural pronunciation coaching, native language explanations, and full session diagnostics."
                : "打破開口障礙！隨身真人級語音外師，支援 30 款音色沉浸對練、中文即時文法解說、道地重述對照與跨 App 桌面懸浮球。");
        hDesc.setTextSize(12);
        hDesc.setTextColor(Color.parseColor("#E0E7FF"));
        hDesc.setLineSpacing(dp(activity, 2), 1.2f);
        hDesc.setPadding(0, dp(activity, 4), 0, 0);
        heroCard.addView(hDesc);
        container.addView(heroCard);

        // 3. Feature Highlights
        LinearLayout featuresCol = new LinearLayout(activity);
        featuresCol.setOrientation(LinearLayout.VERTICAL);
        featuresCol.addView(makeFeatureRow(activity, "🎙️", en ? "30 Authentic Voice Personas" : "30 款頂級真人音色", en ? "Immersive natural conversations & tone selection" : "支援美式、英式等 30 位外師，語調生動自然"));
        featuresCol.addView(makeFeatureRow(activity, "🗣️", en ? "Native Language Explanation" : "支援中文/母語即時解說", en ? "Say '請用中文解釋' anytime to get instant grammar analysis" : "對話中隨時說「請用中文解釋」，外師親切為您拆解"));
        featuresCol.addView(makeFeatureRow(activity, "📊", en ? "Post-Session Diagnostic Report" : "課後 AI 學習成效診斷", en ? "Fluency, Vocab & Grammar radar with 1-tap phrasebook save" : "發音/流暢度雷達評分、道地說法對照與金句一鍵收藏"));
        featuresCol.addView(makeFeatureRow(activity, "🫧", en ? "Desktop Floating Bubble" : "桌面即時懸浮球助教", en ? "Practice speaking over YouTube, Browser, or Social apps" : "看英文網頁、看影片隨時點擊外師展開對話"));
        container.addView(featuresCol);

        // 4. API Key Setup Section
        TextView keyHeader = new TextView(activity);
        keyHeader.setText(en ? "🔑 30-Second Free Setup (Gemini API Key)" : "🔑 30 秒免費啟用 AI 導師大腦 (Gemini API Key)");
        keyHeader.setTextSize(13);
        keyHeader.setTextColor(Color.parseColor("#FBBF24"));
        keyHeader.setTypeface(Typeface.DEFAULT_BOLD);
        keyHeader.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
        container.addView(keyHeader);

        TextView keyHint = new TextView(activity);
        keyHint.setText(en ? "Crew Teacher uses Google's latest Gemini 3.1 Live Preview model. Get your free personal API key directly from Google AI Studio (No credit card needed, 100% free)."
                : "Crew Teacher 搭載 Google 官方最新 Gemini 3.1 Live 即時語音引擎。新用戶可直接從 Google AI Studio 免費領取個人 API Key（完全免費、無廣告、免信用卡）。");
        keyHint.setTextSize(11);
        keyHint.setTextColor(Color.parseColor("#94A3B8"));
        keyHint.setLineSpacing(dp(activity, 2), 1.15f);
        keyHint.setPadding(0, 0, 0, dp(activity, 10));
        container.addView(keyHint);

        // Action Buttons Row: Get Key & Paste
        LinearLayout keyBtnRow = new LinearLayout(activity);
        keyBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button getKeyBtn = new Button(activity);
        getKeyBtn.setText(en ? "🌐 Free API Key" : "🌐 免費領取 Key");
        getKeyBtn.setTextSize(11);
        getKeyBtn.setTextColor(Color.WHITE);
        getKeyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gkb = new GradientDrawable();
        gkb.setColor(Color.parseColor("#2563EB")); // Blue 600
        gkb.setCornerRadius(dp(activity, 10));
        getKeyBtn.setBackground(gkb);
        getKeyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
                    activity.startActivity(browserIntent);
                    Toast.makeText(activity, en ? "Opening Google AI Studio..." : "正在開啟 Google AI Studio 領取頁面...", Toast.LENGTH_SHORT).show();
                } catch (Exception err) {
                    Toast.makeText(activity, "Please open: https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show();
                }
            }
        });
        keyBtnRow.addView(getKeyBtn, new LinearLayout.LayoutParams(0, dp(activity, 40), 1.1f));

        final EditText keyInput = new EditText(activity);

        Button pasteBtn = new Button(activity);
        pasteBtn.setText(en ? "📋 Paste" : "📋 貼上剪貼簿");
        pasteBtn.setTextSize(11);
        pasteBtn.setTextColor(Color.parseColor("#E2E8F0"));
        GradientDrawable pb = new GradientDrawable();
        pb.setColor(Color.parseColor("#334155"));
        pb.setCornerRadius(dp(activity, 10));
        pasteBtn.setBackground(pb);
        LinearLayout.LayoutParams pblp = new LinearLayout.LayoutParams(0, dp(activity, 40), 0.9f);
        pblp.setMargins(dp(activity, 8), 0, 0, 0);
        pasteBtn.setLayoutParams(pblp);
        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                    ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                    CharSequence text = item.getText();
                    if (text != null && text.length() > 0) {
                        keyInput.setText(text.toString().trim());
                        Toast.makeText(activity, en ? "Key pasted from clipboard!" : "已貼上剪貼簿內容！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(activity, en ? "Clipboard is empty" : "剪貼簿中無文字", Toast.LENGTH_SHORT).show();
            }
        });
        keyBtnRow.addView(pasteBtn);
        container.addView(keyBtnRow);

        // Key Input Box
        keyInput.setHint(en ? "Paste API Key here (AIzaSy...)" : "在此貼上 API Key (例如：AIzaSy...)");
        keyInput.setText(AppConfig.getGeminiApiKey(activity));
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.parseColor("#64748B"));
        keyInput.setTextSize(13);
        keyInput.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(Color.parseColor("#111827"));
        inBg.setCornerRadius(dp(activity, 10));
        inBg.setStroke(dp(activity, 1), Color.parseColor("#374151"));
        keyInput.setBackground(inBg);
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.setMargins(0, dp(activity, 8), 0, dp(activity, 14));
        keyInput.setLayoutParams(inLp);
        container.addView(keyInput);

        // 5. Main CTA: Launch AI Onboarding Guide Tutor!
        Button startAdvisorBtn = new Button(activity);
        startAdvisorBtn.setText(en ? "🚀 Start 1st Lesson with AI Guide Tutor" : "🚀 啟動 AI 導師 · 開啟第 1 堂課");
        startAdvisorBtn.setTextSize(14);
        startAdvisorBtn.setTextColor(Color.WHITE);
        startAdvisorBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sabBg = new GradientDrawable();
        sabBg.setColors(new int[]{Color.parseColor("#4F46E5"), Color.parseColor("#7C3AED")}); // Indigo to Violet gradient
        sabBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        sabBg.setCornerRadius(dp(activity, 14));
        startAdvisorBtn.setBackground(sabBg);
        LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48));
        saLp.setMargins(0, 0, 0, dp(activity, 8));
        startAdvisorBtn.setLayoutParams(saLp);
        startAdvisorBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String key = keyInput.getText().toString().trim();
                if (key.isEmpty()) {
                    Toast.makeText(activity, en ? "Please enter or paste your Gemini API Key first!" : "請先貼上或填入 Gemini API Key！", Toast.LENGTH_SHORT).show();
                    return;
                }
                AppConfig.setGeminiApiKey(activity, key);
                dialog.dismiss();

                // Launch directly into Guide Tutor Onboarding Mode!
                Intent intent = new Intent(activity, NativeLiveActivity.class);
                intent.putExtra("EXTRA_ONBOARDING_MODE", true);
                intent.putExtra("EXTRA_TUTOR_PERSONA", "guide");
                activity.startActivity(intent);

                if (onDismissCallback != null) onDismissCallback.run();
            }
        });
        container.addView(startAdvisorBtn);

        // Skip / Explore Home Button
        Button skipBtn = new Button(activity);
        skipBtn.setText(en ? "Explore Main Practice Hub" : "直接進入對話教室探索");
        skipBtn.setTextSize(12);
        skipBtn.setTextColor(Color.parseColor("#94A3B8"));
        skipBtn.setBackground(null);
        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String key = keyInput.getText().toString().trim();
                if (!key.isEmpty()) {
                    AppConfig.setGeminiApiKey(activity, key);
                }
                dialog.dismiss();
                if (onDismissCallback != null) onDismissCallback.run();
            }
        });
        container.addView(skipBtn);

        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static LinearLayout makeFeatureRow(Context context, String icon, String title, String desc) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 4), 0, dp(context, 8));

        TextView iconTv = new TextView(context);
        iconTv.setText(icon);
        iconTv.setTextSize(18);
        iconTv.setPadding(0, 0, dp(context, 10), 0);
        row.addView(iconTv);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tTv = new TextView(context);
        tTv.setText(title);
        tTv.setTextSize(12);
        tTv.setTextColor(Color.WHITE);
        tTv.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tTv);

        TextView dTv = new TextView(context);
        dTv.setText(desc);
        dTv.setTextSize(11);
        dTv.setTextColor(Color.parseColor("#94A3B8"));
        textCol.addView(dTv);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }
}
