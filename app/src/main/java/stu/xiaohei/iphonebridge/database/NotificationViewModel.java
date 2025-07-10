package stu.xiaohei.iphonebridge.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NotificationViewModel extends ViewModel {
    private final NotificationDao notificationDao;
    private final LiveData<List<NotificationEntity>> allNotifications;

    public NotificationViewModel(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
        allNotifications = notificationDao.getAllNotifications();
    }

    public LiveData<List<NotificationEntity>> getAllNotifications() {
        return allNotifications;
    }
}