package activities;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
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

import gis.hereim.R;
import firebase_utils.AuthManager;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private EditText mEmailET;
    private EditText mPasswordET;

    private ProgressBar mProgressBar;

    private Handler mDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailET = findViewById(R.id.login_email_edit_Text);
        mPasswordET = findViewById(R.id.login_password_edit_Text);
        mProgressBar = findViewById(R.id.login_progressbar);
    }

    public void LoginBtnClick(View view) {

        String email = mEmailET.getText().toString().trim();
        String password = mPasswordET.getText().toString().trim();

        try{
            AuthManager.ValidateInputsNotEmpty(new EditText[]{mEmailET, mPasswordET});
            signIn(email, password);
        }catch (Exception ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void signIn(final String email, final String password) {

        mProgressBar.setVisibility(View.VISIBLE);

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, AuthManager.GetErrorMessage(e), Toast.LENGTH_LONG).show();
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, 1500);
    }

    public void SignUpBtnCLick(View view) {
        Intent profileIntent = new Intent(LoginActivity.this, SignUpActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(LoginActivity.this,
                        findViewById(R.id.login_btn),
                        getString(R.string.login_sign_up_trans));
        startActivity(profileIntent, options.toBundle());
    }
}
