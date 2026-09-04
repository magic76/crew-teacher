package com.crewpocket.teacher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

/**
 * Foreground Service that manages Gemini Live voice tutoring independently of Activity.
 */
public class NativeLiveService extends Service {
    private static final String ACTION_START = "com.crewpocket.teacher.NATIVE_LIVE_START";
    private static final String ACTION_STOP = "com.crewpocket.teacher.NATIVE_LIVE_STOP";
    private static final int NOTIFICATION_ID = 8788;
    private static final String CHANNEL_ID = "crew_teacher_live";

    private static volatile boolean active;
    private static NativeLiveService instance;
    private NativeGeminiLiveClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int reconnectAttempts;
    private boolean stopRequested;

    private final Runnable reconnectRunnable = new Runnable() {
        @Override public void run() {
            if (!active || stopRequested) return;
            startLiveClient();
        }
    };

    static boolean isActive() { return active; }

    static void start(Context context) {
        Intent intent = new Intent(context, NativeLiveService.class).setAction(ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Context.class.getMethod("startForegroundService", Intent.class).invoke(context, intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception error) {
            context.startService(intent);
        }
    }

    static void stop(Context context) {
        context.startService(new Intent(context, NativeLiveService.class).setAction(ACTION_STOP));
    }

    static boolean toggleAgentMute() {
        return instance != null && instance.client != null && instance.client.toggleAgentMute();
    }

    static boolean toggleVoiceInterruption() {
        if (instance != null && instance.client != null) {
            boolean current = instance.client.isVoiceInterruptionAllowed();
            instance.client.setAllowVoiceInterruption(!current);
            return !current;
        }
        return true;
    }

    static boolean isVoiceInterruptionAllowed() {
        return instance != null && instance.client != null && instance.client.isVoiceInterruptionAllowed();
    }

    static boolean isAgentMuted() {
        return instance != null && instance.client != null && instance.client.isAgentMuted();
    }

    static boolean isAiSpeaking() {
        return instance != null && instance.client != null && instance.client.isAiSpeaking();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            end("使用者手動掛斷");
            return START_NOT_STICKY;
        }

        stopRequested = false;
        reconnectAttempts = 0;
        startForeground(NOTIFICATION_ID, buildNotification());
        active = true;
        FloatingBubbleManager.getInstance(this).showBubble();
        startLiveClient();
        return START_STICKY;
    }

    private void startLiveClient() {
        if (client != null && client.isRunning()) return;

        String apiKey = AppConfig.getGeminiApiKey(this);
        String voice = AppConfig.getVoiceName(this);
        String lang = AppConfig.getTutorLanguage(this);
        String persona = AppConfig.getTutorPersona(this);
        String noiseMode = AppConfig.getNoiseMode(this);
        int noiseSuppression = AppConfig.getNoiseSuppression(this);
        String liveTone = AppConfig.getLiveTone(this);
        int interruptionSensitivity = AppConfig.getInterruptionSensitivity(this);
        String audioOutput = AppConfig.getAudioOutput(this);
        String customPrompt = AppConfig.getCustomSystemPrompt(this);

        client = new NativeGeminiLiveClient(this, apiKey, voice, lang, persona, noiseMode,
                noiseSuppression, liveTone, interruptionSensitivity, audioOutput, customPrompt,
                new NativeGeminiLiveClient.Listener() {
                    @Override
                    public void onStatus(final String status) {
                        handler.post(new Runnable() {
                            @Override public void run() { updateStatus(status, true); }
                        });
                    }

                    @Override
                    public void onStopped(final String reason) {
                        handler.post(new Runnable() {
                            @Override public void run() { handleClientStopped(reason); }
                        });
                    }

                    @Override
                    public void onTranscript(final String text, final String role) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                FloatingBubbleManager.getInstance(NativeLiveService.this).appendTranscript(text, role);
                            }
                        });
                    }

                    @Override
                    public void onMicrophoneLevel(final double dbfs, final double gateDbfs, final boolean sending) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                FloatingBubbleManager.getInstance(NativeLiveService.this).updateMicrophoneMeter(dbfs, sending);
                            }
                        });
                    }

                    @Override
                    public void onSpeakingChanged(final boolean speaking) {
                        handler.post(new Runnable() {
                            @Override public void run() {
                                FloatingBubbleManager.getInstance(NativeLiveService.this).onSpeakingChanged(speaking);
                                updateStatus(speaking ? "🔊 導師說話中" : "🎙️ 導師聆聽中", true);
                            }
                        });
                    }
                });

        client.start();
    }

    private void handleClientStopped(String reason) {
        if (!active || stopRequested || isGracefulCallEnd(reason)) {
            end(reason);
            return;
        }
        if (reason != null && (reason.contains("API Key") || reason.contains("401") || reason.contains("403") || reason.contains("400"))) {
            end(reason);
            return;
        }
        if (reconnectAttempts >= 3) {
            end("連線中斷（已達重試上限）：" + reason);
            return;
        }
        reconnectAttempts++;
        client = null;
        long delayMs = 1000L * reconnectAttempts;
        updateStatus("連線中斷，正在重新連線（" + reconnectAttempts + "/3）…", true);
        handler.removeCallbacks(reconnectRunnable);
        handler.postDelayed(reconnectRunnable, delayMs);
    }

    private boolean isGracefulCallEnd(String reason) {
        String text = reason == null ? "" : reason.trim();
        return "已結束".equals(text) || text.contains("使用者") || text.contains("掛斷") || text.contains("結束練習");
    }

    private void updateStatus(String status, boolean showOngoing) {
        if (!active) return;
        FloatingBubbleManager.getInstance(this).updateNativeLiveStatus(status, showOngoing);
    }

    private synchronized void end(String reason) {
        stopRequested = true;
        handler.removeCallbacks(reconnectRunnable);
        active = false;
        NativeGeminiLiveClient closing = client;
        client = null;
        if (closing != null && closing.isRunning()) closing.stop();
        FloatingBubbleManager.getInstance(this).updateNativeLiveStatus(reason, false);
        try { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID); } catch (Exception ignored) {}
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Crew Teacher")
                .setContentText("語音口語練習進行中…")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        if (Build.VERSION.SDK_INT >= 26) {
            try { Notification.Builder.class.getMethod("setChannelId", String.class).invoke(builder, CHANNEL_ID); } catch (Exception ignored) {}
        }
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            Class<?> cls = Class.forName("android.app.NotificationChannel");
            Object channel = cls.getConstructor(String.class, CharSequence.class, int.class)
                    .newInstance(CHANNEL_ID, "Crew Teacher Live", NotificationManager.IMPORTANCE_LOW);
            NotificationManager.class.getMethod("createNotificationChannel", cls)
                    .invoke((NotificationManager) getSystemService(NOTIFICATION_SERVICE), channel);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        stopRequested = true;
        handler.removeCallbacks(reconnectRunnable);
        active = false;
        instance = null;
        NativeGeminiLiveClient closing = client;
        client = null;
        if (closing != null && closing.isRunning()) closing.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
