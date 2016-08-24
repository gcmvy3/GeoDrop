package geodrop.geodrop;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

// LocationService runs in the background and tracks the user's location
public class ActiveLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    final String IP = "http://192.168.111.143:8080/geodrop-server/api/getifnearby?";

    private static URL url;

    // Preferred delay between location updates
    public static final long UPDATE_INTERVAL_IN_MS = 50;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MS =
                                UPDATE_INTERVAL_IN_MS / 2;

    // Used to communicate with the Google servers
    protected GoogleApiClient googleApiClient;

    // Used to request the phone's location from Google's servers
    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    private Location currentLocation;

    private boolean nearbyDrop = false;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        ActiveLocationService getService()
        {
            // Returns an instance of an ActiveLocationService so the application can communicate with it
            return ActiveLocationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.i("Active Location Service", "Background service starting!");

        // Create an instance of GoogleAPIClient to use for location tracking
        if (googleApiClient == null)
        {
            buildGoogleApiClient();
        }

        googleApiClient.connect();

        return binder;
    }

    @Override
    public void onCreate()
    {
        Log.i("Active Location Service", "Creating server thread!");

    }

    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
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
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (currentLocation == null)
            {
                Log.i("Active Location Service", "Error: Could not determine location");
            }

            startLocationUpdates();
        } catch (SecurityException e)
        {
            Log.i("Active Location Service", "Error: Location privilege not granted");
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i("Active Location Service", "Error: Lost connection to Google API");
        googleApiClient.connect(); // Attempts to reconnect to the API
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.i("Active Location Service", "Error: Could not connect to Google API");
    }

    @Override
    public void onLocationChanged(Location location)
    {
        currentLocation = location;

        if(location != null)
        {
            // Ask the server whether there are any nearby drops
            new ServerTask().execute(location.getLatitude(), location.getLongitude());
        }
    }

    protected void startLocationUpdates()
    {
        // Begin listening for changes in position
        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, mLocationRequest, this);
        } catch (SecurityException e)
        {
            Log.i("Active Location Service", "Error: location privileges not granted");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        // Disconnect from the google location servers
        if (googleApiClient.isConnected())
        {
            googleApiClient.disconnect();
        }
    }

    public Location getLocation()
    {
        return currentLocation;
    }

    // This AsyncTask takes in a latitude and longitude and sends them to the server
    // If there is a nearby drop, the server returns true
    private class ServerTask extends AsyncTask<Double, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Double... params)
        {
            if (currentLocation != null)
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
                    connection.setConnectTimeout(5000);

                    InputStream is = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr);

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
                        }

                    reader.close();

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

            if(isNearbyDrop)
            {
            }
        }
    }
}


