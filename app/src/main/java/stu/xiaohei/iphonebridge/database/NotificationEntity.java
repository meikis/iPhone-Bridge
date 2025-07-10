package stu.xiaohei.iphonebridge.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class NotificationEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String uid;
    public byte eventId;
    public byte categoryId;
    public byte eventFlags;
    public String appId;
    public String title;
    public String subtitle;
    public String message;
    public String date;
    public String positiveActionLabel;
    public String negativeActionLabel;
    public boolean hasPositiveAction;
    public boolean hasNegativeAction;
    public long timestamp;

    public NotificationEntity(String uid, byte eventId, byte categoryId, byte eventFlags, String appId, String title, String subtitle, String message, String date, String positiveActionLabel, String negativeActionLabel, boolean hasPositiveAction, boolean hasNegativeAction, long timestamp) {
        this.uid = uid;
        this.eventId = eventId;
        this.categoryId = categoryId;
        this.eventFlags = eventFlags;
        this.appId = appId;
        this.title = title;
        this.subtitle = subtitle;
        this.message = message;
        this.date = date;
        this.positiveActionLabel = positiveActionLabel;
        this.negativeActionLabel = negativeActionLabel;
        this.hasPositiveAction = hasPositiveAction;
        this.hasNegativeAction = hasNegativeAction;
        this.timestamp = timestamp;
    }
}