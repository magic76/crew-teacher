package com.crewpocket.teacher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class CrewTheme {
    private CrewTheme() {}

    public static final int BG_PRIMARY     = 0xFF020617;
    public static final int BG_SURFACE     = 0xFF0F172A;
    public static final int BG_ELEVATED    = 0xFF1E293B;
    public static final int BG_CARD        = 0xF00F172A;

    public static final int INDIGO_600     = 0xFF4F46E5;
    public static final int INDIGO_500     = 0xFF6366F1;
    public static final int INDIGO_400     = 0xFF818CF8;
    public static final int INDIGO_300     = 0xFFA5B4FC;
    public static final int TEAL_500       = 0xFF14B8A6;
    public static final int TEAL_400       = 0xFF2DD4BF;
    public static final int TEAL_300       = 0xFF5EEAD4;
    public static final int CYAN_500       = 0xFF06B6D4;
    public static final int CYAN_400       = 0xFF22D3EE;

    public static final int EMERALD_500    = 0xFF10B981;
    public static final int EMERALD_400    = 0xFF34D399;
    public static final int ROSE_500       = 0xFFF43F5E;
    public static final int ROSE_400       = 0xFFFB7185;
    public static final int AMBER_500      = 0xFFF59E0B;
    public static final int AMBER_400      = 0xFFFBBF24;

    public static final int TEXT_PRIMARY   = 0xFFF8FAFC;
    public static final int TEXT_SECONDARY = 0xFF94A3B8;
    public static final int TEXT_MUTED     = 0xFF64748B;
    public static final int TEXT_DISABLED  = 0xFF475569;

    public static final int BORDER_DEFAULT = 0xFF334155;
    public static final int BORDER_SUBTLE  = 0xFF334155;
    public static final int BORDER_INDIGO  = 0x33818CF8;
    public static final int BORDER_TEAL    = 0x332DD4BF;
    public static final int BORDER_EMERALD = 0x3310B981;
    public static final int BORDER_AMBER   = 0x33F59E0B;

    public static int dp(Context ctx, float value) {
        if (ctx == null) return (int) value;
        return (int) (value * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable createCard(Context ctx, int bgColor, int borderColor, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        if (borderColor != Color.TRANSPARENT) {
            gd.setStroke(dp(ctx, 1f), borderColor);
        }
        return gd;
    }
}
