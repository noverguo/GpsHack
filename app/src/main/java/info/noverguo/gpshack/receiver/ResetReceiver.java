package info.noverguo.gpshack.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by noverguo on 2016/6/8.
 */
public class ResetReceiver extends BroadcastReceiver {
    private static final String ACTION_RESET = ResetReceiver.class.getName() + "ACTION_RESET";
    private Callback callback;
    private Context context;
    private ResetReceiver(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }
    public static ResetReceiver register(Context context, Callback callback) {
        if (callback == null) {
            return null;
        }
        IntentFilter intentFilter = new IntentFilter(ACTION_RESET);
        ResetReceiver resetReceiver = new ResetReceiver(context, callback);
        context.registerReceiver(resetReceiver, intentFilter);
        return resetReceiver;
    }

    public void unregister() {
        context.unregisterReceiver(this);
    }

    public static void sendReset(Context context) {
        context.sendBroadcast(new Intent(ACTION_RESET));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith(ACTION_RESET)) {
            callback.onReset();
        }
    }

    public interface Callback {
        void onReset();
    }
}
