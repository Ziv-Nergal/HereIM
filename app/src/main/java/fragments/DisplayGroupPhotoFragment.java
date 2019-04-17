package fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import activities.GroupChatInfoActivity;
import database_classes.GroupChat;
import firebase_utils.DatabaseManager;
import gis.hereim.R;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;
import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sDatabaseManager;

public class DisplayGroupPhotoFragment extends DialogFragment implements View.OnClickListener {

    private Context mContext;

    private ImageView mGroupPhoto;

    private GroupChat mDisplayedGroup;

    public DisplayGroupPhotoFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_display_group_photo, container, false);

        mGroupPhoto = fragmentView.findViewById(R.id.display_group_photo_image_view);

        if(getArguments() != null){
            mDisplayedGroup = (GroupChat) getArguments().getSerializable(GROUP_CHAT_INTENT_EXTRA_KEY);
            Picasso.get().load(mDisplayedGroup.getGroupPhoto()).placeholder(R.drawable.img_blank_group_chat).into(mGroupPhoto);
        }

        fragmentView.findViewById(R.id.display_group_photo_info_btn).setOnClickListener(this);
        fragmentView.findViewById(R.id.display_group_photo_edit_btn).setOnClickListener(this);

        return fragmentView;
    }

    public static DisplayGroupPhotoFragment newInstance(GroupChat groupChat) {
        Bundle args = new Bundle();
        args.putSerializable(GROUP_CHAT_INTENT_EXTRA_KEY, groupChat);
        DisplayGroupPhotoFragment fragment = new DisplayGroupPhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.display_group_photo_info_btn:
                goToGroupInfoActivity();
                break;
            case R.id.display_group_photo_edit_btn:
                changeGroupPhotoBtnClick();
                break;
        }
    }

    private void goToGroupInfoActivity() {
        Intent chatIntent = new Intent(mContext, GroupChatInfoActivity.class);
        chatIntent.putExtra(GROUP_CHAT_INTENT_EXTRA_KEY, mDisplayedGroup);
        startActivity(chatIntent);
    }

    private void changeGroupPhotoBtnClick() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setAspectRatio(1, 1)
                .start(mContext, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                mGroupPhoto.setImageURI(result.getUri());

                try {
                    File userImageFile = new Compressor(mContext)
                            .setQuality(80)
                            .compressToFile(new File(Objects.requireNonNull(result.getUri().getPath())));

                    sDatabaseManager.uploadGroupPhoto(Uri.fromFile(userImageFile), mDisplayedGroup.getGroupId(), new DatabaseManager.GroupPhotoUploadedCallback() {
                        @Override
                        public void onPhotoUploaded() {

                        }
                    });
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
