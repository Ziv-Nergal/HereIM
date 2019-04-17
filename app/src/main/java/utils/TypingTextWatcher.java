package utils;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class TypingTextWatcher implements TextWatcher {

    private static final long DELAY = 1000;

    private String mGroupId;

    private long mLastTextEdit = 0;

    private Handler mDelayHandler = new Handler();

    private Runnable mStopTypingRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() > (mLastTextEdit + DELAY - 500)) {
                sDatabaseManager.groupChatsDbRef().child(mGroupId).child("typing").setValue("nobody");
            }
        }
    };

    public TypingTextWatcher(String groupId) { mGroupId = groupId; }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!TextUtils.isEmpty(s.toString()) && count > before) {
            sDatabaseManager.groupChatsDbRef().child(mGroupId).child("typing").setValue(sCurrentFirebaseUser.getFullName());
            mDelayHandler.removeCallbacks(mStopTypingRunnable);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(s.length() > 0) {
            mLastTextEdit = System.currentTimeMillis();
            mDelayHandler.postDelayed(mStopTypingRunnable, DELAY);
        }
    }
}
