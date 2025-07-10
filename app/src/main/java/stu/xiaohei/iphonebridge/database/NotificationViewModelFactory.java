package stu.xiaohei.iphonebridge.database;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class NotificationViewModelFactory implements ViewModelProvider.Factory {
    private final NotificationDao notificationDao;

    public NotificationViewModelFactory(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NotificationViewModel.class)) {
            return (T) new NotificationViewModel(notificationDao);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}