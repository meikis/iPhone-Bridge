package stu.xiaohei.iphonebridge.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotification(NotificationEntity notification);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<NotificationEntity>> getAllNotifications();

    @Query("SELECT * FROM notifications WHERE uid = :uid LIMIT 1")
    NotificationEntity getNotificationByUid(String uid);

    @Query("DELETE FROM notifications WHERE uid = :uid")
    void deleteNotificationByUid(String uid);

    @Query("DELETE FROM notifications")
    void deleteAllNotifications();

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTimestamp")
    void deleteOldNotifications(long cutoffTimestamp);
}