package geodrop.geodrop;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
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

import java.util.Random;

// LocationService runs in the background and tracks the user's location
public class ActiveLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    // Preferred delay between location updates
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Used to communicate with the Google servers
    protected GoogleApiClient mGoogleApiClient;

    // Used to request the phone's location from Google's servers
    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    private Location mCurrentLocation;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

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
        if (mGoogleApiClient == null)
        {
            buildGoogleApiClient();
        }

        mGoogleApiClient.connect();

        return mBinder;
    }

    @Override
    public void onCreate()
    {
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
                Log.i("Active Location Service", "Error: Could not determine location");
            }

            startLocationUpdates();
        }
        catch(SecurityException e)
        {
            Log.i("Active Location Service", "Error: Location privilege not granted");
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i("Active Location Service", "Error: Lost connection to Google API");
        mGoogleApiClient.connect(); // Attempts to reconnect to the API
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.i("Active Location Service", "Error: Could not connect to Google API");
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mCurrentLocation = location;
        Log.i("Active Location Service", "Location changed!");
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
            Log.i("Active Location Service", "Error: location privileges not granted");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
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

    public Location getLocation()
    {
        return mCurrentLocation;
    }
}
