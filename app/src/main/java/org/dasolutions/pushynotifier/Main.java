package org.dasolutions.pushynotifier;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import me.pushy.sdk.Pushy;
import me.pushy.sdk.util.exceptions.PushyException;

public class Main extends AppCompatActivity {
    TextView mInstructions;
    TextView mDeviceToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load main.xml layout
        setContentView(R.layout.main);

        // Cache TextView objects
        mDeviceToken = (TextView) findViewById(R.id.deviceToken);
        mInstructions = (TextView) findViewById(R.id.instructions);

        // Restart the socket service, in case the user force-closed the app
        Pushy.listen(this);

        // Register device for push notifications (async)
        new RegisterForPushNotificationsAsync().execute();
    }

    private class RegisterForPushNotificationsAsync extends AsyncTask<String, Void, RegistrationResult> {
        ProgressDialog mLoading;

        public RegisterForPushNotificationsAsync() {
            // Create progress dialog and set it up
            mLoading = new ProgressDialog(Main.this);
            mLoading.setMessage(getString(R.string.registeringDevice));
            mLoading.setCancelable(false);

            // Show it
            mLoading.show();
        }

        @Override
        protected RegistrationResult doInBackground(String... params) {
            // Prepare registration result
            RegistrationResult result = new RegistrationResult();

            try {
                // Register device for push notifications
                result.deviceToken = Pushy.register(Main.this);
            }
            catch (PushyException exc) {
                // Store registration error in result
                result.error = exc;
            }

            // Handle result in onPostExecute (UI thread)
            return result;
        }

        @Override
        protected void onPostExecute(RegistrationResult result) {
            // Activity died?
            if (isFinishing()) {
                return;
            }

            // Hide progress bar
            mLoading.dismiss();

            // Registration failed?
            if (result.error != null) {
                // Write error to logcat
                Log.e("Pushy", "Registration failed: " + result.error.getMessage());

                // Display registration failed in app UI
                mInstructions.setText(R.string.restartApp);
                mDeviceToken.setText(R.string.registrationFailed);

                // Display error dialog
                new AlertDialog.Builder(Main.this).setTitle(R.string.registrationError)
                        .setMessage(result.error.getMessage())
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show();
            }
            else {
                // Write device token to logcat
                Log.d("Pushy", "Device token: " + result.deviceToken);

                // Display device token and copy from logcat instructions
                mInstructions.setText(R.string.copyLogcat);
                mDeviceToken.setText(result.deviceToken);
            }
        }
    }
}
