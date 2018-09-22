package li.doerf.hacked.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.utils.NotificationHelper;
import li.doerf.hacked.utils.OreoNotificationHelper;

/**
 * Created by moo on 26.03.17.
 */

public class CheckServiceHelper {
//    private static final String LOGTAG = "CheckServiceHelper";
    private static final String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";
    private final Context myContext;

    public CheckServiceHelper(Context aContext) {
        myContext = aContext;
    }

    public void showNotification() {
        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            OreoNotificationHelper onh = new OreoNotificationHelper(myContext);
            onh.createNotificationChannel();
        }

        String title = myContext.getString(R.string.notification_title_new_breaches_found);
        androidx.core.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(myContext)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(myContext.getString(R.string.notification_text_click_to_open))
                        .setChannelId(OreoNotificationHelper.CHANNEL_ID)
                        .setGroup(NOTIFICATION_GROUP_KEY_BREACHES);

        Intent showBreachDetails = new Intent(myContext, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        myContext,
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.flags |= Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        NotificationHelper.notify(myContext, notification);
    }
}
