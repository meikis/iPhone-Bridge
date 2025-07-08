package stu.xiaohei.iphonebridge;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReconnectWorker extends Worker {

    private static final String TAG = "ReconnectWorker";
    public static final String KEY_DEVICE_ADDRESS = "device_address";

    public ReconnectWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ReconnectWorker started");
        String deviceAddress = getInputData().getString(KEY_DEVICE_ADDRESS);

        if (deviceAddress != null) {
            Intent intent = new Intent(getApplicationContext(), BridgeService.class);
            intent.setAction(BridgeService.ACTION_RECONNECT);
            intent.putExtra(BridgeService.EXTRA_DEVICE_ADDRESS, deviceAddress);
            getApplicationContext().startService(intent);
            Log.d(TAG, "Sent reconnect intent for device: " + deviceAddress);
            return Result.success();
        } else {
            Log.e(TAG, "Device address was null. Cannot reconnect.");
            return Result.failure();
        }
    }
}