package com.example.mobilnaaplikacijatim29.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.model.AppNotification;

public final class SystemNotificationHelper {
    public static final String CHANNEL_ID = "ride_notifications";

    private SystemNotificationHelper() { }

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Obaveštenja o vožnjama", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Dodela vožnje, neuspešno poručivanje i podsetnici");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static boolean show(Context context, AppNotification value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (opensRide(value)) {
            intent.putExtra(MainActivity.EXTRA_OPEN_RIDE_ID, value.getRideId());
        } else {
            intent.putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true);
        }
        PendingIntent pending = PendingIntent.getActivity(context,
                value.getId() == null ? 0 : value.getId().intValue(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title(value.getType()))
                .setContentText(value.getContent())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(value.getContent()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return false;
        NotificationChannel channel = context.getSystemService(NotificationManager.class)
                .getNotificationChannel(CHANNEL_ID);
        if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            return false;
        }
        manager.notify(value.getId() == null ? 1 : value.getId().intValue(),
                notification.build());
        return true;
    }

    private static String title(String type) {
        if ("NEW_RIDE".equals(type)) return "Nova vožnja";
        if ("RIDE_ACCEPTED".equals(type)) return "Vožnja prihvaćena";
        if ("RIDE_REJECTED".equals(type)) return "Vožnja nije prihvaćena";
        if ("RIDE_REMINDER".equals(type)) return "Podsetnik za vožnju";
        if ("LINKED_RIDE".equals(type)) return "Povezani ste sa vožnjom";
        if ("RIDE_STARTED".equals(type)) return "Vožnja je započeta";
        if ("RIDE_FINISHED".equals(type)) return "Vožnja je završena";
        return "Click & Drive";
    }

    static boolean opensRide(AppNotification value) {
        if (value.getRideId() == null) return false;
        String type = value.getType();
        return "LINKED_RIDE".equals(type) || "RIDE_ACCEPTED".equals(type)
                || "RIDE_REMINDER".equals(type) || "RIDE_FINISHED".equals(type)
                || "RIDE_STARTED".equals(type) || "NEW_RIDE".equals(type);
    }
}
