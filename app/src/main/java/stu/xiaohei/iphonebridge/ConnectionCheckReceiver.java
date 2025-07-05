package stu.xiaohei.iphonebridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ConnectionCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectionCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Connection check alarm triggered");
        Intent serviceIntent = new Intent(context, BridgeService.class);
        serviceIntent.setAction(BridgeService.ACTION_CHECK_CONNECTION);
        context.startForegroundService(serviceIntent);
    }
}
