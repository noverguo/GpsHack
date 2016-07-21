package info.noverguo.gpshack;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by noverguo on 2016/7/18.
 */
public class GSetting {
    private static final String PREF_NAME = "setting";
    private static final String KEY_INIT = "init";
    public static boolean isInit(Context context) {
        return getPref(context).getBoolean(KEY_INIT, false);
    }

    public static void setInit(Context context, boolean init) {
        getPref(context).edit().putBoolean(KEY_INIT, init).apply();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
