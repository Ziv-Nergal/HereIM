package activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.github.jorgecastilloprz.listeners.FABProgressListener;
import com.podcopic.animationlib.library.AnimationType;
import com.podcopic.animationlib.library.StartSmartAnimation;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tooltip.Tooltip;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;
import firebase_utils.DatabaseManager;
import gis.hereim.R;
import id.zelory.compressor.Compressor;
import utils.KeyboardUtils;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class CreateNewGroupActivity extends AppCompatActivity implements FABProgressListener {

    private CircleImageView mGroupPhoto;

    private EditText mGroupNameEditText;

    private Uri mGroupPhotoUri;

    private View mGroupIdLayout;

    private Tooltip mGroupIdToolTip;

    private Handler mDelayHandler = new Handler();

    private FABProgressCircle mFABProgressCircle;

    private String mCreatedGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(sCurrentFirebaseUser.IsLoggedIn()){
            setActivityUI();
        }else{
            goToLoginPage();
        }
    }

    private void setActivityUI() {

        setContentView(R.layout.activity_create_new_group);

        mGroupPhoto = findViewById(R.id.create_group_photo_image_view);
        mGroupNameEditText = findViewById(R.id.create_new_group_name_edit_text);
        mGroupIdLayout = findViewById(R.id.create_group_id_layout);
        mFABProgressCircle = findViewById(R.id.create_group_btn);

        mFABProgressCircle.attachListener(this);

        mGroupIdToolTip = new Tooltip.Builder(findViewById(R.id.create_group_share_id_btn))
                .setText(R.string.group_id_instructions_long)
                .setDismissOnClick(true)
                .setArrowEnabled(true)
                .setMaxWidth(1000)
                .setBackgroundColor(getResources().getColor(R.color.tooltip_color))
                .setTextColor(Color.WHITE)
                .setCornerRadius((float)10)
                .build();

        loadToolBar();
    }

    private void loadToolBar() {

        setSupportActionBar((Toolbar) findViewById(R.id.main_tool_bar));

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void AddGroupPhotoBtnClick(View view) {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setAspectRatio(1, 1)
                .start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                mGroupPhoto.setImageURI(result.getUri());

                try {
                    File userImageFile = new Compressor(this)
                            .setQuality(40)
                            .compressToFile(new File(Objects.requireNonNull(result.getUri().getPath())));

                    mGroupPhotoUri = Uri.fromFile(userImageFile);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void CreateGroupBtnClick(View view) {

        final FABProgressCircle fabProgressCircle = (FABProgressCircle) view;

        String groupName = mGroupNameEditText.getText().toString().trim();

        if(!TextUtils.isEmpty(groupName)){
            createGroup(groupName, fabProgressCircle);
        }else {
            StartSmartAnimation.startAnimation(findViewById(R.id.create_new_group_name_edit_text_layout), AnimationType.Shake, 600, 0, false);
        }
    }

    private void createGroup(String groupName, final FABProgressCircle fabProgressCircle) {

        KeyboardUtils.hideKeyboard(this);

        fabProgressCircle.show();

        sDatabaseManager.CreateNewGroup(groupName, mGroupPhotoUri, new DatabaseManager.OnGroupCreatedListener() {
            @Override
            public void onCreated(String groupId) {
                mCreatedGroupId = groupId;
                ((TextView)findViewById(R.id.create_group_id_of_group)).setText(groupId);
                fabProgressCircle.beginFinalAnimation();
            }
        });
    }

    @Override
    public void onFABProgressAnimationEnd() {
        Snackbar.make(mFABProgressCircle, R.string.group_created_seccussfully, Snackbar.LENGTH_LONG).show();
        mGroupIdLayout.setVisibility(View.VISIBLE);
        StartSmartAnimation.startAnimation(mGroupIdLayout, AnimationType.FadeInUp, 400, 0, false);

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGroupIdToolTip.show();
            }
        }, 600);
    }

    public void ShareGroupIdClickBtn(View view) {
        String idMessage = mCreatedGroupId + getString(R.string.group_id_share_message);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, idMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "id_message");
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.group_id_instructions_short)));
    }

    private void goToLoginPage() {
        startActivity(new Intent(CreateNewGroupActivity.this, LoginActivity.class));
        finish();
    }
}
