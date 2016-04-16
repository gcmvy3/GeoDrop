package geodrop.geodrop;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
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

// LocationService runs in the background and tracks the user's location
public class PassiveLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    // Preferred delay between location updates
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 50000;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Used to communicate with the Google servers
    protected GoogleApiClient mGoogleApiClient;

    // Used to request the phone's location from Google's servers
    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    private Location mCurrentLocation;

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
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
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

        return super.onStartCommand(intent,flags,startId);
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
}
