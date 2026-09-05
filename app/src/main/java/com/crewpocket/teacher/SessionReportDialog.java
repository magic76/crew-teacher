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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionReportDialog {

    public static void show(final Activity activity, final LearningDataManager.SessionRecord record,
                            final boolean isLessonMode, final CourseModel.Lesson currentLesson) {
        if (record == null || activity == null || activity.isFinishing()) return;
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

        final List<LearningDataManager.StarredItem> allReportItems = new ArrayList<LearningDataManager.StarredItem>();
        final List<Button> allStarButtons = new ArrayList<Button>();

        // 1. Header Bar: Title & Close Button
        LinearLayout headRow = new LinearLayout(activity);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(en ? "📊 AI Learning Diagnostic Report" : "📊 課後 AI 學習成效診斷報告");
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
            CourseManager.saveLessonProgress(activity, currentLesson.id, stars, record.overallScore);
            CourseModel.LessonProgress prog = CourseManager.getLessonProgress(activity, currentLesson.id);

            LinearLayout lessonSuccessCard = new LinearLayout(activity);
            lessonSuccessCard.setOrientation(LinearLayout.VERTICAL);
            lessonSuccessCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
            GradientDrawable lbg = new GradientDrawable();
            lbg.setColor(Color.parseColor("#064E3B"));
            lbg.setCornerRadius(dp(activity, 14));
            lbg.setStroke(dp(activity, 1), Color.parseColor("#059669"));
            lessonSuccessCard.setBackground(lbg);

            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.setMargins(0, dp(activity, 10), 0, 0);
            lessonSuccessCard.setLayoutParams(llp);

            TextView lTitle = new TextView(activity);
            StringBuilder sb = new StringBuilder();
            for (int s = 0; s < 3; s++) sb.append(s < prog.stars ? "⭐" : "☆");
            lTitle.setText("🎉 " + (en ? "Lesson Result: " : "關卡通關評定：") + sb.toString());
            lTitle.setTextSize(14);
            lTitle.setTextColor(Color.WHITE);
            lTitle.setTypeface(Typeface.DEFAULT_BOLD);
            lessonSuccessCard.addView(lTitle);

            TextView lSub = new TextView(activity);
            lSub.setText((en ? "Missions achieved: " : "達成目標：") + achievedCount + "/" + currentLesson.missions.size()
                    + (prog.stars >= 1 ? (en ? " · Next lesson unlocked! 🔓" : " · 下一關卡已成功解鎖！🔓") : ""));
            lSub.setTextSize(12);
            lSub.setTextColor(Color.parseColor("#A7F3D0"));
            lSub.setPadding(0, dp(activity, 2), 0, 0);
            lessonSuccessCard.addView(lSub);

            container.addView(lessonSuccessCard);
        }

        // 2. Score Hero Banner
        LinearLayout heroScoreCard = new LinearLayout(activity);
        heroScoreCard.setOrientation(LinearLayout.VERTICAL);
        heroScoreCard.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColors(new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#312E81")});
        hBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        hBg.setCornerRadius(dp(activity, 14));
        hBg.setStroke(dp(activity, 1), Color.parseColor("#6366F1"));
        heroScoreCard.setBackground(hBg);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hl.setMargins(0, dp(activity, 14), 0, dp(activity, 14));
        heroScoreCard.setLayoutParams(hl);

        LinearLayout scoreRow = new LinearLayout(activity);
        scoreRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView scoreVal = new TextView(activity);
        scoreVal.setText(String.valueOf(record.overallScore));
        scoreVal.setTextSize(36);
        scoreVal.setTextColor(Color.parseColor("#38BDF8"));
        scoreVal.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        scoreRow.addView(scoreVal);

        LinearLayout metaCol = new LinearLayout(activity);
        metaCol.setOrientation(LinearLayout.VERTICAL);
        metaCol.setPadding(dp(activity, 12), 0, 0, 0);

        TextView ratingTv = new TextView(activity);
        String ratingStr = record.overallScore >= 90 ? (en ? "🌟 Outstanding Mastery" : "🌟 表現優異 · 掌握自如")
                : (record.overallScore >= 75 ? (en ? "👍 Great Fluency" : "👍 表達良好 · 溝通順暢")
                : (en ? "💪 Keep Practicing" : "💪 嚴格診斷 · 持續精進"));
        ratingTv.setText(ratingStr);
        ratingTv.setTextSize(14);
        ratingTv.setTextColor(Color.WHITE);
        ratingTv.setTypeface(Typeface.DEFAULT_BOLD);
        metaCol.addView(ratingTv);

        TextView infoTv = new TextView(activity);
        int mins = record.durationSeconds / 60;
        int secs = record.durationSeconds % 60;
        String timeStr = mins > 0 ? (mins + "分" + secs + "秒") : (secs + "秒");
        infoTv.setText(record.dateString + " · " + timeStr + " · " + record.userTurns + " 輪互動");
        infoTv.setTextSize(11);
        infoTv.setTextColor(Color.parseColor("#C7D2FE"));
        infoTv.setPadding(0, dp(activity, 2), 0, 0);
        metaCol.addView(infoTv);

        scoreRow.addView(metaCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        heroScoreCard.addView(scoreRow);

        // 4 Diagnostic Progress Bars
        LinearLayout barsCol = new LinearLayout(activity);
        barsCol.setOrientation(LinearLayout.VERTICAL);
        barsCol.setPadding(0, dp(activity, 12), 0, 0);
        barsCol.addView(makeScoreBar(activity, en ? "Fluency 流暢度" : "流暢度 (Fluency)", record.fluencyScore, "#38BDF8"));
        barsCol.addView(makeScoreBar(activity, en ? "Vocabulary 詞彙量" : "詞彙量 (Vocabulary)", record.vocabScore, "#FBBF24"));
        barsCol.addView(makeScoreBar(activity, en ? "Grammar 文法準確" : "文法準確 (Grammar)", record.grammarScore, "#A78BFA"));
        barsCol.addView(makeScoreBar(activity, en ? "Phonetic 發音自然" : "發音自然 (Phonetic)", record.phoneticScore, "#34D399"));
        heroScoreCard.addView(barsCol);
        container.addView(heroScoreCard);

        // 3. Summary & Strengths
        if (!record.summary.isEmpty() || !record.strengths.isEmpty()) {
            LinearLayout sumCard = new LinearLayout(activity);
            sumCard.setOrientation(LinearLayout.VERTICAL);
            sumCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(Color.parseColor("#1E293B"));
            sBg.setCornerRadius(dp(activity, 12));
            sBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
            sumCard.setBackground(sBg);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(0, 0, 0, dp(activity, 12));
            sumCard.setLayoutParams(slp);

            if (!record.summary.isEmpty()) {
                TextView stTitle = new TextView(activity);
                stTitle.setText(en ? "📋 Overall Summary & Critique" : "📋 課堂綜合銳評診斷");
                stTitle.setTextSize(12);
                stTitle.setTextColor(Color.parseColor("#60A5FA"));
                stTitle.setTypeface(Typeface.DEFAULT_BOLD);
                sumCard.addView(stTitle);

                TextView stBody = new TextView(activity);
                stBody.setText(record.summary);
                stBody.setTextSize(13);
                stBody.setTextColor(Color.parseColor("#E2E8F0"));
                stBody.setLineSpacing(dp(activity, 2), 1.2f);
                stBody.setPadding(0, dp(activity, 2), 0, dp(activity, 8));
                sumCard.addView(stBody);
            }

            if (!record.strengths.isEmpty()) {
                TextView strTitle = new TextView(activity);
                strTitle.setText(en ? "💪 Strengths & Highlights" : "💪 優勢亮點與進步");
                strTitle.setTextSize(12);
                strTitle.setTextColor(Color.parseColor("#34D399"));
                strTitle.setTypeface(Typeface.DEFAULT_BOLD);
                sumCard.addView(strTitle);

                TextView strBody = new TextView(activity);
                strBody.setText(record.strengths);
                strBody.setTextSize(13);
                strBody.setTextColor(Color.parseColor("#E2E8F0"));
                strBody.setLineSpacing(dp(activity, 2), 1.2f);
                strBody.setPadding(0, dp(activity, 2), 0, 0);
                sumCard.addView(strBody);
            }

            container.addView(sumCard);
        }

        // 4. Recasts Corrections (道地重述對照)
        try {
            JSONArray recasts = new JSONArray(record.recastsJson != null ? record.recastsJson : "[]");
            if (recasts.length() > 0) {
                TextView rcTitle = new TextView(activity);
                rcTitle.setText(en ? "✨ Native Recast & Fixes (Tap ⭐ to save)" : "✨ 母語者道地重述對照（點擊 ⭐ 收藏）");
                rcTitle.setTextSize(13);
                rcTitle.setTextColor(Color.parseColor("#F472B6"));
                rcTitle.setTypeface(Typeface.DEFAULT_BOLD);
                rcTitle.setPadding(0, dp(activity, 4), 0, dp(activity, 8));
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

                    LinearLayout rcCard = new LinearLayout(activity);
                    rcCard.setOrientation(LinearLayout.VERTICAL);
                    rcCard.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                    GradientDrawable rBg = new GradientDrawable();
                    rBg.setColor(Color.parseColor("#1E293B"));
                    rBg.setCornerRadius(dp(activity, 10));
                    rBg.setStroke(dp(activity, 1), Color.parseColor("#374151"));
                    rcCard.setBackground(rBg);
                    LinearLayout.LayoutParams rclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rclp.setMargins(0, 0, 0, dp(activity, 8));
                    rcCard.setLayoutParams(rclp);

                    // Student original (red tint)
                    TextView oTv = new TextView(activity);
                    oTv.setText("🗣️ " + (en ? "You said: " : "原句：") + orig);
                    oTv.setTextSize(12);
                    oTv.setTextColor(Color.parseColor("#FCA5A5"));
                    rcCard.addView(oTv);

                    // Native recast row (green)
                    LinearLayout corRow = new LinearLayout(activity);
                    corRow.setOrientation(LinearLayout.HORIZONTAL);
                    corRow.setGravity(Gravity.CENTER_VERTICAL);
                    corRow.setPadding(0, dp(activity, 4), 0, 0);

                    TextView cTv = new TextView(activity);
                    cTv.setText("✨ " + (en ? "Native: " : "道地說法：") + corr);
                    cTv.setTextSize(13);
                    cTv.setTextColor(Color.parseColor("#6EE7B7"));
                    cTv.setTypeface(Typeface.DEFAULT_BOLD);
                    corRow.addView(cTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    // Listen button
                    Button playBtn = new Button(activity);
                    playBtn.setText("🔊");
                    playBtn.setTextSize(11);
                    playBtn.setTextColor(Color.WHITE);
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#4F46E5"));
                    pBg.setCornerRadius(dp(activity, 6));
                    playBtn.setBackground(pBg);
                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { OralCoachHelper.speak(activity, corr, 1.0f); }
                    });
                    corRow.addView(playBtn, new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 28)));

                    // Drill button (🎙️ 重練跟讀)
                    Button drillBtn = new Button(activity);
                    drillBtn.setText("🎙️");
                    drillBtn.setTextSize(11);
                    drillBtn.setTextColor(Color.WHITE);
                    GradientDrawable dBg = new GradientDrawable();
                    dBg.setColor(Color.parseColor("#0D9488"));
                    dBg.setCornerRadius(dp(activity, 6));
                    drillBtn.setBackground(dBg);
                    drillBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            OralCoachHelper.showPronunciationDrillDialog(activity, corr, orig, expl);
                        }
                    });
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 28));
                    dlp.setMargins(dp(activity, 3), 0, 0, 0);
                    corRow.addView(drillBtn, dlp);

                    // Star button
                    final boolean isSt = LearningDataManager.isStarred(activity, corr);
                    final Button starBtn = new Button(activity);
                    starBtn.setText(isSt ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isSt ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(activity, corr, orig, "correction", expl);
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(activity, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    corRow.addView(starBtn, new LinearLayout.LayoutParams(dp(activity, 34), dp(activity, 28)));
                    allStarButtons.add(starBtn);
                    rcCard.addView(corRow);

                    if (!expl.isEmpty()) {
                        TextView eTv = new TextView(activity);
                        eTv.setText("💡 " + expl);
                        eTv.setTextSize(11);
                        eTv.setTextColor(Color.parseColor("#94A3B8"));
                        eTv.setPadding(dp(activity, 4), dp(activity, 4), 0, 0);
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
                TextView tkTitle = new TextView(activity);
                tkTitle.setText(en ? "💡 Key Takeaways & Useful Expressions" : "💡 課後精選實用金句");
                tkTitle.setTextSize(13);
                tkTitle.setTextColor(Color.parseColor("#FBBF24"));
                tkTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tkTitle.setPadding(0, dp(activity, 6), 0, dp(activity, 8));
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

                    LinearLayout tkCard = new LinearLayout(activity);
                    tkCard.setOrientation(LinearLayout.HORIZONTAL);
                    tkCard.setGravity(Gravity.CENTER_VERTICAL);
                    tkCard.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
                    GradientDrawable tBg = new GradientDrawable();
                    tBg.setColor(Color.parseColor("#1E293B"));
                    tBg.setCornerRadius(dp(activity, 8));
                    tBg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                    tkCard.setBackground(tBg);
                    LinearLayout.LayoutParams tklp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tklp.setMargins(0, 0, 0, dp(activity, 6));
                    tkCard.setLayoutParams(tklp);

                    LinearLayout textCol = new LinearLayout(activity);
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    TextView pTv = new TextView(activity);
                    pTv.setText("• " + phrase);
                    pTv.setTextSize(13);
                    pTv.setTextColor(Color.WHITE);
                    pTv.setTypeface(Typeface.DEFAULT_BOLD);
                    textCol.addView(pTv);

                    if (!trans.isEmpty()) {
                        TextView trTv = new TextView(activity);
                        trTv.setText(trans);
                        trTv.setTextSize(11);
                        trTv.setTextColor(Color.parseColor("#94A3B8"));
                        trTv.setPadding(0, dp(activity, 2), 0, 0);
                        textCol.addView(trTv);
                    }
                    tkCard.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    Button pBtn = new Button(activity);
                    pBtn.setText("🔊");
                    pBtn.setTextSize(11);
                    pBtn.setTextColor(Color.WHITE);
                    GradientDrawable pbBg = new GradientDrawable();
                    pbBg.setColor(Color.parseColor("#4F46E5"));
                    pbBg.setCornerRadius(dp(activity, 6));
                    pBtn.setBackground(pbBg);
                    pBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { OralCoachHelper.speak(activity, phrase, 1.0f); }
                    });
                    tkCard.addView(pBtn, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 28)));

                    final boolean isStarred = LearningDataManager.isStarred(activity, phrase);
                    final Button starBtn = new Button(activity);
                    starBtn.setText(isStarred ? "★" : "☆");
                    starBtn.setTextSize(13);
                    starBtn.setTextColor(isStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                    starBtn.setBackground(null);
                    starBtn.setPadding(0, 0, 0, 0);
                    starBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            boolean nowStarred = LearningDataManager.toggleStarItem(activity, phrase, trans, "phrase", "");
                            starBtn.setText(nowStarred ? "★" : "☆");
                            starBtn.setTextColor(nowStarred ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
                            Toast.makeText(activity, nowStarred ? (en ? "Saved to Phrasebook" : "已收藏至生詞金句本") : (en ? "Removed" : "已取消收藏"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    tkCard.addView(starBtn, new LinearLayout.LayoutParams(dp(activity, 34), dp(activity, 28)));
                    allStarButtons.add(starBtn);

                    container.addView(tkCard);
                }
            }
        } catch (Exception ignored) {}

        // 6. Action Row: One-click Star All + Close
        if (!allReportItems.isEmpty()) {
            Button starAllBtn = new Button(activity);
            starAllBtn.setText(en ? "⭐ Save All Highlights to Phrasebook" : "⭐ 一鍵收藏全部精選金句與生詞");
            starAllBtn.setTextSize(13);
            starAllBtn.setTextColor(Color.WHITE);
            starAllBtn.setTypeface(Typeface.DEFAULT_BOLD);
            GradientDrawable sabBg = new GradientDrawable();
            sabBg.setColor(Color.parseColor("#D97706")); // Amber 600
            sabBg.setCornerRadius(dp(activity, 12));
            starAllBtn.setBackground(sabBg);
            LinearLayout.LayoutParams salp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 44));
            salp.setMargins(0, dp(activity, 10), 0, 0);
            starAllBtn.setLayoutParams(salp);
            starAllBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    for (LearningDataManager.StarredItem item : allReportItems) {
                        LearningDataManager.toggleStarItem(activity, item.originalText, item.translation, item.category, item.notes);
                    }
                    for (Button b : allStarButtons) {
                        b.setText("★");
                        b.setTextColor(Color.parseColor("#FBBF24"));
                    }
                    Toast.makeText(activity, en ? "⭐ All report items saved to Phrasebook!" : "⭐ 已將所有報告金句收藏至個人生詞本！", Toast.LENGTH_SHORT).show();
                }
            });
            container.addView(starAllBtn);
        }

        scroll.addView(container);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private static View makeScoreBar(Context ctx, String label, int score, String hexColor) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(ctx, 3), 0, dp(ctx, 3));

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelTv = new TextView(ctx);
        labelTv.setText(label);
        labelTv.setTextSize(11);
        labelTv.setTextColor(Color.parseColor("#CBD5E1"));
        head.addView(labelTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView scoreTv = new TextView(ctx);
        scoreTv.setText(score + " 分");
        scoreTv.setTextSize(11);
        scoreTv.setTextColor(Color.parseColor(hexColor));
        scoreTv.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(scoreTv);
        row.addView(head);

        LinearLayout track = new LinearLayout(ctx);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setPadding(0, 0, 0, 0);
        GradientDrawable trBg = new GradientDrawable();
        trBg.setColor(Color.parseColor("#1E293B"));
        trBg.setCornerRadius(dp(ctx, 4));
        track.setBackground(trBg);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 6));
        trLp.setMargins(0, dp(ctx, 3), 0, 0);
        track.setLayoutParams(trLp);

        View fill = new View(ctx);
        GradientDrawable fBg = new GradientDrawable();
        fBg.setColor(Color.parseColor(hexColor));
        fBg.setCornerRadius(dp(ctx, 4));
        fill.setBackground(fBg);
        float weight = Math.max(0.01f, Math.min(1.0f, score / 100.0f));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));

        View empty = new View(ctx);
        track.addView(empty, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f - weight));

        row.addView(track);
        return row;
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
