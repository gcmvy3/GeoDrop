package geodrop.geodrop;

import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    // Preferred delay between location updates
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 50000;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    protected static final String TAG = "MainActivity";

    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    public static Location mCurrentLocation;

    // For displaying the user's location on the screen
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    protected void onStart()
    {
        super.onStart();

        // Connect to the Google servers for location tracking
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // Disconnect from the google location servers
        if(mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
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
                Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            }

            startLocationUpdates();
        }
        catch(SecurityException e)
        {
            Log.i(TAG, "User did not grant location permission");
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG, "Connection suspended, trying to reconnect");
        mGoogleApiClient.connect(); // Attempts to reconnect to the API
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.i(TAG, "Connection to Google API failed!");
    }

    @Override
    public void onLocationChanged(Location location)
    {
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

        }
    }

    public void openDebug(View view) {
        Intent intent = new Intent(this, DebugActivity.class);
        startActivity(intent);
    }
}
