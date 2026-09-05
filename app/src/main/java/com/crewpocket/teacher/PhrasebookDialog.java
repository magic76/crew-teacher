package com.crewpocket.teacher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class PhrasebookDialog {

    public static void show(final Activity activity, final Runnable onDismiss) {
        final boolean en = I18n.isEnglish(activity);
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D0000000")));
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 24));

        final LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
        container.setBackground(cBg);

        final Runnable renderList = new Runnable() {
            @Override public void run() {
                container.removeAllViews();

                // Header
                LinearLayout headRow = new LinearLayout(activity);
                headRow.setOrientation(LinearLayout.HORIZONTAL);
                headRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView title = new TextView(activity);
                title.setText(en ? "⭐ My Starred Phrasebook" : "⭐ 我的個人生詞與金句本");
                title.setTextSize(16);
                title.setTextColor(Color.WHITE);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                headRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView closeBtn = new TextView(activity);
                closeBtn.setText("✕");
                closeBtn.setTextSize(18);
                closeBtn.setTextColor(Color.parseColor("#94A3B8"));
                closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                        if (onDismiss != null) onDismiss.run();
                    }
                });
                headRow.addView(closeBtn);
                container.addView(headRow);

                final List<LearningDataManager.StarredItem> items = LearningDataManager.getStarredItems(activity);

                TextView subtitle = new TextView(activity);
                subtitle.setText(en ? ("Collected " + items.size() + " items · Tap 🔊 to listen to authentic pronunciation")
                        : ("累計收藏 " + items.size() + " 條精選金句與生詞 · 點擊 🔊 聽母語發音"));
                subtitle.setTextSize(11);
                subtitle.setTextColor(Color.parseColor("#94A3B8"));
                subtitle.setPadding(0, dp(activity, 4), 0, dp(activity, 10));
                container.addView(subtitle);

                if (!items.isEmpty()) {
                    LinearLayout btnGrid = new LinearLayout(activity);
                    btnGrid.setOrientation(LinearLayout.HORIZONTAL);
                    btnGrid.setPadding(0, 0, 0, dp(activity, 12));

                    Button shadowBtn = new Button(activity);
                    shadowBtn.setText(en ? "🎙️ Read-Along Drill" : "🎙️ 照句跟讀練習");
                    shadowBtn.setTextSize(12);
                    shadowBtn.setTextColor(Color.WHITE);
                    shadowBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable sbg = new GradientDrawable();
                    sbg.setColor(Color.parseColor("#7C3AED"));
                    sbg.setCornerRadius(dp(activity, 10));
                    shadowBtn.setBackground(sbg);
                    shadowBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            OralCoachHelper.showShadowingLabDialog(activity, null, en ? "My Starred" : "我的收藏金句");
                        }
                    });
                    btnGrid.addView(shadowBtn, new LinearLayout.LayoutParams(0, dp(activity, 40), 1f));

                    Button quizBtn = new Button(activity);
                    quizBtn.setText(en ? "🃏 Flashcard Quiz" : "🃏 翻卡測驗遊戲");
                    quizBtn.setTextSize(12);
                    quizBtn.setTextColor(Color.WHITE);
                    quizBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable qbg = new GradientDrawable();
                    qbg.setColor(Color.parseColor("#4F46E5"));
                    qbg.setCornerRadius(dp(activity, 10));
                    quizBtn.setBackground(qbg);
                    LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(0, dp(activity, 40), 1f);
                    qlp.setMargins(dp(activity, 8), 0, 0, 0);
                    quizBtn.setLayoutParams(qlp);
                    quizBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            OralCoachHelper.showFlashcardQuizDialog(activity);
                        }
                    });
                    btnGrid.addView(quizBtn);

                    container.addView(btnGrid);
                }

                if (items.isEmpty()) {
                    LinearLayout emptyCard = new LinearLayout(activity);
                    emptyCard.setOrientation(LinearLayout.VERTICAL);
                    emptyCard.setPadding(dp(activity, 20), dp(activity, 24), dp(activity, 20), dp(activity, 24));
                    emptyCard.setGravity(Gravity.CENTER);
                    GradientDrawable eBg = new GradientDrawable();
                    eBg.setColor(Color.parseColor("#1E293B"));
                    eBg.setCornerRadius(dp(activity, 14));
                    emptyCard.setBackground(eBg);

                    TextView eIcon = new TextView(activity);
                    eIcon.setText("⭐");
                    eIcon.setTextSize(32);
                    emptyCard.addView(eIcon);

                    TextView eText = new TextView(activity);
                    eText.setText(en ? "No starred items yet.\nDuring conversation practice or in session diagnostic reports, tap ⭐ to collect practical sentences here!"
                            : "尚無收藏項目。\n在口語對話教室或課後 AI 診斷報告中，點擊 ★ 即可收藏至專屬生詞本！");
                    eText.setTextSize(12);
                    eText.setTextColor(Color.parseColor("#94A3B8"));
                    eText.setGravity(Gravity.CENTER);
                    eText.setLineSpacing(dp(activity, 3), 1.2f);
                    eText.setPadding(0, dp(activity, 8), 0, 0);
                    emptyCard.addView(eText);

                    container.addView(emptyCard);
                } else {
                    for (final LearningDataManager.StarredItem item : items) {
                        LinearLayout itemCard = new LinearLayout(activity);
                        itemCard.setOrientation(LinearLayout.VERTICAL);
                        itemCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
                        GradientDrawable iBg = new GradientDrawable();
                        iBg.setColor(Color.parseColor("#1E293B"));
                        iBg.setCornerRadius(dp(activity, 12));
                        iBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                        itemCard.setBackground(iBg);
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        ilp.setMargins(0, 0, 0, dp(activity, 10));
                        itemCard.setLayoutParams(ilp);

                        // Tag & Action row
                        LinearLayout tagRow = new LinearLayout(activity);
                        tagRow.setOrientation(LinearLayout.HORIZONTAL);
                        tagRow.setGravity(Gravity.CENTER_VERTICAL);

                        TextView tagTv = new TextView(activity);
                        String catLabel = "correction".equals(item.category) ? (en ? "✨ Native Recast" : "✨ 道地糾錯")
                                : ("hint".equals(item.category) ? (en ? "💡 Reply Hint" : "💡 回答小抄")
                                : ("vocab".equals(item.category) ? (en ? "📖 Vocabulary" : "📖 重點單字") : (en ? "💬 Phrase" : "💬 實用金句")));
                        tagTv.setText(catLabel);
                        tagTv.setTextSize(10);
                        tagTv.setTextColor("correction".equals(item.category) ? Color.parseColor("#F472B6") : Color.parseColor("#FBBF24"));
                        tagTv.setTypeface(Typeface.DEFAULT_BOLD);
                        tagRow.addView(tagTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                        Button playBtn = new Button(activity);
                        playBtn.setText("🔊");
                        playBtn.setTextSize(11);
                        playBtn.setTextColor(Color.WHITE);
                        GradientDrawable pbBg = new GradientDrawable();
                        pbBg.setColor(Color.parseColor("#4F46E5"));
                        pbBg.setCornerRadius(dp(activity, 6));
                        playBtn.setBackground(pbBg);
                        playBtn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) { OralCoachHelper.speak(activity, item.originalText, 1.0f); }
                        });
                        tagRow.addView(playBtn, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 28)));

                        Button delBtn = new Button(activity);
                        delBtn.setText("🗑️");
                        delBtn.setTextSize(11);
                        delBtn.setTextColor(Color.WHITE);
                        GradientDrawable dbBg = new GradientDrawable();
                        dbBg.setColor(Color.parseColor("#334155"));
                        dbBg.setCornerRadius(dp(activity, 6));
                        delBtn.setBackground(dbBg);
                        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 28));
                        dlp.setMargins(dp(activity, 6), 0, 0, 0);
                        delBtn.setLayoutParams(dlp);
                        delBtn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                LearningDataManager.removeStarredItemById(activity, item.id);
                                run();
                            }
                        });
                        tagRow.addView(delBtn);

                        itemCard.addView(tagRow);

                        // Original text
                        TextView textTv = new TextView(activity);
                        textTv.setText(item.originalText);
                        textTv.setTextSize(14);
                        textTv.setTextColor(Color.WHITE);
                        textTv.setTypeface(Typeface.DEFAULT_BOLD);
                        textTv.setPadding(0, dp(activity, 4), 0, dp(activity, 2));
                        itemCard.addView(textTv);

                        // Translation
                        if (!item.translation.isEmpty()) {
                            TextView transTv = new TextView(activity);
                            transTv.setText(item.translation);
                            transTv.setTextSize(12);
                            transTv.setTextColor(Color.parseColor("#94A3B8"));
                            transTv.setPadding(0, 0, 0, dp(activity, 2));
                            itemCard.addView(transTv);
                        }

                        // Notes
                        if (!item.notes.isEmpty()) {
                            TextView notesTv = new TextView(activity);
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

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
