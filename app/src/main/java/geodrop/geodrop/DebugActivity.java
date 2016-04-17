package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DebugActivity extends AppCompatActivity {
    ActiveLocationService activeLocationService;

    URL url;

    // Tracks whether the location service is bound
    private boolean mBound = false;

    private Location mCurrentLocation;

    // This thread updates the location every few seconds
    Thread locationUpdater;

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    protected TextView mLatitudeLabelText;
    protected TextView mLongitudeLabelText;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text_debug));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text_debug));

        mLatitudeLabelText = (TextView) findViewById((R.id.latitude_label_text));
        mLongitudeLabelText = (TextView) findViewById((R.id.longitude_label_text));

        mLatitudeLabelText.setText(mLatitudeLabel);
        mLongitudeLabelText.setText(mLongitudeLabel);

        try {
            url = new URL("http://172.16.100.110:8080/geodrop-server/api/");
        } catch (MalformedURLException e) {
            Log.i("DebugActivity", "Invalid URL!");
        }

        locationUpdater = new Thread() {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (mBound)
                        {
                            // Update the location and the UI
                            // UI update must be done with handler.post
                            mainHandler.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    mCurrentLocation = activeLocationService.getLocation();
                                    updateUI();
                                }
                            });
                        }

                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                }
            }
        };
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();

        // Launch a location service and bind to it
        Intent intent = new Intent(this, ActiveLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (!locationUpdater.isAlive()) {
            locationUpdater.start();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Debug Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://geodrop.geodrop/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Debug Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://geodrop.geodrop/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);

        // Unbind from the location service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        locationUpdater.interrupt();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    private void updateUI() {
        if (mCurrentLocation != null) {
            mLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
        }
    }

    // Used to communicate with the ActiveLocationService
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ActiveLocationService.LocalBinder binder = (ActiveLocationService.LocalBinder) service;
            activeLocationService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void startListening(View view)
    {
        Intent intent = new Intent(this, PassiveLocationService.class);
        startService(intent);
    }

    public void stopListening(View view)
    {

    }
}
