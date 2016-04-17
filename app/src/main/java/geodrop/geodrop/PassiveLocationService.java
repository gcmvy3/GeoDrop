package geodrop.geodrop;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

// LocationService runs in the background and tracks the user's location
public class PassiveLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    final static String IP = "http://192.168.111.143:8080/geodrop-server/api/getifnearby?";

    private static URL url;

    // Preferred delay between location updates
    public static final long UPDATE_INTERVAL_IN_MS = 30000;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MS =
                                    UPDATE_INTERVAL_IN_MS / 2;

    // Used to communicate with the Google servers
    protected GoogleApiClient mGoogleApiClient;

    // Used to request the phone's location from Google's servers
    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    private Location mCurrentLocation;

    private boolean hasNotified = false;

    @Override
    public void onCreate()
    {
        // Create an instance of GoogleAPIClient to use for location tracking
        if (mGoogleApiClient == null)
        {
            buildGoogleApiClient();
        }
    }

    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API) // This line lets us use location services
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        //Called when the GoogleApiClient connects
        try
        {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mCurrentLocation == null)
            {
                Toast.makeText(this, "Error: could not determine location", Toast.LENGTH_SHORT).show();
            }

            startLocationUpdates();
        }
        catch(SecurityException e)
        {
            Toast.makeText(this, "Error: location privileges not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Toast.makeText(this, "Error: lost connection to Google API", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect(); // Attempts to reconnect to the API
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Toast.makeText(this, "Error: could not connect to Google API", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mCurrentLocation = location;

        if(location != null)
        {
            System.out.println("Location Changed!");
            // Ask the server whether there are any nearby drops
            new ServerTask().execute(location.getLatitude(), location.getLongitude());
        }
    }

    protected void startLocationUpdates()
    {
        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        catch(SecurityException e)
        {
            Toast.makeText(this, "Error: location privileges not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "Background service starting", Toast.LENGTH_SHORT).show();

        mGoogleApiClient.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        // Disconnect from the google location servers
        if(mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        // Not needed, since this is not a bound service
        return null;
    }

    public void createNotification()
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notif) //TODO: notification icon
                        .setAutoCancel(true)
                        .setContentTitle("Nearby Drop")
                        .setPriority(1)  //High priority
                        .setDefaults(-1) //Allow lights, sound, and vibration
                        .setVibrate(new long[]{0, 500, 110, 500, 110, 450, 110, 200, 110, 170, 40, 450, 110, 200, 110, 170, 40, 500})
                        .setContentText("You're close to a drop! Tap to see it.");

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Build and display the notification
        // mId allows you to update the notification later on.
        int mId = 28;
        mNotificationManager.notify(mId, mBuilder.build());
    }

    // This AsyncTask takes in a latitude and longitude and sends them to the server
    // If there is a nearby drop, the server returns true
    private class ServerTask extends AsyncTask<Double, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Double... params)
        {
            if (mCurrentLocation != null)
            {
                String urlString = "uninitialized";
                try
                {
                    urlString = IP + "latitude=" + params[0] + "&" + "longitude=" + params[1];

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

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    {
                        String currentLine = null;
                        while ((currentLine = reader.readLine()) != null)
                        {
                            if (currentLine.equals("true"))
                            {
                                return true;
                            }
                            else if (currentLine.equals("false"))
                            {
                                return false;
                            }
                            else
                            {
                                System.out.println("Server Error");
                            }
                        }
                    }

                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                Log.i("Active Location Service", "Sending data to server!");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isNearbyDrop)
        {
            super.onPostExecute(isNearbyDrop);

            // If there is a nearby drop, show a notification
            if(isNearbyDrop && !hasNotified)
            {
               createNotification();
                hasNotified = true;
            }
        }
    }
}
