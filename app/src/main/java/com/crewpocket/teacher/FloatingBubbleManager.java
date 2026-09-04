package com.crewpocket.teacher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Modern, Compact Fluid Bubble with Smart Edge Auto-Docking & Voice Dialog Card.
 */
public class FloatingBubbleManager {
    private static FloatingBubbleManager instance;
    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler;
    private final Vibrator vibrator;

    private FluidBubbleView bubbleView;
    private View dialogView;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams dialogParams;
    private boolean isDialogShowing = false;

    private TextView statusText;
    private LinearLayout bubbleChatContainer;
    private ScrollView transcriptScroll;
    private Button callBtn;
    private Button muteBtn;
    private Button audioOutputBtn;
    private TextView meterText;

    private String latestStatus = "待命中";

    private boolean isDocked = false;
    private ValueAnimator dockAnimator = null;
    private final Handler autoDockHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoDockRunnable = new Runnable() {
        @Override public void run() { autoDockBubble(); }
    };

    private FloatingBubbleManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static synchronized FloatingBubbleManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatingBubbleManager(context);
        }
        return instance;
    }

    private int dp(float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public boolean isBubbleShowing() {
        return bubbleView != null && bubbleView.getParent() != null;
    }

    public void showBubble() {
        mainHandler.post(new Runnable() {
            @Override public void run() { createAndShowBubble(); }
        });
    }

    public void hideBubble() {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                autoDockHandler.removeCallbacks(autoDockRunnable);
                if (dockAnimator != null) {
                    dockAnimator.cancel();
                    dockAnimator = null;
                }
                hideDialog();
                if (bubbleView != null && bubbleView.getParent() != null) {
                    try { windowManager.removeView(bubbleView); } catch (Exception ignored) {}
                    bubbleView = null;
                }
            }
        });
    }

    public void toggleBubble() {
        if (isBubbleShowing()) {
            hideBubble();
            Toast.makeText(context, "已關閉桌面懸浮氣泡", Toast.LENGTH_SHORT).show();
        } else {
            showBubble();
            Toast.makeText(context, "已開啟桌面懸浮氣泡", Toast.LENGTH_SHORT).show();
        }
    }

    public void scheduleAutoDock() {
        autoDockHandler.removeCallbacks(autoDockRunnable);
        if (bubbleView != null && !isDocked && !NativeLiveService.isActive()) {
            autoDockHandler.postDelayed(autoDockRunnable, 3000);
        }
    }

    public void wakeBubbleFromDock() {
        autoDockHandler.removeCallbacks(autoDockRunnable);
        if (bubbleView == null || bubbleParams == null) return;
        if (dockAnimator != null && dockAnimator.isRunning()) {
            dockAnimator.cancel();
        }
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int bSize = bubbleParams.width > 0 ? bubbleParams.width : dp(40);
        int targetX = (bubbleParams.x < screenWidth / 2) ? dp(4) : (screenWidth - bSize - dp(4));

        bubbleParams.x = targetX;
        bubbleView.setAlpha(1.0f);
        try { windowManager.updateViewLayout(bubbleView, bubbleParams); } catch (Exception ignored) {}
        isDocked = false;
    }

    public void autoDockBubble() {
        if (bubbleView == null || bubbleParams == null || isDocked) return;
        if (NativeLiveService.isActive()) return;

        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int bSize = bubbleParams.width > 0 ? bubbleParams.width : dp(40);

        final int startX = bubbleParams.x;
        final int endX = (startX < screenWidth / 2) ? - (bSize * 55 / 100) : (screenWidth - (bSize * 45 / 100));

        if (dockAnimator != null && dockAnimator.isRunning()) {
            dockAnimator.cancel();
        }

        dockAnimator = ValueAnimator.ofFloat(0f, 1f);
        dockAnimator.setDuration(350);
        dockAnimator.setInterpolator(new DecelerateInterpolator());
        dockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = (float) animation.getAnimatedValue();
                if (bubbleView == null || bubbleParams == null) return;
                bubbleParams.x = (int) (startX + (endX - startX) * frac);
                bubbleView.setAlpha(1.0f - 0.60f * frac);
                try { windowManager.updateViewLayout(bubbleView, bubbleParams); } catch (Exception ignored) {}
            }
        });
        dockAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isDocked = true;
            }
        });
        dockAnimator.start();
    }

    private void createAndShowBubble() {
        if (!canDrawOverlays() || (bubbleView != null && bubbleView.getParent() != null)) return;

        int layoutType = Build.VERSION.SDK_INT >= 26
                ? 2038
                : WindowManager.LayoutParams.TYPE_PHONE;

        int size = dp(40); // 🌟 Compact 40dp (matching Crew Helper luxury fluid style)
        bubbleParams = new WindowManager.LayoutParams(
                size, size,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        int screenW = windowManager.getDefaultDisplay().getWidth();
        int screenH = windowManager.getDefaultDisplay().getHeight();
        bubbleParams.x = screenW - size - dp(6);
        bubbleParams.y = screenH / 3;

        bubbleView = new FluidBubbleView(context);
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;
            private boolean isDrag;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        wakeBubbleFromDock();
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        isDrag = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchX);
                        int dy = (int) (event.getRawY() - touchY);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            isDrag = true;
                            bubbleParams.x = initialX + dx;
                            bubbleParams.y = initialY + dy;
                            try { windowManager.updateViewLayout(bubbleView, bubbleParams); } catch (Exception ignored) {}
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDrag) {
                            toggleDialog();
                        } else {
                            scheduleAutoDock();
                        }
                        return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(bubbleView, bubbleParams);
            updateBubbleVoiceState();
            scheduleAutoDock();
        } catch (Exception e) {
            Toast.makeText(context, "無法顯示懸浮氣泡，請確認懸浮視窗權限", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDialog() {
        if (isDialogShowing) hideDialog();
        else showDialog();
    }

    private void showDialog() {
        if (isDialogShowing || !canDrawOverlays()) return;

        int layoutType = Build.VERSION.SDK_INT >= 26
                ? 2038
                : WindowManager.LayoutParams.TYPE_PHONE;

        dialogParams = new WindowManager.LayoutParams(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92f),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        dialogParams.gravity = Gravity.CENTER;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1.5f), Color.parseColor("#334155"));
        root.setBackground(bg);

        boolean en = I18n.isEnglish(context);

        // Header with Title and Action Buttons
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(en ? "🎓 Crew Teacher Tutor" : "🎓 Crew Teacher 口語練習");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Minimize / Collapse Button
        Button minBtn = new Button(context);
        minBtn.setText("—");
        minBtn.setTextColor(Color.parseColor("#94A3B8"));
        minBtn.setBackgroundColor(Color.TRANSPARENT);
        minBtn.setTextSize(18);
        minBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { hideDialog(); }
        });
        header.addView(minBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));

        // Close / Dismiss Bubble Button
        Button closeBubbleBtn = new Button(context);
        closeBubbleBtn.setText("✕");
        closeBubbleBtn.setTextColor(Color.parseColor("#F43F5E"));
        closeBubbleBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBubbleBtn.setTextSize(16);
        closeBubbleBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (NativeLiveService.isActive()) {
                    NativeLiveService.stop(context);
                }
                hideBubble();
                Toast.makeText(context, I18n.isEnglish(context) ? "Floating bubble closed" : "已關閉懸浮氣泡", Toast.LENGTH_SHORT).show();
            }
        });
        header.addView(closeBubbleBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));
        root.addView(header);

        // Status Card
        LinearLayout statusBox = new LinearLayout(context);
        statusBox.setOrientation(LinearLayout.HORIZONTAL);
        statusBox.setGravity(Gravity.CENTER_VERTICAL);
        statusBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(Color.parseColor("#1E293B"));
        sbg.setCornerRadius(dp(10));
        statusBox.setBackground(sbg);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(10), 0, dp(10));
        statusBox.setLayoutParams(sLp);

        statusText = new TextView(context);
        statusText.setText(latestStatus);
        statusText.setTextColor(Color.parseColor("#38BDF8"));
        statusText.setTextSize(13);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusBox.addView(statusText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        meterText = new TextView(context);
        meterText.setText(en ? "🎙️ Standby" : "🎙️ 待命");
        meterText.setTextColor(Color.parseColor("#94A3B8"));
        meterText.setTextSize(11);
        statusBox.addView(meterText);
        root.addView(statusBox);

        // Transcript Scroll View with Conversation Cards
        transcriptScroll = new ScrollView(context);
        transcriptScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));
        transcriptScroll.setPadding(dp(4), dp(4), dp(4), dp(4));

        bubbleChatContainer = new LinearLayout(context);
        bubbleChatContainer.setOrientation(LinearLayout.VERTICAL);
        bubbleChatContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        transcriptScroll.addView(bubbleChatContainer);
        root.addView(transcriptScroll);
        renderBubbleChatCards();

        // Action Buttons Row 1: Call & Mute & Audio Output
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.setMargins(0, dp(12), 0, 0);
        actions.setLayoutParams(aLp);

        // Call button
        callBtn = new Button(context);
        boolean active = NativeLiveService.isActive();
        callBtn.setText(active ? (en ? "End Call" : "掛斷對話") : (en ? "Start Call" : "開始對話"));
        callBtn.setTextColor(Color.WHITE);
        GradientDrawable callBg = new GradientDrawable();
        callBg.setColor(active ? Color.parseColor("#E11D48") : Color.parseColor("#2563EB"));
        callBg.setCornerRadius(dp(12));
        callBtn.setBackground(callBg);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean isEn = I18n.isEnglish(context);
                if (NativeLiveService.isActive()) {
                    NativeLiveService.stop(context);
                    callBtn.setText(isEn ? "Start Call" : "開始對話");
                    ((GradientDrawable) callBtn.getBackground()).setColor(Color.parseColor("#2563EB"));
                } else {
                    NativeLiveService.start(context);
                    callBtn.setText(isEn ? "End Call" : "掛斷對話");
                    ((GradientDrawable) callBtn.getBackground()).setColor(Color.parseColor("#E11D48"));
                }
                updateBubbleVoiceState();
            }
        });
        actions.addView(callBtn, new LinearLayout.LayoutParams(0, dp(42), 1.3f));

        // Mute button
        muteBtn = new Button(context);
        muteBtn.setText(en ? "Mute" : "靜音");
        muteBtn.setTextColor(Color.WHITE);
        GradientDrawable muteBg = new GradientDrawable();
        muteBg.setColor(Color.parseColor("#334155"));
        muteBg.setCornerRadius(dp(12));
        muteBtn.setBackground(muteBg);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        mLp.setMargins(dp(8), 0, 0, 0);
        muteBtn.setLayoutParams(mLp);
        muteBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean muted = NativeLiveService.toggleAgentMute();
                boolean isEn = I18n.isEnglish(context);
                muteBtn.setText(muted ? (isEn ? "Unmute" : "取消靜音") : (isEn ? "Mute" : "靜音"));
                ((GradientDrawable) muteBtn.getBackground()).setColor(muted ? Color.parseColor("#D97706") : Color.parseColor("#334155"));
            }
        });
        actions.addView(muteBtn);

        // Audio Output Mode button (Call vs Media)
        audioOutputBtn = new Button(context);
        String currentOutput = AppConfig.getAudioOutput(context);
        audioOutputBtn.setText("media".equals(currentOutput) ? (en ? "🎵 Media" : "🎵 媒體音") : (en ? "📞 Voice" : "📞 通話音"));
        audioOutputBtn.setTextColor(Color.WHITE);
        audioOutputBtn.setTextSize(11);
        GradientDrawable aoutBg = new GradientDrawable();
        aoutBg.setColor(Color.parseColor("#1E293B"));
        aoutBg.setCornerRadius(dp(12));
        aoutBg.setStroke(dp(1), Color.parseColor("#475569"));
        audioOutputBtn.setBackground(aoutBg);
        LinearLayout.LayoutParams outLp = new LinearLayout.LayoutParams(0, dp(42), 1.1f);
        outLp.setMargins(dp(8), 0, 0, 0);
        audioOutputBtn.setLayoutParams(outLp);
        audioOutputBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String now = AppConfig.getAudioOutput(context);
                String next = "media".equals(now) ? "call" : "media";
                AppConfig.setAudioOutput(context, next);
                boolean isEn = I18n.isEnglish(context);
                audioOutputBtn.setText("media".equals(next) ? (isEn ? "🎵 Media" : "🎵 媒體音") : (isEn ? "📞 Voice" : "📞 通話音"));
                Toast.makeText(context, isEn
                        ? ("Audio output set to: " + ("media".equals(next) ? "Media Audio" : "Voice Call (AEC)"))
                        : ("音訊輸出已切換為：" + ("media".equals(next) ? "媒體音訊 (Media)" : "通話音訊 (Voice/AEC)")), Toast.LENGTH_SHORT).show();
            }
        });
        actions.addView(audioOutputBtn);
        root.addView(actions);

        // Dismiss Bubble Bottom Link
        TextView dismissLink = new TextView(context);
        dismissLink.setText(en ? "Dismiss Floating Bubble" : "關閉桌面氣泡");
        dismissLink.setTextColor(Color.parseColor("#64748B"));
        dismissLink.setTextSize(11);
        dismissLink.setGravity(Gravity.CENTER);
        dismissLink.setPadding(0, dp(10), 0, 0);
        dismissLink.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (NativeLiveService.isActive()) NativeLiveService.stop(context);
                hideBubble();
            }
        });
        root.addView(dismissLink);

        dialogView = root;
        try {
            windowManager.addView(dialogView, dialogParams);
            isDialogShowing = true;
        } catch (Exception ignored) {}
    }

    public void hideDialog() {
        if (!isDialogShowing || dialogView == null) return;
        try {
            if (dialogView.getParent() != null) windowManager.removeView(dialogView);
        } catch (Exception ignored) {}
        dialogView = null;
        transcriptScroll = null;
        bubbleChatContainer = null;
        statusText = null;
        meterText = null;
        isDialogShowing = false;
        scheduleAutoDock();
    }

    public void updateNativeLiveStatus(final String status, final boolean showOngoing) {
        latestStatus = status;
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (statusText != null) statusText.setText(status);
                updateBubbleVoiceState();
            }
        });
    }

    public static class BubbleChatTurn {
        public String role = "ai"; // "user" or "ai"
        public StringBuilder spoken = new StringBuilder();
        public String translation = "";
        public String keyVocab = "";
        public java.util.List<String> hints = new java.util.ArrayList<String>();
        public boolean translationRevealed = false;
    }

    private final java.util.List<BubbleChatTurn> bubbleTurnHistory = new java.util.ArrayList<BubbleChatTurn>();
    private BubbleChatTurn currentBubbleChatTurn = null;

    public void onSpeakingChanged(final boolean speaking) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (speaking) {
                    if (currentBubbleChatTurn != null && "ai".equals(currentBubbleChatTurn.role)) {
                        currentBubbleChatTurn.translationRevealed = false;
                    }
                } else {
                    if (currentBubbleChatTurn != null && "ai".equals(currentBubbleChatTurn.role)) {
                        currentBubbleChatTurn.translationRevealed = true;
                    }
                    renderBubbleChatCards();
                }
                updateBubbleVoiceState();
            }
        });
    }

    public void appendTranscript(final String text, final String role) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (text == null || text.isEmpty()) return;

                if ("translation".equalsIgnoreCase(role)) {
                    applyBubbleTranslation(text);
                    return;
                }

                boolean isAi = "ai".equalsIgnoreCase(role) || "Gemini".equalsIgnoreCase(role);
                String roleKey = isAi ? "ai" : "user";

                if (currentBubbleChatTurn == null || !roleKey.equals(currentBubbleChatTurn.role)) {
                    if (currentBubbleChatTurn != null && currentBubbleChatTurn.spoken.length() > 0) {
                        bubbleTurnHistory.add(currentBubbleChatTurn);
                    }
                    currentBubbleChatTurn = new BubbleChatTurn();
                    currentBubbleChatTurn.role = roleKey;
                    currentBubbleChatTurn.translationRevealed = !isAi;
                }

                currentBubbleChatTurn.spoken.append(text);
                renderBubbleChatCards();
            }
        });
    }

    public void applyStructuredSubtitleData(final String targetText, final String nativeTrans, final String keyVocab, final java.util.List<String> hints) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (currentBubbleChatTurn == null || !"ai".equals(currentBubbleChatTurn.role)) {
                    if (currentBubbleChatTurn != null && currentBubbleChatTurn.spoken.length() > 0) {
                        bubbleTurnHistory.add(currentBubbleChatTurn);
                    }
                    currentBubbleChatTurn = new BubbleChatTurn();
                    currentBubbleChatTurn.role = "ai";
                }
                currentBubbleChatTurn.translation = nativeTrans != null ? nativeTrans.trim() : "";
                currentBubbleChatTurn.keyVocab = keyVocab != null ? keyVocab.trim() : "";
                currentBubbleChatTurn.hints.clear();
                if (hints != null) currentBubbleChatTurn.hints.addAll(hints);
                if (!NativeLiveService.isAiSpeaking()) {
                    currentBubbleChatTurn.translationRevealed = true;
                }
                renderBubbleChatCards();
            }
        });
    }

    private void applyBubbleTranslation(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String clean = text.trim();
        if (currentBubbleChatTurn != null && "ai".equals(currentBubbleChatTurn.role)) {
            if (currentBubbleChatTurn.translation.isEmpty()) {
                currentBubbleChatTurn.translation = clean;
            }
            if (!NativeLiveService.isAiSpeaking()) {
                currentBubbleChatTurn.translationRevealed = true;
            }
            renderBubbleChatCards();
        } else {
            for (int i = bubbleTurnHistory.size() - 1; i >= 0; i--) {
                BubbleChatTurn t = bubbleTurnHistory.get(i);
                if ("ai".equals(t.role) && t.translation.isEmpty()) {
                    t.translation = clean;
                    t.translationRevealed = true;
                    renderBubbleChatCards();
                    return;
                }
            }
            if (currentBubbleChatTurn != null && currentBubbleChatTurn.spoken.length() > 0) {
                bubbleTurnHistory.add(currentBubbleChatTurn);
            }
            currentBubbleChatTurn = new BubbleChatTurn();
            currentBubbleChatTurn.role = "ai";
            currentBubbleChatTurn.translation = clean;
            if (!NativeLiveService.isAiSpeaking()) {
                currentBubbleChatTurn.translationRevealed = true;
            }
            renderBubbleChatCards();
        }
    }

    private void renderBubbleChatCards() {
        if (bubbleChatContainer == null) return;
        bubbleChatContainer.removeAllViews();
        final boolean en = I18n.isEnglish(context);

        if (bubbleTurnHistory.isEmpty() && currentBubbleChatTurn == null) {
            TextView emptyTv = new TextView(context);
            emptyTv.setText(en ? "💬 Dialogue will appear here in cards…" : "💬 對話逐字紀錄與翻譯小抄將以卡片展示…");
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setTextSize(12);
            emptyTv.setPadding(dp(4), dp(4), dp(4), dp(4));
            bubbleChatContainer.addView(emptyTv);
            return;
        }

        for (BubbleChatTurn turn : bubbleTurnHistory) {
            bubbleChatContainer.addView(buildBubbleTurnCard(turn, false));
        }

        if (currentBubbleChatTurn != null && (currentBubbleChatTurn.spoken.length() > 0 || !currentBubbleChatTurn.translation.isEmpty())) {
            boolean isSpeaking = NativeLiveService.isAiSpeaking() && "ai".equals(currentBubbleChatTurn.role);
            bubbleChatContainer.addView(buildBubbleTurnCard(currentBubbleChatTurn, isSpeaking));
        }

        if (transcriptScroll != null) {
            transcriptScroll.post(new Runnable() {
                @Override public void run() {
                    if (transcriptScroll != null) {
                        transcriptScroll.fullScroll(View.FOCUS_DOWN);
                    }
                }
            });
        }
    }

    private View buildBubbleTurnCard(BubbleChatTurn turn, boolean isLiveSpeaking) {
        boolean isAi = "ai".equals(turn.role);
        final boolean en = I18n.isEnglish(context);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cLp);

        GradientDrawable cBg = new GradientDrawable();
        if (isAi) {
            cBg.setColor(Color.parseColor("#0F172A")); // Slate 900
            cBg.setCornerRadii(new float[]{dp(4), dp(4), dp(12), dp(12), dp(12), dp(12), dp(12), dp(12)});
            cBg.setStroke(dp(1), isLiveSpeaking ? Color.parseColor("#0284C7") : Color.parseColor("#1E293B"));
        } else {
            cBg.setColor(Color.parseColor("#1E293B")); // Slate 800
            cBg.setCornerRadii(new float[]{dp(12), dp(12), dp(4), dp(4), dp(12), dp(12), dp(12), dp(12)});
            cBg.setStroke(dp(1), Color.parseColor("#334155"));
        }
        card.setBackground(cBg);

        // Header Row
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView roleLabel = new TextView(context);
        roleLabel.setText(isAi ? (en ? "🤖 AI Tutor" : "🤖 導師") : (en ? "🗣️ You" : "🗣️ 學生"));
        roleLabel.setTextSize(10);
        roleLabel.setTextColor(isAi ? Color.parseColor("#38BDF8") : Color.parseColor("#A5B4FC"));
        roleLabel.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(roleLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (isLiveSpeaking) {
            TextView liveBadge = new TextView(context);
            liveBadge.setText("🔊");
            liveBadge.setTextSize(9);
            header.addView(liveBadge);
        }
        card.addView(header);

        // Spoken content
        TextView spokenTv = new TextView(context);
        String spokenStr = turn.spoken.toString().trim();
        spokenTv.setText(spokenStr.isEmpty() && isAi && isLiveSpeaking ? "🎙️ ..." : spokenStr);
        spokenTv.setTextSize(13);
        spokenTv.setTextColor(Color.WHITE);
        spokenTv.setLineSpacing(dp(2), 1.2f);
        card.addView(spokenTv);

        // Sub-card (Translation + Vocab + Hints)
        if (isAi && turn.translationRevealed && (!turn.translation.isEmpty() || !turn.keyVocab.isEmpty() || !turn.hints.isEmpty())) {
            LinearLayout subCard = new LinearLayout(context);
            subCard.setOrientation(LinearLayout.VERTICAL);
            subCard.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable sBg = new GradientDrawable();
            sBg.setColor(Color.parseColor("#111827"));
            sBg.setCornerRadius(dp(8));
            sBg.setStroke(dp(1), Color.parseColor("#1F2937"));
            subCard.setBackground(sBg);
            LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scLp.setMargins(0, dp(6), 0, 0);
            subCard.setLayoutParams(scLp);

            if (!turn.translation.isEmpty()) {
                TextView tText = new TextView(context);
                tText.setText("📖 " + turn.translation);
                tText.setTextSize(12);
                tText.setTextColor(Color.parseColor("#93C5FD"));
                subCard.addView(tText);
            }

            if (!turn.keyVocab.isEmpty()) {
                TextView vText = new TextView(context);
                vText.setText("💡 " + turn.keyVocab);
                vText.setTextSize(11);
                vText.setTextColor(Color.parseColor("#FDE047"));
                LinearLayout.LayoutParams vtLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vtLp.setMargins(0, dp(4), 0, 0);
                subCard.addView(vText, vtLp);
            }

            if (!turn.hints.isEmpty()) {
                TextView hTitle = new TextView(context);
                hTitle.setText(en ? "💬 Hints:" : "💬 建議小抄：");
                hTitle.setTextSize(10);
                hTitle.setTextColor(Color.parseColor("#34D399"));
                hTitle.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                htLp.setMargins(0, dp(4), 0, dp(2));
                subCard.addView(hTitle, htLp);

                for (String hint : turn.hints) {
                    TextView pText = new TextView(context);
                    pText.setText("• " + hint);
                    pText.setTextSize(11);
                    pText.setTextColor(Color.parseColor("#E0E7FF"));
                    pText.setPadding(0, 0, 0, dp(2));
                    subCard.addView(pText);
                }
            }

            card.addView(subCard);
        }

        return card;
    }

    public void updateMicrophoneMeter(final double dbfs, final boolean sending) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (meterText != null) {
                    meterText.setText(sending ? String.format("🎙️ %.0f dB", dbfs) : "🔇 靜音/保護");
                }
            }
        });
    }

    private void updateBubbleVoiceState() {
        if (bubbleView == null) return;
        boolean active = NativeLiveService.isActive();
        boolean speaking = NativeLiveService.isAiSpeaking();
        if (speaking) {
            bubbleView.setVoiceState(2); // Speaking (Purple gradient)
        } else if (active) {
            bubbleView.setVoiceState(1); // Listening (Cyan / Blue gradient)
        } else {
            bubbleView.setVoiceState(0); // Idle (Slate luxury gradient)
        }
    }

    // 🌊 Cyber Orb Fluid Bubble View (Matching Cyber Orb App Icon with soundwaves & rotating orbit)
    public static class FluidBubbleView extends View {
        private Paint bgPaint;
        private Paint ringPaint;
        private Paint wavePaint;
        private RectF ringBounds = new RectF();
        private SweepGradient idleSweepGradient;
        private SweepGradient activeSweepGradient;
        private SweepGradient speakingSweepGradient;
        private Matrix matrix = new Matrix();
        private float rotationAngle = 0f;
        private float wavePhase = 0f;
        private int voiceState = 0; // 0: idle, 1: listening, 2: speaking
        private ValueAnimator continuousRotator;
        private ValueAnimator waveAnimator;

        public FluidBubbleView(Context context) {
            super(context);
            init();
        }

        private void init() {
            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);

            ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(4.5f);
            ringPaint.setStrokeCap(Paint.Cap.ROUND);

            wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wavePaint.setStyle(Paint.Style.FILL);

            startAnimations();
        }

        private void startAnimations() {
            if (continuousRotator == null) {
                continuousRotator = ValueAnimator.ofFloat(0f, 360f);
                continuousRotator.setDuration(4000);
                continuousRotator.setRepeatCount(ValueAnimator.INFINITE);
                continuousRotator.setInterpolator(new LinearInterpolator());
                continuousRotator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        rotationAngle = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
            }
            if (!continuousRotator.isRunning()) continuousRotator.start();

            if (waveAnimator == null) {
                waveAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
                waveAnimator.setDuration(1200);
                waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
                waveAnimator.setInterpolator(new LinearInterpolator());
                waveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        wavePhase = (float) animation.getAnimatedValue();
                    }
                });
            }
            if (!waveAnimator.isRunning()) waveAnimator.start();
        }

        public void setVoiceState(int state) {
            this.voiceState = state;
            if (continuousRotator != null) {
                continuousRotator.setDuration(state == 2 ? 1400 : (state == 1 ? 2200 : 4000));
            }
            if (waveAnimator != null) {
                waveAnimator.setDuration(state == 2 ? 600 : (state == 1 ? 900 : 1600));
            }
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startAnimations();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (continuousRotator != null) continuousRotator.cancel();
            if (waveAnimator != null) waveAnimator.cancel();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float stroke = ringPaint.getStrokeWidth();
            ringBounds.set(stroke / 2f + 2f, stroke / 2f + 2f, w - stroke / 2f - 2f, h - stroke / 2f - 2f);

            float cx = w / 2f;
            float cy = h / 2f;

            // 1. Idle: Slate 600 / Indigo 500
            int[] idleColors = new int[]{
                    Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"),
                    Color.parseColor("#C084FC"), Color.parseColor("#38BDF8")
            };
            idleSweepGradient = new SweepGradient(cx, cy, idleColors, null);

            // 2. Active Listening: Electric Cyan & Bright Blue
            int[] activeColors = new int[]{
                    Color.parseColor("#00F0FF"), Color.parseColor("#3B82F6"),
                    Color.parseColor("#818CF8"), Color.parseColor("#00F0FF")
            };
            activeSweepGradient = new SweepGradient(cx, cy, activeColors, null);

            // 3. Speaking: Neon Purple & Lavender Glow
            int[] speakColors = new int[]{
                    Color.parseColor("#E879F9"), Color.parseColor("#A855F7"),
                    Color.parseColor("#38BDF8"), Color.parseColor("#E879F9")
            };
            speakingSweepGradient = new SweepGradient(cx, cy, speakColors, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = (Math.min(getWidth(), getHeight()) / 2f) - 2.5f;

            // 1. Radial Dark Indigo/Slate Glassmorphism Body
            int[] coreColors = new int[]{
                    Color.parseColor("#1E1B4B"), Color.parseColor("#0F172A"), Color.parseColor("#020617")
            };
            float[] corePositions = new float[]{0.0f, 0.65f, 1.0f};
            android.graphics.RadialGradient coreGrad = new android.graphics.RadialGradient(
                    cx, cy * 0.9f, radius, coreColors, corePositions, android.graphics.Shader.TileMode.CLAMP
            );
            bgPaint.setShader(coreGrad);
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // 2. Rotating Cyber Orbit Ring
            matrix.setRotate(rotationAngle, cx, cy);
            SweepGradient grad = voiceState == 2 ? speakingSweepGradient : (voiceState == 1 ? activeSweepGradient : idleSweepGradient);
            if (grad != null) {
                grad.setLocalMatrix(matrix);
                ringPaint.setShader(grad);
                canvas.drawOval(ringBounds, ringPaint);
            }

            // 3. Cyber Soundwave Spectrum in Center (4 vertical dynamic bars)
            float barWidth = radius * 0.17f;
            float barSpacing = radius * 0.28f;
            float startX = cx - 1.5f * barSpacing;

            // Base heights for the 4 bars
            float[] baseFactors = new float[]{0.35f, 0.65f, 0.85f, 0.40f};
            int[] barColors = voiceState == 2
                    ? new int[]{Color.parseColor("#F472B6"), Color.parseColor("#C084FC"), Color.parseColor("#A855F7"), Color.parseColor("#38BDF8")}
                    : (voiceState == 1
                    ? new int[]{Color.parseColor("#38BDF8"), Color.parseColor("#60A5FA"), Color.parseColor("#818CF8"), Color.parseColor("#38BDF8")}
                    : new int[]{Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"), Color.parseColor("#C084FC"), Color.parseColor("#38BDF8")});

            float maxH = radius * 0.82f;
            float minH = radius * 0.22f;

            for (int i = 0; i < 4; i++) {
                float bx = startX + i * barSpacing;
                // Dynamic oscillation based on voiceState & wavePhase
                float waveFactor = (float) Math.sin(wavePhase + i * 1.0f);
                float hFactor = baseFactors[i];
                if (voiceState == 2) {
                    hFactor = 0.4f + 0.55f * Math.abs(waveFactor);
                } else if (voiceState == 1) {
                    hFactor = 0.3f + 0.40f * Math.abs(waveFactor);
                } else {
                    hFactor = baseFactors[i] + 0.10f * (float) Math.sin(wavePhase + i * 0.8f);
                }

                float h = minH + (maxH - minH) * Math.max(0.15f, Math.min(1.0f, hFactor));
                float top = cy - h / 2f;
                float bottom = cy + h / 2f;

                wavePaint.setColor(barColors[i]);
                canvas.drawRoundRect(bx - barWidth / 2f, top, bx + barWidth / 2f, bottom, barWidth / 2f, barWidth / 2f, wavePaint);
            }
        }
    }
}
