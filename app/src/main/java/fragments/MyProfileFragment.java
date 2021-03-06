package fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import gis.hereim.R;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;
import static activities.MainActivity.sCurrentFirebaseUser;

public class MyProfileFragment extends Fragment {

    private Context mContext;

    private CircleImageView mUserProfilePhoto;

    private TextView mUserName;
    private TextView mUserEmail;
    private TextView mUserStatus;

    public MyProfileFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_my_profile, container, false);

        mUserProfilePhoto = fragmentView.findViewById(R.id.profile_fragment_photo);
        mUserName = fragmentView.findViewById(R.id.profile_fragment_full_name);
        mUserEmail = fragmentView.findViewById(R.id.profile_fragment_email);
        mUserStatus = fragmentView.findViewById(R.id.profile_fragment_status);

        fragmentView.findViewById(R.id.profile_fragment_change_photo_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePhotoBtnClick();
            }
        });

        loadUserDetails();

        return fragmentView;
    }

    private void loadUserDetails() {
        Picasso.get().load(sCurrentFirebaseUser.getUserPhotoUrl())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.img_blank_profile).into(mUserProfilePhoto, new Callback() {
            @Override
            public void onSuccess() { }

            @Override
            public void onError(Exception e) {
                Picasso.get().load(sCurrentFirebaseUser.getUserPhotoUrl())
                        .placeholder(R.drawable.img_blank_profile).into(mUserProfilePhoto);
            }
        });

        mUserName.setText(sCurrentFirebaseUser.getFullName());
        mUserEmail.setText(sCurrentFirebaseUser.getEmailAddress());
        mUserStatus.setText(sCurrentFirebaseUser.getUserStatus());
    }

    private void changePhotoBtnClick() {
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

                mUserProfilePhoto.setImageURI(result.getUri());

                try {
                    File userImageFile = new Compressor(mContext)
                            .setQuality(40)
                            .compressToFile(new File(Objects.requireNonNull(result.getUri().getPath())));

                    sCurrentFirebaseUser.updatePhoto(Uri.fromFile(userImageFile));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
