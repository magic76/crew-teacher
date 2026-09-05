package com.crewpocket.teacher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class SessionHistoryDialog {

    public interface ReportViewListener {
        void onOpenReport(LearningDataManager.SessionRecord record);
    }

    public static void show(final Activity activity, final ReportViewListener listener) {
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

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.parseColor("#0F172A"));
        cBg.setCornerRadius(dp(activity, 20));
        cBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
        container.setBackground(cBg);

        // Header
        LinearLayout headRow = new LinearLayout(activity);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(en ? "📊 Session History & Diagnostics" : "📊 歷史對話與成效報告");
        title.setTextSize(16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final List<LearningDataManager.SessionRecord> history = LearningDataManager.getSessionHistory(activity);

        if (!history.isEmpty()) {
            TextView clearBtn = new TextView(activity);
            clearBtn.setText(en ? "🗑️ Clear" : "🗑️ 清空");
            clearBtn.setTextSize(12);
            clearBtn.setTextColor(Color.parseColor("#F87171"));
            clearBtn.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));
            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle(en ? "Clear Session History" : "清空歷史記錄")
                            .setMessage(en ? "Are you sure you want to delete all past tutoring records?" : "確定要清除所有過往口語對話歷史記錄嗎？")
                            .setPositiveButton(en ? "Clear" : "確認清空", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int which) {
                                    LearningDataManager.clearSessionHistory(activity);
                                    dialog.dismiss();
                                    show(activity, listener);
                                }
                            })
                            .setNegativeButton(en ? "Cancel" : "取消", null)
                            .show();
                }
            });
            headRow.addView(clearBtn);
        }

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        headRow.addView(closeBtn);
        container.addView(headRow);

        TextView subtitle = new TextView(activity);
        subtitle.setText(en ? ("Total " + history.size() + " completed tutoring sessions · Tap any card to review full report")
                : ("累計 " + history.size() + " 堂完成對話 · 點擊任一卡片可隨時重溫完整診斷報告"));
        subtitle.setTextSize(11);
        subtitle.setTextColor(Color.parseColor("#94A3B8"));
        subtitle.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        container.addView(subtitle);

        if (history.isEmpty()) {
            LinearLayout emptyCard = new LinearLayout(activity);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setPadding(dp(activity, 20), dp(activity, 24), dp(activity, 20), dp(activity, 24));
            emptyCard.setGravity(Gravity.CENTER);
            GradientDrawable eBg = new GradientDrawable();
            eBg.setColor(Color.parseColor("#1E293B"));
            eBg.setCornerRadius(dp(activity, 14));
            emptyCard.setBackground(eBg);

            TextView eIcon = new TextView(activity);
            eIcon.setText("📊");
            eIcon.setTextSize(32);
            emptyCard.addView(eIcon);

            TextView eText = new TextView(activity);
            eText.setText(en ? "No tutoring sessions yet.\nStart a practice session from home to generate your first AI diagnostic report!"
                    : "尚無課堂記錄。\n從首頁點擊開始口語對話，完成後將自動生成專屬學習成效診斷報告！");
            eText.setTextSize(12);
            eText.setTextColor(Color.parseColor("#94A3B8"));
            eText.setGravity(Gravity.CENTER);
            eText.setLineSpacing(dp(activity, 3), 1.2f);
            eText.setPadding(0, dp(activity, 8), 0, 0);
            emptyCard.addView(eText);

            container.addView(emptyCard);
        } else {
            for (final LearningDataManager.SessionRecord record : history) {
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
                itemCard.setClickable(true);
                itemCard.setFocusable(true);

                // Top row: Scenario + Score
                LinearLayout topRow = new LinearLayout(activity);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView scenTv = new TextView(activity);
                scenTv.setText(getScenarioLabel(record.scenario, en));
                scenTv.setTextSize(13);
                scenTv.setTextColor(Color.parseColor("#38BDF8"));
                scenTv.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(scenTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView scoreBadge = new TextView(activity);
                scoreBadge.setText("🎯 " + record.overallScore + " 分");
                scoreBadge.setTextSize(12);
                scoreBadge.setTextColor(record.overallScore >= 80 ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24"));
                scoreBadge.setTypeface(Typeface.DEFAULT_BOLD);
                topRow.addView(scoreBadge);

                itemCard.addView(topRow);

                // Meta row: Date + duration + turns
                int mins = record.durationSeconds / 60;
                int secs = record.durationSeconds % 60;
                String timeStr = mins > 0 ? (mins + "分" + secs + "秒") : (secs + "秒");
                TextView metaTv = new TextView(activity);
                metaTv.setText(record.dateString + " · ⏱️ " + timeStr + " · 💬 " + record.userTurns + " 輪對話");
                metaTv.setTextSize(11);
                metaTv.setTextColor(Color.parseColor("#94A3B8"));
                metaTv.setPadding(0, dp(activity, 2), 0, dp(activity, 4));
                itemCard.addView(metaTv);

                // Summary snippet
                if (!record.summary.isEmpty()) {
                    TextView sumTv = new TextView(activity);
                    sumTv.setText(record.summary);
                    sumTv.setTextSize(12);
                    sumTv.setTextColor(Color.parseColor("#E2E8F0"));
                    sumTv.setMaxLines(2);
                    sumTv.setEllipsize(TextUtils.TruncateAt.END);
                    sumTv.setPadding(0, 0, 0, dp(activity, 4));
                    itemCard.addView(sumTv);
                }

                // View Details link
                TextView viewDetailTv = new TextView(activity);
                viewDetailTv.setText(en ? "Tap to view full diagnostic report ›" : "點擊查看完整診斷報告與生詞 ›");
                viewDetailTv.setTextSize(11);
                viewDetailTv.setTextColor(Color.parseColor("#818CF8"));
                viewDetailTv.setGravity(Gravity.RIGHT);
                itemCard.addView(viewDetailTv);

                itemCard.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (listener != null) {
                            listener.onOpenReport(record);
                        }
                    }
                });

                container.addView(itemCard);
            }
        }

        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static String getScenarioLabel(String scenario, boolean en) {
        if ("travel".equalsIgnoreCase(scenario)) return en ? "✈️ Travel & Dining" : "✈️ 出國旅遊";
        if ("business".equalsIgnoreCase(scenario)) return en ? "💼 Workplace Business" : "💼 職場商務";
        if ("interview".equalsIgnoreCase(scenario)) return en ? "👔 Job Interview" : "👔 求職面試";
        if ("exam".equalsIgnoreCase(scenario)) return en ? "🎯 Exam Prep" : "🎯 口說備考";
        if ("shopping".equalsIgnoreCase(scenario)) return en ? "🛍️ Shopping & Returns" : "🛍️ 購物退稅";
        if ("medical".equalsIgnoreCase(scenario)) return en ? "🏥 Doctor Visit" : "🏥 醫院看診";
        if ("housing".equalsIgnoreCase(scenario)) return en ? "🏠 Renting & Housing" : "🏠 租屋看房";
        if ("dating".equalsIgnoreCase(scenario)) return en ? "💬 Dating & Social" : "💬 社交破冰";
        if ("tech".equalsIgnoreCase(scenario)) return en ? "🤖 Tech & AI" : "🤖 科技創新";
        return en ? "☕ Daily Life" : "☕ 日常生活";
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
