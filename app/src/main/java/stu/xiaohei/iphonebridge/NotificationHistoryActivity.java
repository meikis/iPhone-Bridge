package stu.xiaohei.iphonebridge;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import stu.xiaohei.iphonebridge.database.NotificationDatabase;
import stu.xiaohei.iphonebridge.database.NotificationViewModel;
import stu.xiaohei.iphonebridge.database.NotificationViewModelFactory;

public class NotificationHistoryActivity extends AppCompatActivity {

    private NotificationViewModel notificationViewModel;
    private NotificationHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_history);

        setTitle("通知历史");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotificationHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationHistoryAdapter();
        recyclerView.setAdapter(adapter);

        NotificationDatabase database = NotificationDatabase.getDatabase(getApplicationContext());
        notificationViewModel = new ViewModelProvider(this, new NotificationViewModelFactory(database.notificationDao()))
                .get(NotificationViewModel.class);

        notificationViewModel.getAllNotifications().observe(this, notifications -> {
            adapter.setNotifications(notifications);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}