package geodrop.geodrop;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    // Minimum delay between location updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    protected static final String TAG = "MainActivity";

    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;

    // The most recent location of the user's phone
    private Location mCurrentLocation;

    // For displaying the user's location on the screen
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    protected TextView mLatitudeLabelText;
    protected TextView mLongitudeLabelText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));

        mLatitudeLabelText = (TextView) findViewById((R.id.latitude_label_text));
        mLongitudeLabelText = (TextView) findViewById((R.id.longitude_label_text));

        mLatitudeLabelText.setText(mLatitudeLabel);
        mLongitudeLabelText.setText(mLongitudeLabel);

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
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                            builder.build());
        }
        catch(SecurityException e)
        {
            Log.i(TAG, "Location security not granted");
        }
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
        try
        {
            //Called when the GoogleApiClient connects
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null)
            {
                mLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
                mLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
            }

            if (mCurrentLocation != null)
            {
                // Update the screen to reflect the location
                mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
                        mCurrentLocation.getLatitude()));
                mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
                        mCurrentLocation.getLongitude()));
            }
            else
            {
                Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            }
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
}
