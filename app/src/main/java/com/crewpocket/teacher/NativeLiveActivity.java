package com.crewpocket.teacher;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class NativeLiveActivity extends Activity {
    private static final int REQUEST_RECORD_AUDIO = 301;
    private TextView statusDot;
    private TextView statusText;
    private TextView transcript;
    private ScrollView transcriptScrollView;
    private TextView meterText;
    private Button callButton;
    private Button muteButton;
    private NativeGeminiLiveClient client;
    private final Handler handler = new Handler();

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
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        boolean en = I18n.isEnglish(this);

        // 1. Back button & Title
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText(en ? "‹ Back" : "‹ 返回");
        backBtn.setTextSize(15);
        backBtn.setTextColor(CrewTheme.INDIGO_400);
        backBtn.setTypeface(Typeface.DEFAULT_BOLD);
        backBtn.setPadding(0, 0, dp(16), 0);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(backBtn);

        TextView title = new TextView(this);
        title.setText(en ? "🎓 Oral Practice Classroom" : "🎓 口語即時對話教室");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);
        root.addView(header);

        // 2. Status Row
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.HORIZONTAL);
        statusBox.setGravity(Gravity.CENTER_VERTICAL);
        statusBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(Color.parseColor("#1E293B"));
        sbg.setCornerRadius(dp(12));
        statusBox.setBackground(sbg);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(18), 0, dp(16));
        statusBox.setLayoutParams(sLp);

        statusDot = new TextView(this);
        statusDot.setText("●");
        statusDot.setTextSize(14);
        statusDot.setTextColor(CrewTheme.TEXT_MUTED);
        statusDot.setPadding(0, 0, dp(8), 0);
        statusBox.addView(statusDot);

        statusText = new TextView(this);
        statusText.setText(en ? "Standby (Tap Start below)" : "待命中（點擊下方開始通話）");
        statusText.setTextColor(CrewTheme.TEXT_SECONDARY);
        statusText.setTextSize(13);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusBox.addView(statusText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        meterText = new TextView(this);
        meterText.setText("🎙️ -- dB");
        meterText.setTextColor(CrewTheme.TEXT_MUTED);
        meterText.setTextSize(11);
        statusBox.addView(meterText);

        root.addView(statusBox);

        // 3. Transcript Card
        LinearLayout transcriptCard = new LinearLayout(this);
        transcriptCard.setOrientation(LinearLayout.VERTICAL);
        transcriptCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        transcriptCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_CARD, CrewTheme.BORDER_DEFAULT, 16));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        tLp.setMargins(0, 0, 0, dp(18));
        transcriptCard.setLayoutParams(tLp);

        TextView tTitle = new TextView(this);
        tTitle.setText(en ? "💬 Real-time Transcript" : "💬 即時對話逐字紀錄");
        tTitle.setTextSize(12);
        tTitle.setTextColor(CrewTheme.TEXT_MUTED);
        tTitle.setTypeface(Typeface.DEFAULT_BOLD);
        transcriptCard.addView(tTitle);

        transcriptScrollView = new ScrollView(this);
        transcriptScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        transcriptScrollView.setPadding(0, dp(8), 0, 0);

        transcript = new TextView(this);
        transcript.setText(en
                ? "Tap 'Start Oral Practice' below to connect with Gemini Live.\nSpeak naturally to your phone and the AI tutor will give instant spoken responses and guidance!"
                : "點擊下方「開始口語對話」連線至 Gemini Live 語音引擎。\n連線後直接對著手機說話，AI 外教會即時給予語音回應與引導！");
        transcript.setTextColor(CrewTheme.TEXT_PRIMARY);
        transcript.setTextSize(14);
        transcript.setLineSpacing(dp(3), 1.2f);
        transcript.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (transcriptScrollView != null) {
                    transcriptScrollView.post(new Runnable() {
                        @Override public void run() {
                            if (transcriptScrollView != null) {
                                transcriptScrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        }
                    });
                }
            }
        });
        transcriptScrollView.addView(transcript);
        transcriptCard.addView(transcriptScrollView);

        root.addView(transcriptCard);

        // 4. Control Buttons Row
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        callButton = new Button(this);
        callButton.setText(en ? "🎙️ Start Practice" : "🎙️ 開始口語對話");
        callButton.setTextColor(Color.WHITE);
        callButton.setTextSize(15);
        callButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable callBg = new GradientDrawable();
        callBg.setColor(Color.parseColor("#2563EB"));
        callBg.setCornerRadius(dp(14));
        callButton.setBackground(callBg);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleCall(); }
        });
        controls.addView(callButton, new LinearLayout.LayoutParams(0, dp(50), 1.5f));

        muteButton = new Button(this);
        muteButton.setText(en ? "Mute" : "靜音");
        muteButton.setTextColor(Color.WHITE);
        muteButton.setTextSize(13);
        GradientDrawable muteBg = new GradientDrawable();
        muteBg.setColor(Color.parseColor("#334155"));
        muteBg.setCornerRadius(dp(14));
        muteButton.setBackground(muteBg);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        mLp.setMargins(dp(10), 0, 0, 0);
        muteButton.setLayoutParams(mLp);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (client != null) {
                    boolean muted = client.toggleAgentMute();
                    boolean isEn = I18n.isEnglish(NativeLiveActivity.this);
                    muteButton.setText(muted ? (isEn ? "Unmute" : "取消靜音") : (isEn ? "Mute" : "靜音"));
                    ((GradientDrawable) muteButton.getBackground()).setColor(muted ? Color.parseColor("#D97706") : Color.parseColor("#334155"));
                }
            }
        });
        controls.addView(muteButton);

        root.addView(controls);
        setContentView(root);
    }

    private void toggleCall() {
        if (client != null && client.isRunning()) {
            stopClient("使用者結束對話");
        } else {
            startClient();
        }
    }

    private void startClient() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        String apiKey = AppConfig.getGeminiApiKey(this);
        if (apiKey.isEmpty()) {
            showApiKeyDialog();
            return;
        }

        String voice = AppConfig.getVoiceName(this);
        String lang = AppConfig.getTutorLanguage(this);
        String persona = AppConfig.getTutorPersona(this);
        String noiseMode = AppConfig.getNoiseMode(this);
        int noiseSuppression = AppConfig.getNoiseSuppression(this);
        String liveTone = AppConfig.getLiveTone(this);
        int interruptionSensitivity = AppConfig.getInterruptionSensitivity(this);
        String audioOutput = AppConfig.getAudioOutput(this);
        String customPrompt = AppConfig.getCustomSystemPrompt(this);

        updateStatus(CrewTheme.AMBER_400, "正在連線至語音引擎…");
        callButton.setText("掛斷對話");
        ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#E11D48"));

        client = new NativeGeminiLiveClient(this, apiKey, voice, lang, persona, noiseMode,
                noiseSuppression, liveTone, interruptionSensitivity, audioOutput, customPrompt,
                new NativeGeminiLiveClient.Listener() {
                    @Override
                    public void onStatus(final String status) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                updateStatus(CrewTheme.EMERALD_400, status);
                            }
                        });
                    }

                    @Override
                    public void onStopped(final String reason) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                updateStatus(CrewTheme.TEXT_MUTED, "已結束：" + reason);
                                callButton.setText("🎙️ 開始口語對話");
                                ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#2563EB"));
                            }
                        });
                    }

                    @Override
                    public void onTranscript(final String text, final String role) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                appendLiveTranscript(text, role);
                            }
                        });
                    }

                    @Override
                    public void onMicrophoneLevel(final double dbfs, final double gateDbfs, final boolean sending) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                meterText.setText(sending ? String.format("🎙️ %.0f dB", dbfs) : "🔇 降噪/保護");
                            }
                        });
                    }

                    @Override
                    public void onSpeakingChanged(final boolean speaking) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                if (speaking) {
                                    updateStatus(CrewTheme.CYAN_400, "🔊 導師回答中…");
                                } else {
                                    updateStatus(CrewTheme.EMERALD_400, "🎙️ 導師聆聽中，請說話");
                                }
                            }
                        });
                    }
                });

        client.start();
    }

    private void stopClient(String reason) {
        if (client != null) {
            client.stop();
            client = null;
        }
        updateStatus(CrewTheme.TEXT_MUTED, reason);
        callButton.setText("🎙️ 開始口語對話");
        ((GradientDrawable) callButton.getBackground()).setColor(Color.parseColor("#2563EB"));
    }

    private void updateStatus(int dotColor, String text) {
        statusDot.setTextColor(dotColor);
        statusText.setText(text);
    }

    private String lastTranscriptRole = "";

    private void appendLiveTranscript(String text, String role) {
        if (text == null || text.isEmpty()) return;
        String existing = transcript.getText().toString();
        if (existing.startsWith("點擊下方「開始口語對話」") || existing.startsWith("Tap 'Start Oral Practice'")) {
            existing = "";
        }

        if ("translation".equalsIgnoreCase(role)) {
            transcript.setText(existing + "\n📖 翻譯：" + text);
            lastTranscriptRole = "";
            return;
        }

        boolean isAi = "ai".equalsIgnoreCase(role) || "Gemini".equalsIgnoreCase(role);
        String roleKey = isAi ? "ai" : "user";
        boolean sameSpeaker = roleKey.equals(lastTranscriptRole) && !existing.isEmpty();

        String prefix = sameSpeaker ? "" : (existing.isEmpty() ? "" : "\n\n") + (isAi ? "🤖 導師: " : "🗣️ 你: ");
        lastTranscriptRole = roleKey;
        transcript.setText(existing + prefix + text);
    }

    private void showApiKeyDialog() {
        final boolean en = I18n.isEnglish(this);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(en ? "🔑 Enter Gemini API Key" : "🔑 請輸入 Gemini API Key");
        builder.setMessage(en ? "Gemini Live voice tutor requires a Gemini API Key to connect." : "連線 Gemini Live 口說導師需要填入您的 Gemini API Key 才能開始。");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("AIzaSy...");
        input.setText(AppConfig.getGeminiApiKey(this));
        input.setTextColor(Color.WHITE);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        builder.setView(input);

        builder.setPositiveButton(en ? "Save & Start" : "儲存並連線", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                AppConfig.setGeminiApiKey(NativeLiveActivity.this, key);
                if (!key.isEmpty()) {
                    startClient();
                }
            }
        });
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        stopClient("離開頁面");
        super.onDestroy();
    }
}
