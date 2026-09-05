package com.crewpocket.teacher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class CourseMapDialog {

    private static TextToSpeech tts;
    private static boolean ttsReady = false;

    private static void initTts(Context context) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        tts.setLanguage(Locale.US);
                        ttsReady = true;
                    }
                }
            });
        }
    }

    public static void show(final Activity activity, final String targetTrackId) {
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E8090D16")));
        }

        final List<CourseModel.Track> tracks = CourseManager.getTracks();
        if (tracks.isEmpty()) return;

        final int[] selectedTrackIdx = new int[]{0};
        if (targetTrackId != null) {
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).id.equals(targetTrackId)) {
                    selectedTrackIdx[0] = i;
                    break;
                }
            }
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 20));

        // 1. Top Header
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(activity, 12));

        TextView titleTv = new TextView(activity);
        titleTv.setText(en ? "🗺️ Learning Path & Missions" : "🗺️ 系統口語關卡地圖");
        titleTv.setTextSize(17);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int totalStars = CourseManager.getTotalStars(activity);
        int completedCount = CourseManager.getCompletedLessonsCount(activity);
        int totalCount = CourseManager.getTotalLessonsCount();

        TextView starBadge = new TextView(activity);
        starBadge.setText("⭐ " + totalStars + " · 🏆 " + completedCount + "/" + totalCount);
        starBadge.setTextSize(12);
        starBadge.setTextColor(Color.parseColor("#FBBF24"));
        starBadge.setTypeface(Typeface.DEFAULT_BOLD);
        starBadge.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(Color.parseColor("#1E293B"));
        sbg.setCornerRadius(dp(activity, 12));
        sbg.setStroke(dp(activity, 1), Color.parseColor("#F59E0B"));
        starBadge.setBackground(sbg);
        topBar.addView(starBadge);

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(activity, 12), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        topBar.addView(closeBtn);
        root.addView(topBar);

        // 2. Track Switcher Tabs
        HorizontalScrollView tabScroll = new HorizontalScrollView(activity);
        tabScroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout tabContainer = new LinearLayout(activity);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabContainer);
        root.addView(tabScroll);

        // 3. Units & Lessons Scroll Area
        ScrollView contentScroll = new ScrollView(activity);
        contentScroll.setFillViewport(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.setMargins(0, dp(activity, 10), 0, 0);
        contentScroll.setLayoutParams(clp);

        final LinearLayout unitsContainer = new LinearLayout(activity);
        unitsContainer.setOrientation(LinearLayout.VERTICAL);
        unitsContainer.setPadding(0, dp(activity, 6), 0, dp(activity, 20));
        contentScroll.addView(unitsContainer);
        root.addView(contentScroll);

        final Runnable[] renderTrackContent = new Runnable[1];
        renderTrackContent[0] = new Runnable() {
            @Override
            public void run() {
                tabContainer.removeAllViews();
                for (int i = 0; i < tracks.size(); i++) {
                    final int idx = i;
                    final CourseModel.Track t = tracks.get(i);
                    final boolean isSel = (idx == selectedTrackIdx[0]);

                    TextView tabTv = new TextView(activity);
                    tabTv.setText(t.icon + " " + t.getTitle(en));
                    tabTv.setTextSize(12);
                    tabTv.setTypeface(Typeface.DEFAULT_BOLD);
                    tabTv.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));

                    GradientDrawable tbg = new GradientDrawable();
                    if (isSel) {
                        tbg.setColor(Color.parseColor("#2563EB"));
                        tbg.setCornerRadius(dp(activity, 20));
                        tabTv.setTextColor(Color.WHITE);
                    } else {
                        tbg.setColor(Color.parseColor("#1E293B"));
                        tbg.setCornerRadius(dp(activity, 20));
                        tbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                        tabTv.setTextColor(Color.parseColor("#94A3B8"));
                    }
                    tabTv.setBackground(tbg);

                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tlp.setMargins(0, 0, dp(activity, 8), dp(activity, 8));
                    tabTv.setLayoutParams(tlp);

                    tabTv.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            selectedTrackIdx[0] = idx;
                            renderTrackContent[0].run();
                        }
                    });
                    tabContainer.addView(tabTv);
                }

                unitsContainer.removeAllViews();
                CourseModel.Track curTrack = tracks.get(selectedTrackIdx[0]);

                LinearLayout descBanner = new LinearLayout(activity);
                descBanner.setOrientation(LinearLayout.VERTICAL);
                descBanner.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
                GradientDrawable dbg = new GradientDrawable();
                dbg.setColor(Color.parseColor("#0F172A"));
                dbg.setCornerRadius(dp(activity, 14));
                dbg.setStroke(dp(activity, 1), Color.parseColor("#1E293B"));
                descBanner.setBackground(dbg);

                TextView dt = new TextView(activity);
                dt.setText(curTrack.getTitle(en));
                dt.setTextSize(14);
                dt.setTextColor(Color.parseColor("#38BDF8"));
                dt.setTypeface(Typeface.DEFAULT_BOLD);
                descBanner.addView(dt);

                TextView dd = new TextView(activity);
                dd.setText(curTrack.getDescription(en));
                dd.setTextSize(11);
                dd.setTextColor(Color.parseColor("#94A3B8"));
                dd.setPadding(0, dp(activity, 3), 0, 0);
                descBanner.addView(dd);

                LinearLayout.LayoutParams dblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dblp.setMargins(0, 0, 0, dp(activity, 14));
                descBanner.setLayoutParams(dblp);
                unitsContainer.addView(descBanner);

                for (int u = 0; u < curTrack.units.size(); u++) {
                    CourseModel.Unit unit = curTrack.units.get(u);

                    LinearLayout unitCard = new LinearLayout(activity);
                    unitCard.setOrientation(LinearLayout.VERTICAL);
                    unitCard.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
                    GradientDrawable ubg = new GradientDrawable();
                    ubg.setColor(Color.parseColor("#111827"));
                    ubg.setCornerRadius(dp(activity, 16));
                    ubg.setStroke(dp(activity, 1), Color.parseColor("#1F2937"));
                    unitCard.setBackground(ubg);

                    LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    ulp.setMargins(0, 0, 0, dp(activity, 14));
                    unitCard.setLayoutParams(ulp);

                    TextView ut = new TextView(activity);
                    ut.setText(unit.getTitle(en));
                    ut.setTextSize(13);
                    ut.setTextColor(Color.parseColor("#F1F5F9"));
                    ut.setTypeface(Typeface.DEFAULT_BOLD);
                    unitCard.addView(ut);

                    TextView ud = new TextView(activity);
                    ud.setText(unit.getDescription(en));
                    ud.setTextSize(11);
                    ud.setTextColor(Color.parseColor("#64748B"));
                    ud.setPadding(0, dp(activity, 2), 0, dp(activity, 10));
                    unitCard.addView(ud);

                    for (int l = 0; l < unit.lessons.size(); l++) {
                        final CourseModel.Lesson lesson = unit.lessons.get(l);
                        final boolean isUnlocked = CourseManager.isLessonUnlocked(activity, lesson.id);
                        final CourseModel.LessonProgress progress = CourseManager.getLessonProgress(activity, lesson.id);

                        LinearLayout lessonRow = new LinearLayout(activity);
                        lessonRow.setOrientation(LinearLayout.HORIZONTAL);
                        lessonRow.setGravity(Gravity.CENTER_VERTICAL);
                        lessonRow.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));

                        GradientDrawable lbg = new GradientDrawable();
                        if (!isUnlocked) {
                            lbg.setColor(Color.parseColor("#171E2E"));
                            lbg.setCornerRadius(dp(activity, 12));
                            lbg.setStroke(dp(activity, 1), Color.parseColor("#243048"));
                        } else if (progress.completed) {
                            lbg.setColor(Color.parseColor("#064E3B"));
                            lbg.setCornerRadius(dp(activity, 12));
                            lbg.setStroke(dp(activity, 1), Color.parseColor("#059669"));
                        } else {
                            lbg.setColor(Color.parseColor("#1E293B"));
                            lbg.setCornerRadius(dp(activity, 12));
                            lbg.setStroke(dp(activity, 1), Color.parseColor("#3B82F6"));
                        }
                        lessonRow.setBackground(lbg);

                        LinearLayout.LayoutParams lrlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lrlp.setMargins(0, 0, 0, dp(activity, 8));
                        lessonRow.setLayoutParams(lrlp);

                        TextView stepBadge = new TextView(activity);
                        if (!isUnlocked) {
                            stepBadge.setText("🔒");
                        } else if (progress.completed) {
                            stepBadge.setText("✓");
                            stepBadge.setTextColor(Color.parseColor("#34D399"));
                        } else {
                            stepBadge.setText(String.valueOf(l + 1));
                            stepBadge.setTextColor(Color.parseColor("#38BDF8"));
                        }
                        stepBadge.setTextSize(14);
                        stepBadge.setTypeface(Typeface.DEFAULT_BOLD);
                        stepBadge.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams sblp = new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28));
                        sblp.setMargins(0, 0, dp(activity, 10), 0);
                        stepBadge.setLayoutParams(sblp);
                        lessonRow.addView(stepBadge);

                        LinearLayout infoCol = new LinearLayout(activity);
                        infoCol.setOrientation(LinearLayout.VERTICAL);

                        TextView lt = new TextView(activity);
                        lt.setText(lesson.getTitle(en));
                        lt.setTextSize(13);
                        lt.setTextColor(isUnlocked ? Color.WHITE : Color.parseColor("#64748B"));
                        lt.setTypeface(Typeface.DEFAULT_BOLD);
                        infoCol.addView(lt);

                        TextView ld = new TextView(activity);
                        ld.setText(lesson.getDescription(en));
                        ld.setTextSize(11);
                        ld.setTextColor(isUnlocked ? Color.parseColor("#94A3B8") : Color.parseColor("#475569"));
                        ld.setMaxLines(1);
                        ld.setEllipsize(TextUtils.TruncateAt.END);
                        infoCol.addView(ld);

                        lessonRow.addView(infoCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                        if (isUnlocked) {
                            if (progress.completed) {
                                TextView starsTv = new TextView(activity);
                                StringBuilder sb = new StringBuilder();
                                for (int s = 0; s < 3; s++) {
                                    sb.append(s < progress.stars ? "⭐" : "☆");
                                }
                                starsTv.setText(sb.toString());
                                starsTv.setTextSize(12);
                                lessonRow.addView(starsTv);
                            } else {
                                TextView playBadge = new TextView(activity);
                                playBadge.setText(en ? "START ›" : "闖關 ›");
                                playBadge.setTextSize(11);
                                playBadge.setTextColor(Color.parseColor("#38BDF8"));
                                playBadge.setTypeface(Typeface.DEFAULT_BOLD);
                                lessonRow.addView(playBadge);
                            }

                            lessonRow.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    showLessonIntro(activity, lesson, dialog);
                                }
                            });
                        } else {
                            lessonRow.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    Toast.makeText(activity, en ? "Please complete the previous lesson first!" : "請先完成前一關挑戰以解鎖此課程！", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        unitCard.addView(lessonRow);
                    }
                    unitsContainer.addView(unitCard);
                }
            }
        };

        renderTrackContent[0].run();
        dialog.setContentView(root);
        dialog.show();
    }

    public static void showLessonIntro(final Activity activity, final CourseModel.Lesson lesson, final Dialog parentMapDialog) {
        final boolean en = I18n.isEnglish(activity);
        initTts(activity);

        final Dialog introDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (introDialog.getWindow() != null) {
            introDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F0050810")));
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 20));

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(Color.parseColor("#0F172A"));
        cbg.setCornerRadius(dp(activity, 20));
        cbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
        container.setBackground(cbg);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tag = new TextView(activity);
        tag.setText("🎯 " + (en ? "Lesson Mission Briefing" : "課前通關作戰手冊"));
        tag.setTextSize(12);
        tag.setTextColor(Color.parseColor("#38BDF8"));
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(tag, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.parseColor("#94A3B8"));
        closeBtn.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { introDialog.dismiss(); }
        });
        topRow.addView(closeBtn);
        container.addView(topRow);

        TextView titleTv = new TextView(activity);
        titleTv.setText(lesson.getTitle(en));
        titleTv.setTextSize(18);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, dp(activity, 8), 0, dp(activity, 4));
        container.addView(titleTv);

        TextView descTv = new TextView(activity);
        descTv.setText(lesson.getDescription(en));
        descTv.setTextSize(12);
        descTv.setTextColor(Color.parseColor("#94A3B8"));
        descTv.setPadding(0, 0, 0, dp(activity, 14));
        container.addView(descTv);

        if (!lesson.warmupPhrases.isEmpty()) {
            TextView s1Title = new TextView(activity);
            s1Title.setText(en ? "💡 Core Sentences & Native Pronunciation" : "💡 本課必學句型與原生發音");
            s1Title.setTextSize(13);
            s1Title.setTextColor(Color.parseColor("#FBBF24"));
            s1Title.setTypeface(Typeface.DEFAULT_BOLD);
            s1Title.setPadding(0, dp(activity, 4), 0, dp(activity, 8));
            container.addView(s1Title);

            for (final CourseModel.WarmupPhrase phrase : lesson.warmupPhrases) {
                LinearLayout pCard = new LinearLayout(activity);
                pCard.setOrientation(LinearLayout.VERTICAL);
                pCard.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                GradientDrawable pbg = new GradientDrawable();
                pbg.setColor(Color.parseColor("#1E293B"));
                pbg.setCornerRadius(dp(activity, 12));
                pbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
                pCard.setBackground(pbg);

                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                plp.setMargins(0, 0, 0, dp(activity, 8));
                pCard.setLayoutParams(plp);

                LinearLayout phraseTop = new LinearLayout(activity);
                phraseTop.setOrientation(LinearLayout.HORIZONTAL);
                phraseTop.setGravity(Gravity.CENTER_VERTICAL);

                TextView enTv = new TextView(activity);
                enTv.setText(phrase.en);
                enTv.setTextSize(13);
                enTv.setTextColor(Color.parseColor("#E2E8F0"));
                enTv.setTypeface(Typeface.DEFAULT_BOLD);
                phraseTop.addView(enTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView playBtn = new TextView(activity);
                playBtn.setText("🔊 示範");
                playBtn.setTextSize(11);
                playBtn.setTextColor(Color.parseColor("#38BDF8"));
                playBtn.setPadding(dp(activity, 6), dp(activity, 2), dp(activity, 6), dp(activity, 2));
                playBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (tts != null && ttsReady) {
                            tts.speak(phrase.en, TextToSpeech.QUEUE_FLUSH, null, "phrase_" + phrase.en.hashCode());
                        }
                    }
                });
                phraseTop.addView(playBtn);

                TextView drillBtn = new TextView(activity);
                drillBtn.setText("🎙️ 試讀");
                drillBtn.setTextSize(11);
                drillBtn.setTextColor(Color.parseColor("#34D399"));
                drillBtn.setPadding(dp(activity, 6), dp(activity, 2), dp(activity, 6), dp(activity, 2));
                drillBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        OralCoachHelper.showPronunciationDrillDialog(activity, phrase.en, phrase.zh, phrase.note);
                    }
                });
                phraseTop.addView(drillBtn);
                pCard.addView(phraseTop);

                TextView zhTv = new TextView(activity);
                zhTv.setText(phrase.zh);
                zhTv.setTextSize(12);
                zhTv.setTextColor(Color.parseColor("#94A3B8"));
                zhTv.setPadding(0, dp(activity, 2), 0, 0);
                pCard.addView(zhTv);

                if (phrase.ipa != null && !phrase.ipa.isEmpty()) {
                    TextView ipaTv = new TextView(activity);
                    ipaTv.setText(phrase.ipa);
                    ipaTv.setTextSize(11);
                    ipaTv.setTextColor(Color.parseColor("#A78BFA"));
                    pCard.addView(ipaTv);
                }

                if (phrase.note != null && !phrase.note.isEmpty()) {
                    TextView noteTv = new TextView(activity);
                    noteTv.setText("📌 " + phrase.note);
                    noteTv.setTextSize(11);
                    noteTv.setTextColor(Color.parseColor("#6EE7B7"));
                    noteTv.setPadding(0, dp(activity, 2), 0, 0);
                    pCard.addView(noteTv);
                }

                container.addView(pCard);
            }
        }

        TextView s2Title = new TextView(activity);
        s2Title.setText(en ? "🎯 3 Mission Objectives to Clear Lesson" : "🎯 本課 3 大實戰通關目標");
        s2Title.setTextSize(13);
        s2Title.setTextColor(Color.parseColor("#34D399"));
        s2Title.setTypeface(Typeface.DEFAULT_BOLD);
        s2Title.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        container.addView(s2Title);

        for (int i = 0; i < lesson.missions.size(); i++) {
            CourseModel.Mission mission = lesson.missions.get(i);

            LinearLayout mRow = new LinearLayout(activity);
            mRow.setOrientation(LinearLayout.HORIZONTAL);
            mRow.setGravity(Gravity.CENTER_VERTICAL);
            mRow.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
            GradientDrawable mbg = new GradientDrawable();
            mbg.setColor(Color.parseColor("#1E293B"));
            mbg.setCornerRadius(dp(activity, 10));
            mbg.setStroke(dp(activity, 1), Color.parseColor("#334155"));
            mRow.setBackground(mbg);

            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mlp.setMargins(0, 0, 0, dp(activity, 6));
            mRow.setLayoutParams(mlp);

            TextView mIdx = new TextView(activity);
            mIdx.setText("目標 " + (i + 1));
            mIdx.setTextSize(11);
            mIdx.setTextColor(Color.parseColor("#38BDF8"));
            mIdx.setTypeface(Typeface.DEFAULT_BOLD);
            mIdx.setPadding(0, 0, dp(activity, 10), 0);
            mRow.addView(mIdx);

            TextView mDesc = new TextView(activity);
            mDesc.setText(mission.getDesc(en));
            mDesc.setTextSize(12);
            mDesc.setTextColor(Color.WHITE);
            mRow.addView(mDesc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            container.addView(mRow);
        }

        Button startBtn = new Button(activity);
        startBtn.setText(en ? "🚀 Start Lesson Challenge (5 Mins)" : "🚀 進入實戰闖關 (挑戰 5 分鐘通關)");
        startBtn.setTextSize(14);
        startBtn.setTextColor(Color.WHITE);
        startBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bbg = new GradientDrawable();
        bbg.setColor(Color.parseColor("#2563EB"));
        bbg.setCornerRadius(dp(activity, 14));
        startBtn.setBackground(bbg);

        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48));
        blp.setMargins(0, dp(activity, 16), 0, 0);
        startBtn.setLayoutParams(blp);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                introDialog.dismiss();
                if (parentMapDialog != null) {
                    parentMapDialog.dismiss();
                }

                Intent intent = new Intent(activity, NativeLiveActivity.class);
                intent.putExtra("EXTRA_LESSON_ID", lesson.id);
                intent.putExtra("EXTRA_SCENARIO", lesson.scenario);
                intent.putExtra("EXTRA_TITLE", lesson.getTitle(en));
                activity.startActivity(intent);
            }
        });
        container.addView(startBtn);

        scroll.addView(container);
        introDialog.setContentView(scroll);
        introDialog.show();
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
