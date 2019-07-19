package activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gis.hereim.R;
import firebase_utils.AuthManager;

public class SignUpActivity extends AppCompatActivity {

    public static final Uri DEFAULT_PHOTO_URI = Uri.parse("android.resource://gis.hereim/drawable/img_blank_profile");

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private EditText mEmailET;
    private EditText mFullNameET;
    private EditText mPasswordET;
    private EditText mConfirmPassET;

    private ProgressBar mProgressBar;

    private final Handler mDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mEmailET = findViewById(R.id.sign_up_email_edit_Text);
        mFullNameET = findViewById(R.id.sign_up_full_name_edit_text);
        mPasswordET = findViewById(R.id.sign_up_password_edit_text);
        mConfirmPassET = findViewById(R.id.sign_up_confirm_password_edit_text);
        mProgressBar = findViewById(R.id.sign_up_progressbar);
    }

    public void signUpBtnClick(View view) {

        String email = mEmailET.getText().toString().trim();
        String fullName = mFullNameET.getText().toString().trim();
        String password = mPasswordET.getText().toString().trim();
        String confirmPassword = mConfirmPassET.getText().toString().trim();

        try{
            AuthManager.ValidateInputsNotEmpty(new EditText[]{mFullNameET, mEmailET, mPasswordET, mConfirmPassET});
            validateMatchingPasswords(password, confirmPassword);
            createNewUser(email, fullName, password);
        }catch (Exception ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void validateMatchingPasswords(String password, String confirmPassword) throws Exception{
        if(!password.equals(confirmPassword)){
            throw new Exception("Passwords do not match!");
        }
    }

    private void createNewUser(final String email, final String fullName, final String password) {

        mProgressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        insertNewUserToDataBase(email, fullName);
                        if(task.isSuccessful()) {
                            mDelayHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    goToMainActivity();
                                }
                            }, 1500);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(SignUpActivity.this, AuthManager.GetErrorMessage(e), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void insertNewUserToDataBase(String email, String fullName) {

        if(mAuth.getCurrentUser() != null){

            mAuth.getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(fullName).setPhotoUri(DEFAULT_PHOTO_URI).build());

            final DatabaseReference currentUserRef =  FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

            Map<String, Object> userDetailsList = new HashMap<>();
            userDetailsList.put("uid", mAuth.getCurrentUser().getUid());
            userDetailsList.put("fullName", fullName);
            userDetailsList.put("email", email);
            userDetailsList.put("photoUri", DEFAULT_PHOTO_URI.toString());
            userDetailsList.put("status", getString(R.string.default_status));
            userDetailsList.put("deviceToken", Objects.requireNonNull(FirebaseInstanceId.getInstance().getToken()));

            currentUserRef.updateChildren(userDetailsList).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    currentUserRef.child("online").setValue(true);
                }
            });
        }
    }

    private void goToMainActivity() {
        Intent mainActivityIntent = new Intent(SignUpActivity.this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();
    }

    public void loginBtnClick(View view) {
        onBackPressed();
    }
}
