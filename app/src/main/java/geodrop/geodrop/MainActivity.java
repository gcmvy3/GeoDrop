package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
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

public class MainActivity extends AppCompatActivity
{
    protected static final String TAG = "MainActivity";

    ActiveLocationService activeLocationService;

    // For displaying the user's location on the screen
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    // Tracks whether the location service is bound
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Launch a location service and bind to it
        Intent intent = new Intent(this, ActiveLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // Unbind from the location service
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void openDebug(View view) {
        Intent intent = new Intent(this, DebugActivity.class);
        startActivity(intent);
    }

    // Used to communicate with the ActiveLocationService
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ActiveLocationService.LocalBinder binder = (ActiveLocationService.LocalBinder) service;
            activeLocationService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mBound = false;
        }
    };
}
