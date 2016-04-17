package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class DebugActivity extends AppCompatActivity
{
    ActiveLocationService activeLocationService;

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

        locationUpdater = new Thread()
        {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            public void run()
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        if(mBound)
                        {
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
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        };
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Launch a location service and bind to it
        Intent intent = new Intent(this, ActiveLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if(!locationUpdater.isAlive())
        {
            locationUpdater.start();
        }
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

        locationUpdater.interrupt();
    }

    private void updateUI()
    {
        if(mCurrentLocation != null)
        {
            mLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
        }
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
