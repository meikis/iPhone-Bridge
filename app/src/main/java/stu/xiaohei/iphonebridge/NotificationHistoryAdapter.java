package stu.xiaohei.iphonebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import stu.xiaohei.iphonebridge.database.NotificationEntity;

public class NotificationHistoryAdapter extends RecyclerView.Adapter<NotificationHistoryAdapter.NotificationViewHolder> {

    private List<NotificationEntity> notificationList = new ArrayList<>();

    public void setNotifications(List<NotificationEntity> notifications) {
        this.notificationList = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_history, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationEntity currentNotification = notificationList.get(position);
        holder.bind(currentNotification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView appNameTextView;
        private TextView titleTextView;
        private TextView messageTextView;
        private TextView timestampTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(R.id.text_view_app_name);
            titleTextView = itemView.findViewById(R.id.text_view_title);
            messageTextView = itemView.findViewById(R.id.text_view_message);
            timestampTextView = itemView.findViewById(R.id.text_view_timestamp);
        }

        public void bind(NotificationEntity notification) {
            appNameTextView.setText(notification.appId != null && !notification.appId.isEmpty() ? notification.appId : "未知应用");
            titleTextView.setText(notification.title != null && !notification.title.isEmpty() ? notification.title : "无标题");
            messageTextView.setText(notification.message != null && !notification.message.isEmpty() ? notification.message : "无内容");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            timestampTextView.setText(sdf.format(new Date(notification.timestamp)));
        }
    }
}