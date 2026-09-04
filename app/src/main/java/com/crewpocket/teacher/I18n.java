package com.crewpocket.teacher;

import android.content.Context;

public class I18n {
    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    public static boolean isEnglish(Context context) {
        return LANG_EN.equalsIgnoreCase(AppConfig.getUiLanguage(context));
    }

    public static String t(Context context, String zhText, String enText) {
        return isEnglish(context) ? enText : zhText;
    }
}
