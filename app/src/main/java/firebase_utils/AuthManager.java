package firebase_utils;

import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class AuthManager {

    public static void ValidateInputsNotEmpty(EditText[] iInputs) throws Exception{

        boolean allInputsOk = true;

        for (EditText input : iInputs) {
            if(input.getText().toString().trim().isEmpty()){
                input.setError(input.getTag() == null ? "Input Required" : input.getTag().toString() + " Required");
                allInputsOk = false;
            }
        }

        if(!allInputsOk){
            throw new Exception("Please fill all values!");
        }
    }

    public static String GetErrorMessage(Exception exception){

        String errorMsg = null;

        if (exception instanceof FirebaseAuthInvalidCredentialsException) {

            String errorCode = ((FirebaseAuthInvalidCredentialsException) exception).getErrorCode();

            switch (errorCode) {
                case "ERROR_INVALID_EMAIL": errorMsg = "Invalid email address"; break;
                case "ERROR_WEAK_PASSWORD": errorMsg = "Password should be at least 6 characters"; break;
                default: errorMsg = "Wrong password"; break;
            }
        } else if (exception instanceof FirebaseAuthInvalidUserException) {

            String errorCode = ((FirebaseAuthInvalidUserException) exception).getErrorCode();

            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND": errorMsg = "No matching account found"; break;
                case "ERROR_USER_DISABLED": errorMsg = "FirebaseUser account has been disabled"; break;
                default: errorMsg = exception.getLocalizedMessage();break;
            }
        }else if (exception instanceof FirebaseAuthUserCollisionException){
            errorMsg = "Email is already in use by another account";
        }

        return errorMsg;
    }
}
