package utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;

import gis.hereim.R;

public class SoundFxManager {

    private static final int MAX_STREAMS = 4;

    private static SoundPool mFxPlayer = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 100);

    private static int mMsgFx;
    private static int mGroupRequestFx;
    private static int mDistanceAlertFx;

    public static void InitManager(final Context context) {
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                mMsgFx = mFxPlayer.load(context, R.raw.group_chat_msg_fx, 1);
                mGroupRequestFx = mFxPlayer.load(context, R.raw.group_request_msg_fx, 1);
                mDistanceAlertFx = mFxPlayer.load(context, R.raw.distance_alert, 1);
                return null;
            }
        }.execute();
    }

    public static void PlaySoundFx(eSoundEffect fx){
        switch (fx) {
            case MESSAGE_FX: mFxPlayer.play(mMsgFx, 1, 1, 0, 0, 1); break;
            case GROUP_REQUEST_FX: mFxPlayer.play(mGroupRequestFx, 1, 1, 0, 0, 1); break;
            case DISTANCE_ALERT: mFxPlayer.play(mDistanceAlertFx, 0.5f, 0.5f, 0, 0, 1); break;
        }
    }

    public enum eSoundEffect{
        MESSAGE_FX,
        GROUP_REQUEST_FX,
        DISTANCE_ALERT
    }
}
