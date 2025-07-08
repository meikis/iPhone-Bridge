package stu.xiaohei.iphonebridge;

import android.app.Application;
import androidx.work.Configuration;

public class MyApplication extends Application implements Configuration.Provider {

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Any other application-wide initialization can go here
    }
}
