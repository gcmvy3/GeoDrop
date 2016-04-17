package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SearchActivity extends AppCompatActivity
{
    ActiveLocationService activeLocationService;

    // Tracks whether the location service is bound
    private boolean mBound = false;

    private Location mLocation;

    // This thread updates the location every few seconds
    Thread locationUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        locationUpdater = new Thread()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    if (mBound)
                    {
                        mLocation = activeLocationService.getLocation();
                    }

                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        };
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Launch a location service and bind to it
        Intent intent = new Intent(this, ActiveLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        locationUpdater.start();
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
