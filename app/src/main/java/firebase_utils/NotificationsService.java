package firebase_utils;

import android.content.Context;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import utils.SoundFxManager;

import static activities.MainActivity.sCurrentFirebaseUser;

public class NotificationsService extends FirebaseMessagingService {

    private static final String MSG_NOTIFICATION_DATA = "message";
    private static final String GROUP_REQUEST_NOTIFICATION_DATA = "group_request";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if(remoteMessage.getData().size() > 0 && remoteMessage.getNotification() != null) {

            String notificationType = remoteMessage.getData().get("type");

            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean("pref_notification_vibrate", true)) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                if (vibrator != null) {
                    vibrator.vibrate(100);
                }
            }

            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean("pref_notification_sound", true)) {
                if(notificationType != null) {
                    switch (notificationType) {
                        case MSG_NOTIFICATION_DATA:
                            SoundFxManager
                                    .PlaySoundFx(SoundFxManager.eSoundEffect.MESSAGE_FX);
                            break;
                        case GROUP_REQUEST_NOTIFICATION_DATA:
                            SoundFxManager
                                    .PlaySoundFx(SoundFxManager.eSoundEffect.GROUP_REQUEST_FX);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        sCurrentFirebaseUser.setDeviceToken(s);
    }
}
