package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class SearchActivity extends AppCompatActivity implements SensorEventListener
{
    ActiveLocationService activeLocationService;

    String IP;

    URL url;

    // Tracks whether the location service is bound
    private boolean mBound = false;

    private Location mLocation;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    TextView tvHeading;

    ImageView arrowV;

    // This thread updates the location every few seconds
    Thread locationUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        IP = getResources().getString(R.string.IP) + "getdrops?";

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

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arrowV = (ImageView)findViewById(R.id.arrowView);
        arrowV.setImageResource(R.drawable.arrow);
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

    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
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

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor == mAccelerometer)
        {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer)
        {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet)
        {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            mCurrentDegree = -azimuthInDegress;
        }
        tvHeading.setText("Heading: " + Float.toString(mCurrentDegree) + " degrees");

        arrowV.setRotation(mCurrentDegree);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    // This AsyncTask takes in a latitude and longitude and sends them to the server
    // The server returns a list of nearby drops
    private class ServerTask extends AsyncTask<Double, Void, Drop[]>
    {
        @Override
        protected Drop[] doInBackground(Double... params)
        {
            Drop[] drops = new Drop[0];

            String urlString = "uninitialized";
            try
            {
                urlString = IP + "latitude=" + params[0] + "&longitude=";

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

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                {
                    String currentLine = null;
                    while ((currentLine = reader.readLine()) != null)
                    {
                        //Reads in a huge string from the server and parses drops from it
                        //Format of data from the server:

                        // success
                        // num drops
                        // for : drop:
                        //     lat
                        //     lon
                        //     numLines of message
                        //     message
                        //     */
                        if(currentLine.equals("Success"))
                        {
                            int numDrops = Integer.parseInt(reader.readLine());

                            drops = new Drop[numDrops];

                            double lat;
                            double lon;
                            int numLines;
                            String message;

                            for(int i = 0; i < numDrops; i++)
                            {
                                lat = Double.parseDouble(reader.readLine());
                                lon = Double.parseDouble(reader.readLine());

                                numLines = Integer.parseInt(reader.readLine());
                                message = "";

                                for(int j = 0; j < numLines; j++)
                                {
                                    message += reader.readLine();
                                }

                                Drop drop = new Drop(lat, lon, message);
                                drops[i] = drop;
                            }
                        }
                    }
                }

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            Log.i("Active Location Service", "Pulling drops from server!");

            return drops;
        }

        @Override
        protected void onPostExecute(Drop[] drops)
        {
            super.onPostExecute(drops);
        }
    }
}