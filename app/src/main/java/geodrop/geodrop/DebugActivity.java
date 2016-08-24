package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DebugActivity extends AppCompatActivity {
    ActiveLocationService activeLocationService;

    String IP;
    URL url;

    // Tracks whether the location service is bound
    private boolean bound = false;

    private Location currentLocation;

    // This thread updates the location every few seconds
    Thread locationUpdater;

    protected String latitudeLabel;
    protected String longitudeLabel;
    protected TextView latitudeText;
    protected TextView longitudeText;

    protected TextView latitudeLabelText;
    protected TextView longitudeLabelText;

    protected TextView serverStatusText;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        latitudeLabel = getResources().getString(R.string.latitude_label);
        longitudeLabel = getResources().getString(R.string.longitude_label);
        latitudeText = (TextView) findViewById((R.id.latitude_text_debug));
        longitudeText = (TextView) findViewById((R.id.longitude_text_debug));

        latitudeLabelText = (TextView) findViewById((R.id.latitude_label_text));
        longitudeLabelText = (TextView) findViewById((R.id.longitude_label_text));

        serverStatusText = (TextView) findViewById((R.id.server_status_text));

        latitudeLabelText.setText(latitudeLabel);
        longitudeLabelText.setText(longitudeLabel);

        IP = getResources().getString(R.string.IP);

        try {
            url = new URL(IP);
        } catch (MalformedURLException e) {
            Log.i("DebugActivity", "Invalid URL!");
        }

        locationUpdater = new Thread() {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (bound)
                        {
                            // Update the location and the UI
                            // UI update must be done with handler.post
                            mainHandler.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    currentLocation = activeLocationService.getLocation();
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
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

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
        if (bound) {
            unbindService(connection);
            bound = false;
        }

        locationUpdater.interrupt();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    private void updateUI() {
        if (currentLocation != null) {
            latitudeText.setText(String.valueOf(currentLocation.getLatitude()));
            longitudeText.setText(String.valueOf(currentLocation.getLongitude()));
        }
    }

    // Used to communicate with the ActiveLocationService
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ActiveLocationService.LocalBinder binder = (ActiveLocationService.LocalBinder) service;
            activeLocationService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
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

    public void pingServer(View view)
    {
        new ServerTask().execute();
    }

    // This AsyncTask pings the server to see if it is connected
    private class ServerTask extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
                String urlString = "uninitialized";
                try
                {
                    urlString = IP + "online";

                    url = new URL(urlString);
                }
                catch (MalformedURLException e)
                {
                    Log.i("Active Location Service", "Invalid URL!");
                }

                URLConnection connection = null;
                try
                {
                    connection = url.openConnection();
                    connection.setConnectTimeout(5000);

                    System.out.println("Connected to server!");

                    InputStream is = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr);

                    String currentLine = null;
                    while ((currentLine = reader.readLine()) != null)
                    {
                        if (currentLine.equals("Yes"))
                        {
                            return true;
                        }
                    }
                    reader.close();

                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            return false;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            serverStatusText.setText("Pinging Server...");
        }

        @Override
        protected void onPostExecute(Boolean receivedResponse)
        {
            super.onPostExecute(receivedResponse);

            if(receivedResponse)
            {
                serverStatusText.setText("Server Connected!");
            }
            else
            {
                serverStatusText.setText("No response!");
            }
        }
    }
}
